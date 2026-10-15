package com.stopmotion.animator.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    // 1. Camera & Capture
    val defaultCameraId: String = "0",
    val defaultResolution: String = "1920x1080",
    val playShutterSound: Boolean = true,
    val autoAdvancePlayheadOnSnap: Boolean = true,
    val defaultCountdownSeconds: Int = 3,
    val defaultTimelapseIntervalSeconds: Int = 5,

    // 2. Onion Skin & Guides
    val defaultOnionPastCount: Int = 2,
    val defaultOnionFutureCount: Int = 1,
    val defaultOnionOpacity: Float = 0.40f,
    val defaultTintMode: OnionTintMode = OnionTintMode.RED_GREEN_TINT,
    val showGridByDefault: Boolean = false,
    val showSafeZoneByDefault: Boolean = false,

    // 3. Playback & Timeline
    val defaultFps: Int = 12,
    val defaultHoldDuration: Int = 1,
    val audioScrubbingEnabled: Boolean = true,
    val loopPlaybackByDefault: Boolean = true,

    // 4. Doodle Defaults
    val defaultDoodleStyle: DoodleStyle = DoodleStyle.SOLID_PEN,
    val defaultBrushSize: Float = 6f,
    val defaultBrushColorHex: String = "#FFFF5E36",

    // 5. Export & Storage
    val exportQualityCrf: Int = 18, // 18 = High cinematic quality, 23 = Standard
    val defaultExportFormat: String = "MP4",
    val autoSaveMinutes: Int = 5,
    val maxCacheFrames: Int = 60
)
