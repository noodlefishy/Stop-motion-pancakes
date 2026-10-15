package com.stopmotion.animator.capture

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
     * Lists available video devices on macOS using AVFoundation via FFmpeg.
     * Identifies Continuity Cameras (iPhone/iPad) and filters out desktop screens.
     */
    suspend fun listCameras(): List<CameraDevice> = withContext(Dispatchers.IO) {
        val cameras = mutableListOf<CameraDevice>()
        try {
            val process = ProcessBuilder(
                "/opt/homebrew/bin/ffmpeg",
                "-f", "avfoundation",
                "-list_devices", "true",
                "-i", ""
            ).redirectErrorStream(true).start()

            val lines = process.inputStream.bufferedReader().readLines()
            process.waitFor(3, TimeUnit.SECONDS)

            var inVideoDevices = false
            for (line in lines) {
                if (line.contains("AVFoundation video devices:")) {
                    inVideoDevices = true
                    continue
                }
                if (line.contains("AVFoundation audio devices:")) {
                    inVideoDevices = false
                    break
                }
                if (inVideoDevices && line.contains("] [")) {
                    // Match pattern: [AVFoundation ...] [index] Device Name
                    val match = Regex("""\[(\d+)\]\s+(.+)""").findAll(line).lastOrNull()
                    if (match != null) {
                        val (index, name) = match.destructured
                        val cleanName = name.trim()

                        // Filter out screen capture virtual devices
                        if (!cleanName.startsWith("Capture screen", ignoreCase = true)) {
                            val isContinuity = cleanName.contains("iPhone", ignoreCase = true) ||
                                    cleanName.contains("iPad", ignoreCase = true) ||
                                    cleanName.contains("Desk View", ignoreCase = true) ||
                                    cleanName.contains("Continuity", ignoreCase = true) ||
                                    cleanName.contains("Surveillance", ignoreCase = true) // Matches custom iPhone naming

                            cameras.add(CameraDevice(id = index, name = cleanName, isContinuity = isContinuity))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (cameras.isEmpty()) {
            cameras.add(CameraDevice("0", "FaceTime HD Camera", isContinuity = false))
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
            val cmd = listOf(
                "/opt/homebrew/bin/ffmpeg",
                "-y",
                "-f", "avfoundation",
                "-framerate", "30",
                "-video_size", resolution,
                "-i", "$deviceId:none",
                "-vframes", "1",
                "-update", "1",
                destinationFile.absolutePath
            )

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exitCode = process.waitFor()

            exitCode == 0 && destinationFile.exists() && destinationFile.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
