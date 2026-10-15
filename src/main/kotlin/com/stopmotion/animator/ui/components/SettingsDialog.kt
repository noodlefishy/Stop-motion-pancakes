package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stopmotion.animator.model.AppSettings
import com.stopmotion.animator.model.DoodleStyle
import com.stopmotion.animator.model.OnionTintMode
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.ImageCache

@Composable
fun SettingsDialog(state: EditorState) {
    if (!state.isSettingsDialogVisible) return

    var currentSettings by remember { mutableStateOf(state.settings) }
    var selectedCategory by remember { mutableStateOf(0) }

    val categories = listOf(
        SettingsCategory("Camera & Capture", Icons.Default.Videocam),
        SettingsCategory("Onion Skin & View", Icons.Default.Layers),
        SettingsCategory("Timeline & Playback", Icons.Default.Speed),
        SettingsCategory("Doodles & Paint", Icons.Default.Brush),
        SettingsCategory("Export & Storage", Icons.Default.Save),
        SettingsCategory("System & Cache", Icons.Default.Memory)
    )

    Dialog(onDismissRequest = { state.isSettingsDialogVisible = false }) {
        Surface(
            modifier = Modifier
                .width(720.dp)
                .height(520.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = StudioSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(StudioDark)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                        Text("Preferences & Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                    }

                    IconButton(
                        onClick = { state.isSettingsDialogVisible = false },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Middle: Sidebar Categories + Detail Panel
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Left Navigation Column
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .background(StudioDark)
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEachIndexed { index, cat ->
                            val isSelected = selectedCategory == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedCategory = index },
                                color = if (isSelected) StudioPrimary.copy(alpha = 0.2f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        cat.icon,
                                        null,
                                        tint = if (isSelected) StudioPrimary else StudioTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = cat.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StudioPrimary else StudioTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    VerticalDivider(color = StudioBorder)

                    // Right Detail Panel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(20.dp)
                    ) {
                        when (selectedCategory) {
                            0 -> CameraSettingsPanel(currentSettings) { currentSettings = it }
                            1 -> OnionSkinSettingsPanel(currentSettings) { currentSettings = it }
                            2 -> TimelineSettingsPanel(currentSettings) { currentSettings = it }
                            3 -> DoodleSettingsPanel(currentSettings) { currentSettings = it }
                            4 -> ExportSettingsPanel(currentSettings) { currentSettings = it }
                            5 -> SystemSettingsPanel(state) { currentSettings = it }
                        }
                    }
                }

                HorizontalDivider(color = StudioBorder)

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(StudioDark)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val def = AppSettings()
                        currentSettings = def
                        state.resetSettingsToDefault()
                    }) {
                        Text("Reset to Defaults", color = StudioTextMuted, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { state.isSettingsDialogVisible = false },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                state.updateSettings(currentSettings)
                                state.isSettingsDialogVisible = false
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save & Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private data class SettingsCategory(val title: String, val icon: ImageVector)

@Composable
private fun CameraSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("CAMERA & TETHER PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Configure Continuity Camera defaults and capture triggers.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingRow(
                title = "Default Capture Resolution",
                subtitle = "Preferred resolution for stills from Continuity Camera"
            ) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        color = StudioDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { menuOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(settings.defaultResolution, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioSecondary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        listOf("1920x1080" to "1080p Full HD (Continuity)", "1280x720" to "720p HD", "3840x2160" to "4K Ultra HD").forEach { (res, label) ->
                            DropdownMenuItem(text = { Text(label, fontSize = 11.sp) }, onClick = {
                                onUpdate(settings.copy(defaultResolution = res))
                                menuOpen = false
                            })
                        }
                    }
                }
            }
        }

        item {
            SettingSwitchRow(
                title = "Camera Shutter Sound",
                subtitle = "Play an audible click when snapping a frame",
                checked = settings.playShutterSound,
                onCheckedChange = { onUpdate(settings.copy(playShutterSound = it)) }
            )
        }

        item {
            SettingSwitchRow(
                title = "Auto-Advance Playhead",
                subtitle = "Automatically step to the newly captured frame in timeline",
                checked = settings.autoAdvancePlayheadOnSnap,
                onCheckedChange = { onUpdate(settings.copy(autoAdvancePlayheadOnSnap = it)) }
            )
        }

        item {
            SettingSliderRow(
                title = "Default Countdown Timer",
                valueLabel = "${settings.defaultCountdownSeconds}s",
                value = settings.defaultCountdownSeconds.toFloat(),
                valueRange = 0f..5f,
                steps = 4,
                onValueChange = { onUpdate(settings.copy(defaultCountdownSeconds = it.toInt())) }
            )
        }
    }
}

@Composable
private fun OnionSkinSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("ONION SKIN & COMPOSITION GUIDES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Fine-tune how past and future frames ghost over your viewport.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingSliderRow(
                title = "Default Past Frames",
                valueLabel = "${settings.defaultOnionPastCount} frames",
                value = settings.defaultOnionPastCount.toFloat(),
                valueRange = 1f..5f,
                steps = 3,
                onValueChange = { onUpdate(settings.copy(defaultOnionPastCount = it.toInt())) }
            )
        }

        item {
            SettingSliderRow(
                title = "Default Future Frames",
                valueLabel = "${settings.defaultOnionFutureCount} frames",
                value = settings.defaultOnionFutureCount.toFloat(),
                valueRange = 0f..5f,
                steps = 4,
                onValueChange = { onUpdate(settings.copy(defaultOnionFutureCount = it.toInt())) }
            )
        }

        item {
            SettingSliderRow(
                title = "Default Ghost Opacity",
                valueLabel = "${(settings.defaultOnionOpacity * 100).toInt()}%",
                value = settings.defaultOnionOpacity,
                valueRange = 0.1f..0.9f,
                steps = 7,
                onValueChange = { onUpdate(settings.copy(defaultOnionOpacity = it)) }
            )
        }

        item {
            SettingRow(title = "Default Tint Color Mode", subtitle = "Ghost color scheme") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.defaultTintMode == OnionTintMode.RED_GREEN_TINT,
                        onClick = { onUpdate(settings.copy(defaultTintMode = OnionTintMode.RED_GREEN_TINT)) },
                        label = { Text("Red / Green", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = settings.defaultTintMode == OnionTintMode.TRUE_COLOR,
                        onClick = { onUpdate(settings.copy(defaultTintMode = OnionTintMode.TRUE_COLOR)) },
                        label = { Text("True Color", fontSize = 10.sp) }
                    )
                }
            }
        }

        item {
            SettingSwitchRow(
                title = "Show Rule-of-Thirds Grid on Launch",
                subtitle = "Display 3x3 alignment lines automatically",
                checked = settings.showGridByDefault,
                onCheckedChange = { onUpdate(settings.copy(showGridByDefault = it)) }
            )
        }
    }
}

@Composable
private fun TimelineSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("TIMELINE & PLAYBACK PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Control frame rate timing, hold exposures, and audio synchronization.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingRow(title = "Default Project FPS", subtitle = "Standard animation frame rate") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(8, 12, 15, 24, 30).forEach { fps ->
                        FilterChip(
                            selected = settings.defaultFps == fps,
                            onClick = { onUpdate(settings.copy(defaultFps = fps)) },
                            label = { Text("${fps} FPS", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingRow(title = "Default Frame Hold (Exposures)", subtitle = "Hold duration for newly captured/imported frames") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "1s (Singles)", 2 to "2s (Doubles)", 3 to "3s (Triples)").forEach { (h, label) ->
                        FilterChip(
                            selected = settings.defaultHoldDuration == h,
                            onClick = { onUpdate(settings.copy(defaultHoldDuration = h)) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingSwitchRow(
                title = "Audio Scrubbing",
                subtitle = "Play short audio snippets when moving the playhead for lip-syncing",
                checked = settings.audioScrubbingEnabled,
                onCheckedChange = { onUpdate(settings.copy(audioScrubbingEnabled = it)) }
            )
        }

        item {
            SettingSwitchRow(
                title = "Loop Playback by Default",
                subtitle = "Automatically loop animation playback seamlessly",
                checked = settings.loopPlaybackByDefault,
                onCheckedChange = { onUpdate(settings.copy(loopPlaybackByDefault = it)) }
            )
        }
    }
}

@Composable
private fun DoodleSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("DOODLE & ROTOSCOPING DEFAULTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Defaults for mixed-media hand-drawn animation on frames.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingRow(title = "Default Doodle Style", subtitle = "Active brush style on pen launch") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        DoodleStyle.SOLID_PEN to "Pen",
                        DoodleStyle.NEON_GLOW to "Glow",
                        DoodleStyle.SKETCH_CHALK to "Chalk",
                        DoodleStyle.DASHED_TRAIL to "Trail"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = settings.defaultDoodleStyle == style,
                            onClick = { onUpdate(settings.copy(defaultDoodleStyle = style)) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingSliderRow(
                title = "Default Brush Size",
                valueLabel = "${settings.defaultBrushSize.toInt()} px",
                value = settings.defaultBrushSize,
                valueRange = 2f..30f,
                steps = 13,
                onValueChange = { onUpdate(settings.copy(defaultBrushSize = it)) }
            )
        }
    }
}

@Composable
private fun ExportSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("EXPORT & PROJECT STORAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Video encoding quality presets and autosave settings.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingRow(title = "Default Export Format", subtitle = "Primary output option") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MP4", "GIF", "ZIP").forEach { fmt ->
                        FilterChip(
                            selected = settings.defaultExportFormat == fmt,
                            onClick = { onUpdate(settings.copy(defaultExportFormat = fmt)) },
                            label = { Text(fmt, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingRow(title = "H.264 Video Quality (CRF)", subtitle = "Lower values yield higher visual quality") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        18 to "High (CRF 18)",
                        23 to "Balanced (CRF 23)",
                        28 to "Compact (CRF 28)"
                    ).forEach { (crf, label) ->
                        FilterChip(
                            selected = settings.exportQualityCrf == crf,
                            onClick = { onUpdate(settings.copy(exportQualityCrf = crf)) },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingSliderRow(
                title = "Auto-Save Interval",
                valueLabel = if (settings.autoSaveMinutes > 0) "${settings.autoSaveMinutes} min" else "Disabled",
                value = settings.autoSaveMinutes.toFloat(),
                valueRange = 0f..15f,
                steps = 2,
                onValueChange = { onUpdate(settings.copy(autoSaveMinutes = it.toInt())) }
            )
        }
    }
}

@Composable
private fun SystemSettingsPanel(state: EditorState, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("SYSTEM & PERFORMANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Hardware acceleration and runtime memory management.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, null, tint = StudioAccent, modifier = Modifier.size(18.dp))
                        Text("Skiko GPU Renderer: Metal (macOS Hardware Accelerated)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                    }
                    Text("Zero CPU overhead viewport rendering with Metal hardware pipelines.", fontSize = 11.sp, color = StudioTextSecondary)
                }
            }
        }

        item {
            SettingRow(
                title = "Thumbnail Memory Cache",
                subtitle = "Clear cached frame bitmaps from RAM"
            ) {
                Button(
                    onClick = {
                        ImageCache.clear()
                        state.showToast("Memory thumbnail cache cleared")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioDark),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Purge Cache", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(subtitle, fontSize = 10.sp, color = StudioTextSecondary)
        }
        control()
    }
}

@Composable
private fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(subtitle, fontSize = 10.sp, color = StudioTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StudioPrimary,
                checkedTrackColor = StudioPrimary.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun SettingSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(valueLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
