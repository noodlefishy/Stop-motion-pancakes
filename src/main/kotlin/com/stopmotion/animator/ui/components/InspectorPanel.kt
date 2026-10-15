package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.model.EditorTool
import com.stopmotion.animator.model.OnionTintMode
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*

@Composable
fun InspectorPanel(state: EditorState, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Settings", "Frame", "X-Sheet", "Audio")

    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(StudioSurface)
            .border(width = 1.dp, color = StudioBorder)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioDark,
            contentColor = StudioPrimary,
            divider = { HorizontalDivider(color = StudioBorder) }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) StudioPrimary else StudioTextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (selectedTab) {
                0 -> SettingsTabContent(state)
                1 -> FrameTabContent(state)
                2 -> XSheetTabContent(state)
                3 -> AudioTabContent(state)
            }
        }
    }
}

@Composable
private fun SettingsTabContent(state: EditorState) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Onion Skin Section
        item {
            SectionHeader(title = "ONION SKINNING", icon = Icons.Default.Layers)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable Ghosting", fontSize = 12.sp, color = StudioTextPrimary)
                Switch(
                    checked = state.onionSkinEnabled,
                    onCheckedChange = { state.onionSkinEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = StudioPrimary, checkedTrackColor = StudioPrimary.copy(alpha = 0.4f))
                )
            }

            if (state.onionSkinEnabled) {
                Spacer(Modifier.height(6.dp))

                // Past frames slider
                Text("Past Frames: ${state.onionSkinPastCount}", fontSize = 11.sp, color = StudioTextSecondary)
                Slider(
                    value = state.onionSkinPastCount.toFloat(),
                    onValueChange = { state.onionSkinPastCount = it.toInt() },
                    valueRange = 1f..5f,
                    steps = 3
                )

                // Future frames slider
                Text("Future Frames: ${state.onionSkinFutureCount}", fontSize = 11.sp, color = StudioTextSecondary)
                Slider(
                    value = state.onionSkinFutureCount.toFloat(),
                    onValueChange = { state.onionSkinFutureCount = it.toInt() },
                    valueRange = 0f..5f,
                    steps = 4
                )

                // Opacity slider
                Text("Ghost Opacity: ${(state.onionSkinOpacity * 100).toInt()}%", fontSize = 11.sp, color = StudioTextSecondary)
                Slider(
                    value = state.onionSkinOpacity,
                    onValueChange = { state.onionSkinOpacity = it },
                    valueRange = 0.1f..0.9f
                )

                // Tint mode
                Text("Tint Color Mode", fontSize = 11.sp, color = StudioTextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = state.onionTintMode == OnionTintMode.RED_GREEN_TINT,
                        onClick = { state.onionTintMode = OnionTintMode.RED_GREEN_TINT },
                        label = { Text("Red/Green", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = state.onionTintMode == OnionTintMode.TRUE_COLOR,
                        onClick = { state.onionTintMode = OnionTintMode.TRUE_COLOR },
                        label = { Text("True Color", fontSize = 10.sp) }
                    )
                }
            }
        }

        // Brush & Paint Settings
        item {
            HorizontalDivider(color = StudioBorder)
            Spacer(Modifier.height(8.dp))
            SectionHeader(title = "BRUSH & RIG REMOVAL", icon = Icons.Default.Brush)
            Spacer(Modifier.height(8.dp))

            Text("Brush Size: ${state.brushSize.toInt()} px", fontSize = 11.sp, color = StudioTextSecondary)
            Slider(
                value = state.brushSize,
                onValueChange = { state.brushSize = it },
                valueRange = 2f..40f
            )

            if (state.activeTool != EditorTool.RIG_REMOVAL) {
                Text("Color Palette", fontSize = 11.sp, color = StudioTextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        Color(0xFFFF5E36), // Studio Orange
                        Color(0xFF38BDF8), // Cyan Laser
                        Color(0xFF10B981), // Emerald
                        Color(0xFFFACC15), // Yellow Sparks
                        Color(0xFFEC4899), // Pink
                        Color.White
                    )
                    colors.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (state.brushColor == col) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { state.brushColor = col }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { state.clearFrameDrawings() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear Frame Drawings", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FrameTabContent(state: EditorState) {
    val frame = state.selectedFrame

    if (frame == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No frame selected", color = StudioTextMuted, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Frame Timing & Hold Duration
        item {
            SectionHeader(title = "EXPOSURE & TIMING", icon = Icons.Default.Timer)
            Spacer(Modifier.height(8.dp))
            Text("Frame Hold (Exposure): ${frame.holdDuration} beat(s)", fontSize = 12.sp, color = StudioTextPrimary)
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2, 3, 4).forEach { hold ->
                    Button(
                        onClick = { state.setHoldForSelected(hold) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (frame.holdDuration == hold) StudioPrimary else StudioCard,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            when (hold) {
                                1 -> "1s"
                                2 -> "2s"
                                3 -> "3s"
                                else -> "${hold}s"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Color & Exposure Adjustment
        item {
            HorizontalDivider(color = StudioBorder)
            Spacer(Modifier.height(8.dp))
            SectionHeader(title = "COLOR & DE-FLICKER", icon = Icons.Default.Tune)
            Spacer(Modifier.height(8.dp))

            // Brightness / Exposure offset
            Text("Brightness Offset: %+.2f".format(frame.brightness), fontSize = 11.sp, color = StudioTextSecondary)
            Slider(
                value = frame.brightness,
                onValueChange = { state.updateFrameColor(brightness = it) },
                valueRange = -0.5f..0.5f
            )

            // Contrast
            Text("Contrast: %.2f".format(frame.contrast), fontSize = 11.sp, color = StudioTextSecondary)
            Slider(
                value = frame.contrast,
                onValueChange = { state.updateFrameColor(contrast = it) },
                valueRange = 0.5f..1.8f
            )

            Spacer(Modifier.height(4.dp))

            // Auto De-Flicker Button
            Button(
                onClick = { state.autoDeFlicker() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StudioSecondary, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Auto Match Exposure (De-flicker)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Rig Removal Clean Plate Reference
        item {
            HorizontalDivider(color = StudioBorder)
            Spacer(Modifier.height(8.dp))
            SectionHeader(title = "RIG REMOVAL", icon = Icons.Default.Healing)
            Spacer(Modifier.height(8.dp))

            val isCleanPlate = state.project.cleanPlateFrameId == frame.id
            Button(
                onClick = { state.setCleanPlateForProject(frame.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCleanPlate) StudioAccent else StudioCard,
                    contentColor = if (isCleanPlate) Color.Black else StudioTextPrimary
                )
            ) {
                Icon(if (isCleanPlate) Icons.Default.CheckCircle else Icons.Default.Panorama, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isCleanPlate) "Active Clean Plate" else "Set as Clean Plate Reference", fontSize = 11.sp)
            }
        }

        // Delete Frame Action
        item {
            HorizontalDivider(color = StudioBorder)
            Spacer(Modifier.height(8.dp))
            SectionHeader(title = "FRAME ACTIONS", icon = Icons.Default.DeleteOutline)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { state.deleteSelectedFrames() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    contentColor = Color(0xFFEF4444)
                )
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete Frame #${state.selectedFrameIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun XSheetTabContent(state: EditorState) {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "EXPOSURE SHEET (X-SHEET)", icon = Icons.Default.TableChart)
        Spacer(Modifier.height(8.dp))
        Text("Dope sheet for phoneme lip-sync and action notes.", fontSize = 11.sp, color = StudioTextMuted)
        Spacer(Modifier.height(8.dp))

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioDark)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("FRAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioTextMuted, modifier = Modifier.width(50.dp))
            Text("PHONEME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioPrimary, modifier = Modifier.width(60.dp))
            Text("ACTION NOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioSecondary, modifier = Modifier.weight(1f))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.project.frames.size) { idx ->
                val isSelected = state.selectedFrameIndex == idx
                val row = state.project.xSheetRows.find { it.frameIndex == idx }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) StudioPrimary.copy(alpha = 0.2f) else StudioCard)
                        .clickable { state.selectFrame(idx) }
                        .padding(vertical = 5.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#%03d".format(idx + 1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) StudioPrimary else StudioTextSecondary,
                        modifier = Modifier.width(50.dp)
                    )

                    Text(
                        text = row?.phoneme?.ifEmpty { "—" } ?: "—",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (row?.phoneme?.isNotEmpty() == true) StudioPrimary else StudioTextMuted,
                        modifier = Modifier.width(60.dp)
                    )

                    Text(
                        text = row?.actionNote?.ifEmpty { "—" } ?: "—",
                        fontSize = 11.sp,
                        color = StudioTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioTabContent(state: EditorState) {
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "AUDIO TRACKS", icon = Icons.Default.Audiotrack)
        Spacer(Modifier.height(8.dp))

        if (state.project.audioTracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No audio tracks loaded.\nImport a WAV file to start.", color = StudioTextMuted, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.project.audioTracks) { track ->
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(track.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                                IconButton(
                                    onClick = {
                                        val nextTracks = state.project.audioTracks.map {
                                            if (it.id == track.id) it.copy(isMuted = !it.isMuted) else it
                                        }
                                        state.project = state.project.copy(audioTracks = nextTracks)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        null,
                                        tint = if (track.isMuted) StudioWarning else StudioAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text("Volume: ${(track.volume * 100).toInt()}%", fontSize = 10.sp, color = StudioTextSecondary)
                            Slider(
                                value = track.volume,
                                onValueChange = { v ->
                                    val nextTracks = state.project.audioTracks.map {
                                        if (it.id == track.id) it.copy(volume = v) else it
                                    }
                                    state.project = state.project.copy(audioTracks = nextTracks)
                                },
                                valueRange = 0f..1f
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary,
            letterSpacing = 0.8.sp
        )
    }
}
