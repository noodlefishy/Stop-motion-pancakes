package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.stopmotion.animator.model.DoodleStyle
import com.stopmotion.animator.model.EditorTool
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*

@Composable
fun DoodleStudioBar(state: EditorState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
        color = StudioSurface.copy(alpha = 0.94f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Doodle Brush Style Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DoodleStyleButton(
                    label = "Pen",
                    icon = Icons.Default.Edit,
                    isSelected = state.doodleStyle == DoodleStyle.SOLID_PEN && state.activeTool == EditorTool.DRAW_PEN
                ) {
                    state.doodleStyle = DoodleStyle.SOLID_PEN
                    state.activeTool = EditorTool.DRAW_PEN
                }

                DoodleStyleButton(
                    label = "Glow",
                    icon = Icons.Default.AutoFixHigh,
                    isSelected = state.doodleStyle == DoodleStyle.NEON_GLOW || state.activeTool == EditorTool.DRAW_GLOW,
                    selectedColor = StudioSecondary
                ) {
                    state.doodleStyle = DoodleStyle.NEON_GLOW
                    state.activeTool = EditorTool.DRAW_GLOW
                }

                DoodleStyleButton(
                    label = "Chalk",
                    icon = Icons.Default.Brush,
                    isSelected = state.doodleStyle == DoodleStyle.SKETCH_CHALK || state.activeTool == EditorTool.DRAW_CHALK,
                    selectedColor = StudioWarning
                ) {
                    state.doodleStyle = DoodleStyle.SKETCH_CHALK
                    state.activeTool = EditorTool.DRAW_CHALK
                }

                DoodleStyleButton(
                    label = "Trail",
                    icon = Icons.Default.LinearScale,
                    isSelected = state.doodleStyle == DoodleStyle.DASHED_TRAIL || state.activeTool == EditorTool.DRAW_DASHED,
                    selectedColor = Color(0xFFEC4899)
                ) {
                    state.doodleStyle = DoodleStyle.DASHED_TRAIL
                    state.activeTool = EditorTool.DRAW_DASHED
                }
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(24.dp))

            // Color Palette
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val doodleColors = listOf(
                    Color(0xFFFF5E36), // Vibrant Orange
                    Color(0xFF38BDF8), // Laser Cyan
                    Color(0xFF10B981), // Emerald
                    Color(0xFFFACC15), // Comic Yellow
                    Color(0xFFEC4899), // Hot Pink
                    Color(0xFFA855F7), // Magic Purple
                    Color.White
                )

                doodleColors.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (state.brushColor == col) 2.5.dp else 0.dp,
                                color = if (col == Color.White) Color.Black else Color.White,
                                shape = CircleShape
                            )
                            .clickable { state.brushColor = col }
                    )
                }
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(24.dp))

            // Brush Size Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("${state.brushSize.toInt()}px", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioTextSecondary)
                Slider(
                    value = state.brushSize,
                    onValueChange = { state.brushSize = it },
                    valueRange = 2f..36f,
                    modifier = Modifier.width(70.dp)
                )
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(24.dp))

            // Quick Doodle Shape Stamps
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DoodleStampButton("★", "Star Stamp") { state.stampDoodle("STAR") }
                DoodleStampButton("⚡", "Lightning Stamp") { state.stampDoodle("LIGHTNING") }
                DoodleStampButton("💨", "Speed Lines") { state.stampDoodle("SPEED_LINES") }
                DoodleStampButton("♥", "Heart Stamp") { state.stampDoodle("HEART") }
            }

            VerticalDivider(color = StudioBorder, modifier = Modifier.height(24.dp))

            // Frame-to-frame workflow helpers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Copy Doodles to Next Frame (Key Stop-Motion Doodle feature)
                Button(
                    onClick = { state.copyDoodlesToNextFrame() },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCard, contentColor = StudioPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy to Next", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Undo Last Doodle Stroke
                IconButton(
                    onClick = { state.undoLastDoodleStroke() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Undo, "Undo Stroke", tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                }

                // Clear Doodles
                IconButton(
                    onClick = { state.clearFrameDrawings() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, "Clear Frame Doodles", tint = StudioTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DoodleStyleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color = StudioPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) selectedColor.copy(alpha = 0.25f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (isSelected) selectedColor else StudioTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectedColor else StudioTextSecondary
            )
        }
    }
}

@Composable
private fun DoodleStampButton(label: String, tooltip: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        color = StudioCard
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 13.sp)
        }
    }
}
