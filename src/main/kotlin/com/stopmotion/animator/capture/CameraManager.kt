package com.stopmotion.animator.capture

import com.stopmotion.animator.util.PlatformUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object CameraManager {

    data class CameraDevice(
        val id: String,
        val name: String,
        val isContinuity: Boolean = false
    )

    /**
     * Lists available video devices cross-platform via FFmpeg.
     * Identifies Continuity Cameras (iPhone/iPad) on macOS and filters out desktop screens.
     */
    suspend fun listCameras(): List<CameraDevice> = withContext(Dispatchers.IO) {
        val cameras = mutableListOf<CameraDevice>()
        val ffmpeg = PlatformUtils.findFFmpeg()
        val format = PlatformUtils.getCameraFormat()

        try {
            val listCmd = if (PlatformUtils.isWindows) {
                listOf(ffmpeg, "-f", "dshow", "-list_devices", "true", "-i", "dummy")
            } else if (PlatformUtils.isMac) {
                listOf(ffmpeg, "-f", "avfoundation", "-list_devices", "true", "-i", "")
            } else {
                listOf(ffmpeg, "-f", "v4l2", "-list_formats", "all", "-i", "/dev/video0")
            }

            val process = ProcessBuilder(listCmd).redirectErrorStream(true).start()
            val lines = process.inputStream.bufferedReader().readLines()
            process.waitFor(3, TimeUnit.SECONDS)

            var inVideoDevices = false
            for (line in lines) {
                if (PlatformUtils.isMac) {
                    if (line.contains("AVFoundation video devices:")) {
                        inVideoDevices = true
                        continue
                    }
                    if (line.contains("AVFoundation audio devices:")) {
                        inVideoDevices = false
                        break
                    }
                    if (inVideoDevices && line.contains("] [")) {
                        val match = Regex("""\[(\d+)\]\s+(.+)""").findAll(line).lastOrNull()
                        if (match != null) {
                            val (index, name) = match.destructured
                            val cleanName = name.trim()

                            if (!cleanName.startsWith("Capture screen", ignoreCase = true)) {
                                val isContinuity = cleanName.contains("iPhone", ignoreCase = true) ||
                                        cleanName.contains("iPad", ignoreCase = true) ||
                                        cleanName.contains("Desk View", ignoreCase = true) ||
                                        cleanName.contains("Continuity", ignoreCase = true) ||
                                        cleanName.contains("Surveillance", ignoreCase = true)

                                cameras.add(CameraDevice(id = index, name = cleanName, isContinuity = isContinuity))
                            }
                        }
                    }
                } else if (PlatformUtils.isWindows) {
                    if (line.contains("DirectShow video devices")) {
                        inVideoDevices = true
                        continue
                    }
                    if (line.contains("DirectShow audio devices")) {
                        inVideoDevices = false
                        break
                    }
                    if (inVideoDevices && line.contains("\"")) {
                        val quoteMatch = Regex("""\"([^\"]+)\"""").find(line)
                        if (quoteMatch != null) {
                            val name = quoteMatch.groupValues[1]
                            if (!cameras.any { it.name == name }) {
                                cameras.add(CameraDevice(id = name, name = name, isContinuity = false))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (cameras.isEmpty()) {
            val defaultName = if (PlatformUtils.isMac) "FaceTime HD Camera" else "Default Camera"
            val defaultId = if (PlatformUtils.isWindows) "video0" else "0"
            cameras.add(CameraDevice(defaultId, defaultName, isContinuity = false))
        }

        // Put Continuity Cameras first or prominently
        cameras.sortedByDescending { it.isContinuity }
    }

    /**
     * Captures a single high-resolution still frame from the selected camera device into a destination file.
     * Supports Continuity Camera at full 1080p / 4K.
     */
    suspend fun captureFrame(
        deviceId: String,
        destinationFile: File,
        resolution: String = "1920x1080"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            destinationFile.parentFile?.mkdirs()
            val ffmpeg = PlatformUtils.findFFmpeg()
            val format = PlatformUtils.getCameraFormat()
            val inputTarget = PlatformUtils.getCameraInputTarget(deviceId)

            val cmd = listOf(
                ffmpeg,
                "-y",
                "-f", format,
                "-framerate", "30",
                "-video_size", resolution,
                "-i", inputTarget,
                // Skip initial 3 frames so sensor exposure & auto-focus lock in cleanly
                "-vf", "select=gte(n\\,3)",
                "-vframes", "1",
                "-update", "1",
                destinationFile.absolutePath
            )

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exitCode = process.waitFor(5, TimeUnit.SECONDS)

            destinationFile.exists() && destinationFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
