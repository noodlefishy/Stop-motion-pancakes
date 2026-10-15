package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.NativeFileDialogs
import java.io.File

@Composable
fun ExportDialog(state: EditorState) {
    if (!state.isExportDialogVisible) return

    var selectedFormat by remember { mutableStateOf("MP4") }
    var outputPath by remember {
        mutableStateOf(
            File(System.getProperty("user.home"), "Desktop/animation_export.mp4").absolutePath
        )
    }

    Dialog(onDismissRequest = {
        if (!state.isExporting) state.isExportDialogVisible = false
    }) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = StudioSurface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.MovieCreation, null, tint = StudioPrimary, modifier = Modifier.size(24.dp))
                    Text("Export Animation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                }

                Text(
                    "Export all ${state.project.frames.size} frames (${state.project.totalTimelineFrames} exposures at ${state.project.fps} FPS) with doodle layers and audio.",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                // Format Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExportFormatCard(
                        title = "MP4 Video",
                        subtitle = "H.264 + Audio",
                        isSelected = selectedFormat == "MP4",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedFormat = "MP4"
                        outputPath = outputPath.replace(Regex("\\.[a-zA-Z0-9]+$"), ".mp4")
                    }

                    ExportFormatCard(
                        title = "Animated GIF",
                        subtitle = "Optimized 256c",
                        isSelected = selectedFormat == "GIF",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedFormat = "GIF"
                        outputPath = outputPath.replace(Regex("\\.[a-zA-Z0-9]+$"), ".gif")
                    }

                    ExportFormatCard(
                        title = "Image ZIP",
                        subtitle = "PNG Sequence",
                        isSelected = selectedFormat == "ZIP",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedFormat = "ZIP"
                        outputPath = outputPath.replace(Regex("\\.[a-zA-Z0-9]+$"), ".zip")
                    }
                }

                // Output File Location Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Save To", fontSize = 11.sp, color = StudioTextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = outputPath,
                            onValueChange = { outputPath = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !state.isExporting,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = StudioDark,
                                unfocusedContainerColor = StudioDark,
                                focusedTextColor = StudioTextPrimary,
                                unfocusedTextColor = StudioTextPrimary
                            )
                        )
                        Button(
                            onClick = {
                                val extension = when (selectedFormat) {
                                    "GIF" -> "gif"
                                    "ZIP" -> "zip"
                                    else -> "mp4"
                                }
                                val defaultFile = File(outputPath)
                                val chosen = NativeFileDialogs.saveExport(
                                    defaultName = defaultFile.name,
                                    extension = extension
                                )
                                if (chosen != null) {
                                    outputPath = chosen.absolutePath
                                }
                            },
                            enabled = !state.isExporting,
                            colors = ButtonDefaults.buttonColors(containerColor = StudioCard)
                        ) {
                            Text("Browse", fontSize = 11.sp)
                        }
                    }
                }

                // Progress Bar if exporting
                if (state.isExporting) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress = { state.exportProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = StudioPrimary,
                            trackColor = StudioDark
                        )
                        Text(state.exportMessage, fontSize = 11.sp, color = StudioPrimary)
                    }
                }

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { state.isExportDialogVisible = false },
                        enabled = !state.isExporting
                    ) {
                        Text("Cancel", color = StudioTextSecondary)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            state.startExport(selectedFormat, File(outputPath))
                        },
                        enabled = !state.isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioPrimary)
                    ) {
                        Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (state.isExporting) "Exporting..." else "Start Export", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudioPrimary else StudioBorder,
                shape = RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) StudioPrimary.copy(alpha = 0.15f) else StudioCard
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) StudioPrimary else StudioTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = StudioTextSecondary)
        }
    }
}
