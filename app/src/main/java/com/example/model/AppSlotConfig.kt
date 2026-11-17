package com.example.model

import android.graphics.drawable.Drawable

enum class TileColorPreset(val id: String, val bengaliName: String) {
    EMERALD("emerald", "সবুজ"),
    COBALT("cobalt", "নীল"),
    CRIMSON("crimson", "লাল"),
    TEAL("teal", "আকাশি"),
    AMBER("amber", "সোনালি"),
    PLUM("plum", "বেগুনি");

    companion object {
        fun fromId(id: String): TileColorPreset =
            entries.firstOrNull { it.id == id } ?: EMERALD
    }
}

enum class SlotActionType(val id: String) {
    PACKAGE("package"),
    PHONE_DIALER("phone_dialer");

    companion object {
        fun fromId(id: String): SlotActionType =
            entries.firstOrNull { it.id == id } ?: PACKAGE
    }
}

data class AppSlotConfig(
    val slotIndex: Int,
    val bengaliLabel: String,
    val packageName: String,
    val fallbackPackageName: String = "",
    val actionType: SlotActionType = SlotActionType.PACKAGE,
    val colorPreset: TileColorPreset = TileColorPreset.EMERALD,
    val wifiOnlyGuard: Boolean = false,
    val enabled: Boolean = true
) {
    companion object {
        fun defaults(): List<AppSlotConfig> = listOf(
            AppSlotConfig(
                slotIndex = 0,
                bengaliLabel = "হোয়াটসঅ্যাপ",
                packageName = "com.whatsapp",
                fallbackPackageName = "com.whatsapp.w4b",
                actionType = SlotActionType.PACKAGE,
                colorPreset = TileColorPreset.EMERALD,
                wifiOnlyGuard = false,
                enabled = true
            ),
            AppSlotConfig(
                slotIndex = 1,
                bengaliLabel = "ফেসবুক লাইট",
                packageName = "com.facebook.lite",
                fallbackPackageName = "com.facebook.katana",
                actionType = SlotActionType.PACKAGE,
                colorPreset = TileColorPreset.COBALT,
                wifiOnlyGuard = true,
                enabled = true
            ),
            AppSlotConfig(
                slotIndex = 2,
                bengaliLabel = "ইউটিউব",
                packageName = "com.google.android.youtube",
                fallbackPackageName = "com.google.android.apps.youtube.lite",
                actionType = SlotActionType.PACKAGE,
                colorPreset = TileColorPreset.CRIMSON,
                wifiOnlyGuard = true,
                enabled = true
            ),
            AppSlotConfig(
                slotIndex = 3,
                bengaliLabel = "ফোন কল",
                packageName = "",
                fallbackPackageName = "",
                actionType = SlotActionType.PHONE_DIALER,
                colorPreset = TileColorPreset.TEAL,
                wifiOnlyGuard = false,
                enabled = true
            )
        )
    }
}

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isDialer: Boolean = false
)

data class ResolvedHomeSlot(
    val config: AppSlotConfig,
    val isInstalled: Boolean,
    val resolvedPackageName: String?,
    val appIcon: Drawable?
)

data class NetworkStatus(
    val isWifiEnabled: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isMobileDataActive: Boolean = false,
    val isMetered: Boolean = false,
    val wifiSsid: String? = null
)

data class RingerVibrationState(
    val ringerMode: Int = android.media.AudioManager.RINGER_MODE_NORMAL,
    val hasNotificationPolicyAccess: Boolean = false,
    val hasVibrator: Boolean = true
)

data class LauncherSettings(
    val slots: List<AppSlotConfig> = AppSlotConfig.defaults(),
    val isDefaultPin: Boolean = true,
    val preferredWifiSsid: String = "",
    val preferredWifiPassword: String = "",
    val wifiWatchdogEnabled: Boolean = true,
    val mobileDataGuardEnabled: Boolean = true,
    val strictMobileDataBlock: Boolean = false,
    val hapticVibrationEnabled: Boolean = true,
    val tapConfirmationEnabled: Boolean = false,
    val highContrastDarkMode: Boolean = false,
    val kioskLockTaskEnabled: Boolean = false,
    val lastBootTimestamp: Long = 0L
)
