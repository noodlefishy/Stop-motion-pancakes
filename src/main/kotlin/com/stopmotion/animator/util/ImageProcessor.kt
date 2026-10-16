package com.stopmotion.animator.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.stopmotion.animator.model.DrawingPoint
import com.stopmotion.animator.model.DrawingStroke
import com.stopmotion.animator.model.FrameItem
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object ImageCache {
    private val fullSizeCache = ConcurrentHashMap<String, BufferedImage>()
    private val thumbnailCache = ConcurrentHashMap<String, ImageBitmap>()
    private val composeBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

    fun getBufferedImage(path: String): BufferedImage? {
        if (!File(path).exists()) return null
        return fullSizeCache.getOrPut(path) {
            try {
                ImageIO.read(File(path)) ?: createFallbackImage("Error reading file")
            } catch (e: Exception) {
                createFallbackImage("Missing image")
            }
        }
    }

    fun getThumbnail(path: String, width: Int = 120, height: Int = 80): ImageBitmap? {
        val cacheKey = "${path}_${width}x${height}"
        return thumbnailCache.getOrPut(cacheKey) {
            val original = getBufferedImage(path) ?: return null
            val thumb = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g = thumb.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(original, 0, 0, width, height, null)
            g.dispose()
            thumb.toComposeImageBitmap()
        }
    }

    fun invalidate(path: String) {
        fullSizeCache.remove(path)
        thumbnailCache.keys.filter { it.startsWith(path) }.forEach { thumbnailCache.remove(it) }
        composeBitmapCache.keys.filter { it.startsWith(path) }.forEach { composeBitmapCache.remove(it) }
    }

    fun clear() {
        fullSizeCache.clear()
        thumbnailCache.clear()
        composeBitmapCache.clear()
    }

    fun createFallbackImage(text: String, width: Int = 640, height: Int = 360): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(30, 33, 38)
        g.fillRect(0, 0, width, height)
        g.color = Color(240, 240, 240)
        g.drawString(text, 40, height / 2)
        g.dispose()
        return img
    }
}

object ImageProcessor {

    /**
     * Applies brightness, contrast, drawings, and optional clean-plate rig removal.
     */
    /**
     * Applies brightness, contrast, drawings, and optional clean-plate rig removal.
     */
    fun processFrame(
        frame: FrameItem,
        cleanPlate: BufferedImage? = null,
        includeDrawings: Boolean = true
    ): BufferedImage? {
        val base = ImageCache.getBufferedImage(frame.imagePath) ?: return null
        val w = base.width
        val h = base.height

        // Fast path: if no adjustments and no drawings requested, return base
        if (frame.brightness == 0f && frame.contrast == 1f && frame.saturation == 1f &&
            frame.temperature == 0f && (!includeDrawings || frame.drawings.isEmpty())
        ) {
            return base
        }

        val result = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = result.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Draw base image with brightness/contrast
        if (frame.brightness != 0f || frame.contrast != 1f) {
            val scaleFactor = frame.contrast.coerceIn(0.2f, 3.0f)
            val offset = frame.brightness * 128f
            val rescale = RescaleOp(scaleFactor, offset, null)
            val filteredBase = rescale.filter(base, null)
            g.drawImage(filteredBase, 0, 0, null)
        } else {
            g.drawImage(base, 0, 0, null)
        }

        // Rig removal / Clean Plate inpainting layer
        val rigStrokes = frame.drawings.filter { it.isRigRemoval }
        if (rigStrokes.isNotEmpty() && cleanPlate != null) {
            for (stroke in rigStrokes) {
                if (stroke.points.size < 2) continue
                for (pt in stroke.points) {
                    val px = (pt.x * w).toInt()
                    val py = (pt.y * h).toInt()
                    val radius = (stroke.strokeWidth * (w / 400f)).toInt().coerceAtLeast(6)
                    val sx = (px - radius).coerceIn(0, w - 1)
                    val sy = (py - radius).coerceIn(0, h - 1)
                    val sw = (radius * 2).coerceAtMost(w - sx)
                    val sh = (radius * 2).coerceAtMost(h - sy)
                    if (sw > 0 && sh > 0) {
                        g.drawImage(cleanPlate.getSubimage(sx, sy, sw, sh), sx, sy, null)
                    }
                }
            }
        }

        // Standard drawings & glow effects (only if requested)
        if (includeDrawings) {
            val regularStrokes = frame.drawings.filter { !it.isRigRemoval }
            for (stroke in regularStrokes) {
                if (stroke.points.size < 2) continue
                val awtColor = parseHexColor(stroke.colorHex)
                val strokeWidthPixels = stroke.strokeWidth * (w / 800f).coerceAtLeast(1f)

                when (stroke.style) {
                    com.stopmotion.animator.model.DoodleStyle.NEON_GLOW -> {
                        val auraAlpha = (awtColor.alpha * 0.25f).toInt().coerceIn(10, 255)
                        g.color = Color(awtColor.red, awtColor.green, awtColor.blue, auraAlpha)
                        g.stroke = BasicStroke(strokeWidthPixels * 4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)

                        val glowAlpha = (awtColor.alpha * 0.6f).toInt().coerceIn(20, 255)
                        g.color = Color(awtColor.red, awtColor.green, awtColor.blue, glowAlpha)
                        g.stroke = BasicStroke(strokeWidthPixels * 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)

                        g.color = Color(255, 255, 255, (awtColor.alpha * 0.9f).toInt())
                        g.stroke = BasicStroke(strokeWidthPixels * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)
                    }
                    com.stopmotion.animator.model.DoodleStyle.DASHED_TRAIL -> {
                        val dashPattern = floatArrayOf(strokeWidthPixels * 3f, strokeWidthPixels * 2f)
                        g.color = awtColor
                        g.stroke = BasicStroke(strokeWidthPixels, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dashPattern, 0f)
                        drawStrokePath(g, stroke.points, w, h)
                    }
                    com.stopmotion.animator.model.DoodleStyle.SKETCH_CHALK -> {
                        val chalkAlpha = (awtColor.alpha * 0.65f).toInt().coerceIn(20, 255)
                        g.color = Color(awtColor.red, awtColor.green, awtColor.blue, chalkAlpha)
                        g.stroke = BasicStroke(strokeWidthPixels * 1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)
                        g.color = awtColor
                        g.stroke = BasicStroke(strokeWidthPixels * 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)
                    }
                    com.stopmotion.animator.model.DoodleStyle.SOLID_PEN -> {
                        if (stroke.isGlow) {
                            val glowAlpha = (awtColor.alpha * 0.35f).toInt().coerceIn(10, 255)
                            g.color = Color(awtColor.red, awtColor.green, awtColor.blue, glowAlpha)
                            g.stroke = BasicStroke(strokeWidthPixels * 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                            drawStrokePath(g, stroke.points, w, h)
                        }
                        g.color = awtColor
                        g.stroke = BasicStroke(strokeWidthPixels, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                        drawStrokePath(g, stroke.points, w, h)
                    }
                }
            }
        }

        g.dispose()
        return result
    }

    private fun drawStrokePath(g: Graphics2D, points: List<DrawingPoint>, w: Int, h: Int) {
        val xPoints = IntArray(points.size)
        val yPoints = IntArray(points.size)
        for (i in points.indices) {
            xPoints[i] = (points[i].x * w).toInt()
            yPoints[i] = (points[i].y * h).toInt()
        }
        g.drawPolyline(xPoints, yPoints, points.size)
    }

    /**
     * Compute difference between two frames to highlight lighting flicker or shifts.
     */
    fun computeDifference(curr: BufferedImage, prev: BufferedImage): BufferedImage {
        val w = curr.width.coerceAtMost(prev.width)
        val h = curr.height.coerceAtMost(prev.height)
        val diff = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)

        val currPixels = curr.getRGB(0, 0, w, h, null, 0, w)
        val prevPixels = prev.getRGB(0, 0, w, h, null, 0, w)
        val diffPixels = IntArray(w * h)

        for (i in 0 until w * h) {
            val c = currPixels[i]
            val p = prevPixels[i]

            val rDiff = Math.abs(((c shr 16) and 0xFF) - ((p shr 16) and 0xFF))
            val gDiff = Math.abs(((c shr 8) and 0xFF) - ((p shr 8) and 0xFF))
            val bDiff = Math.abs((c and 0xFF) - (p and 0xFF))

            // Amplify difference for high visual clarity
            val rAmp = (rDiff * 3).coerceAtMost(255)
            val gAmp = (gDiff * 3).coerceAtMost(255)
            val bAmp = (bDiff * 3).coerceAtMost(255)

            diffPixels[i] = (rAmp shl 16) or (gAmp shl 8) or bAmp
        }

        diff.setRGB(0, 0, w, h, diffPixels, 0, w)
        return diff
    }

    fun parseHexColor(colorHex: String): Color {
        val clean = colorHex.removePrefix("#").trim()
        return try {
            when {
                clean.length == 8 -> {
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                clean.length == 6 -> {
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    Color(r, g, b, 255)
                }
                clean.length > 8 -> {
                    // Extract the last 8 hex digits to gracefully handle 16-char values
                    val last8 = clean.takeLast(8)
                    val a = last8.substring(0, 2).toInt(16)
                    val r = last8.substring(2, 4).toInt(16)
                    val g = last8.substring(4, 6).toInt(16)
                    val b = last8.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                }
                else -> Color(255, 94, 54) // Fallback to Studio Primary Orange
            }
        } catch (e: Exception) {
            Color(255, 94, 54)
        }
    }
}
