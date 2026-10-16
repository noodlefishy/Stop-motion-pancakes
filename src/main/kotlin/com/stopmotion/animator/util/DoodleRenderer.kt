package com.stopmotion.animator.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.stopmotion.animator.model.DoodleStyle
import com.stopmotion.animator.model.DrawingPoint
import com.stopmotion.animator.model.DrawingStroke

object DoodleRenderer {

    /**
     * Renders a committed [DrawingStroke] within the given destination rect.
     */
    fun DrawScope.drawStroke(
        stroke: DrawingStroke,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleFactor: Float = 1f
    ) {
        if (stroke.isRigRemoval || stroke.points.size < 2) return
        val awtColor = ImageProcessor.parseHexColor(stroke.colorHex)
        val composeColor = Color(awtColor.red, awtColor.green, awtColor.blue, awtColor.alpha)

        drawPointsPath(
            points = stroke.points,
            color = composeColor,
            baseStrokeWidth = stroke.strokeWidth,
            style = stroke.style,
            isGlow = stroke.isGlow,
            originX = originX,
            originY = originY,
            width = width,
            height = height,
            scaleFactor = scaleFactor
        )
    }

    /**
     * Renders a sequence of [points] (used for both committed and live in-progress strokes).
     */
    fun DrawScope.drawPointsPath(
        points: List<DrawingPoint>,
        color: Color,
        baseStrokeWidth: Float,
        style: DoodleStyle,
        isGlow: Boolean,
        originX: Float,
        originY: Float,
        width: Float,
        height: Float,
        scaleFactor: Float = 1f
    ) {
        if (points.size < 2) return
        val strokePx = (baseStrokeWidth * (width / 800f) * scaleFactor).coerceAtLeast(1.2f)

        val path = Path().apply {
            moveTo(originX + points[0].x * width, originY + points[0].y * height)
            for (i in 1 until points.size) {
                lineTo(originX + points[i].x * width, originY + points[i].y * height)
            }
        }

        when (style) {
            DoodleStyle.NEON_GLOW -> {
                // Outer glow aura
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.30f),
                    style = Stroke(width = strokePx * 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Mid glow
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.70f),
                    style = Stroke(width = strokePx * 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // White core
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.95f),
                    style = Stroke(width = (strokePx * 0.7f).coerceAtLeast(1f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            DoodleStyle.DASHED_TRAIL -> {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokePx * 2.5f, strokePx * 2f), 0f)
                    )
                )
            }
            DoodleStyle.SKETCH_CHALK -> {
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.55f),
                    style = Stroke(width = strokePx * 1.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.85f),
                    style = Stroke(width = strokePx * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            DoodleStyle.SOLID_PEN -> {
                if (isGlow) {
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.35f),
                        style = Stroke(width = strokePx * 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}