package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.PinEntryDialog
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.SohojHomeTheme
import com.example.viewmodel.LauncherViewModel

object Routes {
    const val HOME = "home"
    const val ADMIN = "admin"
}

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
            val adminState by viewModel.adminState.collectAsStateWithLifecycle()
            val pinDialogState by viewModel.pinDialogState.collectAsStateWithLifecycle()
            val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.refreshDeviceStatus()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val darkTheme = homeUiState.settings.highContrastDarkMode || isSystemInDarkTheme()

            SohojHomeTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            uiState = homeUiState,
                            onTileClick = viewModel::onHomeTileClicked,
                            onAdminAccessTriggered = viewModel::openPinDialog,
                            onDismissMissingApp = viewModel::dismissMissingAppDialog,
                            onProceedDataGuard = viewModel::proceedDataGuardAndLaunch,
                            onDismissDataGuard = viewModel::dismissDataGuardDialog,
                            onConfirmTapLaunch = viewModel::confirmTapAndLaunch,
                            onDismissTapConfirm = viewModel::dismissTapConfirm
                        )
                    }

                    composable(Routes.ADMIN) {
                        if (isAdminAuthenticated) {
                            AdminScreen(
                                homeUiState = homeUiState,
                                adminState = adminState,
                                viewModel = viewModel,
                                onExitToHome = {
                                    viewModel.lockAdminSession()
                                    navController.popBackStack(Routes.HOME, inclusive = false)
                                }
                            )
                        } else {
                            // Safety guard: never display admin without active PIN session
                            HomeScreen(
                                uiState = homeUiState,
                                onTileClick = viewModel::onHomeTileClicked,
                                onAdminAccessTriggered = viewModel::openPinDialog,
                                onDismissMissingApp = viewModel::dismissMissingAppDialog,
                                onProceedDataGuard = viewModel::proceedDataGuardAndLaunch,
                                onDismissDataGuard = viewModel::dismissDataGuardDialog,
                                onConfirmTapLaunch = viewModel::confirmTapAndLaunch,
                                onDismissTapConfirm = viewModel::dismissTapConfirm
                            )
                        }
                    }
                }

                PinEntryDialog(
                    state = pinDialogState,
                    isDefaultPin = homeUiState.settings.isDefaultPin,
                    onDigitClick = { digit ->
                        viewModel.onPinDigitEntered(digit) {
                            navController.navigate(Routes.ADMIN) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onBackspace = viewModel::onPinBackspace,
                    onSubmit = {
                        viewModel.submitPin {
                            navController.navigate(Routes.ADMIN) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onDismiss = viewModel::closePinDialog
                )
            }
        }
    }
}
