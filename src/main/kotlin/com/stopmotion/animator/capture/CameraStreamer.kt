package com.stopmotion.animator.capture

import com.stopmotion.animator.util.PlatformUtils
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

class CameraStreamer {
    private var streamJob: Job? = null
    private var process: Process? = null
    var isStreaming = false
        private set

    // Holds the latest decoded clean frame for instantaneous capture
    @Volatile
    var latestBufferedImage: BufferedImage? = null
        private set

    /**
     * Start the live camera stream at the given [streamRes].
     *
     * Higher resolutions use a lower framerate so that Continuity Camera
     * (iPhone/iPad) feeds remain stable — avfoundation at 30 fps + 1080p
     * over wireless often stalls or delivers a single frame.
     */
    fun start(
        deviceId: String,
        streamRes: String = "1280x720",
        scope: CoroutineScope,
        onFrame: (ImageBitmap) -> Unit
    ) {
        stop()

        streamJob = scope.launch(Dispatchers.IO) {
            try {
                isStreaming = true
                val ffmpeg = PlatformUtils.findFFmpeg()
                val format = PlatformUtils.getCameraFormat()
                val inputTarget = PlatformUtils.getCameraInputTarget(deviceId)

                // Adapt framerate to resolution so Continuity Cameras cope.
                val fps = when (streamRes) {
                    "1920x1080" -> "15"
                    "1280x720"  -> "20"
                    else        -> "30"   // 640x480 and below
                }

                val cmd = listOf(
                    ffmpeg,
                    "-y",
                    "-f", format,
                    "-framerate", fps,
                    "-video_size", streamRes,
                    "-i", inputTarget,
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-q:v", "2",   // high quality — these frames are also our captures
                    "-"
                )

                val proc = ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                process = proc

                val inputStream = BufferedInputStream(proc.inputStream, 65536)
                val buffer = ByteArrayOutputStream(262144)
                var prevByte = -1
                var inImage = false

                while (isActive && proc.isAlive) {
                    val b = inputStream.read()
                    if (b == -1) break

                    if (!inImage) {
                        if (prevByte == 0xFF && b == 0xD8) {
                            inImage = true
                            buffer.reset()
                            buffer.write(0xFF)
                            buffer.write(0xD8)
                        }
                    } else {
                        buffer.write(b)
                        if (buffer.size() > 4_000_000) {
                            // Drop corrupted / oversized packets
                            inImage = false
                            buffer.reset()
                        } else if (prevByte == 0xFF && b == 0xD9) {
                            inImage = false
                            val jpegBytes = buffer.toByteArray()
                            try {
                                val bais = ByteArrayInputStream(jpegBytes)
                                val img = ImageIO.read(bais)
                                if (img != null) {
                                    latestBufferedImage = img
                                    val bitmap = img.toComposeImageBitmap()
                                    withContext(Dispatchers.Main) {
                                        onFrame(bitmap)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    prevByte = b
                }
            } catch (_: Exception) {
            } finally {
                isStreaming = false
                process?.destroyForcibly()
                process = null
            }
        }
    }

    /**
     * Instantly capture a frame by saving the current live-stream buffer
     * to [destFile] as a PNG.
     *
     * This **never** interrupts the stream — no camera switching,
     * no Continuity Camera reconnection, no delay.  The captured image
     * is at whatever resolution the stream is running at.
     */
    fun snapCurrentFrame(destFile: File, scope: CoroutineScope, onDone: (Boolean) -> Unit) {
        val img = latestBufferedImage
        if (img == null) {
            onDone(false)
            return
        }
        scope.launch(Dispatchers.IO) {
            val success = try {
                destFile.parentFile?.mkdirs()
                ImageIO.write(img, "PNG", destFile)
                destFile.exists() && destFile.length() > 0
            } catch (_: Exception) {
                false
            }
            withContext(Dispatchers.Main) { onDone(success) }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        try {
            process?.destroyForcibly()
        } catch (_: Exception) {}
        process = null
        isStreaming = false
        latestBufferedImage = null
    }
}