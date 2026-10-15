package com.stopmotion.animator.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stopmotion.animator.state.EditorState
import com.stopmotion.animator.ui.theme.*
import com.stopmotion.animator.util.ImageCache

@Composable
fun FilmstripTimeline(state: EditorState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Auto-scroll filmstrip to selected frame when playback progresses
    LaunchedEffect(state.selectedFrameIndex) {
        if (state.selectedFrameIndex in state.project.frames.indices) {
            listState.animateScrollToItem((state.selectedFrameIndex - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(StudioDark)
            .border(width = 1.dp, color = StudioBorder)
    ) {
        // Timeline Header: Quick Editing Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(StudioSurface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Selection details and frame count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${state.project.frames.size} frames (${state.project.totalTimelineFrames} exposures)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StudioTextSecondary
                )

                if (state.selectedFrameIndices.size > 1) {
                    Surface(
                        color = StudioPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${state.selectedFrameIndices.size} selected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Right: Batch actions (Move, Duplicate, Delete, Reverse, Quick Hold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimelineActionButton(
                    icon = Icons.Default.ArrowBack,
                    tooltip = "Move Left",
                    onClick = { state.moveSelectedFrame(-1) }
                )
                TimelineActionButton(
                    icon = Icons.Default.ArrowForward,
                    tooltip = "Move Right",
                    onClick = { state.moveSelectedFrame(1) }
                )
                TimelineActionButton(
                    icon = Icons.Default.ContentCopy,
                    tooltip = "Duplicate Frame",
                    onClick = { state.duplicateSelectedFrame() }
                )
                TimelineActionButton(
                    icon = Icons.Default.SwapHoriz,
                    tooltip = "Reverse Selection",
                    onClick = { state.reverseSelectedFrames() }
                )
                TimelineActionButton(
                    icon = Icons.Default.DeleteOutline,
                    tooltip = "Delete Frame(s)",
                    onClick = { state.deleteSelectedFrames() }
                )

                Spacer(Modifier.width(8.dp))

                // Quick hold presets
                listOf(1, 2, 3).forEach { hold ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { state.setHoldForSelected(hold) },
                        color = StudioCard
                    ) {
                        Text(
                            text = "${hold}s",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Filmstrip Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 6.dp)
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(state.project.frames) { idx, frame ->
                    val isSelected = state.selectedFrameIndex == idx || state.selectedFrameIndices.contains(idx)
                    val isCleanPlate = state.project.cleanPlateFrameId == frame.id

                    val thumb = remember(frame.imagePath) {
                        ImageCache.getThumbnail(frame.imagePath, 110, 75)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(105.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioCard)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) StudioPrimary else StudioBorder,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                state.selectFrame(idx)
                            }
                    ) {
                        // Thumbnail image container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumb != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = thumb,
                                    contentDescription = "Frame #$idx",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    null,
                                    tint = StudioTextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Quick Delete Frame button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(18.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .clickable { state.deleteFrameAt(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete Frame",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            // Hold duration badge
                            if (frame.holdDuration > 1) {
                                Surface(
                                    color = StudioPrimary.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 4.dp, topEnd = 0.dp, bottomStart = 0.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "${frame.holdDuration}s",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Clean plate badge
                            if (isCleanPlate) {
                                Surface(
                                    color = StudioAccent.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(2.dp)
                                ) {
                                    Text(
                                        text = "CLEAN",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Bottom label bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) StudioPrimary else StudioSurface)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#%03d".format(idx + 1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else StudioTextSecondary
                            )

                            if (frame.drawings.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Brush,
                                    null,
                                    tint = if (isSelected) Color.White else StudioSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio Waveform Track Row
        AudioWaveformRow(state)
    }
}

@Composable
private fun AudioWaveformRow(state: EditorState) {
    val activeTrack = state.project.audioTracks.firstOrNull { !it.isMuted }
    val peaks = activeTrack?.waveformPeaks ?: emptyList()
    val totalTimelineFrames = state.project.totalTimelineFrames.coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(StudioSurface)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.width(90.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.GraphicEq,
                null,
                tint = if (activeTrack != null) StudioAccent else StudioTextMuted,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = activeTrack?.name?.take(10) ?: "No Audio",
                fontSize = 10.sp,
                color = StudioTextSecondary,
                maxLines = 1
            )
        }

        // Waveform Canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Baseline
            drawLine(
                color = StudioBorder,
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f
            )

            // Audio Peaks
            if (peaks.isNotEmpty()) {
                val stepX = w / peaks.size.coerceAtLeast(1)
                for (i in peaks.indices) {
                    val peak = peaks[i]
                    val barH = (peak * (h / 2f)).coerceAtLeast(1.5f)
                    val x = i * stepX
                    drawLine(
                        color = StudioAccent.copy(alpha = 0.8f),
                        start = Offset(x, midY - barH),
                        end = Offset(x, midY + barH),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Playhead indicator line
            val playheadProgress = state.currentTimelineIndex.toFloat() / totalTimelineFrames
            val playheadX = playheadProgress * w
            drawLine(
                color = StudioPrimary,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, h),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun TimelineActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(icon, tooltip, tint = StudioTextSecondary, modifier = Modifier.size(16.dp))
    }
}
