package com.stopmotion.animator.sample

import com.stopmotion.animator.model.AnimationProject
import com.stopmotion.animator.model.AspectRatio
import com.stopmotion.animator.model.FrameItem
import com.stopmotion.animator.model.XSheetRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.sin

object DemoAnimationGenerator {

    /**
     * Generates an animated stop-motion claymation demo project with 16 frames,
     * a clean background stage plate, and an audio sound effect file.
     */
    suspend fun generateDemoProject(): AnimationProject = withContext(Dispatchers.IO) {
        val demoDir = File(System.getProperty("user.home"), ".stopmotion_animator/demo_project")
        demoDir.mkdirs()

        val width = 960
        val height = 540

        // 1. Generate Clean Plate (Empty stage with wooden table and studio backdrop)
        val cleanPlateFile = File(demoDir, "clean_plate_stage.png")
        if (!cleanPlateFile.exists()) {
            val cleanImg = renderStageBackdrop(width, height)
            ImageIO.write(cleanImg, "PNG", cleanPlateFile)
        }

        // 2. Generate 16 stop motion animation frames of a lively clay ball bouncing with squash and stretch
        val frameItems = mutableListOf<FrameItem>()
        val totalFrames = 16

        for (i in 0 until totalFrames) {
            val frameFile = File(demoDir, "demo_frame_%02d.png".format(i))
            val t = i.toFloat() / (totalFrames - 1)

            // Physics of bounce: parabola
            val bounceCycle = (t * 2f) % 1f
            val arc = 4f * bounceCycle * (1f - bounceCycle) // 0 -> 1 -> 0

            val clayX = (width * 0.2f + (width * 0.6f) * t)
            val groundY = height * 0.72f
            val maxHeight = height * 0.40f
            val clayY = groundY - (arc * maxHeight)

            // Squash & stretch: squashes at bottom, stretches during ascent/descent
            val isImpact = arc < 0.15f
            val squashX = if (isImpact) 1.4f else 0.85f + (0.15f * (1f - arc))
            val squashY = if (isImpact) 0.65f else 1.15f - (0.15f * (1f - arc))

            val img = renderStageBackdrop(width, height)
            val g = img.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Clay shadow on ground
            val shadowWidth = (90f * squashX * (1f - (arc * 0.4f))).toInt()
            val shadowHeight = (22f * (1f - (arc * 0.6f))).toInt()
            val shadowAlpha = ((1f - (arc * 0.6f)) * 0.45f).coerceIn(0.1f, 0.5f)
            g.color = Color(0f, 0f, 0f, shadowAlpha)
            g.fill(Ellipse2D.Float(clayX - shadowWidth / 2f, groundY + 10f, shadowWidth.toFloat(), shadowHeight.toFloat()))

            // Clay character (Orange clay creature with clay thumb-prints & googly eyes)
            val radius = 55f
            val bodyW = radius * 2f * squashX
            val bodyH = radius * 2f * squashY
            val bodyX = clayX - bodyW / 2f
            val bodyY = clayY - bodyH / 2f

            // Support wire / Armature Rig visible in frames 3-6 (to test Rig Removal!)
            if (i in 3..6) {
                g.color = Color(180, 180, 190, 220) // Metal rig rod
                g.stroke = BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g.drawLine(clayX.toInt(), (bodyY - 40).toInt(), clayX.toInt() + 80, -20)
                g.color = Color(100, 100, 110)
                g.fillOval(clayX.toInt() - 6, (bodyY - 45).toInt(), 12, 12)
            }

            // Clay body with clay gradient
            val gradient = RadialGradientPaint(
                clayX - 15f, clayY - 15f, radius * 1.2f,
                floatArrayOf(0.0f, 0.7f, 1.0f),
                arrayOf(Color(255, 138, 101), Color(230, 81, 0), Color(191, 54, 12))
            )
            g.paint = gradient
            g.fill(Ellipse2D.Float(bodyX, bodyY, bodyW, bodyH))

            // Stop motion clay texture (fingerprint grooves)
            g.color = Color(255, 255, 255, 25)
            g.stroke = BasicStroke(2f)
            g.drawArc((bodyX + 10).toInt(), (bodyY + 15).toInt(), (bodyW * 0.6f).toInt(), (bodyH * 0.5f).toInt(), 20, 140)

            // Googly eyes!
            val eyeSpacing = 16f * squashX
            val eyeY = bodyY + (bodyH * 0.35f)
            val eyeSize = 18f

            // Left eye
            g.color = Color.WHITE
            g.fill(Ellipse2D.Float(clayX - eyeSpacing - eyeSize / 2f, eyeY, eyeSize, eyeSize))
            g.color = Color.BLACK
            val pupilOffset = sin(t * 2 * PI).toFloat() * 3f
            val isBlink = (i == 7 || i == 8)
            if (!isBlink) {
                g.fill(Ellipse2D.Float(clayX - eyeSpacing - 4f + pupilOffset, eyeY + 4f, 8f, 8f))
            } else {
                g.stroke = BasicStroke(3f)
                g.drawLine((clayX - eyeSpacing - 7f).toInt(), (eyeY + 9f).toInt(), (clayX - eyeSpacing + 7f).toInt(), (eyeY + 9f).toInt())
            }

            // Right eye
            g.color = Color.WHITE
            g.fill(Ellipse2D.Float(clayX + eyeSpacing - eyeSize / 2f, eyeY, eyeSize, eyeSize))
            g.color = Color.BLACK
            if (!isBlink) {
                g.fill(Ellipse2D.Float(clayX + eyeSpacing - 4f + pupilOffset, eyeY + 4f, 8f, 8f))
            } else {
                g.stroke = BasicStroke(3f)
                g.drawLine((clayX + eyeSpacing - 7f).toInt(), (eyeY + 9f).toInt(), (clayX + eyeSpacing + 7f).toInt(), (eyeY + 9f).toInt())
            }

            // Smile
            g.color = Color(100, 20, 5)
            g.stroke = BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            val mouthW = 22f * squashX
            val mouthY = eyeY + 16f
            g.drawArc((clayX - mouthW / 2f).toInt(), mouthY.toInt(), mouthW.toInt(), 14, 200, 140)

            g.dispose()
            ImageIO.write(img, "PNG", frameFile)

            frameItems.add(
                FrameItem(
                    imagePath = frameFile.absolutePath,
                    holdDuration = if (i == 0 || i == 15) 2 else 1, // Hold start and end on 2s
                    notes = when (i) {
                        0 -> "Anticipation"
                        4 -> "Apex of arc"
                        7 -> "Squash & Blink"
                        12 -> "Second landing"
                        else -> ""
                    }
                )
            )
        }

        // 3. Generate a sample WAV sound effect (Bouncing cartoon boing with audio waveform)
        val sfxFile = File(demoDir, "clay_bounce_sfx.wav")
        if (!sfxFile.exists()) {
            generateBoingWav(sfxFile, totalFrames = 16, fps = 12)
        }

        val cleanPlateFrame = FrameItem(
            imagePath = cleanPlateFile.absolutePath,
            isCleanPlate = true,
            notes = "Clean Background Plate (for Rig Removal)"
        )

        val xSheets = (0 until totalFrames).map { idx ->
            XSheetRow(
                frameIndex = idx,
                phoneme = when (idx) {
                    0 -> "M"
                    1 -> "A"
                    4 -> "O"
                    7 -> "E"
                    12 -> "A"
                    else -> ""
                },
                actionNote = when (idx) {
                    0 -> "Hold pose"
                    3 -> "Rig wire active"
                    7 -> "Squash contact"
                    15 -> "Settle"
                    else -> ""
                }
            )
        }

        AnimationProject(
            title = "Bouncing Clay Creature (Demo)",
            fps = 12,
            aspectRatio = AspectRatio.RATIO_16_9,
            frames = frameItems,
            cleanPlateFrameId = cleanPlateFrame.id,
            xSheetRows = xSheets,
            projectDirectory = demoDir.absolutePath
        )
    }

    private fun renderStageBackdrop(width: Int, height: Int): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Studio backdrop wall (Warm dark slate with vignette)
        val wallPaint = GradientPaint(
            0f, 0f, Color(38, 43, 52),
            0f, height * 0.7f, Color(24, 27, 33)
        )
        g.paint = wallPaint
        g.fillRect(0, 0, width, (height * 0.72f).toInt())

        // Stop motion studio lighting spot
        val spotLight = RadialGradientPaint(
            width * 0.5f, height * 0.35f, width * 0.55f,
            floatArrayOf(0.0f, 0.6f, 1.0f),
            arrayOf(Color(255, 255, 255, 28), Color(255, 230, 200, 10), Color(0, 0, 0, 0))
        )
        g.paint = spotLight
        g.fillRect(0, 0, width, height)

        // Wooden stop-motion animation tabletop
        val tableY = (height * 0.72f).toInt()
        val tablePaint = GradientPaint(
            0f, tableY.toFloat(), Color(101, 67, 45),
            0f, height.toFloat(), Color(60, 38, 24)
        )
        g.paint = tablePaint
        g.fillRect(0, tableY, width, height - tableY)

        // Wood grain lines
        g.color = Color(75, 48, 30, 120)
        g.stroke = BasicStroke(1.5f)
        for (y in tableY + 12 until height step 24) {
            g.drawLine(0, y, width, y)
        }

        // Table edge highlight
        g.color = Color(160, 115, 80, 160)
        g.drawLine(0, tableY, width, tableY)

        g.dispose()
        return img
    }

    private fun generateBoingWav(file: File, totalFrames: Int, fps: Int) {
        val sampleRate = 44100
        val durationSeconds = totalFrames.toDouble() / fps
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val frameT = t * fps
            val bounceCycle = frameT % 8.0
            val decay = (-bounceCycle * 1.8).coerceAtMost(0.0)
            val freq = 160.0 + (180.0 * Math.sin(bounceCycle * 3.0))
            val sample = (Math.sin(2.0 * Math.PI * freq * t) * Math.exp(decay) * 22000).toInt().toShort()
            buffer.putShort(sample)
        }

        FileOutputStream(file).use { fos ->
            writeWavHeader(fos, totalSamples * 2, sampleRate, 1, 16)
            fos.write(buffer.array())
        }
    }

    private fun writeWavHeader(out: FileOutputStream, dataSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalDataLen = dataSize + 36

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // Subchunk1Size (16 for PCM)
        header[20] = 1; header[21] = 0 // AudioFormat (1 for PCM)
        header[22] = channels.toByte(); header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = blockAlign.toByte(); header[33] = 0
        header[34] = bitsPerSample.toByte(); header[35] = 0
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
