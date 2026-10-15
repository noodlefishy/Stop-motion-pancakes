package com.stopmotion.animator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.ui.theme.StudioBorder
import com.stopmotion.animator.ui.theme.StudioCard

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudioTooltip(
    text: String,
    shortcut: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (text.isBlank()) {
        content()
        return
    }

    TooltipArea(
        modifier = modifier,
        delayMillis = 350,
        tooltipPlacement = TooltipPlacement.CursorPoint(
            alignment = Alignment.BottomCenter,
            offset = DpOffset(0.dp, 16.dp)
        ),
        tooltip = {
            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(6.dp),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    if (shortcut != null) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = shortcut,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}
