package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ResolvedHomeSlot
import com.example.model.SlotActionType
import com.example.model.TileColorPreset
import com.example.ui.components.BengaliFormatter
import com.example.ui.theme.TileAmberAccent
import com.example.ui.theme.TileAmberBg
import com.example.ui.theme.TileCobaltAccent
import com.example.ui.theme.TileCobaltBg
import com.example.ui.theme.TileCrimsonAccent
import com.example.ui.theme.TileCrimsonBg
import com.example.ui.theme.TileEmeraldAccent
import com.example.ui.theme.TileEmeraldBg
import com.example.ui.theme.TilePlumAccent
import com.example.ui.theme.TilePlumBg
import com.example.ui.theme.TileTealAccent
import com.example.ui.theme.TileTealBg
import com.example.viewmodel.HomeUiState
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onTileClick: (ResolvedHomeSlot) -> Unit,
    onAdminAccessTriggered: () -> Unit,
    onDismissMissingApp: () -> Unit,
    onProceedDataGuard: () -> Unit,
    onDismissDataGuard: () -> Unit,
    onConfirmTapLaunch: () -> Unit,
    onDismissTapConfirm: () -> Unit
) {
    // Prevent accidental Back navigation out of Launcher
    BackHandler(enabled = true) { /* Intentionally blocked for elderly safety */ }

    var currentCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentCalendar = Calendar.getInstance()
            delay(15_000L)
        }
    }

    // Secret Admin Tap Counter (3 rapid taps OR 1 long press opens Admin PIN)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    fun handleDiscreetAdminTap() {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapTime < 1200L) tapCount + 1 else 1
        lastTapTime = now
        if (tapCount >= 3) {
            tapCount = 0
            onAdminAccessTriggered()
        }
    }

    val isDark = uiState.settings.highContrastDarkMode || isSystemInDarkTheme()
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color(0xFF0A1210), Color(0xFF0F1D1A))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFFF6F4EC), Color(0xFFECEFE8))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Clock + Status + Discreet Admin Lock
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onAdminAccessTriggered
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = BengaliFormatter.formatBengaliTime(currentCalendar),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = BengaliFormatter.formatDayPeriod(currentCalendar),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            text = BengaliFormatter.formatBengaliDate(currentCalendar),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Discreet Admin Lock Badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = { handleDiscreetAdminTap() },
                                    onLongClick = { onAdminAccessTriggered() }
                                )
                                .testTag("admin_lock_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "অ্যাডমিন প্যানেল লক",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Compact Status Indicators (Battery & Network)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                icon = if (uiState.networkStatus.isWifiConnected) {
                                    Icons.Filled.Wifi
                                } else if (uiState.networkStatus.isMobileDataActive) {
                                    Icons.Filled.SignalCellularAlt
                                } else {
                                    Icons.Filled.WifiOff
                                },
                                label = when {
                                    uiState.networkStatus.isWifiConnected -> "Wi-Fi"
                                    uiState.networkStatus.isMobileDataActive -> "ডেটা"
                                    else -> "অফ"
                                },
                                isAlert = !uiState.networkStatus.isWifiConnected &&
                                    uiState.settings.wifiWatchdogEnabled
                            )
                            StatusPill(
                                icon = Icons.Filled.BatteryStd,
                                label = "${BengaliFormatter.toBengaliNumber(uiState.batteryPercent)}%",
                                isAlert = uiState.batteryPercent <= 15
                            )
                        }
                    }
                }
            }

            // Wi-Fi Watchdog Alert (shown when Wi-Fi is off/disconnected and watchdog is enabled)
            AnimatedVisibility(
                visible = uiState.settings.wifiWatchdogEnabled && !uiState.networkStatus.isWifiConnected
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "সতর্কতা",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (uiState.networkStatus.isMobileDataActive) {
                                "Wi-Fi বন্ধ আছে — এখন মোবাইল ডেটা চালু"
                            } else {
                                "Wi-Fi সংযোগ বিচ্ছিন্ন আছে"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // 4 Giant Elderly-Friendly App Tiles
            val activeSlots = uiState.resolvedSlots.filter { it.config.enabled }.take(4)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val isLandscapeOrWide = maxWidth > 540.dp && maxHeight < 520.dp
                if (isLandscapeOrWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        activeSlots.forEach { slot ->
                            GiantAppTile(
                                slot = slot,
                                isDarkTheme = isDark,
                                onClick = { onTileClick(slot) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                } else {
                    val topRow = activeSlots.take(2)
                    val bottomRow = activeSlots.drop(2).take(2)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            topRow.forEach { slot ->
                                GiantAppTile(
                                    slot = slot,
                                    isDarkTheme = isDark,
                                    onClick = { onTileClick(slot) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                        if (bottomRow.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                bottomRow.forEach { slot ->
                                    GiantAppTile(
                                        slot = slot,
                                        isDarkTheme = isDark,
                                        onClick = { onTileClick(slot) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // First-time Admin setup guide (disappears automatically once Default Launcher is set or PIN is changed)
            if (!uiState.isDefaultLauncher && uiState.settings.isDefaultPin) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { handleDiscreetAdminTap() },
                            onLongClick = { onAdminAccessTriggered() }
                        )
                        .testTag("first_time_setup_hint")
                ) {
                    Text(
                        text = "অ্যাডমিন সেটআপ: তালা আইকনে দীর্ঘক্ষণ চাপুন অথবা ৩ বার চাপুন (পিন: ১২৩৪)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Missing App Dialog
    uiState.missingAppSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = onDismissMissingApp,
            title = {
                Text(
                    text = "অ্যাপ পাওয়া যায়নি",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "${slot.config.bengaliLabel} এই ফোনে ইনস্টল করা নেই। অনুগ্রহ করে অ্যাডমিনকে জানান।",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = onDismissMissingApp,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("ঠিক আছে", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    // Mobile Data Guard Dialog
    uiState.dataGuardPendingSlot?.let { slot ->
        val strictBlock = uiState.settings.strictMobileDataBlock
        AlertDialog(
            onDismissRequest = onDismissDataGuard,
            title = {
                Text(
                    text = if (strictBlock) "Wi-Fi ছাড়া খোলা যাবে না" else "মোবাইল ডেটা সতর্কতা",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = if (strictBlock) {
                        "মোবাইল ডেটা বাঁচাতে অ্যাডমিন ${slot.config.bengaliLabel} বন্ধ রেখেছেন। Wi-Fi সংযোগ হলে আবার চেষ্টা করুন।"
                    } else {
                        "এখন Wi-Fi সংযোগ নেই। ${slot.config.bengaliLabel} খুললে মোবাইল ডেটা খরচ হবে। আপনি কি খুলতে চান?"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                if (strictBlock) {
                    Button(
                        onClick = onDismissDataGuard,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("ঠিক আছে", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = onProceedDataGuard,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("তবুও খুলুন", style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
            dismissButton = {
                if (!strictBlock) {
                    OutlinedButton(
                        onClick = onDismissDataGuard,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("বাতিল", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        )
    }

    // Accidental Tap Confirmation Dialog
    uiState.tapConfirmPendingSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = onDismissTapConfirm,
            title = {
                Text(
                    text = "${slot.config.bengaliLabel} খুলবেন?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "নিশ্চিত করতে নিচের \"হ্যাঁ, খুলুন\" বাটনে চাপ দিন।",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmTapLaunch,
                    modifier = Modifier.height(54.dp)
                ) {
                    Text("হ্যাঁ, খুলুন", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismissTapConfirm,
                    modifier = Modifier.height(54.dp)
                ) {
                    Text("না", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}

@Composable
private fun StatusPill(
    icon: ImageVector,
    label: String,
    isAlert: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAlert) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isAlert) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun GiantAppTile(
    slot: ResolvedHomeSlot,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "tile_scale"
    )

    val (accentColor, lightSurface) = when (slot.config.colorPreset) {
        TileColorPreset.EMERALD -> TileEmeraldAccent to TileEmeraldBg
        TileColorPreset.COBALT -> TileCobaltAccent to TileCobaltBg
        TileColorPreset.CRIMSON -> TileCrimsonAccent to TileCrimsonBg
        TileColorPreset.TEAL -> TileTealAccent to TileTealBg
        TileColorPreset.AMBER -> TileAmberAccent to TileAmberBg
        TileColorPreset.PLUM -> TilePlumAccent to TilePlumBg
    }

    val containerColor = if (isDarkTheme) {
        accentColor.copy(alpha = 0.22f)
    } else {
        lightSurface
    }
    val borderColor = if (isDarkTheme) accentColor else accentColor.copy(alpha = 0.45f)
    val labelColor = if (isDarkTheme) Color(0xFFF5F8F7) else Color(0xFF0E1B18)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        border = BorderStroke(2.5.dp, borderColor),
        tonalElevation = 3.dp,
        modifier = modifier
            .scale(scale)
            .minimumInteractiveComponentSize()
            .testTag("home_app_tile_${slot.config.slotIndex}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmap = remember(slot.appIcon) {
                slot.appIcon?.let { drawableToBitmap(it) }
            }

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = slot.config.bengaliLabel,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Icon(
                        imageVector = fallbackIconForSlot(slot),
                        contentDescription = slot.config.bengaliLabel,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = slot.config.bengaliLabel,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!slot.isInstalled) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "ইনস্টল নেই",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun fallbackIconForSlot(slot: ResolvedHomeSlot): ImageVector {
    if (slot.config.actionType == SlotActionType.PHONE_DIALER) {
        return Icons.Filled.Call
    }
    val pkg = slot.config.packageName.lowercase()
    return when {
        pkg.contains("whatsapp") -> Icons.AutoMirrored.Filled.Chat
        pkg.contains("facebook") -> Icons.Filled.Public
        pkg.contains("youtube") -> Icons.Filled.PlayArrow
        else -> Icons.Filled.Public
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
