package com.stopmotion.animator.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectRatio(val label: String, val ratio: Float) {
    RATIO_16_9("16:9 (Widescreen)", 16f / 9f),
    RATIO_4_3("4:3 (Classic TV)", 4f / 3f),
    RATIO_1_1("1:1 (Square)", 1f),
    RATIO_9_16("9:16 (Vertical/Reels)", 9f / 16f),
    RATIO_2_39_1("2.39:1 (Cinematic)", 2.39f)
}

@Serializable
enum class OnionTintMode {
    TRUE_COLOR,
    RED_GREEN_TINT,
    MONOCHROME,
    DIFFERENCE
}

@Serializable
enum class EditorTool {
    SELECT,
    DRAW_PEN,
    DRAW_GLOW,
    DRAW_CHALK,
    DRAW_DASHED,
    ERASER,
    RIG_REMOVAL,
    COLOR_GRADE,
    CHROMA_KEY
}

@Serializable
enum class DoodleStyle {
    SOLID_PEN,
    NEON_GLOW,
    SKETCH_CHALK,
    DASHED_TRAIL
}

@Serializable
data class DrawingPoint(
    val x: Float, // Normalized 0.0 .. 1.0 coordinates relative to frame
    val y: Float
)

@Serializable
data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DrawingPoint> = emptyList(),
    val colorHex: String = "#FFFFFFFF",
    val strokeWidth: Float = 4f,
    val isGlow: Boolean = false,
    val isRigRemoval: Boolean = false,
    val style: DoodleStyle = DoodleStyle.SOLID_PEN
)

fun androidx.compose.ui.graphics.Color.toHexArgb(): String {
    val a = (alpha * 255f).toInt().coerceIn(0, 255)
    val r = (red * 255f).toInt().coerceIn(0, 255)
    val g = (green * 255f).toInt().coerceIn(0, 255)
    val b = (blue * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X%02X".format(a, r, g, b)
}

@Serializable
data class FrameItem(
    val id: String = UUID.randomUUID().toString(),
    val imagePath: String = "", // Relative or absolute file path
    val holdDuration: Int = 1, // Number of frame intervals to hold (1 = on 1s, 2 = on 2s, etc.)
    val brightness: Float = 0f, // -1.0 to 1.0
    val contrast: Float = 1f, // 0.2 to 2.0
    val saturation: Float = 1f, // 0.0 to 2.0
    val temperature: Float = 0f, // -1.0 (cool) to 1.0 (warm)
    val drawings: List<DrawingStroke> = emptyList(),
    val isCleanPlate: Boolean = false,
    val notes: String = ""
)

@Serializable
data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String,
    val startFrame: Int = 0,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val waveformPeaks: List<Float> = emptyList(),
    val durationFrames: Int = 0
)

@Serializable
data class XSheetRow(
    val frameIndex: Int,
    val phoneme: String = "",
    val actionNote: String = "",
    val cameraNote: String = ""
)

@Serializable
data class AnimationProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Animation",
    val fps: Int = 12, // Default to 12 FPS standard stop-motion animation
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val frames: List<FrameItem> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val xSheetRows: List<XSheetRow> = emptyList(),
    val cleanPlateFrameId: String? = null,
    val projectDirectory: String? = null
) {
    /**
     * Total play duration in timeline frames, taking into account hold exposures (on 1s, on 2s, etc.)
     */
    val totalTimelineFrames: Int
        get() = frames.sumOf { it.holdDuration }

    /**
     * Map timeline frame index (0..totalTimelineFrames-1) to the actual FrameItem index in `frames`.
     */
    fun timelineIndexToFrameIndex(timelineIndex: Int): Int {
        if (frames.isEmpty()) return 0
        var accumulated = 0
        for ((idx, frame) in frames.withIndex()) {
            accumulated += frame.holdDuration
            if (timelineIndex < accumulated) {
                return idx
            }
        }
        return (frames.size - 1).coerceAtLeast(0)
    }

    /**
     * Get the timeline start index for a given frame index.
     */
    fun frameIndexToTimelineIndex(frameIndex: Int): Int {
        if (frameIndex <= 0) return 0
        var accumulated = 0
        for (i in 0 until frameIndex.coerceAtMost(frames.size)) {
            accumulated += frames[i].holdDuration
        }
        return accumulated
    }
}
