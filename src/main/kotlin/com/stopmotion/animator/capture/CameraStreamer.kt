package com.stopmotion.animator.capture

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class CameraStreamer {
    private var streamJob: Job? = null
    private var process: Process? = null
    var isStreaming = false
        private set

    fun start(
        deviceId: String,
        scope: CoroutineScope,
        onFrame: (ImageBitmap) -> Unit
    ) {
        stop()

        streamJob = scope.launch(Dispatchers.IO) {
            try {
                isStreaming = true
                val cmd = listOf(
                    "/opt/homebrew/bin/ffmpeg",
                    "-y",
                    "-f", "avfoundation",
                    "-framerate", "30",
                    "-video_size", "640x480",
                    "-i", "$deviceId:none",
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-"
                )

                val proc = ProcessBuilder(cmd)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                process = proc

                val inputStream = BufferedInputStream(proc.inputStream, 16384)
                val buffer = ByteArrayOutputStream(65536)
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
                        if (prevByte == 0xFF && b == 0xD9) {
                            inImage = false
                            val jpegBytes = buffer.toByteArray()
                            try {
                                val bais = ByteArrayInputStream(jpegBytes)
                                val img = ImageIO.read(bais)
                                if (img != null) {
                                    val bitmap = img.toComposeImageBitmap()
                                    withContext(Dispatchers.Main) {
                                        onFrame(bitmap)
                                    }
                                }
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                    prevByte = b
                }
            } catch (e: Exception) {
                // Ignore cancellation or stream exit
            } finally {
                isStreaming = false
                process?.destroyForcibly()
                process = null
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
        try {
            process?.destroyForcibly()
        } catch (ignored: Exception) {
        }
        process = null
        isStreaming = false
    }
}
