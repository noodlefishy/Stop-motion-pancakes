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

    // The device ID and capture resolution currently streaming, used by snapHighRes
    @Volatile private var currentDeviceId: String = "0"
    @Volatile private var captureResolution: String = "1920x1080"

    /**
     * Start the live preview at a fast low resolution (always 640x480) regardless
     * of the user's chosen capture resolution.  The capture resolution is remembered
     * so that snapHighRes() can grab a full-quality still separately.
     */
    fun start(
        deviceId: String,
        captureRes: String = "1920x1080", // the resolution used when snapping
        scope: CoroutineScope,
        onFrame: (ImageBitmap) -> Unit
    ) {
        stop()
        currentDeviceId = deviceId
        captureResolution = captureRes

        streamJob = scope.launch(Dispatchers.IO) {
            try {
                isStreaming = true
                val ffmpeg = PlatformUtils.findFFmpeg()
                val format = PlatformUtils.getCameraFormat()
                val inputTarget = PlatformUtils.getCameraInputTarget(deviceId)

                // Always stream preview at 640x480 — avfoundation initialises
                // much faster and iphone continuity feeds stay continuous.
                val previewRes = "640x480"

                val cmd = listOf(
                    ffmpeg,
                    "-y",
                    "-f", format,
                    "-framerate", "30",
                    "-video_size", previewRes,
                    "-i", inputTarget,
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-q:v", "5",   // decent quality MJPEG without crushing the pipe
                    "-"
                )

                val proc = ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                process = proc

                val inputStream = BufferedInputStream(proc.inputStream, 32768)
                val buffer = ByteArrayOutputStream(131072)
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
                        if (buffer.size() > 1_500_000) {
                            // Drop corrupted packets
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
     * Snap a full-resolution still from the camera.
     *
     * On macOS avfoundation only allows one process at a time on a given device,
     * so we must **stop** the live preview stream, run the high-res single-frame
     * capture, and then **restart** the preview.  The caller supplies a
     * [restartPreview] callback that will be invoked (on the main thread) after
     * the capture completes so the live feed resumes.
     */
    fun snapHighRes(
        destFile: File,
        scope: CoroutineScope,
        restartPreview: (() -> Unit)? = null,
        onDone: (Boolean) -> Unit
    ) {
        // Tear down the live stream so the camera device is free.
        stop()

        scope.launch(Dispatchers.IO) {
            destFile.parentFile?.mkdirs()
            val success = try {
                CameraManager.captureFrame(currentDeviceId, destFile, captureResolution)
            } catch (_: Exception) {
                false
            }
            withContext(Dispatchers.Main) {
                // Restart the preview first, then report result.
                restartPreview?.invoke()
                onDone(success)
            }
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