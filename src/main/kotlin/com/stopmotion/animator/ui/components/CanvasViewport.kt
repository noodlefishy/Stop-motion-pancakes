package com.stopmotion.animator.ui.components

import com.stopmotion.animator.util.DoodleRenderer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            StopMotionCanvas(state)

            if (state.activeTool in listOf(
                    EditorTool.DRAW_PEN,
                    EditorTool.DRAW_GLOW,
                    EditorTool.DRAW_CHALK,
                    EditorTool.DRAW_DASHED,
                    EditorTool.ERASER
                )
            ) {
                DoodleStudioBar(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                )
            }

            ZoomControlsHUD(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
            )
        }

        PlaybackTransportBar(state)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StopMotionCanvas(state: EditorState) {
    var activePoints by remember { mutableStateOf(listOf<DrawingPoint>()) }
    var hoverCursorPos by remember { mutableStateOf<Offset?>(null) }
    var isDraggingBackground by remember { mutableStateOf(false) }

    val currFrame = state.currentFrame
    val currIdx = state.selectedFrameIndex

    val cleanPlateImg = remember(state.project.cleanPlateFrameId, state.project.frames) {
        state.project.cleanPlateFrameId?.let { id ->
            state.project.frames.find { it.id == id }?.let { frame ->
                ImageCache.getBufferedImage(frame.imagePath)
            }
        }
    }

    val isPanningActive = state.isSpaceHeld || state.activeTool == EditorTool.HAND

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF07090C))
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerHoverIcon(if (isPanningActive || isDraggingBackground) PointerIcon.Hand else PointerIcon.Default)
                // 1. Hover tracking across the entire screen
                .onPointerEvent(PointerEventType.Move) { event ->
                    hoverCursorPos = event.changes.firstOrNull()?.position
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hoverCursorPos = null
                }
                // 2. Trackpad two-finger scroll pan and Cmd/Ctrl + Scroll zoom
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val change = event.changes.firstOrNull() ?: return@onPointerEvent
                    val delta = change.scrollDelta
                    if (event.keyboardModifiers.isMetaPressed || event.keyboardModifiers.isCtrlPressed) {
                        if (delta.y < 0) state.zoomIn() else if (delta.y > 0) state.zoomOut()
                    } else {
                        state.panOffsetX -= delta.x * 24f
                        state.panOffsetY -= delta.y * 24f
                    }
                }
                // 3. Gestures: Drawing, Erasing, and Panning with full screen coverage
                .pointerInput(
                    state.activeTool,
                    state.selectedFrameIndex,
                    state.doodleStyle,
                    state.brushColor,
                    state.brushSize,
                    state.zoomScale,
                    state.panOffsetX,
                    state.panOffsetY,
                    state.isSpaceHeld,
                    state.project.aspectRatio
                ) {
                    val viewportW = size.width.toFloat()
                    val viewportH = size.height.toFloat()
                    val targetAspect = state.project.aspectRatio.ratio

                    // Compute base frame dimensions that fit inside viewport at 1x zoom
                    val baseW: Float
                    val baseH: Float
                    if (viewportW / viewportH > targetAspect) {
                        baseH = viewportH * 0.95f
                        baseW = baseH * targetAspect
                    } else {
                        baseW = viewportW * 0.95f
                        baseH = baseW / targetAspect
                    }

                    val centerX = viewportW / 2f
                    val centerY = viewportH / 2f

                    // Inverse coordinate projection: screen (x, y) -> frame normalized (0..1)
                    fun screenToFrameNorm(pos: Offset): DrawingPoint {
                        val frameW = baseW * state.zoomScale
                        val frameH = baseH * state.zoomScale
                        val nx = 0.5f + (pos.x - centerX - state.panOffsetX) / frameW
                        val ny = 0.5f + (pos.y - centerY - state.panOffsetY) / frameH
                        return DrawingPoint(nx, ny)
                    }

                    detectDragGestures(
                        onDragStart = { startPos ->
                            hoverCursorPos = startPos
                            val pt = screenToFrameNorm(startPos)
                            val isInsideFrame = pt.x in 0f..1f && pt.y in 0f..1f
                            
                            state.activeDrawingPoints = listOf(DrawingPoint(pt.x.coerceIn(0f, 1f), pt.y.coerceIn(0f, 1f)))
                            
                            if (isPanningActive || state.activeTool == EditorTool.SELECT || !isInsideFrame) {
                                // User clicked outside the frame or has hand/space tool active -> Pan
                                isDraggingBackground = true
                                state.wasPanDragTriggered = true
                            } else if (state.activeTool == EditorTool.ERASER) {
                                state.eraseStrokesNear(
                                    normX = pt.x,
                                    normY = pt.y,
                                    canvasWidth = baseW,
                                    canvasHeight = baseH,
                                    radiusPixels = state.brushSize * 1.5f * (baseW / 800f).coerceAtLeast(0.5f)
                                )
                            } else if (state.activeTool in listOf(
                                    EditorTool.DRAW_PEN,
                                    EditorTool.DRAW_GLOW,
                                    EditorTool.DRAW_CHALK,
                                    EditorTool.DRAW_DASHED,
                                    EditorTool.RIG_REMOVAL
                                )
                            ) {
                                activePoints = listOf(DrawingPoint(pt.x.coerceIn(0f, 1f), pt.y.coerceIn(0f, 1f)))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            hoverCursorPos = change.position
                            val pt = screenToFrameNorm(change.position)
                            state.activeDrawingPoints += DrawingPoint(pt.x.coerceIn(0f, 1f), pt.y.coerceIn(0f, 1f))
                            
                            
                            if (isDraggingBackground || isPanningActive || state.activeTool == EditorTool.SELECT) {
                                state.wasPanDragTriggered = true
                                val maxPanX = (viewportW + baseW * state.zoomScale) * 0.75f
                                val maxPanY = (viewportH + baseH * state.zoomScale) * 0.75f
                                state.panOffsetX = (state.panOffsetX + dragAmount.x).coerceIn(-maxPanX, maxPanX)
                                state.panOffsetY = (state.panOffsetY + dragAmount.y).coerceIn(-maxPanY, maxPanY)
                            } else {
                               

                                if (state.activeTool == EditorTool.ERASER) {
                                    state.eraseStrokesNear(
                                        normX = pt.x.coerceIn(0f, 1f),
                                        normY = pt.y.coerceIn(0f, 1f),
                                        canvasWidth = baseW,
                                        canvasHeight = baseH,
                                        radiusPixels = state.brushSize * 1.5f * (baseW / 800f).coerceAtLeast(0.5f)
                                    )
                                } else if (state.activeTool in listOf(
                                        EditorTool.DRAW_PEN,
                                        EditorTool.DRAW_GLOW,
                                        EditorTool.DRAW_CHALK,
                                        EditorTool.DRAW_DASHED,
                                        EditorTool.RIG_REMOVAL
                                    )
                                ) {
                                    activePoints = activePoints + DrawingPoint(pt.x.coerceIn(0f, 1f), pt.y.coerceIn(0f, 1f))
                                } else if (state.splitCompare) {
                                    state.splitPosition = pt.x.coerceIn(0.05f, 0.95f)
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingBackground = false                            
                            val points = state.activeDrawingPoints
                            state.activeDrawingPoints = emptyList()
                            if (points.size >= 2) {
                                val currentStyle = when (state.activeTool) {
                                    EditorTool.DRAW_GLOW -> DoodleStyle.NEON_GLOW
                                    EditorTool.DRAW_CHALK -> DoodleStyle.SKETCH_CHALK
                                    EditorTool.DRAW_DASHED -> DoodleStyle.DASHED_TRAIL
                                    EditorTool.DRAW_PEN -> state.doodleStyle
                                    else -> DoodleStyle.SOLID_PEN
                                }
                                val stroke = DrawingStroke(
                                    points = points,
                                    colorHex = if (state.activeTool == EditorTool.RIG_REMOVAL) "#00000000" else state.brushColor.toHexArgb(),
                                    strokeWidth = state.brushSize,
                                    isGlow = currentStyle == DoodleStyle.NEON_GLOW,
                                    isRigRemoval = state.activeTool == EditorTool.RIG_REMOVAL,
                                    style = currentStyle
                                )
                                state.addDrawingStroke(stroke)
                            }
                          
                        }
                    )
                }
        ) {
            val viewportW = size.width
            val viewportH = size.height
            val targetAspect = state.project.aspectRatio.ratio

            val baseW: Float
            val baseH: Float
            if (viewportW / viewportH > targetAspect) {
                baseH = viewportH * 0.95f
                baseW = baseH * targetAspect
            } else {
                baseW = viewportW * 0.95f
                baseH = baseW / targetAspect
            }

            val centerX = viewportW / 2f
            val centerY = viewportH / 2f
            val frameSize = Size(baseW, baseH)

            // Everything inside this block renders strictly into the transformed frame space
            withTransform({
                translate(left = centerX + state.panOffsetX, top = centerY + state.panOffsetY)
                scale(scaleX = state.zoomScale, scaleY = state.zoomScale, pivot = Offset.Zero)
                translate(left = -baseW / 2f, top = -baseH / 2f)
            }) {
                // Background dark stage
                drawRect(color = Color.Black, topLeft = Offset.Zero, size = frameSize)

                // -------------------------------------------------------------
                // LAYER 1: CURRENT FRAME BASE PHOTO (Opaque at 1.0f alpha)
                // -------------------------------------------------------------
                if (currFrame != null) {
                    if (state.differenceMode && currIdx > 0) {
                        val prevFrame = state.project.frames[currIdx - 1]
                        val prevImg = ImageCache.getBufferedImage(prevFrame.imagePath)
                        val currProcessed = ImageProcessor.processFrame(currFrame, cleanPlateImg)
                            ?: ImageCache.getBufferedImage(currFrame.imagePath)

                        if (prevImg != null && currProcessed != null) {
                            val diff = ImageProcessor.computeDifference(currProcessed, prevImg)
                            drawFrameImage(diff, frameSize, alpha = 1.0f)
                        }
                    } else if (state.splitCompare && currIdx > 0) {
                        val prevFrame = state.project.frames[currIdx - 1]
                        val prevImg = ImageCache.getBufferedImage(prevFrame.imagePath)
                        val currProcessed = ImageProcessor.processFrame(currFrame, cleanPlateImg)
                            ?: ImageCache.getBufferedImage(currFrame.imagePath)

                        if (prevImg != null) drawFrameImage(prevImg, frameSize, alpha = 1.0f)
                        if (currProcessed != null) drawFrameImage(currProcessed, frameSize, alpha = 1.0f)

                        val splitX = frameSize.width * state.splitPosition
                        drawLine(
                            color = StudioPrimary,
                            start = Offset(splitX, 0f),
                            end = Offset(splitX, frameSize.height),
                            strokeWidth = 2.5f / state.zoomScale
                        )
                    } else {
                        // Draw current stage photo (without drawings baked in)
                        val currentBaseImg = ImageProcessor.processFrame(currFrame, cleanPlateImg, includeDrawings = false)
                            ?: ImageCache.getBufferedImage(currFrame.imagePath)
                        if (currentBaseImg != null) {
                            drawFrameImage(currentBaseImg, frameSize, alpha = 1.0f)
                        }
                    }
                }

                // -------------------------------------------------------------
                // LAYER 2: ONION SKIN — PAST FRAMES (Drawn ON TOP of current photo)
                // -------------------------------------------------------------
                if (state.onionSkinEnabled && !state.isPlaying && state.onionSkinPastCount > 0) {
                    for (p in state.onionSkinPastCount downTo 1) {
                        val pastIdx = currIdx - p
                        if (pastIdx in state.project.frames.indices) {
                            val pastFrame = state.project.frames[pastIdx]
                            val alpha = (state.onionSkinOpacity / p).coerceIn(0.08f, 0.85f)

                            // 2a. Ghost past frame photo (unless "Doodles Only" mode is on)
                            if (!state.doodleOnionSkinOnly) {
                                val pastBaseImg = ImageProcessor.processFrame(pastFrame, cleanPlateImg, includeDrawings = false)
                                    ?: ImageCache.getBufferedImage(pastFrame.imagePath)
                                pastBaseImg?.let { img ->
                                    drawFrameImage(
                                        img = img,
                                        canvasSize = frameSize,
                                        alpha = alpha * 0.75f,
                                        tint = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) OnionSkinPastColor else null
                                    )
                                }
                            }

                            // 2b. Ghost past drawings (sharply rendered in red/pink)
                            for (stroke in pastFrame.drawings) {
                                val ghostColor = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) {
                                    OnionSkinPastColor.copy(alpha = alpha)
                                } else {
                                    val awt = ImageProcessor.parseHexColor(stroke.colorHex)
                                    Color(awt.red, awt.green, awt.blue, (alpha * 255).toInt())
                                }
                                with(DoodleRenderer) {
                                    drawPointsPath(
                                        points = stroke.points,
                                        color = ghostColor,
                                        baseStrokeWidth = stroke.strokeWidth,
                                        style = stroke.style,
                                        isGlow = stroke.isGlow,
                                        originX = 0f,
                                        originY = 0f,
                                        width = baseW,
                                        height = baseH
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // LAYER 3: ONION SKIN — FUTURE FRAMES (Drawn ON TOP of current photo)
                // -------------------------------------------------------------
                if (state.onionSkinEnabled && !state.isPlaying && state.onionSkinFutureCount > 0) {
                    for (f in state.onionSkinFutureCount downTo 1) {
                        val futureIdx = currIdx + f
                        if (futureIdx in state.project.frames.indices) {
                            val futureFrame = state.project.frames[futureIdx]
                            val alpha = (state.onionSkinOpacity / f).coerceIn(0.08f, 0.85f)

                            // 3a. Ghost future frame photo (unless "Doodles Only" mode is on)
                            if (!state.doodleOnionSkinOnly) {
                                val futureBaseImg = ImageProcessor.processFrame(futureFrame, cleanPlateImg, includeDrawings = false)
                                    ?: ImageCache.getBufferedImage(futureFrame.imagePath)
                                futureBaseImg?.let { img ->
                                    drawFrameImage(
                                        img = img,
                                        canvasSize = frameSize,
                                        alpha = alpha * 0.75f,
                                        tint = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) OnionSkinFutureColor else null
                                    )
                                }
                            }

                            // 3b. Ghost future drawings (sharply rendered in green/mint)
                            for (stroke in futureFrame.drawings) {
                                val ghostColor = if (state.onionTintMode == OnionTintMode.RED_GREEN_TINT) {
                                    OnionSkinFutureColor.copy(alpha = alpha)
                                } else {
                                    val awt = ImageProcessor.parseHexColor(stroke.colorHex)
                                    Color(awt.red, awt.green, awt.blue, (alpha * 255).toInt())
                                }
                                with(DoodleRenderer) {
                                    drawPointsPath(
                                        points = stroke.points,
                                        color = ghostColor,
                                        baseStrokeWidth = stroke.strokeWidth,
                                        style = stroke.style,
                                        isGlow = stroke.isGlow,
                                        originX = 0f,
                                        originY = 0f,
                                        width = baseW,
                                        height = baseH
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // LAYER 4: CURRENT FRAME DRAWINGS (100% Full Opacity & True Colors)
                // -------------------------------------------------------------
                if (currFrame != null) {
                    for (stroke in currFrame.drawings) {
                        with(DoodleRenderer) {
                            drawStroke(
                                stroke = stroke,
                                originX = 0f,
                                originY = 0f,
                                width = baseW,
                                height = baseH
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // LAYER 5: LIVE IN-PROGRESS DRAWING STROKE
                // -------------------------------------------------------------
                if (state.activeDrawingPoints.size >= 2) {
                    val currentStyle = when (state.activeTool) {
                        EditorTool.DRAW_GLOW -> DoodleStyle.NEON_GLOW
                        EditorTool.DRAW_CHALK -> DoodleStyle.SKETCH_CHALK
                        EditorTool.DRAW_DASHED -> DoodleStyle.DASHED_TRAIL
                        EditorTool.DRAW_PEN -> state.doodleStyle
                        else -> DoodleStyle.SOLID_PEN
                    }
                    with(DoodleRenderer) {
                        drawPointsPath(
                            points = state.activeDrawingPoints,
                            color = if (state.activeTool == EditorTool.RIG_REMOVAL) Color.Cyan.copy(alpha = 0.5f) else state.brushColor,
                            baseStrokeWidth = state.brushSize,
                            style = currentStyle,
                            isGlow = currentStyle == DoodleStyle.NEON_GLOW,
                            originX = 0f,
                            originY = 0f,
                            width = baseW,
                            height = baseH
                        )
                    }
                }

                // -------------------------------------------------------------
                // LAYER 6: GUIDES & FRAME BORDER
                // -------------------------------------------------------------
                if (state.showGrid) drawGridGuides(frameSize)
                if (state.showSafeZone) drawSafeActionMargins(frameSize)
                if (state.showCenterCross) drawCenterCrosshair(frameSize)

                drawRect(
                    color = StudioBorder.copy(alpha = 0.8f),
                    topLeft = Offset.Zero,
                    size = frameSize,
                    style = Stroke(width = 1.5f / state.zoomScale)
                )
            }

            // 7. Live Cursor Ring (Rendered in screen space)
            if (!isPanningActive && !isDraggingBackground) {
                hoverCursorPos?.let { pos ->
                    val frameScale = (baseW / 800f).coerceAtLeast(0.5f) * state.zoomScale
                    val radius = if (state.activeTool == EditorTool.ERASER) {
                        state.brushSize * 1.5f * frameScale
                    } else {
                        (state.brushSize * frameScale) / 2f
                    }
                    drawCircle(
                        color = if (state.activeTool == EditorTool.ERASER) Color.Red.copy(alpha = 0.8f) else state.brushColor.copy(alpha = 0.9f),
                        radius = radius.coerceAtLeast(3f),
                        center = pos,
                        style = Stroke(width = 1.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ZoomControlsHUD(state: EditorState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = StudioSurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StudioTooltip("Zoom Out", "Cmd -") {
                IconButton(
                    onClick = { state.zoomOut() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Remove, "Zoom Out", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                }
            }

            StudioTooltip("Reset Zoom & Center", "Cmd 0") {
                Text(
                    text = "${(state.zoomScale * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { state.resetZoom() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            StudioTooltip("Zoom In", "Cmd +") {
                IconButton(
                    onClick = { state.zoomIn() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Add, "Zoom In", tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                }
            }

            if (state.zoomScale != 1.0f || state.panOffsetX != 0f || state.panOffsetY != 0f) {
                StudioTooltip("Center & Fit Viewport") {
                    IconButton(
                        onClick = { state.resetZoom() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.FitScreen, "Fit to Screen", tint = StudioAccent, modifier = Modifier.size(14.dp))
                    }
                }
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

    drawLine(color, Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
    drawLine(color, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), 1f)
    drawLine(color, Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
    drawLine(color, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), 1f)
}

private fun DrawScope.drawSafeActionMargins(canvasSize: Size) {
    val w = canvasSize.width
    val h = canvasSize.height

    drawRect(
        color = StudioSecondary.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.05f, h * 0.05f),
        size = Size(w * 0.90f, h * 0.90f),
        style = Stroke(width = 1f)
    )

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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioTooltip("Jump to Start", "Home") {
                IconButton(onClick = { state.jumpToStart() }) {
                    Icon(Icons.Default.FirstPage, "Jump to Start", tint = StudioTextPrimary)
                }
            }

            StudioTooltip("Step Back (-1 Frame)", "Left Arrow") {
                IconButton(onClick = { state.stepFrame(-1) }) {
                    Icon(Icons.Default.ChevronLeft, "Step Back (-1)", tint = StudioTextPrimary)
                }
            }

            StudioTooltip(if (state.isPlaying) "Pause" else "Play", "Spacebar") {
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
            }

            StudioTooltip("Step Forward (+1 Frame)", "Right Arrow") {
                IconButton(onClick = { state.stepFrame(1) }) {
                    Icon(Icons.Default.ChevronRight, "Step Forward (+1)", tint = StudioTextPrimary)
                }
            }

            StudioTooltip("Jump to End", "End") {
                IconButton(onClick = { state.jumpToEnd() }) {
                    Icon(Icons.Default.LastPage, "Jump to End", tint = StudioTextPrimary)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioTooltip("Loop Playback") {
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
            }

            StudioTooltip("Ping-Pong Reverse Playback") {
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
}