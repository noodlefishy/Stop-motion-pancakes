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
import com.stopmotion.animator.model.AppThemePreset
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
        SettingsCategory("Appearance & Themes", Icons.Default.Palette),
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
                .width(780.dp)
                .height(580.dp)
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
                            .width(210.dp)
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
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
                            .padding(22.dp)
                    ) {
                        when (selectedCategory) {
                            0 -> AppearanceSettingsPanel(currentSettings) { updated ->
                                currentSettings = updated
                                CurrentStudioPalette = StudioPalettes.get(updated.themePreset)
                            }
                            1 -> CameraSettingsPanel(currentSettings) { currentSettings = it }
                            2 -> OnionSkinSettingsPanel(currentSettings) { currentSettings = it }
                            3 -> TimelineSettingsPanel(currentSettings) { currentSettings = it }
                            4 -> DoodleSettingsPanel(currentSettings) { currentSettings = it }
                            5 -> ExportSettingsPanel(currentSettings) { currentSettings = it }
                            6 -> SystemSettingsPanel(state) { currentSettings = it }
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
                        CurrentStudioPalette = StudioPalettes.get(def.themePreset)
                    }) {
                        Text("Reset to Defaults", color = StudioTextMuted, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                // Revert live preview if canceled
                                CurrentStudioPalette = StudioPalettes.get(state.settings.themePreset)
                                state.isSettingsDialogVisible = false
                            },
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
private fun AppearanceSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("APPEARANCE & COLOR THEMES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Choose a curated visual theme for the stop motion animation workspace.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppThemePreset.values().forEach { preset ->
                    val isSelected = settings.themePreset == preset
                    val palette = StudioPalettes.get(preset)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StudioPrimary else StudioBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onUpdate(settings.copy(themePreset = preset))
                            },
                        color = if (isSelected) StudioCard else StudioDark
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Theme Color Swatches Preview
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.dark)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.surface)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.secondary)
                                    )
                                }

                                Column {
                                    Text(
                                        text = preset.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) StudioPrimary else StudioTextPrimary
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 11.sp,
                                        color = StudioTextSecondary
                                    )
                                }
                            }

                            if (isSelected) {
                                Surface(
                                    color = StudioPrimary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, StudioBorder, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("CAMERA & TETHER PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Configure Continuity Camera defaults and capture triggers.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingInlineRow(
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
            SettingStackedRow(
                title = "Countdown Timer Duration",
                subtitle = "Self-timer delay before snapping frame"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Off", 2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                        FilterChip(
                            selected = settings.defaultCountdownSeconds == sec,
                            onClick = { onUpdate(settings.copy(defaultCountdownSeconds = sec)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnionSkinSettingsPanel(settings: AppSettings, onUpdate: (AppSettings) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("ONION SKIN & COMPOSITION GUIDES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Tune ghosting transparency and tint for seamless motion alignment.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingSliderRow(
                title = "Default Onion Skin Opacity",
                valueLabel = "${(settings.defaultOnionOpacity * 100).toInt()}%",
                value = settings.defaultOnionOpacity,
                valueRange = 0.1f..0.9f,
                onValueChange = { onUpdate(settings.copy(defaultOnionOpacity = it)) }
            )
        }

        item {
            SettingStackedRow(
                title = "Past Frames Count",
                subtitle = "Number of preceding frames ghosted in onion skin"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..4).forEach { count ->
                        FilterChip(
                            selected = settings.defaultOnionPastCount == count,
                            onClick = { onUpdate(settings.copy(defaultOnionPastCount = count)) },
                            label = { Text("$count frame${if (count > 1) "s" else ""}", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingStackedRow(
                title = "Color Tint Mode",
                subtitle = "Color differentiation for past vs future frames"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        OnionTintMode.RED_GREEN_TINT to "Red / Green Tint",
                        OnionTintMode.TRUE_COLOR to "True Color",
                        OnionTintMode.MONOCHROME to "Monochrome"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.defaultTintMode == mode,
                            onClick = { onUpdate(settings.copy(defaultTintMode = mode)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingSwitchRow(
                title = "Show Rule-of-Thirds Grid by Default",
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
            SettingStackedRow(
                title = "Default Project FPS",
                subtitle = "Standard animation frame rate"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(8, 12, 15, 24, 30).forEach { fps ->
                        FilterChip(
                            selected = settings.defaultFps == fps,
                            onClick = { onUpdate(settings.copy(defaultFps = fps)) },
                            label = { Text("${fps} FPS", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingStackedRow(
                title = "Default Frame Hold (Exposures)",
                subtitle = "Hold duration for newly captured/imported frames"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1s (Singles)", 2 to "2s (Doubles)", 3 to "3s (Triples)").forEach { (h, label) ->
                        FilterChip(
                            selected = settings.defaultHoldDuration == h,
                            onClick = { onUpdate(settings.copy(defaultHoldDuration = h)) },
                            label = { Text(label, fontSize = 11.sp) }
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
            SettingStackedRow(
                title = "Default Doodle Style",
                subtitle = "Active brush style on pen launch"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        DoodleStyle.SOLID_PEN to "Pen",
                        DoodleStyle.NEON_GLOW to "Glow",
                        DoodleStyle.SKETCH_CHALK to "Chalk",
                        DoodleStyle.DASHED_TRAIL to "Trail"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = settings.defaultDoodleStyle == style,
                            onClick = { onUpdate(settings.copy(defaultDoodleStyle = style)) },
                            label = { Text(label, fontSize = 11.sp) }
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
            Text("DEFAULT STOP MOTION WORKSPACE PATH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Set the default folder where all projects, captures, and exports are stored.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            Surface(
                color = StudioDark,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderSpecial, null, tint = StudioPrimary, modifier = Modifier.size(20.dp))
                        Text(settings.defaultProjectsPath, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = StudioTextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val chosen = com.stopmotion.animator.util.NativeFileDialogs.chooseDirectory(
                                    title = "Choose Default Stop Motion Folder",
                                    defaultDirectory = settings.defaultProjectsPath
                                )
                                if (chosen != null) {
                                    onUpdate(settings.copy(defaultProjectsPath = chosen.absolutePath))
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCard)
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Browse Folder...", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val dir = java.io.File(settings.defaultProjectsPath)
                                dir.mkdirs()
                                if (java.awt.Desktop.isDesktopSupported()) {
                                    java.awt.Desktop.getDesktop().open(dir)
                                }
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open in Finder", fontSize = 11.sp)
                        }

                        TextButton(
                            onClick = {
                                val defaultDir = java.io.File(System.getProperty("user.home"), "StopMotionProjects").absolutePath
                                onUpdate(settings.copy(defaultProjectsPath = defaultDir))
                            }
                        ) {
                            Text("Reset", fontSize = 11.sp, color = StudioTextMuted)
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = StudioBorder)
            Spacer(Modifier.height(8.dp))
            Text("EXPORT & PROJECT STORAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Video encoding quality presets and autosave settings.", fontSize = 11.sp, color = StudioTextSecondary)
        }

        item {
            SettingStackedRow(
                title = "Default Export Format",
                subtitle = "Primary output option"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MP4", "GIF", "ZIP").forEach { fmt ->
                        FilterChip(
                            selected = settings.defaultExportFormat == fmt,
                            onClick = { onUpdate(settings.copy(defaultExportFormat = fmt)) },
                            label = { Text(fmt, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        item {
            SettingStackedRow(
                title = "H.264 Video Quality (CRF)",
                subtitle = "Lower values yield higher visual quality and larger file size"
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        18 to "High (CRF 18)",
                        23 to "Balanced (CRF 23)",
                        28 to "Compact (CRF 28)"
                    ).forEach { (crf, label) ->
                        FilterChip(
                            selected = settings.exportQualityCrf == crf,
                            onClick = { onUpdate(settings.copy(exportQualityCrf = crf)) },
                            label = { Text(label, fontSize = 11.sp) }
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
            SettingInlineRow(
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
private fun SettingStackedRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(subtitle, fontSize = 11.sp, color = StudioTextSecondary)
        }
        control()
    }
}

@Composable
private fun SettingInlineRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(subtitle, fontSize = 11.sp, color = StudioTextSecondary)
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
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StudioTextPrimary)
            Text(subtitle, fontSize = 11.sp, color = StudioTextSecondary)
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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
