package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.stopmotion.animator.model.AspectRatio
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun TopBar(state: EditorState) {
    var fpsDropdownOpen by remember { mutableStateOf(false) }
    var aspectDropdownOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(StudioDark)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: App Logo & Project Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MovieFilter,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column {
                Text(
                    text = "STOP MOTION STUDIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioPrimary,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = state.project.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StudioTextPrimary
                )
            }

            Spacer(Modifier.width(8.dp))

            // Undo / Redo
            StudioTooltip("Undo", "Cmd+Z") {
                IconButton(
                    onClick = { state.undo() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Undo, "Undo", tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            StudioTooltip("Redo", "Cmd+Shift+Z") {
                IconButton(
                    onClick = { state.redo() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Redo, "Redo", tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Center: Timing & Project Settings (FPS, Aspect Ratio)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // FPS Selector
            Box {
                StudioTooltip("Playback Frame Rate") {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { fpsDropdownOpen = true },
                        color = StudioCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Speed, null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
                            Text("${state.project.fps} FPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                DropdownMenu(
                    expanded = fpsDropdownOpen,
                    onDismissRequest = { fpsDropdownOpen = false }
                ) {
                    listOf(8, 12, 15, 24, 30).forEach { fps ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (fps) {
                                        12 -> "12 FPS (Stop-Motion Standard)"
                                        24 -> "24 FPS (Cinematic / On 2s)"
                                        else -> "$fps FPS"
                                    }
                                )
                            },
                            onClick = {
                                state.setFps(fps)
                                fpsDropdownOpen = false
                            }
                        )
                    }
                }
            }

            // Aspect Ratio Selector
            Box {
                StudioTooltip("Canvas Aspect Ratio") {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { aspectDropdownOpen = true },
                        color = StudioCard,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, null, tint = StudioSecondary, modifier = Modifier.size(16.dp))
                            Text(state.project.aspectRatio.label.split(" ").first(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                DropdownMenu(
                    expanded = aspectDropdownOpen,
                    onDismissRequest = { aspectDropdownOpen = false }
                ) {
                    AspectRatio.values().forEach { ratio ->
                        DropdownMenuItem(
                            text = { Text(ratio.label) },
                            onClick = {
                                state.setAspectRatio(ratio)
                                aspectDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Right: Import & Export Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Load Demo Animation
            StudioTooltip("Load Sample Animation") {
                OutlinedButton(
                    onClick = {
                        state.scope.launch { state.loadDemo() }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioTextSecondary)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = StudioWarning)
                    Spacer(Modifier.width(6.dp))
                    Text("Demo", fontSize = 12.sp)
                }
            }

            // Import Images / Frames
            StudioTooltip("Import Still Frames", "Cmd+I") {
                Button(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.isMultiSelectionEnabled = true
                        chooser.dialogTitle = "Import Frame Images"
                        chooser.fileFilter = FileNameExtensionFilter("Image Files (PNG, JPG, JPEG, WebP)", "png", "jpg", "jpeg", "webp")
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            state.addImportedImages(chooser.selectedFiles.toList())
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCard, contentColor = StudioTextPrimary)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(16.dp), tint = StudioSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Import Frames", fontSize = 12.sp)
                }
            }

            // Import Audio
            StudioTooltip("Import Soundtrack / Audio", "WAV/AIFF") {
                Button(
                    onClick = {
                        val chooser = JFileChooser()
                        chooser.dialogTitle = "Import Audio Track (WAV, AIFF)"
                        chooser.fileFilter = FileNameExtensionFilter("Audio Files (WAV, AIFF)", "wav", "aiff", "aif")
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            state.importAudioFile(chooser.selectedFile)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCard, contentColor = StudioTextPrimary)
                ) {
                    Icon(Icons.Default.AudioFile, null, modifier = Modifier.size(16.dp), tint = StudioAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Import Audio", fontSize = 12.sp)
                }
            }

            // Camera Capture
            StudioTooltip("Open Continuity Camera Studio", "C") {
                Button(
                    onClick = { state.isCameraCaptureDialogVisible = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCard, contentColor = StudioTextPrimary)
                ) {
                    Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp), tint = StudioAccent)
                    Spacer(Modifier.width(6.dp))
                    Text("Capture", fontSize = 12.sp)
                }
            }

            // Export Video
            StudioTooltip("Export Video / Animation", "Cmd+E") {
                Button(
                    onClick = { state.isExportDialogVisible = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Settings Button
            StudioTooltip("Preferences & Themes", "Cmd+,") {
                IconButton(
                    onClick = { state.isSettingsDialogVisible = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Preferences (Cmd+,)",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
