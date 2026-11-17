package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSlotConfig
import com.example.model.LauncherSettings
import com.example.model.SlotActionType
import com.example.model.TileColorPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sohoj_home_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("admin_pin_hash")
        val PIN_SALT = stringPreferencesKey("admin_pin_salt")
        val IS_DEFAULT_PIN = booleanPreferencesKey("is_default_pin")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val PIN_LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")

        val PREFERRED_WIFI_SSID = stringPreferencesKey("preferred_wifi_ssid")
        val PREFERRED_WIFI_PASS = stringPreferencesKey("preferred_wifi_pass")
        val WIFI_WATCHDOG = booleanPreferencesKey("wifi_watchdog_enabled")
        val MOBILE_DATA_GUARD = booleanPreferencesKey("mobile_data_guard_enabled")
        val STRICT_DATA_BLOCK = booleanPreferencesKey("strict_mobile_data_block")
        val HAPTIC_VIBRATION = booleanPreferencesKey("haptic_vibration_enabled")
        val TAP_CONFIRMATION = booleanPreferencesKey("tap_confirmation_enabled")
        val HIGH_CONTRAST_DARK = booleanPreferencesKey("high_contrast_dark_mode")
        val KIOSK_LOCK_TASK = booleanPreferencesKey("kiosk_lock_task_enabled")
        val LAST_BOOT_TIMESTAMP = longPreferencesKey("last_boot_timestamp")

        fun slotLabel(index: Int) = stringPreferencesKey("slot_${index}_label")
        fun slotPackage(index: Int) = stringPreferencesKey("slot_${index}_pkg")
        fun slotFallback(index: Int) = stringPreferencesKey("slot_${index}_fallback")
        fun slotActionType(index: Int) = stringPreferencesKey("slot_${index}_action")
        fun slotColor(index: Int) = stringPreferencesKey("slot_${index}_color")
        fun slotWifiOnly(index: Int) = booleanPreferencesKey("slot_${index}_wifi_only")
        fun slotEnabled(index: Int) = booleanPreferencesKey("slot_${index}_enabled")
    }

    val settingsFlow: Flow<LauncherSettings> = context.launcherDataStore.data.map { prefs ->
        val defaults = AppSlotConfig.defaults()
        val slots = defaults.map { def ->
            val i = def.slotIndex
            AppSlotConfig(
                slotIndex = i,
                bengaliLabel = prefs[Keys.slotLabel(i)] ?: def.bengaliLabel,
                packageName = prefs[Keys.slotPackage(i)] ?: def.packageName,
                fallbackPackageName = prefs[Keys.slotFallback(i)] ?: def.fallbackPackageName,
                actionType = prefs[Keys.slotActionType(i)]?.let { SlotActionType.fromId(it) }
                    ?: def.actionType,
                colorPreset = prefs[Keys.slotColor(i)]?.let { TileColorPreset.fromId(it) }
                    ?: def.colorPreset,
                wifiOnlyGuard = prefs[Keys.slotWifiOnly(i)] ?: def.wifiOnlyGuard,
                enabled = prefs[Keys.slotEnabled(i)] ?: def.enabled
            )
        }

        LauncherSettings(
            slots = slots,
            isDefaultPin = prefs[Keys.IS_DEFAULT_PIN] ?: true,
            preferredWifiSsid = prefs[Keys.PREFERRED_WIFI_SSID] ?: "",
            preferredWifiPassword = prefs[Keys.PREFERRED_WIFI_PASS] ?: "",
            wifiWatchdogEnabled = prefs[Keys.WIFI_WATCHDOG] ?: true,
            mobileDataGuardEnabled = prefs[Keys.MOBILE_DATA_GUARD] ?: true,
            strictMobileDataBlock = prefs[Keys.STRICT_DATA_BLOCK] ?: false,
            hapticVibrationEnabled = prefs[Keys.HAPTIC_VIBRATION] ?: true,
            tapConfirmationEnabled = prefs[Keys.TAP_CONFIRMATION] ?: false,
            highContrastDarkMode = prefs[Keys.HIGH_CONTRAST_DARK] ?: false,
            kioskLockTaskEnabled = prefs[Keys.KIOSK_LOCK_TASK] ?: false,
            lastBootTimestamp = prefs[Keys.LAST_BOOT_TIMESTAMP] ?: 0L
        )
    }

    val lockoutUntilFlow: Flow<Long> = context.launcherDataStore.data.map { prefs ->
        prefs[Keys.PIN_LOCKOUT_UNTIL] ?: 0L
    }

    val failedAttemptsFlow: Flow<Int> = context.launcherDataStore.data.map { prefs ->
        prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0
    }

    suspend fun verifyAdminPin(pin: String): PinVerifyResult {
        val prefs = context.launcherDataStore.data.first()
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[Keys.PIN_LOCKOUT_UNTIL] ?: 0L
        if (now < lockoutUntil) {
            return PinVerifyResult.LockedOut(remainingSeconds = ((lockoutUntil - now) / 1000L).coerceAtLeast(1L))
        }

        val isDefault = prefs[Keys.IS_DEFAULT_PIN] ?: true
        val storedHash = prefs[Keys.PIN_HASH]
        val storedSalt = prefs[Keys.PIN_SALT]

        val matches = if (isDefault && storedHash == null) {
            pin == DEFAULT_PIN
        } else if (storedHash != null && storedSalt != null) {
            hashPin(pin, storedSalt) == storedHash
        } else {
            pin == DEFAULT_PIN
        }

        return if (matches) {
            context.launcherDataStore.edit { mutable ->
                mutable[Keys.FAILED_PIN_ATTEMPTS] = 0
                mutable[Keys.PIN_LOCKOUT_UNTIL] = 0L
            }
            PinVerifyResult.Success
        } else {
            val attempts = (prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0) + 1
            val newLockoutUntil = if (attempts >= MAX_PIN_ATTEMPTS) {
                now + LOCKOUT_DURATION_MS
            } else {
                0L
            }
            context.launcherDataStore.edit { mutable ->
                mutable[Keys.FAILED_PIN_ATTEMPTS] = if (attempts >= MAX_PIN_ATTEMPTS) 0 else attempts
                mutable[Keys.PIN_LOCKOUT_UNTIL] = newLockoutUntil
            }
            if (newLockoutUntil > 0L) {
                PinVerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000L)
            } else {
                PinVerifyResult.Invalid(attemptsRemaining = MAX_PIN_ATTEMPTS - attempts)
            }
        }
    }

    suspend fun updateAdminPin(newPin: String) {
        val salt = generateSalt()
        val hash = hashPin(newPin, salt)
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.IS_DEFAULT_PIN] = false
            prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
            prefs[Keys.PIN_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun updateSlot(slot: AppSlotConfig) {
        val i = slot.slotIndex
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.slotLabel(i)] = slot.bengaliLabel
            prefs[Keys.slotPackage(i)] = slot.packageName
            prefs[Keys.slotFallback(i)] = slot.fallbackPackageName
            prefs[Keys.slotActionType(i)] = slot.actionType.id
            prefs[Keys.slotColor(i)] = slot.colorPreset.id
            prefs[Keys.slotWifiOnly(i)] = slot.wifiOnlyGuard
            prefs[Keys.slotEnabled(i)] = slot.enabled
        }
    }

    suspend fun restoreDefaultSlots() {
        context.launcherDataStore.edit { prefs ->
            AppSlotConfig.defaults().forEach { def ->
                val i = def.slotIndex
                prefs[Keys.slotLabel(i)] = def.bengaliLabel
                prefs[Keys.slotPackage(i)] = def.packageName
                prefs[Keys.slotFallback(i)] = def.fallbackPackageName
                prefs[Keys.slotActionType(i)] = def.actionType.id
                prefs[Keys.slotColor(i)] = def.colorPreset.id
                prefs[Keys.slotWifiOnly(i)] = def.wifiOnlyGuard
                prefs[Keys.slotEnabled(i)] = def.enabled
            }
        }
    }

    suspend fun updatePreferredWifi(ssid: String, password: String) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.PREFERRED_WIFI_SSID] = ssid.trim()
            prefs[Keys.PREFERRED_WIFI_PASS] = password
        }
    }

    suspend fun setWifiWatchdog(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.WIFI_WATCHDOG] = enabled }
    }

    suspend fun setMobileDataGuard(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.MOBILE_DATA_GUARD] = enabled }
    }

    suspend fun setStrictMobileDataBlock(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.STRICT_DATA_BLOCK] = enabled }
    }

    suspend fun setHapticVibration(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.HAPTIC_VIBRATION] = enabled }
    }

    suspend fun setTapConfirmation(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.TAP_CONFIRMATION] = enabled }
    }

    suspend fun setHighContrastDarkMode(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.HIGH_CONTRAST_DARK] = enabled }
    }

    suspend fun setKioskLockTask(enabled: Boolean) {
        context.launcherDataStore.edit { it[Keys.KIOSK_LOCK_TASK] = enabled }
    }

    suspend fun recordBootTimestamp(timestamp: Long) {
        context.launcherDataStore.edit { it[Keys.LAST_BOOT_TIMESTAMP] = timestamp }
    }

    suspend fun resetAllPreferences() {
        context.launcherDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(digest.digest(input), Base64.NO_WRAP)
    }

    sealed class PinVerifyResult {
        data object Success : PinVerifyResult()
        data class Invalid(val attemptsRemaining: Int) : PinVerifyResult()
        data class LockedOut(val remainingSeconds: Long) : PinVerifyResult()
    }

    companion object {
        const val DEFAULT_PIN = "1234"
        const val MAX_PIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}
