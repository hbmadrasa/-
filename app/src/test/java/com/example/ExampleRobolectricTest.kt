package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.LauncherPreferencesRepository
import com.example.model.AppSlotConfig
import com.example.model.NetworkStatus
import com.example.model.ResolvedHomeSlot
import com.example.ui.components.BengaliFormatter
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.SohojHomeTheme
import com.example.viewmodel.HomeUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Sohoj Home in Bengali`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("সহজ হোম", appName)
    }

    @Test
    fun `bengali formatter converts numerals accurately`() {
        assertEquals("১২:৩০", BengaliFormatter.toBengaliDigits("12:30"))
        assertEquals("৮৫", BengaliFormatter.toBengaliNumber(85))
    }

    @Test
    fun `admin pin verification, change, and lockout work properly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LauncherPreferencesRepository(context)
        repo.resetAllPreferences()

        // Default PIN succeeds
        val defaultOk = repo.verifyAdminPin("1234")
        assertTrue(defaultOk is LauncherPreferencesRepository.PinVerifyResult.Success)

        // Update PIN
        repo.updateAdminPin("5678")
        val settings = repo.settingsFlow.first()
        assertEquals(false, settings.isDefaultPin)

        // Old PIN fails, new PIN succeeds
        assertTrue(repo.verifyAdminPin("1234") is LauncherPreferencesRepository.PinVerifyResult.Invalid)
        assertTrue(repo.verifyAdminPin("5678") is LauncherPreferencesRepository.PinVerifyResult.Success)

        // 5 failed attempts trigger lockout
        repeat(4) {
            assertTrue(repo.verifyAdminPin("0000") is LauncherPreferencesRepository.PinVerifyResult.Invalid)
        }
        val fifth = repo.verifyAdminPin("0000")
        assertTrue(fifth is LauncherPreferencesRepository.PinVerifyResult.LockedOut)
    }

    @Test
    fun `home screen displays four large Bengali tiles and triggers admin access on long press`() {
        val defaults = AppSlotConfig.defaults().map {
            ResolvedHomeSlot(
                config = it,
                isInstalled = true,
                resolvedPackageName = it.packageName,
                appIcon = null
            )
        }
        var adminTriggered = false

        composeTestRule.setContent {
            SohojHomeTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        resolvedSlots = defaults,
                        networkStatus = NetworkStatus(isWifiEnabled = true, isWifiConnected = true),
                        batteryPercent = 85,
                        isDefaultLauncher = true
                    ),
                    onTileClick = {},
                    onAdminAccessTriggered = { adminTriggered = true },
                    onDismissMissingApp = {},
                    onProceedDataGuard = {},
                    onDismissDataGuard = {},
                    onConfirmTapLaunch = {},
                    onDismissTapConfirm = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("হোয়াটসঅ্যাপ").assertIsDisplayed()
        composeTestRule.onNodeWithText("ফেসবুক লাইট").assertIsDisplayed()
        composeTestRule.onNodeWithText("ইউটিউব").assertIsDisplayed()
        composeTestRule.onNodeWithText("ফোন কল").assertIsDisplayed()

        composeTestRule.onNodeWithTag("admin_lock_badge").performTouchInput {
            longClick()
        }
        composeTestRule.waitForIdle()
        assertTrue(adminTriggered)
    }
}
