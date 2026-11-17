package com.example.data

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.admin.SohojDeviceAdminReceiver
import com.example.model.AppSlotConfig
import com.example.model.InstalledAppInfo
import com.example.model.NetworkStatus
import com.example.model.ResolvedHomeSlot
import com.example.model.RingerVibrationState
import com.example.model.SlotActionType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DeviceControlManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    fun resolveHomeSlots(slots: List<AppSlotConfig>): List<ResolvedHomeSlot> {
        return slots.map { slot ->
            when (slot.actionType) {
                SlotActionType.PHONE_DIALER -> {
                    val dialIntent = Intent(Intent.ACTION_DIAL)
                    val resolved = packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    val pkg = resolved?.activityInfo?.packageName
                    val icon = pkg?.let { safeAppIcon(it) }
                    ResolvedHomeSlot(
                        config = slot,
                        isInstalled = resolved != null,
                        resolvedPackageName = pkg,
                        appIcon = icon
                    )
                }

                SlotActionType.PACKAGE -> {
                    val primaryLaunch = if (slot.packageName.isNotBlank()) {
                        packageManager.getLaunchIntentForPackage(slot.packageName)
                    } else null
                    val fallbackLaunch = if (primaryLaunch == null && slot.fallbackPackageName.isNotBlank()) {
                        packageManager.getLaunchIntentForPackage(slot.fallbackPackageName)
                    } else null

                    val targetPkg = when {
                        primaryLaunch != null -> slot.packageName
                        fallbackLaunch != null -> slot.fallbackPackageName
                        else -> null
                    }
                    ResolvedHomeSlot(
                        config = slot,
                        isInstalled = targetPkg != null,
                        resolvedPackageName = targetPkg,
                        appIcon = targetPkg?.let { safeAppIcon(it) }
                    )
                }
            }
        }
    }

    fun queryInstalledLaunchableApps(): List<InstalledAppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = packageManager.queryIntentActivities(mainIntent, 0)
        val seenPackages = HashSet<String>()
        val result = mutableListOf<InstalledAppInfo>()

        // Add explicit Phone Dialer entry first
        result.add(
            InstalledAppInfo(
                packageName = "",
                appName = "ফোন ডায়ালার (ডিফল্ট কল)",
                icon = packageManager.resolveActivity(
                    Intent(Intent.ACTION_DIAL),
                    PackageManager.MATCH_DEFAULT_ONLY
                )?.activityInfo?.packageName?.let { safeAppIcon(it) },
                isDialer = true
            )
        )

        for (resolveInfo in activities) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (pkg == context.packageName || !seenPackages.add(pkg)) continue
            val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                .ifBlank { pkg }
            val icon = try {
                resolveInfo.loadIcon(packageManager)
            } catch (_: Exception) {
                null
            }
            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isDialer = false
                )
            )
        }

        val dialer = result.first()
        val sortedApps = result.drop(1).sortedBy { it.appName.lowercase() }
        return listOf(dialer) + sortedApps
    }

    fun launchSlot(slot: ResolvedHomeSlot): Boolean {
        return try {
            val intent = when (slot.config.actionType) {
                SlotActionType.PHONE_DIALER -> Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                SlotActionType.PACKAGE -> {
                    val pkg = slot.resolvedPackageName ?: return false
                    packageManager.getLaunchIntentForPackage(pkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    } ?: return false
                }
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun createHomeRoleIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return null
    }

    fun openHomeSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openSystemSettings() }
    }

    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openMobileDataSettings() {
        val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(primary) }.onFailure {
            val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openDataUsageSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { openMobileDataSettings() }
    }

    fun observeNetworkStatus(): Flow<NetworkStatus> = callbackFlow {
        fun emitCurrent() {
            trySend(readNetworkStatus())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrent()
            override fun onLost(network: Network) = emitCurrent()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = emitCurrent()
        }

        emitCurrent()
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        runCatching { connectivityManager.registerNetworkCallback(request, callback) }

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    @Suppress("DEPRECATION")
    fun readNetworkStatus(): NetworkStatus {
        val isWifiEnabled = runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }

        val isWifiConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMobileDataActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isMetered = caps?.let {
            !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } ?: false

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val ssid = if (isWifiConnected && hasLocationPermission) {
            runCatching {
                val raw = wifiManager.connectionInfo?.ssid
                if (raw.isNullOrBlank() || raw == "<unknown ssid>") null
                else raw.removePrefix("\"").removeSuffix("\"")
            }.getOrNull()
        } else {
            null
        }

        return NetworkStatus(
            isWifiEnabled = isWifiEnabled,
            isWifiConnected = isWifiConnected,
            isMobileDataActive = isMobileDataActive,
            isMetered = isMetered,
            wifiSsid = ssid
        )
    }

    fun applyPreferredWifiSuggestion(ssid: String, password: String): WifiSuggestionResult {
        if (ssid.isBlank()) return WifiSuggestionResult.InvalidInput
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return WifiSuggestionResult.OpenSettingsFallback
        }
        return try {
            val builder = WifiNetworkSuggestion.Builder().setSsid(ssid.trim())
            if (password.isNotEmpty()) {
                builder.setWpa2Passphrase(password)
            }
            val suggestion = builder.build()
            wifiManager.removeNetworkSuggestions(emptyList())
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                WifiSuggestionResult.Applied
            } else {
                WifiSuggestionResult.Error(status)
            }
        } catch (_: SecurityException) {
            WifiSuggestionResult.OpenSettingsFallback
        } catch (_: Exception) {
            WifiSuggestionResult.OpenSettingsFallback
        }
    }

    fun clearWifiSuggestions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { wifiManager.removeNetworkSuggestions(emptyList()) }
        }
    }

    fun getRingerVibrationState(): RingerVibrationState {
        val vibrator = getVibrator()
        return RingerVibrationState(
            ringerMode = audioManager.ringerMode,
            hasNotificationPolicyAccess = notificationManager.isNotificationPolicyAccessGranted,
            hasVibrator = vibrator?.hasVibrator() == true
        )
    }

    fun setRingerMode(mode: Int): Boolean {
        return try {
            audioManager.ringerMode = mode
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun openNotificationPolicySettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun triggerHapticPulse(durationMs: Long = 45L) {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun getBatteryPercentage(): Int {
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .coerceIn(0, 100)
    }

    fun isDeviceOwner(): Boolean {
        return runCatching {
            devicePolicyManager.isDeviceOwnerApp(context.packageName)
        }.getOrDefault(false)
    }

    fun isDeviceAdminActive(): Boolean {
        val component = ComponentName(context, SohojDeviceAdminReceiver::class.java)
        return runCatching {
            devicePolicyManager.isAdminActive(component)
        }.getOrDefault(false)
    }

    fun startKioskLockTask(activity: Activity): Boolean {
        return runCatching {
            if (isDeviceOwner()) {
                devicePolicyManager.setLockTaskPackages(
                    ComponentName(context, SohojDeviceAdminReceiver::class.java),
                    arrayOf(context.packageName)
                )
            }
            activity.startLockTask()
            true
        }.getOrDefault(false)
    }

    fun stopKioskLockTask(activity: Activity) {
        runCatching { activity.stopLockTask() }
    }

    private fun safeAppIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (_: Exception) {
        null
    }

    sealed class WifiSuggestionResult {
        data object Applied : WifiSuggestionResult()
        data object OpenSettingsFallback : WifiSuggestionResult()
        data object InvalidInput : WifiSuggestionResult()
        data class Error(val code: Int) : WifiSuggestionResult()
    }
}
