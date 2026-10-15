package com.stopmotion.animator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.model.*
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.ImageCache
import com.stopmotion.animator.util.ImageProcessor
import java.awt.image.BufferedImage

@Composable
fun CanvasViewport(state: EditorState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stage Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            StopMotionCanvas(state)

            // Floating Doodle Studio Bar when in drawing mode
            if (state.activeTool in listOf(
                    EditorTool.DRAW_PEN,
                    EditorTool.DRAW_GLOW,
                    EditorTool.DRAW_CHALK,
                    EditorTool.DRAW_DASHED
                )
            ) {
                DoodleStudioBar(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                )
            }
        }

        // Playback Transport Bar
        PlaybackTransportBar(state)
    }
}

@Composable
fun StopMotionCanvas(state: EditorState) {
    var activePoints by remember { mutableStateOf(listOf<DrawingPoint>()) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val targetAspect = state.project.aspectRatio.ratio
        val boxWidth = maxWidth.value
        val boxHeight = maxHeight.value

        val (canvasW, canvasH) = if (boxWidth / boxHeight > targetAspect) {
            Pair(boxHeight * targetAspect, boxHeight)
        } else {
            Pair(boxWidth, boxWidth / targetAspect)
        }

        val currFrame = state.currentFrame
        val currIdx = state.selectedFrameIndex

        // Cached clean plate
        val cleanPlateImg = remember(state.project.cleanPlateFrameId, state.project.frames) {
            state.project.cleanPlateFrameId?.let { id ->
                state.project.frames.find { it.id == id }?.let { frame ->
                    ImageCache.getBufferedImage(frame.imagePath)
                }
            }
        }

        Canvas(
            modifier = Modifier
                .size(canvasW.dp, canvasH.dp)
                .pointerInput(state.activeTool, state.selectedFrameIndex, state.doodleStyle, state.brushColor, state.brushSize) {
                    if (state.activeTool in listOf(
                            EditorTool.DRAW_PEN,
                            EditorTool.DRAW_GLOW,
                            EditorTool.DRAW_CHALK,
                            EditorTool.DRAW_DASHED,
                            EditorTool.RIG_REMOVAL
                        )
                    ) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                activePoints = listOf(DrawingPoint(normX, normY))
                            },
                            onDrag = { change, _ ->
                                val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                                val normY = (change.position.y / size.height).coerceIn(0f, 1f)
                                activePoints = activePoints + DrawingPoint(normX, normY)
                            },
                            onDragEnd = {
                                if (activePoints.size >= 2) {
                                    val currentStyle = when (state.activeTool) {
                                        EditorTool.DRAW_GLOW -> DoodleStyle.NEON_GLOW
                                        EditorTool.DRAW_CHALK -> DoodleStyle.SKETCH_CHALK
                                        EditorTool.DRAW_DASHED -> DoodleStyle.DASHED_TRAIL
                                        EditorTool.DRAW_PEN -> state.doodleStyle
                                        else -> DoodleStyle.SOLID_PEN
                                    }
                                    val stroke = DrawingStroke(
                                        points = activePoints,
                                        colorHex = if (state.activeTool == EditorTool.RIG_REMOVAL) "#00000000" else state.brushColor.toHexArgb(),
                                        strokeWidth = state.brushSize,
                                        isGlow = currentStyle == DoodleStyle.NEON_GLOW,
                                        isRigRemoval = state.activeTool == EditorTool.RIG_REMOVAL,
                                        style = currentStyle
                                    )
                                    state.addDrawingStroke(stroke)
                                }
                                activePoints = emptyList()
                            }
                        )
                    } else if (state.splitCompare) {
                        detectDragGestures { change, _ ->
                            state.splitPosition = (change.position.x / size.width).coerceIn(0.05f, 0.95f)
                        }
                    }
                }
        ) {
            val canvasSize = size

            // 1. Draw Onion Skin (Past Frames)
            if (state.onionSkinEnabled && !state.isPlaying && state.onionSkinPastCount > 0) {
                for (p in state.onionSkinPastCount downTo 1) {
                    val pastIdx = currIdx - p
                    if (pastIdx >= 0 && pastIdx < state.project.frames.size) {
                        val pastFrame = state.project.frames[pastIdx]
                        val pastImg = ImageProcessor.processFrame(pastFrame, cleanPlateImg)
                            ?: ImageCache.getBufferedImage(pastFrame.imagePath)
                        pastImg?.let { img ->
                            val alpha = (state.onionSkinOpacity / p).coerceIn(0.08f, 0.8f)
                            drawFrameImage(
                                img,
                                canvasSize,
                                alpha = alpha,
                                tint = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) OnionSkinPastColor else null
                            )
                        }
                    }
                }
            }

            // 2. Draw Current Frame (or Difference Mode)
            if (currFrame != null) {
                val processedImg = ImageProcessor.processFrame(currFrame, cleanPlateImg)
                    ?: ImageCache.getBufferedImage(currFrame.imagePath)

                if (processedImg != null) {
                    if (state.differenceMode && currIdx > 0) {
                        val prevFrame = state.project.frames[currIdx - 1]
                        val prevImg = ImageCache.getBufferedImage(prevFrame.imagePath)
                        if (prevImg != null) {
                            val diff = ImageProcessor.computeDifference(processedImg, prevImg)
                            drawFrameImage(diff, canvasSize, alpha = 1.0f)
                        } else {
                            drawFrameImage(processedImg, canvasSize, alpha = 1.0f)
                        }
                    } else if (state.splitCompare && currIdx > 0) {
                        // Split screen: Left is Current, Right is Previous
                        val prevFrame = state.project.frames[currIdx - 1]
                        val prevImg = ImageCache.getBufferedImage(prevFrame.imagePath)
                        val splitX = canvasSize.width * state.splitPosition

                        // Draw Previous on right side
                        if (prevImg != null) {
                            drawFrameImage(prevImg, canvasSize, alpha = 1.0f)
                        }
                        // Draw Current on left side (clip)
                        drawFrameImage(processedImg, canvasSize, alpha = 1.0f)

                        // Split line
                        drawLine(
                            color = StudioPrimary,
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, canvasSize.height),
                            strokeWidth = 2.5f
                        )
                    } else {
                        drawFrameImage(processedImg, canvasSize, alpha = 1.0f)
                    }
                }
            }

            // 3. Draw Onion Skin (Future Frames)
            if (state.onionSkinEnabled && !state.isPlaying && state.onionSkinFutureCount > 0) {
                for (f in 1..state.onionSkinFutureCount) {
                    val futureIdx = currIdx + f
                    if (futureIdx in state.project.frames.indices) {
                        val futureFrame = state.project.frames[futureIdx]
                        val futureImg = ImageProcessor.processFrame(futureFrame, cleanPlateImg)
                            ?: ImageCache.getBufferedImage(futureFrame.imagePath)
                        futureImg?.let { img ->
                            val alpha = (state.onionSkinOpacity / f).coerceIn(0.08f, 0.8f)
                            drawFrameImage(
                                img,
                                canvasSize,
                                alpha = alpha,
                                tint = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) OnionSkinFutureColor else null
                            )
                        }
                    }
                }
            }

            // 4. Draw Active Drag Stroke in Progress
            if (activePoints.size >= 2) {
                val strokeColor = if (state.activeTool == EditorTool.RIG_REMOVAL) Color.Cyan.copy(alpha = 0.5f) else state.brushColor
                for (i in 0 until activePoints.size - 1) {
                    val p1 = activePoints[i]
                    val p2 = activePoints[i + 1]
                    drawLine(
                        color = strokeColor,
                        start = Offset(p1.x * canvasSize.width, p1.y * canvasSize.height),
                        end = Offset(p2.x * canvasSize.width, p2.y * canvasSize.height),
                        strokeWidth = state.brushSize,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 5. Draw Visual Guides
            if (state.showGrid) {
                drawGridGuides(canvasSize)
            }
            if (state.showSafeZone) {
                drawSafeActionMargins(canvasSize)
            }
            if (state.showCenterCross) {
                drawCenterCrosshair(canvasSize)
            }
        }
    }
}

private fun DrawScope.drawFrameImage(img: BufferedImage, canvasSize: Size, alpha: Float, tint: Color? = null) {
    val composeBitmap = img.toComposeImageBitmap()
    val colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.Modulate) }
    drawImage(
        image = composeBitmap,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(canvasSize.width.toInt(), canvasSize.height.toInt()),
        alpha = alpha,
        colorFilter = colorFilter
    )
}

private fun DrawScope.drawGridGuides(canvasSize: Size) {
    val color = Color.White.copy(alpha = 0.25f)
    val w = canvasSize.width
    val h = canvasSize.height

    // Rule of thirds
    drawLine(color, Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
    drawLine(color, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), 1f)
    drawLine(color, Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
    drawLine(color, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), 1f)
}

private fun DrawScope.drawSafeActionMargins(canvasSize: Size) {
    val w = canvasSize.width
    val h = canvasSize.height

    // Action Safe (90%)
    drawRect(
        color = StudioSecondary.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.05f, h * 0.05f),
        size = Size(w * 0.90f, h * 0.90f),
        style = Stroke(width = 1f)
    )

    // Title Safe (80%)
    drawRect(
        color = StudioWarning.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.10f, h * 0.10f),
        size = Size(w * 0.80f, h * 0.80f),
        style = Stroke(width = 1f)
    )
}

private fun DrawScope.drawCenterCrosshair(canvasSize: Size) {
    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val len = 20f
    val color = Color.White.copy(alpha = 0.4f)

    drawLine(color, Offset(cx - len, cy), Offset(cx + len, cy), 1.5f)
    drawLine(color, Offset(cx, cy - len), Offset(cx, cy + len), 1.5f)
}

@Composable
fun PlaybackTransportBar(state: EditorState) {
    val totalFrames = state.project.totalTimelineFrames
    val currentFrame = (state.currentTimelineIndex + 1).coerceAtMost(totalFrames)

    val seconds = if (state.project.fps > 0) state.currentTimelineIndex / state.project.fps else 0
    val framesRemainder = if (state.project.fps > 0) state.currentTimelineIndex % state.project.fps else 0
    val timecode = "%02d:%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60, framesRemainder)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(StudioSurface)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Timecode and Frame Number Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = StudioDark,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = timecode,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "FRAME $currentFrame / ${totalFrames.coerceAtLeast(1)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = StudioTextSecondary
            )
        }

        // Center: Transport Control Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { state.jumpToStart() }) {
                Icon(Icons.Default.FirstPage, "Jump to Start", tint = StudioTextPrimary)
            }

            IconButton(onClick = { state.stepFrame(-1) }) {
                Icon(Icons.Default.ChevronLeft, "Step Back (-1)", tint = StudioTextPrimary)
            }

            // Big Play / Pause Button
            IconButton(
                onClick = { state.togglePlayback() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(StudioPrimary)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = { state.stepFrame(1) }) {
                Icon(Icons.Default.ChevronRight, "Step Forward (+1)", tint = StudioTextPrimary)
            }

            IconButton(onClick = { state.jumpToEnd() }) {
                Icon(Icons.Default.LastPage, "Jump to End", tint = StudioTextPrimary)
            }
        }

        // Right: Loop and Ping-Pong playback toggles
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { state.isLooping = !state.isLooping },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Repeat,
                    "Loop Playback",
                    tint = if (state.isLooping) StudioPrimary else StudioTextMuted
                )
            }

            IconButton(
                onClick = { state.isPingPong = !state.isPingPong },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.SyncAlt,
                    "Ping-Pong Playback",
                    tint = if (state.isPingPong) StudioSecondary else StudioTextMuted
                )
            }
        }
    }
}
