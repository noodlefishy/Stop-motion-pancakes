package com.stopmotion.animator.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.capture.CameraManager
import com.stopmotion.animator.capture.CameraStreamer
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.ImageCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraCaptureDialog(state: EditorState) {
    if (!state.isCameraCaptureDialogVisible) return

    val coroutineScope = rememberCoroutineScope()
    val streamer = remember { CameraStreamer() }

    var cameras by remember { mutableStateOf(listOf<CameraManager.CameraDevice>()) }
    var selectedCameraId by remember { mutableStateOf("0") }
    var cameraDropdownOpen by remember { mutableStateOf(false) }
    var isRefreshingCameras by remember { mutableStateOf(false) }

    var livePreviewFrame by remember { mutableStateOf<ImageBitmap?>(null) }

    var isCapturing by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf<Int?>(null) }
    var intervalometerActive by remember { mutableStateOf(false) }
    var intervalSeconds by remember { mutableStateOf(5) }

    var viewfinderOnionSkin by remember { mutableStateOf(true) }
    var viewfinderOnionOpacity by remember { mutableStateOf(0.40f) }

    fun restartStream(deviceId: String) {
        livePreviewFrame = null
        streamer.start(
            deviceId = deviceId,
            scope = coroutineScope
        ) { frame ->
            livePreviewFrame = frame
        }
    }

    fun refreshCameras() {
        isRefreshingCameras = true
        coroutineScope.launch {
            val list = CameraManager.listCameras()
            cameras = list
            if (list.isNotEmpty()) {
                val currentExists = list.any { it.id == selectedCameraId }
                if (!currentExists) {
                    val preferred = list.find { it.isContinuity } ?: list.first()
                    selectedCameraId = preferred.id
                    restartStream(preferred.id)
                } else {
                    restartStream(selectedCameraId)
                }
            }
            isRefreshingCameras = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCameras()
    }

    fun triggerSnap() {
        if (isCapturing) return
        isCapturing = true

        val captureDir = if (!state.project.projectDirectory.isNullOrBlank()) {
            File(state.project.projectDirectory, "captures")
        } else {
            File(state.settings.defaultProjectsPath, "captures")
        }
        captureDir.mkdirs()

        val destFile = File(captureDir, "capture_${System.currentTimeMillis()}.png")

        streamer.snapCurrentFrame(destFile, coroutineScope) { success ->
            isCapturing = false
            if (success) {
                state.addImportedImages(listOf(destFile))
                state.showToast("Captured frame added to timeline!")
            } else {
                state.showToast("Capture failed — no live frame available yet.")
            }
        }
    }

    fun startCountdownSnap() {
        coroutineScope.launch {
            countdownValue = 3
            delay(1000)
            countdownValue = 2
            delay(1000)
            countdownValue = 1
            delay(1000)
            countdownValue = null
            triggerSnap()
        }
    }

    DisposableEffect(Unit) {
        state.onCameraSnapRequested = { triggerSnap() }
        onDispose {
            streamer.stop()
            state.onCameraSnapRequested = null
        }
    }

    // IN-APP MODAL OVERLAY (Never drops behind the main window, never auto-dismisses on focus change)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = { /* Intercept clicks so background doesn't trigger */ }),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(680.dp)
                .wrapContentHeight()
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Videocam, null, tint = StudioAccent, modifier = Modifier.size(22.dp))
                        Text(
                            "Stop Motion Pancake — Camera Studio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            streamer.stop()
                            state.isCameraCaptureDialogVisible = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                // Camera Device Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Device:", fontSize = 12.sp, color = StudioTextSecondary)

                    val currentCam = cameras.find { it.id == selectedCameraId }

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = StudioDark,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { cameraDropdownOpen = true }
                                .border(
                                    1.dp,
                                    if (currentCam?.isContinuity == true) StudioAccent else StudioBorder,
                                    RoundedCornerShape(6.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        if (currentCam?.isContinuity == true) Icons.Default.PhoneIphone else Icons.Default.Videocam,
                                        null,
                                        tint = if (currentCam?.isContinuity == true) StudioAccent else StudioSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )

                                    Text(
                                        text = currentCam?.name ?: "Detecting cameras...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (currentCam?.isContinuity == true) StudioAccent else StudioTextPrimary,
                                        maxLines = 1
                                    )
                                }

                                Icon(Icons.Default.ArrowDropDown, null, tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = cameraDropdownOpen,
                            onDismissRequest = { cameraDropdownOpen = false }
                        ) {
                            cameras.forEach { cam ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            cam.name + if (cam.isContinuity) " (Continuity Camera)" else "",
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        selectedCameraId = cam.id
                                        cameraDropdownOpen = false
                                        restartStream(cam.id)
                                    }
                                )
                            }
                        }
                    }

                    // // Resolution Dropdown
                    // Box {
                    //     Surface(
                    //         color = StudioDark,
                    //         shape = RoundedCornerShape(6.dp),
                    //         modifier = Modifier
                    //             .height(38.dp)
                    //             .clip(RoundedCornerShape(6.dp))
                    //             .clickable { resolutionDropdownOpen = true }
                    //             .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    //     ) {
                    //         Row(
                    //             modifier = Modifier
                    //                 .fillMaxHeight()
                    //                 .padding(horizontal = 10.dp),
                    //             verticalAlignment = Alignment.CenterVertically,
                    //             horizontalArrangement = Arrangement.spacedBy(4.dp)
                    //         ) {
                    //             Text(selectedResolution, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioSecondary)
                    //             Icon(Icons.Default.ArrowDropDown, null, tint = StudioTextSecondary, modifier = Modifier.size(14.dp))
                    //         }
                    //     }

                    //     DropdownMenu(
                    //         expanded = resolutionDropdownOpen,
                    //         onDismissRequest = { resolutionDropdownOpen = false }
                    //     ) {
                    //         listOf("1920x1080" to "1080p (Full HD)", "1280x720" to "720p (HD)", "640x480" to "480p (Fast)").forEach { (res, label) ->
                    //             DropdownMenuItem(
                    //                 text = { Text(label, fontSize = 11.sp) },
                    //                 onClick = {
                    //                     selectedResolution = res
                    //                     resolutionDropdownOpen = false
                    //                     restartStream(selectedCameraId)
                    //                 }
                    //             )
                    //         }
                    //     }
                    // }
                
                }

                // Viewfinder Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val lastFrame = state.selectedFrame ?: state.project.frames.lastOrNull()
                    val lastFrameThumb = remember(lastFrame?.imagePath) {
                        lastFrame?.let { ImageCache.getThumbnail(it.imagePath, 640, 360) }
                    }

                    val liveFrame = livePreviewFrame
                    if (liveFrame != null) {
                        Image(
                            bitmap = liveFrame,
                            contentDescription = "Live Camera Feed",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = StudioAccent,
                                strokeWidth = 2.5.dp
                            )
                            Text("Connecting live camera feed...", fontSize = 12.sp, color = StudioTextSecondary)
                        }
                    }

                    if (viewfinderOnionSkin && lastFrameThumb != null) {
                        Image(
                            bitmap = lastFrameThumb,
                            contentDescription = "Onion Skin Previous Frame",
                            contentScale = ContentScale.Fit,
                            alpha = viewfinderOnionOpacity,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    countdownValue?.let { count ->
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(StudioPrimary.copy(alpha = 0.90f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(count.toString(), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Viewfinder Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = viewfinderOnionSkin,
                            onClick = { viewfinderOnionSkin = !viewfinderOnionSkin },
                            label = { Text("Ghost Previous", fontSize = 11.sp) }
                        )

                        if (viewfinderOnionSkin) {
                            Text("${(viewfinderOnionOpacity * 100).toInt()}%", fontSize = 10.sp, color = StudioTextSecondary)
                            Slider(
                                value = viewfinderOnionOpacity,
                                onValueChange = { viewfinderOnionOpacity = it },
                                valueRange = 0.1f..0.9f,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { startCountdownSnap() },
                            enabled = !isCapturing && countdownValue == null,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("3s Timer", fontSize = 11.sp)
                        }

                        FilterChip(
                            selected = intervalometerActive,
                            onClick = {
                                intervalometerActive = !intervalometerActive
                                if (intervalometerActive) {
                                    coroutineScope.launch {
                                        while (intervalometerActive && state.isCameraCaptureDialogVisible) {
                                            triggerSnap()
                                            delay(intervalSeconds * 1000L)
                                        }
                                    }
                                }
                            },
                            label = { Text("Timelapse (${intervalSeconds}s)", fontSize = 11.sp) }
                        )
                    }
                }

                // SNAP FRAME Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hint: Press Spacebar or Enter to snap frame",
                        fontSize = 11.sp,
                        color = StudioTextMuted
                    )

                    Button(
                        onClick = { triggerSnap() },
                        enabled = !isCapturing && countdownValue == null,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isCapturing) "Capturing..." else "SNAP FRAME",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}