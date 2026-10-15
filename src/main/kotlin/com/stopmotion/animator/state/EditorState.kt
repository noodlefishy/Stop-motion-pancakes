package com.stopmotion.animator.state

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.stopmotion.animator.audio.AudioEngine
import com.stopmotion.animator.capture.CameraManager
import com.stopmotion.animator.export.VideoExporter
import com.stopmotion.animator.model.*
import com.stopmotion.animator.sample.DemoAnimationGenerator
import com.stopmotion.animator.util.ImageCache
import com.stopmotion.animator.util.ImageProcessor
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class EditorState(val scope: CoroutineScope) {

    val audioEngine = AudioEngine()

    var project by mutableStateOf(AnimationProject())

    var currentTimelineIndex by mutableStateOf(0)
    var selectedFrameIndex by mutableStateOf(0)
    var selectedFrameIndices by mutableStateOf(setOf<Int>())

    var isPlaying by mutableStateOf(false)
    var isLooping by mutableStateOf(true)
    var isPingPong by mutableStateOf(false)
    private var pingPongForward = true

    // Onion Skin settings
    var onionSkinEnabled by mutableStateOf(true)
    var onionSkinPastCount by mutableStateOf(2)
    var onionSkinFutureCount by mutableStateOf(1)
    var onionSkinOpacity by mutableStateOf(0.40f)
    var onionTintMode by mutableStateOf(OnionTintMode.RED_GREEN_TINT)

    // Visual Guides & Comparison
    var showGrid by mutableStateOf(false)
    var showSafeZone by mutableStateOf(false)
    var showCenterCross by mutableStateOf(false)
    var differenceMode by mutableStateOf(false)
    var splitCompare by mutableStateOf(false)
    var splitPosition by mutableStateOf(0.5f)

    // Editor Tools
    var activeTool by mutableStateOf(EditorTool.SELECT)
    var doodleStyle by mutableStateOf(DoodleStyle.SOLID_PEN)
    var brushColor by mutableStateOf(Color(0xFFFF5E36))
    var brushSize by mutableStateOf(6f)
    var isGlowActive by mutableStateOf(false)
    var doodleOnionSkinOnly by mutableStateOf(false) // Option to onion-skin ONLY doodles

    // Settings & Dialogs
    var settings by mutableStateOf(com.stopmotion.animator.util.SettingsManager.loadSettings())
    var isSettingsDialogVisible by mutableStateOf(false)
    var isCameraCaptureDialogVisible by mutableStateOf(false)
    var isExportDialogVisible by mutableStateOf(false)
    var isExporting by mutableStateOf(false)
    var exportProgress by mutableStateOf(0f)
    var exportMessage by mutableStateOf("")

    // Status toast
    var toastMessage by mutableStateOf<String?>(null)

    // History for Undo / Redo
    private val undoStack = mutableListOf<AnimationProject>()
    private val redoStack = mutableListOf<AnimationProject>()

    private var playbackJob: Job? = null

    val currentFrame: FrameItem?
        get() {
            if (project.frames.isEmpty()) return null
            val idx = project.timelineIndexToFrameIndex(currentTimelineIndex)
            return project.frames.getOrNull(idx)
        }

    val selectedFrame: FrameItem?
        get() = project.frames.getOrNull(selectedFrameIndex)

    init {
        // Automatically load demo on startup if project is empty
        scope.launch {
            loadDemo()
        }
    }

    private fun pushUndo() {
        undoStack.add(project)
        if (undoStack.size > 30) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(project)
            project = undoStack.removeAt(undoStack.lastIndex)
            clampIndices()
            showToast("Undo")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(project)
            project = redoStack.removeAt(redoStack.lastIndex)
            clampIndices()
            showToast("Redo")
        }
    }

    fun showToast(msg: String) {
        toastMessage = msg
        scope.launch {
            delay(2500)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    fun setFps(newFps: Int) {
        pushUndo()
        project = project.copy(fps = newFps.coerceIn(1, 60))
        if (isPlaying) {
            startPlaybackLoop()
        }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        pushUndo()
        project = project.copy(aspectRatio = ratio)
    }

    fun togglePlayback() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (project.frames.isEmpty()) return
        isPlaying = true
        startPlaybackLoop()
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = scope.launch(Dispatchers.Default) {
            val total = project.totalTimelineFrames
            if (total == 0) return@launch

            val delayMillis = (1000L / project.fps).coerceAtLeast(16L)
            while (isActive && isPlaying) {
                delay(delayMillis)
                val totalFrames = project.totalTimelineFrames
                if (totalFrames <= 0) break

                if (isPingPong) {
                    if (pingPongForward) {
                        if (currentTimelineIndex >= totalFrames - 1) {
                            pingPongForward = false
                            currentTimelineIndex = (totalFrames - 2).coerceAtLeast(0)
                        } else {
                            currentTimelineIndex++
                        }
                    } else {
                        if (currentTimelineIndex <= 0) {
                            pingPongForward = true
                            currentTimelineIndex = 1.coerceAtMost(totalFrames - 1)
                        } else {
                            currentTimelineIndex--
                        }
                    }
                } else {
                    if (currentTimelineIndex >= totalFrames - 1) {
                        if (isLooping) {
                            currentTimelineIndex = 0
                        } else {
                            isPlaying = false
                            break
                        }
                    } else {
                        currentTimelineIndex++
                    }
                }

                // Trigger audio scrub snippet or sync
                project.audioTracks.forEach { track ->
                    audioEngine.scrubFrame(track, currentTimelineIndex, project.fps)
                }

                selectedFrameIndex = project.timelineIndexToFrameIndex(currentTimelineIndex)
            }
        }
    }

    fun stepFrame(delta: Int) {
        pause()
        val total = project.totalTimelineFrames
        if (total == 0) return
        val nextIdx = (currentTimelineIndex + delta).coerceIn(0, total - 1)
        currentTimelineIndex = nextIdx
        selectedFrameIndex = project.timelineIndexToFrameIndex(nextIdx)

        // Scrub audio on manual step
        project.audioTracks.forEach { track ->
            audioEngine.scrubFrame(track, currentTimelineIndex, project.fps)
        }
    }

    fun jumpToStart() {
        pause()
        currentTimelineIndex = 0
        selectedFrameIndex = 0
    }

    fun jumpToEnd() {
        pause()
        val total = project.totalTimelineFrames
        if (total > 0) {
            currentTimelineIndex = total - 1
            selectedFrameIndex = project.timelineIndexToFrameIndex(total - 1)
        }
    }

    fun selectFrame(index: Int) {
        if (index in project.frames.indices) {
            selectedFrameIndex = index
            selectedFrameIndices = setOf(index)
            currentTimelineIndex = project.frameIndexToTimelineIndex(index)
            project.audioTracks.forEach { track ->
                audioEngine.scrubFrame(track, currentTimelineIndex, project.fps)
            }
        }
    }

    fun toggleFrameMultiSelect(index: Int) {
        val next = selectedFrameIndices.toMutableSet()
        if (next.contains(index)) {
            next.remove(index)
        } else {
            next.add(index)
        }
        selectedFrameIndices = next
        selectedFrameIndex = index
    }

    fun setHoldForSelected(hold: Int) {
        pushUndo()
        val targets = if (selectedFrameIndices.isNotEmpty()) selectedFrameIndices else setOf(selectedFrameIndex)
        val updated = project.frames.mapIndexed { idx, frame ->
            if (targets.contains(idx)) frame.copy(holdDuration = hold.coerceIn(1, 12)) else frame
        }
        project = project.copy(frames = updated)
        showToast("Set hold to ${hold}s for ${targets.size} frame(s)")
    }

    fun duplicateSelectedFrame() {
        if (project.frames.isEmpty()) return
        pushUndo()
        val current = project.frames[selectedFrameIndex]
        val nextFrames = project.frames.toMutableList()
        val clone = current.copy(id = java.util.UUID.randomUUID().toString())
        nextFrames.add(selectedFrameIndex + 1, clone)
        project = project.copy(frames = nextFrames)
        selectFrame(selectedFrameIndex + 1)
        showToast("Duplicated frame")
    }

    fun deleteSelectedFrames() {
        if (project.frames.isEmpty()) return
        pushUndo()
        val targets = if (selectedFrameIndices.isNotEmpty()) selectedFrameIndices else setOf(selectedFrameIndex)
        val count = targets.size
        val nextFrames = project.frames.filterIndexed { idx, _ -> !targets.contains(idx) }
        project = project.copy(frames = nextFrames)
        clampIndices()
        selectedFrameIndices = if (project.frames.isNotEmpty()) setOf(selectedFrameIndex) else emptySet()
        showToast("Deleted $count frame(s)")
    }

    fun deleteFrameAt(index: Int) {
        if (index !in project.frames.indices) return
        pushUndo()
        val nextFrames = project.frames.toMutableList()
        nextFrames.removeAt(index)
        project = project.copy(frames = nextFrames)
        clampIndices()
        selectedFrameIndices = if (project.frames.isNotEmpty()) setOf(selectedFrameIndex) else emptySet()
        showToast("Deleted Frame #${index + 1}")
    }

    fun reverseSelectedFrames() {
        val targets = (if (selectedFrameIndices.isNotEmpty()) selectedFrameIndices else project.frames.indices.toSet()).sorted()
        if (targets.size <= 1) return
        pushUndo()
        val framesList = project.frames.toMutableList()
        val subList = targets.map { framesList[it] }.reversed()
        for (i in targets.indices) {
            framesList[targets[i]] = subList[i]
        }
        project = project.copy(frames = framesList)
        showToast("Reversed ${targets.size} frames")
    }

    fun moveSelectedFrame(direction: Int) {
        val fromIdx = selectedFrameIndex
        val toIdx = fromIdx + direction
        if (toIdx !in project.frames.indices) return
        pushUndo()
        val list = project.frames.toMutableList()
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        project = project.copy(frames = list)
        selectFrame(toIdx)
    }

    fun updateFrameColor(brightness: Float? = null, contrast: Float? = null, saturation: Float? = null) {
        val idx = selectedFrameIndex
        if (idx !in project.frames.indices) return
        val current = project.frames[idx]
        val updated = current.copy(
            brightness = brightness ?: current.brightness,
            contrast = contrast ?: current.contrast,
            saturation = saturation ?: current.saturation
        )
        val frames = project.frames.toMutableList()
        frames[idx] = updated
        project = project.copy(frames = frames)
        ImageCache.invalidate(current.imagePath)
    }

    fun autoDeFlicker() {
        val idx = selectedFrameIndex
        if (idx <= 0 || idx !in project.frames.indices) {
            showToast("Requires a preceding frame to match exposure")
            return
        }
        val currImg = ImageCache.getBufferedImage(project.frames[idx].imagePath) ?: return
        val prevImg = ImageCache.getBufferedImage(project.frames[idx - 1].imagePath) ?: return

        // Compute average luminance difference
        var currSum = 0L
        var prevSum = 0L
        val w = currImg.width.coerceAtMost(prevImg.width)
        val h = currImg.height.coerceAtMost(prevImg.height)
        val step = 4 // sample every 4th pixel for speed

        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                val c = currImg.getRGB(x, y)
                val p = prevImg.getRGB(x, y)
                currSum += (((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)) / 3
                prevSum += (((p shr 16) and 0xFF) + ((p shr 8) and 0xFF) + (p and 0xFF)) / 3
            }
        }

        val currAvg = currSum.toDouble()
        val prevAvg = prevSum.toDouble()
        if (currAvg > 0) {
            val ratio = (prevAvg / currAvg).toFloat()
            val brightnessDelta = (ratio - 1.0f).coerceIn(-0.3f, 0.3f)
            updateFrameColor(brightness = brightnessDelta)
            showToast("Auto de-flickered: offset adjusted by %+.2f".format(brightnessDelta))
        }
    }

    fun addDrawingStroke(stroke: DrawingStroke) {
        val idx = selectedFrameIndex
        if (idx !in project.frames.indices) return
        pushUndo()
        val frame = project.frames[idx]
        val updated = frame.copy(drawings = frame.drawings + stroke)
        val frames = project.frames.toMutableList()
        frames[idx] = updated
        project = project.copy(frames = frames)
    }

    fun undoLastDoodleStroke() {
        val idx = selectedFrameIndex
        if (idx !in project.frames.indices) return
        val frame = project.frames[idx]
        if (frame.drawings.isEmpty()) return
        pushUndo()
        val updated = frame.copy(drawings = frame.drawings.dropLast(1))
        val frames = project.frames.toMutableList()
        frames[idx] = updated
        project = project.copy(frames = frames)
        showToast("Undid last doodle stroke")
    }

    fun copyDoodlesToNextFrame() {
        val idx = selectedFrameIndex
        if (idx >= project.frames.size - 1) return
        val currentFrame = project.frames[idx]
        if (currentFrame.drawings.isEmpty()) {
            showToast("No doodles to copy on current frame")
            return
        }
        pushUndo()
        val nextIdx = idx + 1
        val nextFrame = project.frames[nextIdx]
        val updated = nextFrame.copy(drawings = nextFrame.drawings + currentFrame.drawings)
        val frames = project.frames.toMutableList()
        frames[nextIdx] = updated
        project = project.copy(frames = frames)
        selectFrame(nextIdx)
        showToast("Copied doodles to Frame #${nextIdx + 1}")
    }

    fun stampDoodle(shapeName: String) {
        val idx = selectedFrameIndex
        if (idx !in project.frames.indices) return
        pushUndo()

        val colorHex = brushColor.toHexArgb()
        val strokes = when (shapeName.uppercase()) {
            "STAR" -> listOf(
                DrawingStroke(
                    points = listOf(
                        DrawingPoint(0.50f, 0.35f), DrawingPoint(0.53f, 0.45f),
                        DrawingPoint(0.63f, 0.45f), DrawingPoint(0.55f, 0.52f),
                        DrawingPoint(0.58f, 0.62f), DrawingPoint(0.50f, 0.56f),
                        DrawingPoint(0.42f, 0.62f), DrawingPoint(0.45f, 0.52f),
                        DrawingPoint(0.37f, 0.45f), DrawingPoint(0.47f, 0.45f),
                        DrawingPoint(0.50f, 0.35f)
                    ),
                    colorHex = colorHex,
                    strokeWidth = brushSize,
                    style = doodleStyle
                )
            )
            "LIGHTNING" -> listOf(
                DrawingStroke(
                    points = listOf(
                        DrawingPoint(0.52f, 0.30f), DrawingPoint(0.46f, 0.48f),
                        DrawingPoint(0.54f, 0.48f), DrawingPoint(0.47f, 0.70f)
                    ),
                    colorHex = colorHex,
                    strokeWidth = brushSize * 1.2f,
                    isGlow = true,
                    style = DoodleStyle.NEON_GLOW
                )
            )
            "SPEED_LINES" -> listOf(
                DrawingStroke(
                    points = listOf(DrawingPoint(0.35f, 0.50f), DrawingPoint(0.45f, 0.50f)),
                    colorHex = colorHex,
                    strokeWidth = brushSize,
                    style = DoodleStyle.DASHED_TRAIL
                ),
                DrawingStroke(
                    points = listOf(DrawingPoint(0.32f, 0.55f), DrawingPoint(0.47f, 0.55f)),
                    colorHex = colorHex,
                    strokeWidth = brushSize,
                    style = DoodleStyle.DASHED_TRAIL
                ),
                DrawingStroke(
                    points = listOf(DrawingPoint(0.36f, 0.60f), DrawingPoint(0.44f, 0.60f)),
                    colorHex = colorHex,
                    strokeWidth = brushSize,
                    style = DoodleStyle.DASHED_TRAIL
                )
            )
            "HEART" -> listOf(
                DrawingStroke(
                    points = listOf(
                        DrawingPoint(0.50f, 0.55f), DrawingPoint(0.45f, 0.48f),
                        DrawingPoint(0.42f, 0.42f), DrawingPoint(0.46f, 0.38f),
                        DrawingPoint(0.50f, 0.42f), DrawingPoint(0.54f, 0.38f),
                        DrawingPoint(0.58f, 0.42f), DrawingPoint(0.55f, 0.48f),
                        DrawingPoint(0.50f, 0.55f)
                    ),
                    colorHex = colorHex,
                    strokeWidth = brushSize,
                    style = doodleStyle
                )
            )
            else -> emptyList()
        }

        if (strokes.isNotEmpty()) {
            val frame = project.frames[idx]
            val updated = frame.copy(drawings = frame.drawings + strokes)
            val frames = project.frames.toMutableList()
            frames[idx] = updated
            project = project.copy(frames = frames)
            showToast("Added $shapeName doodle")
        }
    }

    fun clearFrameDrawings() {
        val idx = selectedFrameIndex
        if (idx !in project.frames.indices) return
        pushUndo()
        val frame = project.frames[idx]
        val updated = frame.copy(drawings = emptyList())
        val frames = project.frames.toMutableList()
        frames[idx] = updated
        project = project.copy(frames = frames)
        showToast("Cleared drawings")
    }

    fun setCleanPlateForProject(frameId: String) {
        pushUndo()
        val isSame = project.cleanPlateFrameId == frameId
        project = project.copy(cleanPlateFrameId = if (isSame) null else frameId)
        showToast(if (isSame) "Clean plate removed" else "Set as clean plate reference")
    }

    fun addImportedImages(files: List<File>) {
        if (files.isEmpty()) return
        pushUndo()
        val newFrames = files.map { file ->
            FrameItem(imagePath = file.absolutePath)
        }
        val combined = project.frames + newFrames
        project = project.copy(frames = combined)
        selectFrame((combined.size - newFrames.size).coerceAtLeast(0))
        showToast("Imported ${newFrames.size} frames")
    }

    fun importAudioFile(file: File) {
        scope.launch {
            val track = audioEngine.loadAudioTrack(file, project.fps)
            if (track != null) {
                pushUndo()
                project = project.copy(audioTracks = project.audioTracks + track)
                showToast("Added audio track: ${track.name}")
            } else {
                showToast("Failed to load audio: ${file.name}")
            }
        }
    }

    suspend fun loadDemo() {
        pause()
        showToast("Loading claymation demo animation...")
        val demo = DemoAnimationGenerator.generateDemoProject()
        project = demo

        // Auto load demo audio sound effect
        val audioFile = File(demo.projectDirectory, "clay_bounce_sfx.wav")
        if (audioFile.exists()) {
            audioEngine.loadAudioTrack(audioFile, demo.fps)?.let { track ->
                project = project.copy(audioTracks = listOf(track))
            }
        }

        clampIndices()
        showToast("Loaded demo project! Press Space to Play")
    }

    fun saveProjectToJson(file: File) {
        try {
            val json = Json { prettyPrint = true }
            val text = json.encodeToString(AnimationProject.serializer(), project)
            file.writeText(text)
            showToast("Saved project to ${file.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error saving project: ${e.message}")
        }
    }

    fun loadProjectFromJson(file: File) {
        try {
            val text = file.readText()
            val json = Json { ignoreUnknownKeys = true }
            val loaded = json.decodeFromString(AnimationProject.serializer(), text)
            project = loaded
            clampIndices()
            showToast("Opened project: ${loaded.title}")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Error opening project: ${e.message}")
        }
    }

    fun startExport(format: String, outputFile: File) {
        isExporting = true
        exportProgress = 0f
        exportMessage = "Starting export..."
        scope.launch {
            val result = when (format.uppercase()) {
                "MP4" -> VideoExporter.exportToMp4(project, outputFile) { p, m ->
                    exportProgress = p
                    exportMessage = m
                }
                "GIF" -> VideoExporter.exportToGif(project, outputFile) { p, m ->
                    exportProgress = p
                    exportMessage = m
                }
                "ZIP" -> VideoExporter.exportToZip(project, outputFile) { p, m ->
                    exportProgress = p
                    exportMessage = m
                }
                else -> Result.failure(Exception("Unknown format"))
            }

            isExporting = false
            if (result.isSuccess) {
                showToast("Export saved to: ${outputFile.name}")
                isExportDialogVisible = false
            } else {
                showToast("Export error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateSettings(newSettings: com.stopmotion.animator.model.AppSettings) {
        settings = newSettings
        com.stopmotion.animator.util.SettingsManager.saveSettings(newSettings)
        onionSkinPastCount = newSettings.defaultOnionPastCount
        onionSkinFutureCount = newSettings.defaultOnionFutureCount
        onionSkinOpacity = newSettings.defaultOnionOpacity
        onionTintMode = newSettings.defaultTintMode
        brushSize = newSettings.defaultBrushSize
        doodleStyle = newSettings.defaultDoodleStyle
        isLooping = newSettings.loopPlaybackByDefault
        showToast("Preferences saved")
    }

    fun resetSettingsToDefault() {
        val def = com.stopmotion.animator.model.AppSettings()
        updateSettings(def)
        showToast("Preferences reset to defaults")
    }

    private fun clampIndices() {
        val total = project.totalTimelineFrames
        currentTimelineIndex = currentTimelineIndex.coerceIn(0, (total - 1).coerceAtLeast(0))
        selectedFrameIndex = selectedFrameIndex.coerceIn(0, (project.frames.size - 1).coerceAtLeast(0))
    }
}
