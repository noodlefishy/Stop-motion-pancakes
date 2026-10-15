package com.stopmotion.animator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.model.EditorTool
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*

@Composable
fun LeftToolbar(state: EditorState) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(StudioDark)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Editing Tools
        ToolIconButton(
            icon = Icons.Default.NearMe,
            tooltip = "Select / Pan Tool",
            isSelected = state.activeTool == EditorTool.SELECT,
            onClick = { state.activeTool = EditorTool.SELECT }
        )

        ToolIconButton(
            icon = Icons.Default.Edit,
            tooltip = "Doodle Pen (Draw on frame)",
            isSelected = state.activeTool == EditorTool.DRAW_PEN,
            onClick = {
                state.activeTool = EditorTool.DRAW_PEN
                state.doodleStyle = com.stopmotion.animator.model.DoodleStyle.SOLID_PEN
            }
        )

        ToolIconButton(
            icon = Icons.Default.AutoFixHigh,
            tooltip = "Neon Glow / Lightsaber FX",
            isSelected = state.activeTool == EditorTool.DRAW_GLOW,
            selectedColor = StudioSecondary,
            onClick = {
                state.activeTool = EditorTool.DRAW_GLOW
                state.doodleStyle = com.stopmotion.animator.model.DoodleStyle.NEON_GLOW
            }
        )

        ToolIconButton(
            icon = Icons.Default.Brush,
            tooltip = "Sketch Chalk Doodle",
            isSelected = state.activeTool == EditorTool.DRAW_CHALK,
            selectedColor = StudioWarning,
            onClick = {
                state.activeTool = EditorTool.DRAW_CHALK
                state.doodleStyle = com.stopmotion.animator.model.DoodleStyle.SKETCH_CHALK
            }
        )

        ToolIconButton(
            icon = Icons.Default.LinearScale,
            tooltip = "Dashed Speed Trail",
            isSelected = state.activeTool == EditorTool.DRAW_DASHED,
            selectedColor = Color(0xFFEC4899),
            onClick = {
                state.activeTool = EditorTool.DRAW_DASHED
                state.doodleStyle = com.stopmotion.animator.model.DoodleStyle.DASHED_TRAIL
            }
        )

        ToolIconButton(
            icon = Icons.Default.Healing,
            tooltip = "Rig Removal (Clean Plate inpaint)",
            isSelected = state.activeTool == EditorTool.RIG_REMOVAL,
            selectedColor = StudioAccent,
            onClick = { state.activeTool = EditorTool.RIG_REMOVAL }
        )

        ToolIconButton(
            icon = Icons.Default.Tune,
            tooltip = "Color & Exposure / De-flicker",
            isSelected = state.activeTool == EditorTool.COLOR_GRADE,
            onClick = { state.activeTool = EditorTool.COLOR_GRADE }
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = StudioBorder
        )

        // Visual Guides & Stop-Motion Features
        GuideIconButton(
            icon = Icons.Default.Layers,
            tooltip = "Onion Skinning",
            isActive = state.onionSkinEnabled,
            activeColor = StudioPrimary,
            onClick = { state.onionSkinEnabled = !state.onionSkinEnabled }
        )

        GuideIconButton(
            icon = Icons.Default.GridOn,
            tooltip = "Rule of Thirds Grid",
            isActive = state.showGrid,
            activeColor = StudioSecondary,
            onClick = { state.showGrid = !state.showGrid }
        )

        GuideIconButton(
            icon = Icons.Default.FilterFrames,
            tooltip = "Safe Zones & Center",
            isActive = state.showSafeZone,
            activeColor = StudioSecondary,
            onClick = {
                state.showSafeZone = !state.showSafeZone
                state.showCenterCross = state.showSafeZone
            }
        )

        GuideIconButton(
            icon = Icons.Default.CompareArrows,
            tooltip = "Split Screen Compare",
            isActive = state.splitCompare,
            activeColor = StudioWarning,
            onClick = { state.splitCompare = !state.splitCompare }
        )

        GuideIconButton(
            icon = Icons.Default.Difference,
            tooltip = "Flicker / Difference Mode",
            isActive = state.differenceMode,
            activeColor = Color(0xFFEC4899),
            onClick = { state.differenceMode = !state.differenceMode }
        )
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    tooltip: String,
    isSelected: Boolean,
    selectedColor: Color = StudioPrimary,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isSelected) selectedColor.copy(alpha = 0.25f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isSelected) selectedColor else StudioTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun GuideIconButton(
    icon: ImageVector,
    tooltip: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .background(
                if (isActive) activeColor.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isActive) activeColor else StudioTextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}
