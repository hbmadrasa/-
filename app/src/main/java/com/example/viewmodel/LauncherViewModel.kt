package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeviceControlManager
import com.example.data.LauncherPreferencesRepository
import com.example.model.AppSlotConfig
import com.example.model.InstalledAppInfo
import com.example.model.LauncherSettings
import com.example.model.NetworkStatus
import com.example.model.ResolvedHomeSlot
import com.example.model.RingerVibrationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val resolvedSlots: List<ResolvedHomeSlot> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val networkStatus: NetworkStatus = NetworkStatus(),
    val batteryPercent: Int = 100,
    val isDefaultLauncher: Boolean = false,
    val missingAppSlot: ResolvedHomeSlot? = null,
    val dataGuardPendingSlot: ResolvedHomeSlot? = null,
    val tapConfirmPendingSlot: ResolvedHomeSlot? = null
)

data class AdminPanelState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val ringerVibrationState: RingerVibrationState = RingerVibrationState(),
    val isDefaultLauncher: Boolean = false,
    val isDeviceOwner: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val statusMessage: String? = null
)

data class PinDialogState(
    val isVisible: Boolean = false,
    val enteredPin: String = "",
    val errorMessage: String? = null,
    val lockoutRemainingSeconds: Long = 0L
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherPreferencesRepository(application)
    val deviceControl = DeviceControlManager(application)

    private val _resolvedSlots = MutableStateFlow<List<ResolvedHomeSlot>>(emptyList())
    private val _batteryPercent = MutableStateFlow(deviceControl.getBatteryPercentage())
    private val _isDefaultLauncher = MutableStateFlow(deviceControl.isDefaultLauncher())
    private val _missingAppSlot = MutableStateFlow<ResolvedHomeSlot?>(null)
    private val _dataGuardPendingSlot = MutableStateFlow<ResolvedHomeSlot?>(null)
    private val _tapConfirmPendingSlot = MutableStateFlow<ResolvedHomeSlot?>(null)

    private val _adminState = MutableStateFlow(AdminPanelState())
    val adminState: StateFlow<AdminPanelState> = _adminState

    private val _pinDialogState = MutableStateFlow(PinDialogState())
    val pinDialogState: StateFlow<PinDialogState> = _pinDialogState

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated

    private var lockoutTimerJob: Job? = null

    val homeUiState: StateFlow<HomeUiState> = combine(
        repository.settingsFlow,
        deviceControl.observeNetworkStatus(),
        _resolvedSlots,
        _batteryPercent,
        _isDefaultLauncher
    ) { settings, network, resolved, battery, isDefault ->
        HomeUiState(
            resolvedSlots = resolved.ifEmpty { deviceControl.resolveHomeSlots(settings.slots) },
            settings = settings,
            networkStatus = network,
            batteryPercent = battery,
            isDefaultLauncher = isDefault,
            missingAppSlot = _missingAppSlot.value,
            dataGuardPendingSlot = _dataGuardPendingSlot.value,
            tapConfirmPendingSlot = _tapConfirmPendingSlot.value
        )
    }.combine(_missingAppSlot) { state, missing ->
        state.copy(missingAppSlot = missing)
    }.combine(_dataGuardPendingSlot) { state, dataGuard ->
        state.copy(dataGuardPendingSlot = dataGuard)
    }.combine(_tapConfirmPendingSlot) { state, tapConfirm ->
        state.copy(tapConfirmPendingSlot = tapConfirm)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                val resolved = withContext(Dispatchers.IO) {
                    deviceControl.resolveHomeSlots(settings.slots)
                }
                _resolvedSlots.value = resolved
            }
        }
        viewModelScope.launch {
            repository.lockoutUntilFlow.collect { lockoutUntil ->
                val remaining = (lockoutUntil - System.currentTimeMillis()) / 1000L
                if (remaining > 0L) {
                    startLockoutCountdown(remaining)
                }
            }
        }
        refreshDeviceStatus()
    }

    fun refreshDeviceStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val isDefault = deviceControl.isDefaultLauncher()
            val battery = deviceControl.getBatteryPercentage()
            val ringer = deviceControl.getRingerVibrationState()
            val isOwner = deviceControl.isDeviceOwner()
            val isAdmin = deviceControl.isDeviceAdminActive()
            val resolved = deviceControl.resolveHomeSlots(homeUiState.value.settings.slots)

            _isDefaultLauncher.value = isDefault
            _batteryPercent.value = battery
            _resolvedSlots.value = resolved
            _adminState.value = _adminState.value.copy(
                ringerVibrationState = ringer,
                isDefaultLauncher = isDefault,
                isDeviceOwner = isOwner,
                isDeviceAdminActive = isAdmin
            )
        }
    }

    fun onHomeTileClicked(slot: ResolvedHomeSlot) {
        val settings = homeUiState.value.settings
        if (settings.hapticVibrationEnabled) {
            deviceControl.triggerHapticPulse(45L)
        }

        if (!slot.config.enabled) return

        if (!slot.isInstalled) {
            _missingAppSlot.value = slot
            return
        }

        val network = homeUiState.value.networkStatus
        val onMeteredMobileData = !network.isWifiConnected && (network.isMobileDataActive || network.isMetered)

        if (settings.mobileDataGuardEnabled && slot.config.wifiOnlyGuard && onMeteredMobileData) {
            _dataGuardPendingSlot.value = slot
            return
        }

        if (settings.tapConfirmationEnabled) {
            _tapConfirmPendingSlot.value = slot
            return
        }

        launchResolvedSlot(slot)
    }

    fun confirmTapAndLaunch() {
        val slot = _tapConfirmPendingSlot.value ?: return
        _tapConfirmPendingSlot.value = null
        launchResolvedSlot(slot)
    }

    fun dismissTapConfirm() {
        _tapConfirmPendingSlot.value = null
    }

    fun proceedDataGuardAndLaunch() {
        val slot = _dataGuardPendingSlot.value ?: return
        _dataGuardPendingSlot.value = null
        if (!homeUiState.value.settings.strictMobileDataBlock) {
            launchResolvedSlot(slot)
        }
    }

    fun dismissDataGuardDialog() {
        _dataGuardPendingSlot.value = null
    }

    fun dismissMissingAppDialog() {
        _missingAppSlot.value = null
    }

    private fun launchResolvedSlot(slot: ResolvedHomeSlot) {
        val launched = deviceControl.launchSlot(slot)
        if (!launched) {
            _missingAppSlot.value = slot
        }
    }

    fun openPinDialog() {
        if (homeUiState.value.settings.hapticVibrationEnabled) {
            deviceControl.triggerHapticPulse(60L)
        }
        _pinDialogState.value = _pinDialogState.value.copy(
            isVisible = true,
            enteredPin = "",
            errorMessage = null
        )
    }

    fun closePinDialog() {
        _pinDialogState.value = _pinDialogState.value.copy(
            isVisible = false,
            enteredPin = "",
            errorMessage = null
        )
    }

    fun onPinDigitEntered(digit: Char, onSuccess: () -> Unit) {
        val current = _pinDialogState.value
        if (current.lockoutRemainingSeconds > 0L) return
        if (current.enteredPin.length >= 6) return

        val updated = current.enteredPin + digit
        _pinDialogState.value = current.copy(enteredPin = updated, errorMessage = null)

        if (updated.length == 4) {
            verifyPinInternal(updated, autoSubmit = true, onSuccess = onSuccess)
        }
    }

    fun onPinBackspace() {
        val current = _pinDialogState.value
        if (current.enteredPin.isNotEmpty()) {
            _pinDialogState.value = current.copy(
                enteredPin = current.enteredPin.dropLast(1),
                errorMessage = null
            )
        }
    }

    fun submitPin(onSuccess: () -> Unit) {
        val pin = _pinDialogState.value.enteredPin
        if (pin.length < 4) {
            _pinDialogState.value = _pinDialogState.value.copy(
                errorMessage = "কমপক্ষে ৪ সংখ্যার PIN দিন"
            )
            return
        }
        verifyPinInternal(pin, autoSubmit = false, onSuccess = onSuccess)
    }

    private fun verifyPinInternal(pin: String, autoSubmit: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = repository.verifyAdminPin(pin)) {
                is LauncherPreferencesRepository.PinVerifyResult.Success -> {
                    _isAdminAuthenticated.value = true
                    _pinDialogState.value = PinDialogState(isVisible = false)
                    loadInstalledAppsForAdmin()
                    refreshDeviceStatus()
                    onSuccess()
                }

                is LauncherPreferencesRepository.PinVerifyResult.Invalid -> {
                    if (!autoSubmit || pin.length >= 4) {
                        _pinDialogState.value = _pinDialogState.value.copy(
                            enteredPin = "",
                            errorMessage = "ভুল PIN! আর ${result.attemptsRemaining} বার চেষ্টা করতে পারবেন"
                        )
                    }
                }

                is LauncherPreferencesRepository.PinVerifyResult.LockedOut -> {
                    startLockoutCountdown(result.remainingSeconds)
                    _pinDialogState.value = _pinDialogState.value.copy(
                        enteredPin = "",
                        errorMessage = "অনেকবার ভুল PIN দেওয়া হয়েছে। অপেক্ষা করুন।"
                    )
                }
            }
        }
    }

    private fun startLockoutCountdown(seconds: Long) {
        lockoutTimerJob?.cancel()
        lockoutTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _pinDialogState.value = _pinDialogState.value.copy(
                    lockoutRemainingSeconds = remaining
                )
                delay(1000L)
                remaining--
            }
            _pinDialogState.value = _pinDialogState.value.copy(
                lockoutRemainingSeconds = 0L,
                errorMessage = null
            )
        }
    }

    fun lockAdminSession() {
        _isAdminAuthenticated.value = false
    }

    fun loadInstalledAppsForAdmin() {
        if (_adminState.value.isLoadingApps) return
        _adminState.value = _adminState.value.copy(isLoadingApps = true)
        viewModelScope.launch(Dispatchers.IO) {
            val apps = deviceControl.queryInstalledLaunchableApps()
            _adminState.value = _adminState.value.copy(
                installedApps = apps,
                isLoadingApps = false
            )
        }
    }

    fun updateSlot(slot: AppSlotConfig) {
        viewModelScope.launch {
            repository.updateSlot(slot)
            showAdminMessage("বাটন ${slot.slotIndex + 1} আপডেট করা হয়েছে")
        }
    }

    fun restoreDefaultSlots() {
        viewModelScope.launch {
            repository.restoreDefaultSlots()
            showAdminMessage("মূল ৪টি অ্যাপ পুনরুদ্ধার করা হয়েছে")
        }
    }

    fun saveAndSuggestWifi(ssid: String, password: String) {
        viewModelScope.launch {
            repository.updatePreferredWifi(ssid, password)
            when (val res = deviceControl.applyPreferredWifiSuggestion(ssid, password)) {
                is DeviceControlManager.WifiSuggestionResult.Applied -> {
                    showAdminMessage("'$ssid' Wi-Fi নেটওয়ার্ক অগ্রাধিকার হিসেবে সেট করা হয়েছে")
                }

                is DeviceControlManager.WifiSuggestionResult.OpenSettingsFallback -> {
                    showAdminMessage("সেটিংস সংরক্ষিত হয়েছে — অনুগ্রহ করে Wi-Fi প্যানেল থেকে সংযোগ নিশ্চিত করুন")
                    deviceControl.openWifiPanel()
                }

                is DeviceControlManager.WifiSuggestionResult.InvalidInput -> {
                    showAdminMessage("সঠিক Wi-Fi নাম (SSID) লিখুন")
                }

                is DeviceControlManager.WifiSuggestionResult.Error -> {
                    showAdminMessage("Wi-Fi পরামর্শ সংরক্ষণে সমস্যা (কোড ${res.code}) — Wi-Fi প্যানেল থেকে যুক্ত করুন")
                }
            }
        }
    }

    fun clearWifiSuggestions() {
        viewModelScope.launch {
            deviceControl.clearWifiSuggestions()
            repository.updatePreferredWifi("", "")
            showAdminMessage("সংরক্ষিত Wi-Fi নেটওয়ার্ক মুছে ফেলা হয়েছে")
        }
    }

    fun setWifiWatchdog(enabled: Boolean) {
        viewModelScope.launch { repository.setWifiWatchdog(enabled) }
    }

    fun setMobileDataGuard(enabled: Boolean) {
        viewModelScope.launch { repository.setMobileDataGuard(enabled) }
    }

    fun setStrictMobileDataBlock(enabled: Boolean) {
        viewModelScope.launch { repository.setStrictMobileDataBlock(enabled) }
    }

    fun setHapticVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHapticVibration(enabled)
            if (enabled) deviceControl.triggerHapticPulse(70L)
        }
    }

    fun setTapConfirmation(enabled: Boolean) {
        viewModelScope.launch { repository.setTapConfirmation(enabled) }
    }

    fun setHighContrastDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setHighContrastDarkMode(enabled) }
    }

    fun setRingerModeVibrate(vibrateOnly: Boolean) {
        val targetMode = if (vibrateOnly) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
        val ok = deviceControl.setRingerMode(targetMode)
        if (!ok) {
            showAdminMessage("Do Not Disturb অনুমতি প্রয়োজন — অনুগ্রহ করে অনুমতি দিন")
            deviceControl.openNotificationPolicySettings()
        } else {
            if (vibrateOnly) deviceControl.triggerHapticPulse(80L)
            showAdminMessage(if (vibrateOnly) "ফোন ভাইব্রেশন মোডে সেট হয়েছে" else "ফোন সাধারণ রিং মোডে সেট হয়েছে")
        }
        refreshDeviceStatus()
    }

    fun testVibration() {
        deviceControl.triggerHapticPulse(120L)
    }

    fun updateAdminPin(newPin: String) {
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            showAdminMessage("নতুন PIN অবশ্যই ৪ সংখ্যার হতে হবে")
            return
        }
        viewModelScope.launch {
            repository.updateAdminPin(newPin)
            showAdminMessage("অ্যাডমিন PIN সফলভাবে পরিবর্তন হয়েছে")
        }
    }

    fun toggleKioskLockTask(activity: Activity?, enabled: Boolean) {
        viewModelScope.launch {
            if (activity == null) return@launch
            if (enabled) {
                val started = deviceControl.startKioskLockTask(activity)
                repository.setKioskLockTask(started)
                if (started) {
                    showAdminMessage("স্ক্রিন লক / পিনিং মোড চালু হয়েছে")
                } else {
                    showAdminMessage("এই ডিভাইসে স্ক্রিন পিনিং চালু করা যায়নি")
                }
            } else {
                deviceControl.stopKioskLockTask(activity)
                repository.setKioskLockTask(false)
                showAdminMessage("স্ক্রিন লক / পিনিং মোড বন্ধ হয়েছে")
            }
        }
    }

    fun createHomeRoleIntent(): Intent? = deviceControl.createHomeRoleIntent()

    fun openHomeSettings() = deviceControl.openHomeSettings()

    fun openSystemSettings() = deviceControl.openSystemSettings()

    fun openWifiPanel() = deviceControl.openWifiPanel()

    fun openMobileDataSettings() = deviceControl.openMobileDataSettings()

    fun openDataUsageSettings() = deviceControl.openDataUsageSettings()

    fun restoreAllControls(activity: Activity?, openHomeChooserAfter: Boolean) {
        viewModelScope.launch {
            activity?.let { deviceControl.stopKioskLockTask(it) }
            deviceControl.clearWifiSuggestions()
            repository.resetAllPreferences()
            refreshDeviceStatus()
            showAdminMessage("সব নিয়ন্ত্রণ ও সেটিংস পুনরুদ্ধার করা হয়েছে (ডিফল্ট PIN: ১২৩৪)")
            if (openHomeChooserAfter) {
                deviceControl.openHomeSettings()
            }
        }
    }

    fun clearAdminMessage() {
        _adminState.value = _adminState.value.copy(statusMessage = null)
    }

    private fun showAdminMessage(message: String) {
        _adminState.value = _adminState.value.copy(statusMessage = message)
    }
}
