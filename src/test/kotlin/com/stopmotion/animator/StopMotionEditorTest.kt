package com.stopmotion.animator

import com.stopmotion.animator.model.*
import com.stopmotion.animator.sample.DemoAnimationGenerator
import com.stopmotion.animator.util.ImageProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File

class StopMotionEditorTest {
    @Test
    fun testStrokeEraserLogic() {
        val stroke1 = DrawingStroke(
            points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.2f, 0.2f)),
            strokeWidth = 4f
        )
        val stroke2 = DrawingStroke(
            points = listOf(DrawingPoint(0.8f, 0.8f), DrawingPoint(0.9f, 0.9f)),
            strokeWidth = 4f
        )

        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        val frame = FrameItem(drawings = listOf(stroke1, stroke2))
        state.project = AnimationProject(frames = listOf(frame))
        state.selectedFrameIndex = 0

        // Erasing near (0.15, 0.15) should delete stroke1 but keep stroke2
        val erased = state.eraseStrokesNear(
            normX = 0.15f,
            normY = 0.15f,
            canvasWidth = 1000f,
            canvasHeight = 1000f,
            radiusPixels = 20f
        )

        assertTrue(erased)
        assertEquals(1, state.project.frames[0].drawings.size)
        assertEquals(stroke2.id, state.project.frames[0].drawings[0].id)
    }

    @Test
    fun testBrushSizeDoesNotRetroactivelyModifyExistingStrokes() {
        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        val frame = FrameItem()
        state.project = AnimationProject(frames = listOf(frame))
        state.selectedFrameIndex = 0

        // 1. Draw first stroke with brush size 6
        state.brushSize = 6f
        state.addDrawingStroke(DrawingStroke(strokeWidth = state.brushSize))
        assertEquals(6f, state.project.frames[0].drawings[0].strokeWidth, 0.01f)

        // 2. Change brush size to 24 for the NEXT stroke
        state.brushSize = 24f
        state.addDrawingStroke(DrawingStroke(strokeWidth = state.brushSize))

        // Line 1 should remain 6f, line 2 should be 24f
        assertEquals(2, state.project.frames[0].drawings.size)
        assertEquals(6f, state.project.frames[0].drawings[0].strokeWidth, 0.01f)
        assertEquals(24f, state.project.frames[0].drawings[1].strokeWidth, 0.01f)
    }

    @Test
    fun testZoomLevelsAndReset() {
        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        assertEquals(1.0f, state.zoomScale, 0.001f)

        state.zoomIn()
        assertTrue(state.zoomScale > 1.0f)

        state.zoomOut()
        state.zoomOut()
        assertTrue(state.zoomScale < 1.0f)

        state.resetZoom()
        assertEquals(1.0f, state.zoomScale, 0.001f)
        assertEquals(0f, state.panOffsetX, 0.001f)
        assertEquals(0f, state.panOffsetY, 0.001f)
    }
    @Test
    fun testTimelineMappingWithHoldExposures() {
        val frame1 = FrameItem(holdDuration = 2) // frames 0, 1
        val frame2 = FrameItem(holdDuration = 1) // frame 2
        val frame3 = FrameItem(holdDuration = 3) // frames 3, 4, 5

        val project = AnimationProject(
            fps = 12,
            frames = listOf(frame1, frame2, frame3)
        )

        assertEquals(6, project.totalTimelineFrames)
        assertEquals(0, project.timelineIndexToFrameIndex(0))
        assertEquals(0, project.timelineIndexToFrameIndex(1))
        assertEquals(1, project.timelineIndexToFrameIndex(2))
        assertEquals(2, project.timelineIndexToFrameIndex(3))
        assertEquals(2, project.timelineIndexToFrameIndex(4))
        assertEquals(2, project.timelineIndexToFrameIndex(5))

        assertEquals(0, project.frameIndexToTimelineIndex(0))
        assertEquals(2, project.frameIndexToTimelineIndex(1))
        assertEquals(3, project.frameIndexToTimelineIndex(2))
    }

    @Test
    fun testDoodleStrokesCreation() {
        val stroke = DrawingStroke(
            points = listOf(DrawingPoint(0.1f, 0.1f), DrawingPoint(0.2f, 0.2f)),
            colorHex = "#FF38BDF8",
            strokeWidth = 8f,
            style = DoodleStyle.NEON_GLOW
        )

        val frame = FrameItem(
            imagePath = "/tmp/test.png",
            drawings = listOf(stroke)
        )

        assertEquals(1, frame.drawings.size)
        assertEquals(DoodleStyle.NEON_GLOW, frame.drawings[0].style)
        assertEquals(8f, frame.drawings[0].strokeWidth, 0.01f)
    }

    @Test
    fun testImageDifferenceComputation() {
        val img1 = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        val img2 = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)

        // Set one pixel to red in img1
        img1.setRGB(10, 10, java.awt.Color.RED.rgb)
        img2.setRGB(10, 10, java.awt.Color.BLACK.rgb)

        val diff = ImageProcessor.computeDifference(img1, img2)
        assertNotNull(diff)
        assertEquals(100, diff.width)
        assertEquals(100, diff.height)

        val diffPixel = diff.getRGB(10, 10)
        val redComp = (diffPixel shr 16) and 0xFF
        assertTrue("Difference should amplify red channel", redComp > 0)
    }

    @Test
    fun testDemoProjectGeneration() = runBlocking {
        val project = DemoAnimationGenerator.generateDemoProject()
        assertNotNull(project)
        assertEquals(16, project.frames.size)
        assertEquals(12, project.fps)
        assertTrue(project.totalTimelineFrames >= 16)

        // Check clean plate stage exists
        assertNotNull(project.cleanPlateFrameId)
        val demoDir = File(project.projectDirectory ?: "")
        assertTrue(demoDir.exists())

        // Check WAV sound effect exists
        val sfx = File(demoDir, "clay_bounce_sfx.wav")
        assertTrue(sfx.exists())
        assertTrue(sfx.length() > 44) // Valid WAV file
    }

    @Test
    fun testFrameDeletionLogic() {
        val f1 = FrameItem(id = "1", holdDuration = 1)
        val f2 = FrameItem(id = "2", holdDuration = 2)
        val f3 = FrameItem(id = "3", holdDuration = 1)

        val project = AnimationProject(frames = listOf(f1, f2, f3))
        assertEquals(3, project.frames.size)

        // Delete frame at index 1 (f2)
        val afterSingleDelete = project.frames.filterIndexed { index, _ -> index != 1 }
        assertEquals(2, afterSingleDelete.size)
        assertEquals("1", afterSingleDelete[0].id)
        assertEquals("3", afterSingleDelete[1].id)

        // Batch delete frames with indices in {0, 2}
        val targets = setOf(0, 2)
        val afterBatchDelete = project.frames.filterIndexed { index, _ -> !targets.contains(index) }
        assertEquals(1, afterBatchDelete.size)
        assertEquals("2", afterBatchDelete[0].id)
    }

    @Test
    fun testContinuityCameraIdentification() {
        val macCam = com.stopmotion.animator.capture.CameraManager.CameraDevice("0", "FaceTime HD Camera", isContinuity = false)
        val iPhoneCam = com.stopmotion.animator.capture.CameraManager.CameraDevice("1", "Leuw's iPhone 15 Pro Camera", isContinuity = true)

        assertFalse(macCam.isContinuity)
        assertTrue(iPhoneCam.isContinuity)
    }

    @Test
    fun testDoodleColorParsing() {
        val testColors = listOf(
            androidx.compose.ui.graphics.Color(0xFFFF5E36), // Orange
            androidx.compose.ui.graphics.Color(0xFF38BDF8), // Cyan
            androidx.compose.ui.graphics.Color(0xFF10B981), // Emerald
            androidx.compose.ui.graphics.Color(0xFFFACC15), // Yellow
            androidx.compose.ui.graphics.Color(0xFFEC4899), // Pink
            androidx.compose.ui.graphics.Color(0xFFA855F7), // Purple
            androidx.compose.ui.graphics.Color.White
        )

        for (composeColor in testColors) {
            val hex = composeColor.toHexArgb()
            val parsedAwt = ImageProcessor.parseHexColor(hex)

            val expR = (composeColor.red * 255f).toInt()
            val expG = (composeColor.green * 255f).toInt()
            val expB = (composeColor.blue * 255f).toInt()

            assertEquals("Red mismatch for $hex", expR, parsedAwt.red)
            assertEquals("Green mismatch for $hex", expG, parsedAwt.green)
            assertEquals("Blue mismatch for $hex", expB, parsedAwt.blue)
        }
    }

    @Test
    fun testThemePresetsAndPalettes() {
        val presets = AppThemePreset.values()
        assertEquals(6, presets.size)

        for (preset in presets) {
            val palette = com.stopmotion.animator.ui.theme.StudioPalettes.get(preset)
            assertNotNull(palette.dark)
            assertNotNull(palette.surface)
            assertNotNull(palette.card)
            assertNotNull(palette.primary)
            assertNotNull(palette.secondary)
            assertNotNull(palette.accent)
        }
    }

    @Test
    fun testAppSettingsThemePresetSerialization() {
        val original = AppSettings(themePreset = AppThemePreset.CYBER_CYAN)
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val serialized = json.encodeToString(AppSettings.serializer(), original)
        val deserialized = json.decodeFromString(AppSettings.serializer(), serialized)

        assertEquals(AppThemePreset.CYBER_CYAN, deserialized.themePreset)
    }

    @Test
    fun testCameraSnapCallbackTrigger() {
        var snapTriggered = false
        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        state.onCameraSnapRequested = {
            snapTriggered = true
        }

        state.triggerCameraSnap()
        assertTrue("Camera snap callback should be called", snapTriggered)
    }

    @Test
    fun testPassiveProjectSave() {
        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        val tempFile = File.createTempFile("pancake_test", ".smproj")
        tempFile.deleteOnExit()


        var promptCalled = false
        // When unsaved (currentProjectFile == null), it should invoke the prompt callback
        state.saveProjectPassively {
            promptCalled = true
        }
        assertTrue("Should prompt for save when file is not set", promptCalled)

        // Once saved to a file, passive save should directly write to file without prompting
        state.saveProjectToJson(tempFile)
        assertEquals(tempFile, state.currentProjectFile)

        var promptedAgain = false
        state.saveProjectPassively {
            promptedAgain = true
        }
        assertFalse("Should passively save without prompting when file is already set", promptedAgain)
        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 0)
    }

    @Test
    fun testNativeFileDialogFlag() {
        assertFalse("Dialog should default to not visible", com.stopmotion.animator.util.NativeFileDialogs.isNativeDialogVisible)
    }

    @Test
    fun testLastOpenedProjectPersistence() {
        val state = com.stopmotion.animator.state.EditorState(skipInit = true)
        val tempFile = File.createTempFile("pancake_last_opened", ".smproj")
        tempFile.deleteOnExit()

        state.project = com.stopmotion.animator.model.AnimationProject(title = "Custom User Project")
        state.saveProjectToJson(tempFile)

        assertEquals(tempFile.absolutePath, state.settings.lastOpenedProjectPath)

        // Verify loaded from SettingsManager
        val savedSettings = com.stopmotion.animator.util.SettingsManager.loadSettings()
        assertEquals(tempFile.absolutePath, savedSettings.lastOpenedProjectPath)

        // Verify loadProjectFromJson also records last opened project
        val anotherTemp = File.createTempFile("pancake_another", ".smproj")
        anotherTemp.deleteOnExit()
        anotherTemp.writeText(tempFile.readText())

        state.loadProjectFromJson(anotherTemp)
        assertEquals(anotherTemp.absolutePath, state.settings.lastOpenedProjectPath)
    }
}
