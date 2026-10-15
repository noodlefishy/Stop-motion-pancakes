package com.stopmotion.animator.util

import java.io.File

object PlatformUtils {
    val osName: String = System.getProperty("os.name", "").lowercase()
    val isMac: Boolean = osName.contains("mac")
    val isWindows: Boolean = osName.contains("win")
    val isLinux: Boolean = osName.contains("linux") || osName.contains("unix")

    fun findFFmpeg(): String {
        val candidates = when {
            isMac -> listOf(
                "/opt/homebrew/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "ffmpeg"
            )
            isWindows -> listOf(
                "ffmpeg.exe",
                "ffmpeg",
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe"
            )
            else -> listOf(
                "/usr/bin/ffmpeg",
                "/usr/local/bin/ffmpeg",
                "ffmpeg"
            )
        }

        for (c in candidates) {
            try {
                val proc = ProcessBuilder(c, "-version").start()
                if (proc.waitFor() == 0) return c
            } catch (ignored: Exception) {
            }
        }
        return if (isWindows) "ffmpeg.exe" else "ffmpeg"
    }

    fun getCameraFormat(): String {
        return when {
            isMac -> "avfoundation"
            isWindows -> "dshow"
            else -> "v4l2"
        }
    }

    fun getCameraInputTarget(deviceId: String): String {
        return when {
            isMac -> "$deviceId:none"
            isWindows -> "video=$deviceId"
            else -> if (deviceId.startsWith("/dev/")) deviceId else "/dev/video$deviceId"
        }
    }
}
