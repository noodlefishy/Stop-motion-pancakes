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
        val state = com.stopmotion.animator.state.EditorState()
        state.onCameraSnapRequested = {
            snapTriggered = true
        }

        state.triggerCameraSnap()
        assertTrue("Camera snap callback should be called", snapTriggered)
    }
}
