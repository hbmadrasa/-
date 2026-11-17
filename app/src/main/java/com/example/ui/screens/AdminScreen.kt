package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppSlotConfig
import com.example.model.InstalledAppInfo
import com.example.model.ResolvedHomeSlot
import com.example.model.SlotActionType
import com.example.model.TileColorPreset
import com.example.ui.theme.TileAmberAccent
import com.example.ui.theme.TileCobaltAccent
import com.example.ui.theme.TileCrimsonAccent
import com.example.ui.theme.TileEmeraldAccent
import com.example.ui.theme.TilePlumAccent
import com.example.ui.theme.TileTealAccent
import com.example.viewmodel.AdminPanelState
import com.example.viewmodel.HomeUiState
import com.example.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    homeUiState: HomeUiState,
    adminState: AdminPanelState,
    viewModel: LauncherViewModel,
    onExitToHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        onExitToHome()
    }

    LaunchedEffect(adminState.statusMessage) {
        val msg = adminState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAdminMessage()
        }
    }

    val homeRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDeviceStatus()
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        viewModel.refreshDeviceStatus()
    }

    var editingSlot by remember { mutableStateOf<AppSlotConfig?>(null) }
    var showAppPickerForSlot by remember { mutableStateOf<AppSlotConfig?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showLimitationsExpanded by remember { mutableStateOf(false) }

    var ssidInput by remember(homeUiState.settings.preferredWifiSsid) {
        mutableStateOf(homeUiState.settings.preferredWifiSsid)
    }
    var wifiPassInput by remember(homeUiState.settings.preferredWifiPassword) {
        mutableStateOf(homeUiState.settings.preferredWifiPassword)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "অ্যাডমিন কন্ট্রোল প্যানেল",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExitToHome,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "হোমে ফিরে যান"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onExitToHome,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("admin_lock_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("লক করুন")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (homeUiState.settings.isDefaultPin) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "নিরাপত্তা পরামর্শ: প্রাথমিক PIN (১২৩৪) পরিবর্তন করুন",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "সাধারণ ব্যবহারকারীর হাত থেকে সুরক্ষার জন্য নিজের গোপন PIN সেট করুন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 1. Launcher & Default Home App
                AdminSectionCard(
                    title = "লঞ্চার সেটিংস ও অটো-স্টার্ট",
                    icon = Icons.Filled.Home
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ডিফল্ট হোম অ্যাপ অবস্থা",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (adminState.isDefaultLauncher) {
                                    "সক্রিয় — ফোন Restart হলেও স্বয়ংক্রিয়ভাবে চালু হবে"
                                } else {
                                    "সক্রিয় নয় — হোম বাটন চাপলে ফোনের পুরনো লঞ্চার আসে"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (adminState.isDefaultLauncher) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                        if (adminState.isDefaultLauncher) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "সক্রিয়",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val roleIntent = viewModel.createHomeRoleIntent()
                            if (roleIntent != null && !adminState.isDefaultLauncher) {
                                homeRoleLauncher.launch(roleIntent)
                            } else {
                                viewModel.openHomeSettings()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("set_default_launcher_button")
                    ) {
                        Text(
                            text = if (adminState.isDefaultLauncher) {
                                "ডিফল্ট হোম অ্যাপ পরিবর্তন করুন"
                            } else {
                                "সহজ হোম-কে ডিফল্ট লঞ্চার সেট করুন"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // 2. 4 Home App Buttons Configuration
                AdminSectionCard(
                    title = "হোম স্ক্রিনের ৪টি অ্যাপ নিয়ন্ত্রণ",
                    icon = Icons.Filled.Apps
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        homeUiState.resolvedSlots.forEach { resolved ->
                            SlotAdminRow(
                                slot = resolved,
                                onEdit = { editingSlot = resolved.config }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.restoreDefaultSlots() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("restore_default_slots_button")
                    ) {
                        Text("মূল ৪টি অ্যাপ (WhatsApp, FB Lite, YouTube, Phone) ফিরিয়ে আনুন")
                    }
                }

                // 3. Wi-Fi & Mobile Data Controls
                AdminSectionCard(
                    title = "Wi-Fi ও মোবাইল ডেটা নিয়ন্ত্রণ",
                    icon = Icons.Filled.Wifi
                ) {
                    val net = homeUiState.networkStatus
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "বর্তমান সংযোগ: " + when {
                                    net.isWifiConnected -> "Wi-Fi সংযুক্ত${net.wifiSsid?.let { " ($it)" } ?: ""}"
                                    net.isMobileDataActive -> "মোবাইল ডেটা চালু (Wi-Fi নেই)"
                                    else -> "কোনো ইন্টারনেট সংযোগ নেই"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Wi-Fi সুইচ: ${if (net.isWifiEnabled) "চালু" else "বন্ধ"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasLocationPermission) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("সংযুক্ত Wi-Fi নাম (SSID) দেখতে লোকেশন অনুমতি দিন")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.openWifiPanel() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_wifi_panel_button")
                        ) {
                            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wi-Fi প্যানেল")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.openMobileDataSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("open_mobile_data_button")
                        ) {
                            Icon(Icons.Filled.DataUsage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("মোবাইল ডেটা")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "নির্দিষ্ট Wi-Fi নেটওয়ার্কে সংযোগ (Android 10+)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ssidInput,
                        onValueChange = { ssidInput = it },
                        label = { Text("Wi-Fi নাম (SSID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wifiPassInput,
                        onValueChange = { wifiPassInput = it },
                        label = { Text("Wi-Fi পাসওয়ার্ড (খোলা হলে ফাঁকা রাখুন)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.saveAndSuggestWifi(ssidInput, wifiPassInput) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_wifi_suggestion_button")
                        ) {
                            Text("নির্দিষ্ট Wi-Fi সেট করুন")
                        }
                        if (homeUiState.settings.preferredWifiSsid.isNotBlank()) {
                            OutlinedButton(
                                onClick = { viewModel.clearWifiSuggestions() },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("মুছুন")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    AdminSwitchRow(
                        title = "Wi-Fi বিচ্ছিন্ন সতর্কতা",
                        subtitle = "Wi-Fi বন্ধ হলে হোম স্ক্রিনে সতর্কবার্তা দেখাবে",
                        checked = homeUiState.settings.wifiWatchdogEnabled,
                        onCheckedChange = viewModel::setWifiWatchdog
                    )
                    AdminSwitchRow(
                        title = "মোবাইল ডেটা সীমিত মোড (Data Guard)",
                        subtitle = "Wi-Fi না থাকলে YouTube / Facebook খোলার আগে সতর্ক করবে",
                        checked = homeUiState.settings.mobileDataGuardEnabled,
                        onCheckedChange = viewModel::setMobileDataGuard
                    )
                    AdminSwitchRow(
                        title = "কঠোর ডেটা ব্লক",
                        subtitle = "Wi-Fi ছাড়া ভারী অ্যাপ একদমই খুলতে দেবে না",
                        checked = homeUiState.settings.strictMobileDataBlock,
                        enabled = homeUiState.settings.mobileDataGuardEnabled,
                        onCheckedChange = viewModel::setStrictMobileDataBlock
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.openDataUsageSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সিস্টেম ডেটা ব্যবহারের সীমা খুলুন")
                    }
                }

                // 4. Vibration & Accessibility Controls
                AdminSectionCard(
                    title = "ভাইব্রেশন ও স্পর্শ নিয়ন্ত্রণ",
                    icon = Icons.Filled.Vibration
                ) {
                    val ringerLabel = when (adminState.ringerVibrationState.ringerMode) {
                        AudioManager.RINGER_MODE_VIBRATE -> "ভাইব্রেশন মোড চালু"
                        AudioManager.RINGER_MODE_SILENT -> "সাইলেন্ট মোড"
                        else -> "সাধারণ রিং + ভাইব্রেশন"
                    }
                    Text(
                        text = "বর্তমান রিংগার অবস্থা: $ringerLabel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.setRingerModeVibrate(true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("ভাইব্রেশন মোড")
                        }
                        OutlinedButton(
                            onClick = { viewModel.setRingerModeVibrate(false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("সাধারণ রিং")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.testVibration() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ভাইব্রেশন পরীক্ষা করুন")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    AdminSwitchRow(
                        title = "বাটন স্পর্শ ভাইব্রেশন (Haptic)",
                        subtitle = "বড় বাটনে চাপ দিলে হালকা ভাইব্রেট হবে",
                        checked = homeUiState.settings.hapticVibrationEnabled,
                        onCheckedChange = viewModel::setHapticVibration
                    )
                    AdminSwitchRow(
                        title = "ভুল চাপ প্রতিরোধ মোড",
                        subtitle = "অ্যাপ খোলার আগে হ্যাঁ/না নিশ্চিতকরণ চাইবে",
                        checked = homeUiState.settings.tapConfirmationEnabled,
                        onCheckedChange = viewModel::setTapConfirmation
                    )
                    AdminSwitchRow(
                        title = "উচ্চ কনট্রাস্ট ডার্ক থিম",
                        subtitle = "রাতের বেলা বা কম আলোতে চোখের আরামের জন্য",
                        checked = homeUiState.settings.highContrastDarkMode,
                        onCheckedChange = viewModel::setHighContrastDarkMode
                    )
                }

                // 5. Security, PIN & Android 10 Limitations Guide
                AdminSectionCard(
                    title = "নিরাপত্তা ও পিন নিয়ন্ত্রণ",
                    icon = Icons.Filled.Security
                ) {
                    Button(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("change_admin_pin_button")
                    ) {
                        Icon(Icons.Filled.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অ্যাডমিন PIN পরিবর্তন করুন")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AdminSwitchRow(
                        title = "স্ক্রিন পিনিং / কিয়স্ক লক",
                        subtitle = if (adminState.isDeviceOwner) {
                            "Device Owner সক্রিয় — সম্পূর্ণ লক টাস্ক সম্ভব"
                        } else {
                            "সিস্টেম স্ক্রিন পিনিং চালু করে লঞ্চার থেকে বের হওয়া আটকাবে"
                        },
                        checked = homeUiState.settings.kioskLockTaskEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleKioskLockTask(activity, enabled)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showLimitationsExpanded = !showLimitationsExpanded }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Android 10 সীমাবদ্ধতা ও সহায়িকা",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Icon(
                                    imageVector = if (showLimitationsExpanded) {
                                        Icons.Filled.ExpandLess
                                    } else {
                                        Icons.Filled.ExpandMore
                                    },
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = showLimitationsExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "১. Wi-Fi ও মোবাইল ডেটা সুইচ: Android 10 (API 29) থেকে সাধারণ অ্যাপ সরাসরি ব্যাকগ্রাউন্ডে সিস্টেম Wi-Fi বা মোবাইল ডেটা সুইচ অন/অফ করতে পারে না। তাই এই অ্যাপ Wi-Fi প্যানেল, নির্দিষ্ট Wi-Fi পরামর্শ (WifiNetworkSuggestion) এবং মোবাইল ডেটা গার্ড ব্যবহার করে।",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "২. নোটিফিকেশন বার লক: সাধারণ লঞ্চার অ্যাপ Android-এর উপরের Quick Settings টানা সম্পূর্ণ ব্লক করতে পারে না। সম্পূর্ণ কিয়স্ক লক চাইলে কম্পিউটার দিয়ে ADB কমান্ডে Device Owner সেট করা যায় অথবা স্ক্রিন পিনিং চালু করা যায়।",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "৩. রিস্টার্টের পর অটো-স্টার্ট: একবার ডিফল্ট হোম অ্যাপ হিসেবে সেট করলে Samsung Galaxy M10 রিস্টার্ট হওয়ার সাথে সাথে এই লঞ্চারই স্বয়ংক্রিয়ভাবে চালু হবে।",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Emergency & Recovery
                AdminSectionCard(
                    title = "জরুরি সেটিংস ও পুনরুদ্ধার",
                    icon = Icons.Filled.Settings
                ) {
                    OutlinedButton(
                        onClick = { viewModel.openSystemSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("open_android_settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("সাধারণ Android Settings খুলুন")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.openHomeSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("ফোনের স্বাভাবিক লঞ্চারে ফিরে যান")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("restore_all_controls_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("সব নিয়ন্ত্রণ পুনরুদ্ধার করুন")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Slot Editor Dialog
    editingSlot?.let { slot ->
        SlotEditorDialog(
            slot = slot,
            onPickApp = { showAppPickerForSlot = it },
            onSave = { updated ->
                viewModel.updateSlot(updated)
                editingSlot = null
            },
            onDismiss = { editingSlot = null }
        )
    }

    // Installed Apps Picker Dialog
    showAppPickerForSlot?.let { targetSlot ->
        InstalledAppPickerDialog(
            apps = adminState.installedApps,
            isLoading = adminState.isLoadingApps,
            onSelectApp = { picked ->
                val updated = if (picked.isDialer) {
                    targetSlot.copy(
                        actionType = SlotActionType.PHONE_DIALER,
                        packageName = "",
                        fallbackPackageName = "",
                        bengaliLabel = targetSlot.bengaliLabel.ifBlank { "ফোন কল" }
                    )
                } else {
                    targetSlot.copy(
                        actionType = SlotActionType.PACKAGE,
                        packageName = picked.packageName,
                        fallbackPackageName = "",
                        bengaliLabel = picked.appName
                    )
                }
                editingSlot = updated
                showAppPickerForSlot = null
            },
            onDismiss = { showAppPickerForSlot = null }
        )
    }

    // Change Admin PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onSavePin = { newPin ->
                viewModel.updateAdminPin(newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Restore All Controls Confirmation Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("সব নিয়ন্ত্রণ পুনরুদ্ধার করবেন?") },
            text = {
                Text("এতে অ্যাডমিন PIN পুনরায় ১২৩৪ হবে, সংরক্ষিত Wi-Fi পরামর্শ মুছে যাবে, মূল ৪টি অ্যাপ ফিরে আসবে এবং লক মোড বন্ধ হবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        viewModel.restoreAllControls(activity, openHomeChooserAfter = false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("পুনরুদ্ধার করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SlotAdminRow(
    slot: ResolvedHomeSlot,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val bmp = remember(slot.appIcon) { slot.appIcon?.toBitmap(96, 96) }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(presetColor(slot.config.colorPreset)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = slot.config.bengaliLabel,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Text(
                            text = (slot.config.slotIndex + 1).toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.config.bengaliLabel,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val detail = buildString {
                        append(
                            if (slot.config.actionType == SlotActionType.PHONE_DIALER) {
                                "ফোন ডায়ালার"
                            } else {
                                slot.resolvedPackageName ?: slot.config.packageName
                            }
                        )
                        if (!slot.isInstalled) append(" • ইনস্টল নেই")
                        if (!slot.config.enabled) append(" • লুকানো")
                        if (slot.config.wifiOnlyGuard) append(" • শুধু Wi-Fi")
                    }
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_slot_${slot.config.slotIndex}")
            ) {
                Text("পরিবর্তন")
            }
        }
    }
}

@Composable
private fun AdminSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SlotEditorDialog(
    slot: AppSlotConfig,
    onPickApp: (AppSlotConfig) -> Unit,
    onSave: (AppSlotConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(slot) { mutableStateOf(slot.bengaliLabel) }
    var colorPreset by remember(slot) { mutableStateOf(slot.colorPreset) }
    var wifiOnly by remember(slot) { mutableStateOf(slot.wifiOnlyGuard) }
    var enabled by remember(slot) { mutableStateOf(slot.enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বাটন ${slot.slotIndex + 1} সম্পাদনা") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        onPickApp(
                            slot.copy(
                                bengaliLabel = label,
                                colorPreset = colorPreset,
                                wifiOnlyGuard = wifiOnly,
                                enabled = enabled
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (slot.actionType == SlotActionType.PHONE_DIALER) {
                            "অ্যাপ: ফোন ডায়ালার (বদলান)"
                        } else {
                            "অ্যাপ বদলান (${slot.packageName})"
                        }
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("বাংলা নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("রঙ নির্বাচন করুন:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TileColorPreset.entries.forEach { preset ->
                        val selected = preset == colorPreset
                        Surface(
                            onClick = { colorPreset = preset },
                            shape = CircleShape,
                            color = presetColor(preset),
                            border = if (selected) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            } else null,
                            modifier = Modifier.size(36.dp)
                        ) {}
                    }
                }

                AdminSwitchRow(
                    title = "শুধু Wi-Fi ডেটা গার্ড",
                    subtitle = "Wi-Fi না থাকলে সতর্ক করবে",
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it }
                )

                AdminSwitchRow(
                    title = "হোম স্ক্রিনে দেখাবে",
                    subtitle = "বন্ধ করলে বাটন লুকানো থাকবে",
                    checked = enabled,
                    onCheckedChange = { enabled = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        slot.copy(
                            bengaliLabel = label.trim().ifBlank { slot.bengaliLabel },
                            colorPreset = colorPreset,
                            wifiOnlyGuard = wifiOnly,
                            enabled = enabled
                        )
                    )
                }
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun InstalledAppPickerDialog(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    onSelectApp: (InstalledAppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(apps, search) {
        if (search.isBlank()) apps
        else apps.filter {
            it.appName.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ইনস্টল করা অ্যাপ নির্বাচন করুন",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("অ্যাপ খুঁজুন...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("অ্যাপ তালিকা লোড হচ্ছে...")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.packageName + it.isDialer }) { app ->
                            val bmp = remember(app.icon) { app.icon?.toBitmap(80, 80) }
                            Surface(
                                onClick = { onSelectApp(app) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = app.appName,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (app.packageName.isNotBlank()) {
                                            Text(
                                                text = app.packageName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("বন্ধ করুন")
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন ৪ সংখ্যার PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) newPin = it },
                    label = { Text("নতুন PIN (৪ সংখ্যা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text("পুনরায় PIN লিখুন") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        newPin.length != 4 -> error = "PIN অবশ্যই ৪ সংখ্যার হতে হবে"
                        newPin != confirmPin -> error = "দুটি PIN মেলেনি"
                        else -> onSavePin(newPin)
                    }
                }
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

private fun presetColor(preset: TileColorPreset): Color = when (preset) {
    TileColorPreset.EMERALD -> TileEmeraldAccent
    TileColorPreset.COBALT -> TileCobaltAccent
    TileColorPreset.CRIMSON -> TileCrimsonAccent
    TileColorPreset.TEAL -> TileTealAccent
    TileColorPreset.AMBER -> TileAmberAccent
    TileColorPreset.PLUM -> TilePlumAccent
}
