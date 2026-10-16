package com.stopmotion.animator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.stopmotion.animator.model.EditorTool
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.components.*
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.NativeFileDialogs
import kotlinx.coroutines.launch
import java.io.File

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1380.dp, 880.dp),
        position = WindowPosition(Alignment.Center)
    )

    val coroutineScope = rememberCoroutineScope()
    val editorState = remember { EditorState(coroutineScope) }

    Window(
        onCloseRequest = {
            editorState.audioEngine.release()
            exitApplication()
        },
        title = "Stop Motion Pancake — ${editorState.project.title}",
        state = windowState,
onKeyEvent = { keyEvent ->
            if (NativeFileDialogs.isNativeDialogVisible) {
                return@Window false
            }

            // Spacebar release: if we were not dragging to pan, toggle playback
            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Spacebar) {
                val wasPanning = editorState.wasPanDragTriggered
                editorState.isSpaceHeld = false
                editorState.wasPanDragTriggered = false
                if (!wasPanning && !editorState.isCameraCaptureDialogVisible) {
                    editorState.togglePlayback()
                }
                return@Window true
            }

            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    // Spacebar press: enter hold-to-pan mode
                    keyEvent.key == Key.Spacebar && !keyEvent.isMetaPressed -> {
                        if (editorState.isCameraCaptureDialogVisible) {
                            editorState.triggerCameraSnap()
                            true
                        } else {
                            editorState.isSpaceHeld = true
                            true
                        }
                    }
                    // H: Hand / Pan Tool
                    keyEvent.key == Key.H && !keyEvent.isMetaPressed -> {
                        editorState.activeTool = EditorTool.HAND
                        editorState.showToast("Hand Pan Tool (or hold Spacebar to pan)")
                        true
                    }
                    // Cmd+N: New Project in default path
                    keyEvent.isMetaPressed && keyEvent.key == Key.N -> {
                        val defaultDir = editorState.settings.defaultProjectsPath
                        java.io.File(defaultDir).mkdirs()
                        val target = NativeFileDialogs.chooseNewProjectLocation(defaultDirectory = defaultDir)
                        if (target != null) {
                            val title = target.nameWithoutExtension.ifBlank { "Untitled" }
                            editorState.project = com.stopmotion.animator.model.AnimationProject(
                                title = title,
                                projectDirectory = target.parentFile?.absolutePath ?: defaultDir
                            )
                            editorState.saveProjectToJson(target)
                            editorState.showToast("Created project: $title")
                        }
                        true
                    }
                    // Cmd+S: Save Project Passively using project or default directory
                    keyEvent.isMetaPressed && keyEvent.key == Key.S && !keyEvent.isShiftPressed -> {
                        editorState.saveProjectPassively {
                            val dir = editorState.project.projectDirectory ?: editorState.settings.defaultProjectsPath
                            val file = NativeFileDialogs.saveProject(
                                defaultName = "${editorState.project.title}.smproj",
                                defaultDirectory = dir
                            )
                            if (file != null) {
                                editorState.saveProjectToJson(file)
                            }
                        }
                        true
                    }
                    // Cmd+Shift+S: Save Project As...
                    keyEvent.isMetaPressed && keyEvent.key == Key.S && keyEvent.isShiftPressed -> {
                        val dir = editorState.project.projectDirectory ?: editorState.settings.defaultProjectsPath
                        val file = NativeFileDialogs.saveProject(
                            defaultName = "${editorState.project.title}.smproj",
                            defaultDirectory = dir
                        )
                        if (file != null) {
                            editorState.saveProjectToJson(file)
                        }
                        true
                    }
                    keyEvent.key == Key.Enter -> {
                        if (editorState.isCameraCaptureDialogVisible) {
                            editorState.triggerCameraSnap()
                            true
                        } else false
                    }
                    // Left Arrow: Step -1 frame
                    keyEvent.key == Key.DirectionLeft -> {
                        editorState.stepFrame(-1)
                        true
                    }
                    // Right Arrow: Step +1 frame
                    keyEvent.key == Key.DirectionRight -> {
                        editorState.stepFrame(1)
                        true
                    }
                    // Home: Jump to start
                    keyEvent.key == Key.MoveHome -> {
                        editorState.jumpToStart()
                        true
                    }
                    // End: Jump to end
                    keyEvent.key == Key.MoveEnd -> {
                        editorState.jumpToEnd()
                        true
                    }
                    // Cmd+Z: Undo
                    keyEvent.isMetaPressed && keyEvent.key == Key.Z && !keyEvent.isShiftPressed -> {
                        editorState.undo()
                        true
                    }
                    // Cmd+Shift+Z: Redo
                    keyEvent.isMetaPressed && keyEvent.key == Key.Z && keyEvent.isShiftPressed -> {
                        editorState.redo()
                        true
                    }
                    // Cmd+D: Duplicate Frame
                    keyEvent.isMetaPressed && keyEvent.key == Key.D && !keyEvent.isShiftPressed -> {
                        editorState.duplicateSelectedFrame()
                        true
                    }
                    // Cmd+Shift+D: Copy Doodles to Next Frame
                    keyEvent.isMetaPressed && keyEvent.key == Key.D && keyEvent.isShiftPressed -> {
                        editorState.copyDoodlesToNextFrame()
                        true
                    }
                    // O: Toggle Onion Skin
                    keyEvent.key == Key.O && !keyEvent.isMetaPressed -> {
                        editorState.onionSkinEnabled = !editorState.onionSkinEnabled
                        editorState.showToast("Onion skin: " + if (editorState.onionSkinEnabled) "ON" else "OFF")
                        true
                    }
                    // B: Brush / Doodle Tool
                    keyEvent.key == Key.B && !keyEvent.isMetaPressed -> {
                        editorState.activeTool = EditorTool.DRAW_PEN
                        editorState.showToast("Doodle Pen Tool")
                        true
                    }
                    keyEvent.key == Key.E && !keyEvent.isMetaPressed -> {
                        editorState.activeTool = EditorTool.ERASER
                        editorState.showToast("Eraser Tool")
                        true
                    }
                    keyEvent.key == Key.C && !keyEvent.isMetaPressed -> {
                        editorState.isCameraCaptureDialogVisible = true
                        true
                    }
                    // Delete / Backspace: Delete selected frame(s)
                    (keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete) -> {
                        editorState.deleteSelectedFrames()
                        true
                    }
                    // Cmd+,: Preferences / Settings
                    keyEvent.isMetaPressed && keyEvent.key == Key.Comma -> {
                        editorState.isSettingsDialogVisible = true
                        true
                    }
                    // E: Eraser Tool
                    keyEvent.key == Key.E && !keyEvent.isMetaPressed -> {
                        editorState.activeTool = EditorTool.ERASER
                        editorState.showToast("Eraser Tool")
                        true
                    }
                    // Cmd + Plus / Equals: Zoom In
                    keyEvent.isMetaPressed && (keyEvent.key == Key.Equals || keyEvent.key == Key.NumPadAdd) -> {
                        editorState.zoomIn()
                        true
                    }
                    keyEvent.isMetaPressed && (keyEvent.key == Key.Minus || keyEvent.key == Key.NumPadSubtract) -> {
                        editorState.zoomOut()
                        true
                    }
                    // Cmd + 0: Reset Zoom
                    keyEvent.isMetaPressed && (keyEvent.key == Key.Zero || keyEvent.key == Key.NumPad0) -> {
                        editorState.resetZoom()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    ) {
        // macOS Native Menu Bar
        MenuBar {
            Menu("File", mnemonic = 'F') {
                Item("New Project", shortcut = KeyShortcut(Key.N, meta = true)) {
                    val target = NativeFileDialogs.chooseNewProjectLocation()
                    if (target != null) {
                        val title = target.nameWithoutExtension.ifBlank { "Untitled" }
                        editorState.project = com.stopmotion.animator.model.AnimationProject(
                            title = title,
                            projectDirectory = target.parentFile?.absolutePath
                        )
                        editorState.saveProjectToJson(target)
                        editorState.showToast("Created project: $title")
                    }
                }
                Item("Open Project...", shortcut = KeyShortcut(Key.O, meta = true)) {
                    val file = NativeFileDialogs.openProject()
                    if (file != null) {
                        editorState.loadProjectFromJson(file)
                    }
                }
                Item("Save Project", shortcut = KeyShortcut(Key.S, meta = true)) {
                    editorState.saveProjectPassively {
                        val dir = editorState.project.projectDirectory ?: System.getProperty("user.home")
                        val file = NativeFileDialogs.saveProject(
                            defaultName = "${editorState.project.title}.smproj",
                            defaultDirectory = dir
                        )
                        if (file != null) {
                            editorState.saveProjectToJson(file)
                        }
                    }
                }
                Item("Save Project As...", shortcut = KeyShortcut(Key.S, meta = true, shift = true)) {
                    val dir = editorState.project.projectDirectory ?: System.getProperty("user.home")
                    val file = NativeFileDialogs.saveProject(
                        defaultName = "${editorState.project.title}.smproj",
                        defaultDirectory = dir
                    )
                    if (file != null) {
                        editorState.saveProjectToJson(file)
                    }
                }
                Separator()
                Item("Load Demo Animation") {
                    coroutineScope.launch { editorState.loadDemo() }
                }
                Item("Import Frames...", shortcut = KeyShortcut(Key.I, meta = true)) {
                    val files = NativeFileDialogs.openImages()
                    if (files.isNotEmpty()) {
                        editorState.addImportedImages(files)
                    }
                }
                Separator()
                Item("Export Video (MP4 / GIF / ZIP)...", shortcut = KeyShortcut(Key.E, meta = true)) {
                    editorState.isExportDialogVisible = true
                }
                Separator()
                Item("Preferences / Settings...", shortcut = KeyShortcut(Key.Comma, meta = true)) {
                    editorState.isSettingsDialogVisible = true
                }
                Separator()
                Item("Quit", shortcut = KeyShortcut(Key.Q, meta = true)) {
                    exitApplication()
                }
            }

            Menu("Edit", mnemonic = 'E') {
                Item("Undo", shortcut = KeyShortcut(Key.Z, meta = true)) { editorState.undo() }
                Item("Redo", shortcut = KeyShortcut(Key.Z, meta = true, shift = true)) { editorState.redo() }
                Separator()
                Item("Duplicate Frame", shortcut = KeyShortcut(Key.D, meta = true)) { editorState.duplicateSelectedFrame() }
                Item("Delete Frame(s)", shortcut = KeyShortcut(Key.Backspace)) { editorState.deleteSelectedFrames() }
                Item("Reverse Frames") { editorState.reverseSelectedFrames() }
                Separator()
                Item("Hold on 1s") { editorState.setHoldForSelected(1) }
                Item("Hold on 2s") { editorState.setHoldForSelected(2) }
                Item("Hold on 3s") { editorState.setHoldForSelected(3) }
            }

            Menu("Doodles", mnemonic = 'D') {
                Item("Doodle Pen Tool", shortcut = KeyShortcut(Key.B)) { editorState.activeTool = EditorTool.DRAW_PEN }
                Item("Neon Glow Doodle") { editorState.activeTool = EditorTool.DRAW_GLOW }
                Item("Sketch Chalk Doodle") { editorState.activeTool = EditorTool.DRAW_CHALK }
                Item("Dashed Speed Trail") { editorState.activeTool = EditorTool.DRAW_DASHED }
                Separator()
                Item("Copy Doodles to Next Frame", shortcut = KeyShortcut(Key.D, meta = true, shift = true)) {
                    editorState.copyDoodlesToNextFrame()
                }
                Item("Undo Last Doodle Stroke") { editorState.undoLastDoodleStroke() }
                Item("Clear Frame Doodles") { editorState.clearFrameDrawings() }
                Item("Eraser Tool", shortcut = KeyShortcut(Key.E)) { editorState.activeTool = EditorTool.ERASER }
                Separator()
                Item("Stamp Star ★") { editorState.stampDoodle("STAR") }
                Item("Stamp Lightning ⚡") { editorState.stampDoodle("LIGHTNING") }
                Item("Stamp Speed Lines 💨") { editorState.stampDoodle("SPEED_LINES") }
            }

            Menu("Playback", mnemonic = 'P') {
                Item("Play / Pause", shortcut = KeyShortcut(Key.Spacebar)) { editorState.togglePlayback() }
                Item("Step Forward", shortcut = KeyShortcut(Key.DirectionRight)) { editorState.stepFrame(1) }
                Item("Step Backward", shortcut = KeyShortcut(Key.DirectionLeft)) { editorState.stepFrame(-1) }
                Item("Jump to Start") { editorState.jumpToStart() }
                Item("Jump to End") { editorState.jumpToEnd() }
                Separator()
                CheckboxItem("Loop Playback", checked = editorState.isLooping) { editorState.isLooping = !editorState.isLooping }
                CheckboxItem("Ping-Pong Playback", checked = editorState.isPingPong) { editorState.isPingPong = !editorState.isPingPong }
            }

            Menu("View", mnemonic = 'V') {
                CheckboxItem("Onion Skinning", checked = editorState.onionSkinEnabled, shortcut = KeyShortcut(Key.O)) {
                    editorState.onionSkinEnabled = !editorState.onionSkinEnabled
                }
                CheckboxItem("Rule of Thirds Grid", checked = editorState.showGrid) { editorState.showGrid = !editorState.showGrid }
                CheckboxItem("Safe Action Margins", checked = editorState.showSafeZone) { editorState.showSafeZone = !editorState.showSafeZone }
                CheckboxItem("Split Screen Compare", checked = editorState.splitCompare) { editorState.splitCompare = !editorState.splitCompare }
                CheckboxItem("Difference / Flicker Mode", checked = editorState.differenceMode) { editorState.differenceMode = !editorState.differenceMode }
                Separator()
                Item("Zoom In", shortcut = KeyShortcut(Key.Equals, meta = true)) { editorState.zoomIn() }
                Item("Zoom Out", shortcut = KeyShortcut(Key.Minus, meta = true)) { editorState.zoomOut() }
                Item("Fit to Screen / 100%", shortcut = KeyShortcut(Key.Zero, meta = true)) { editorState.resetZoom() }
            }
        }

        StopMotionTheme(preset = editorState.settings.themePreset) {
            Box(modifier = Modifier.fillMaxSize().background(StudioDark)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Top Bar
                    TopBar(editorState)

                    // 2. Middle Work Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Left Tool Bar
                        LeftToolbar(editorState)

                        // Center Viewport & Stage
                        CanvasViewport(
                            state = editorState,
                            modifier = Modifier.weight(1f)
                        )

                        // Right Inspector Panel
                        InspectorPanel(editorState)
                    }

                    // 3. Bottom Filmstrip & Audio Timeline
                    FilmstripTimeline(editorState)
                }

                // Toast Notification Overlay
                editorState.toastMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 195.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E2126).copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, null, tint = StudioPrimary, modifier = Modifier.size(16.dp))
                            Text(msg, fontSize = 12.sp, color = StudioTextPrimary)
                        }
                    }
                }

                // Modal Dialogs
                ExportDialog(editorState)
                CameraCaptureDialog(editorState)
                SettingsDialog(editorState)
            }
        }
    }
}
