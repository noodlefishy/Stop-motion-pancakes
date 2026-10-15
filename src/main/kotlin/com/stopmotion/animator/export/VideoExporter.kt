package com.stopmotion.animator.export

import com.stopmotion.animator.model.AnimationProject
import com.stopmotion.animator.util.ImageCache
import com.stopmotion.animator.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

object VideoExporter {

    private fun findFFmpeg(): String {
        val candidates = listOf(
            "/opt/homebrew/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "ffmpeg"
        )
        for (c in candidates) {
            try {
                val proc = ProcessBuilder(c, "-version").start()
                if (proc.waitFor() == 0) return c
            } catch (ignored: Exception) {
            }
        }
        return "ffmpeg"
    }

    /**
     * Exports project frames into a temporary folder, expanding hold exposures.
     */
    private suspend fun renderFramesToFolder(
        project: AnimationProject,
        tempDir: File,
        onProgress: (Float, String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        tempDir.mkdirs()
        var exportedIndex = 0
        val totalRendered = project.totalTimelineFrames

        val cleanPlate = project.cleanPlateFrameId?.let { id ->
            project.frames.find { it.id == id }?.let { frame ->
                ImageCache.getBufferedImage(frame.imagePath)
            }
        }

        for ((idx, frame) in project.frames.withIndex()) {
            val processed = ImageProcessor.processFrame(frame, cleanPlate)
                ?: ImageCache.getBufferedImage(frame.imagePath)
                ?: continue

            for (h in 0 until frame.holdDuration) {
                val frameFile = File(tempDir, "frame_%05d.png".format(exportedIndex))
                ImageIO.write(processed, "PNG", frameFile)
                exportedIndex++
                val progress = exportedIndex.toFloat() / totalRendered.coerceAtLeast(1)
                onProgress(progress * 0.5f, "Rendering frame $exportedIndex of $totalRendered...")
            }
        }
        exportedIndex
    }

    /**
     * Export project to MP4 using FFmpeg with audio tracks.
     */
    suspend fun exportToMp4(
        project: AnimationProject,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "stopmotion_export_${System.currentTimeMillis()}")
        try {
            onProgress(0.05f, "Preparing frame sequence...")
            val frameCount = renderFramesToFolder(project, tempDir, onProgress)
            if (frameCount == 0) {
                return@withContext Result.failure(Exception("No valid frames to export"))
            }

            onProgress(0.55f, "Encoding MP4 video...")
            val ffmpeg = findFFmpeg()
            val inputPattern = File(tempDir, "frame_%05d.png").absolutePath

            val cmd = mutableListOf(
                ffmpeg,
                "-y", // overwrite
                "-framerate", project.fps.toString(),
                "-i", inputPattern
            )

            // Include first active audio track if available
            val activeAudio = project.audioTracks.firstOrNull { !it.isMuted && File(it.filePath).exists() }
            if (activeAudio != null) {
                cmd.addAll(listOf("-i", activeAudio.filePath, "-c:a", "aac", "-b:a", "192k", "-shortest"))
            }

            cmd.addAll(
                listOf(
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-crf", "18", // High cinematic quality
                    "-preset", "fast",
                    outputFile.absolutePath
                )
            )

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                onProgress(1.0f, "Export complete!")
                Result.success(outputFile)
            } else {
                val errorLog = process.inputStream.bufferedReader().readText()
                Result.failure(Exception("FFmpeg failed with exit code $exitCode: $errorLog"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Export project to high-quality animated GIF.
     */
    suspend fun exportToGif(
        project: AnimationProject,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "stopmotion_gif_${System.currentTimeMillis()}")
        try {
            onProgress(0.05f, "Preparing frame sequence for GIF...")
            val frameCount = renderFramesToFolder(project, tempDir, onProgress)
            if (frameCount == 0) {
                return@withContext Result.failure(Exception("No valid frames to export"))
            }

            onProgress(0.6f, "Generating GIF palette and optimizing...")
            val ffmpeg = findFFmpeg()
            val inputPattern = File(tempDir, "frame_%05d.png").absolutePath

            // 2-pass palettegen and paletteuse for crisp, banding-free stop-motion GIF
            val filter = "fps=${project.fps},split[s0][s1];[s0]palettegen=max_colors=256:stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3"

            val cmd = listOf(
                ffmpeg,
                "-y",
                "-framerate", project.fps.toString(),
                "-i", inputPattern,
                "-vf", filter,
                outputFile.absolutePath
            )

            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exitCode = process.waitFor()

            if (exitCode == 0 && outputFile.exists()) {
                onProgress(1.0f, "GIF export complete!")
                Result.success(outputFile)
            } else {
                val errorLog = process.inputStream.bufferedReader().readText()
                Result.failure(Exception("GIF export failed: $errorLog"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Export frames as a numbered ZIP archive.
     */
    suspend fun exportToZip(
        project: AnimationProject,
        outputZip: File,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val total = project.totalTimelineFrames
            var counter = 0
            val cleanPlate = project.cleanPlateFrameId?.let { id ->
                project.frames.find { it.id == id }?.let { frame ->
                    ImageCache.getBufferedImage(frame.imagePath)
                }
            }

            ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
                for (frame in project.frames) {
                    val processed = ImageProcessor.processFrame(frame, cleanPlate)
                        ?: ImageCache.getBufferedImage(frame.imagePath)
                        ?: continue

                    for (h in 0 until frame.holdDuration) {
                        val entryName = "frame_%05d.png".format(counter)
                        zos.putNextEntry(ZipEntry(entryName))
                        ImageIO.write(processed, "PNG", zos)
                        zos.closeEntry()
                        counter++
                        val prog = counter.toFloat() / total.coerceAtLeast(1)
                        onProgress(prog, "Archiving frame $counter of $total...")
                    }
                }
            }
            onProgress(1.0f, "ZIP export complete!")
            Result.success(outputZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
