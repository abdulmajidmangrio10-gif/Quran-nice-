package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun TimelineView(
    projectState: EditorProjectState,
    onSeekTo: (Long) -> Unit,
    onSelectClip: (String?, ClipType?) -> Unit,
    onMoveClip: (String, ClipType, Long) -> Unit,
    onTrimClipStart: (String, ClipType, Long) -> Unit,
    onTrimClipEnd: (String, ClipType, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var pixelsPerSecond by remember { mutableFloatStateOf(40f) } // Timeline zoom
    val scrollState = rememberScrollState()

    val totalDurationSec = (projectState.durationMs / 1000f).coerceAtLeast(15f)
    val timelineWidthDp = (totalDurationSec * pixelsPerSecond + 300f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        // Zoom and Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatTime(projectState.currentTimeMs),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " / ${formatTime(projectState.durationMs)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Zoom controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { pixelsPerSecond = (pixelsPerSecond - 10f).coerceAtLeast(20f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = "${pixelsPerSecond.roundToInt()}x",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = { pixelsPerSecond = (pixelsPerSecond + 10f).coerceAtMost(100f) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Divider(color = DarkBorder, thickness = 0.5.dp)

        // Main Multi-Track Timeline with Ruler & Playhead
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Horizontal scroll container
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .width(timelineWidthDp)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Time Ruler
                        TimeRuler(
                            totalDurationSec = totalDurationSec,
                            pixelsPerSecond = pixelsPerSecond,
                            onSeekTo = onSeekTo
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Dynamic Tracks
                        // Video Track (if clips exist)
                        if (projectState.videoClips.isNotEmpty()) {
                            TrackRow(
                                title = "VIDEO",
                                icon = Icons.Default.Videocam,
                                trackColor = VideoTrackColor
                            ) {
                                for (clip in projectState.videoClips) {
                                    ClipBlock(
                                        title = clip.name,
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = VideoTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.VIDEO) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.VIDEO, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.VIDEO, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.VIDEO, newEnd) }
                                    )
                                }
                            }
                        }

                        // Quran Track (if clips exist)
                        if (projectState.quranClips.isNotEmpty()) {
                            TrackRow(
                                title = "QURAN",
                                icon = Icons.Default.MenuBook,
                                trackColor = QuranTrackColor
                            ) {
                                for (clip in projectState.quranClips) {
                                    ClipBlock(
                                        title = "${clip.surahName} ${clip.ayahNumber}",
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = QuranTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.QURAN) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.QURAN, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.QURAN, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.QURAN, newEnd) }
                                    )
                                }
                            }
                        }

                        // Text Track (if clips exist)
                        if (projectState.textClips.isNotEmpty()) {
                            TrackRow(
                                title = "TEXT",
                                icon = Icons.Default.TextFields,
                                trackColor = TextTrackColor
                            ) {
                                for (clip in projectState.textClips) {
                                    ClipBlock(
                                        title = clip.text,
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = TextTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.TEXT) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.TEXT, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.TEXT, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.TEXT, newEnd) }
                                    )
                                }
                            }
                        }

                        // Audio Track (if clips exist)
                        if (projectState.audioClips.isNotEmpty()) {
                            TrackRow(
                                title = "AUDIO",
                                icon = Icons.Default.Audiotrack,
                                trackColor = AudioTrackColor
                            ) {
                                for (clip in projectState.audioClips) {
                                    ClipBlock(
                                        title = clip.name,
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = AudioTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.AUDIO) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.AUDIO, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.AUDIO, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.AUDIO, newEnd) }
                                    )
                                }
                            }
                        }

                        // Image Track (if clips exist)
                        if (projectState.imageClips.isNotEmpty()) {
                            TrackRow(
                                title = "IMAGE",
                                icon = Icons.Default.Image,
                                trackColor = ImageTrackColor
                            ) {
                                for (clip in projectState.imageClips) {
                                    ClipBlock(
                                        title = "Image",
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = ImageTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.IMAGE) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.IMAGE, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.IMAGE, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.IMAGE, newEnd) }
                                    )
                                }
                            }
                        }

                        // Sticker Track (if clips exist)
                        if (projectState.stickerClips.isNotEmpty()) {
                            TrackRow(
                                title = "STICKER",
                                icon = Icons.Default.EmojiEmotions,
                                trackColor = StickerTrackColor
                            ) {
                                for (clip in projectState.stickerClips) {
                                    ClipBlock(
                                        title = clip.label,
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = StickerTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.STICKER) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.STICKER, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.STICKER, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.STICKER, newEnd) }
                                    )
                                }
                            }
                        }

                        // Effect Track (if clips exist)
                        if (projectState.effectClips.isNotEmpty()) {
                            TrackRow(
                                title = "EFFECT",
                                icon = Icons.Default.AutoFixHigh,
                                trackColor = EffectTrackColor
                            ) {
                                for (clip in projectState.effectClips) {
                                    ClipBlock(
                                        title = clip.type.name,
                                        startMs = clip.timelineStartMs,
                                        endMs = clip.timelineEndMs,
                                        isSelected = projectState.selectedClipId == clip.id,
                                        trackColor = EffectTrackColor,
                                        pixelsPerSecond = pixelsPerSecond,
                                        onSelect = { onSelectClip(clip.id, ClipType.EFFECT) },
                                        onMove = { delta -> onMoveClip(clip.id, ClipType.EFFECT, delta) },
                                        onTrimStart = { newStart -> onTrimClipStart(clip.id, ClipType.EFFECT, newStart) },
                                        onTrimEnd = { newEnd -> onTrimClipEnd(clip.id, ClipType.EFFECT, newEnd) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Playhead (Vertical Red Line)
                    PlayheadLine(
                        currentMs = projectState.currentTimeMs,
                        pixelsPerSecond = pixelsPerSecond,
                        onSeekTo = onSeekTo
                    )
                }
            }
        }
    }
}

@Composable
fun TimeRuler(
    totalDurationSec: Float,
    pixelsPerSecond: Float,
    onSeekTo: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFF131722))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val posSec = change.position.x / pixelsPerSecond
                    onSeekTo((posSec * 1000f).toLong())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalSeconds = totalDurationSec.toInt() + 10
            for (sec in 0..totalSeconds) {
                val x = sec * pixelsPerSecond
                val isMajor = sec % 5 == 0
                val tickHeight = if (isMajor) 16f else 8f
                val tickColor = if (isMajor) Color(0xFF94A3B8) else Color(0xFF475569)

                drawLine(
                    color = tickColor,
                    start = Offset(x, 28f - tickHeight),
                    end = Offset(x, 28f),
                    strokeWidth = if (isMajor) 2f else 1f
                )
            }
        }

        // Interval labels (00:00, 00:05, 00:10...)
        val stepSec = if (pixelsPerSecond < 35f) 10 else 5
        val count = (totalDurationSec / stepSec).toInt() + 2
        for (i in 0..count) {
            val sec = i * stepSec
            val xDp = (sec * pixelsPerSecond).dp
            Text(
                text = formatSeconds(sec),
                color = TextSecondary,
                fontSize = 9.sp,
                modifier = Modifier
                    .offset(x = xDp + 4.dp, y = 2.dp)
            )
        }
    }
}

@Composable
fun TrackRow(
    title: String,
    icon: ImageVector,
    trackColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Header Label
        Surface(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            color = DarkSurfaceVariant,
            shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = trackColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Track Clips Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0F131C)),
            content = content
        )
    }
}

@Composable
fun ClipBlock(
    title: String,
    startMs: Long,
    endMs: Long,
    isSelected: Boolean,
    trackColor: Color,
    pixelsPerSecond: Float,
    onSelect: () -> Unit,
    onMove: (Long) -> Unit,
    onTrimStart: (Long) -> Unit,
    onTrimEnd: (Long) -> Unit
) {
    val startSec = startMs / 1000f
    val durationSec = ((endMs - startMs) / 1000f).coerceAtLeast(0.5f)

    val leftOffsetDp = (startSec * pixelsPerSecond).dp
    val widthDp = (durationSec * pixelsPerSecond).dp

    Box(
        modifier = Modifier
            .offset(x = leftOffsetDp)
            .width(widthDp)
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(trackColor.copy(alpha = if (isSelected) 0.95f else 0.75f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GoldLight else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onSelect() }
    ) {
        // Left Trim Handle
        Box(
            modifier = Modifier
                .width(14.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(if (isSelected) GoldAccent.copy(alpha = 0.5f) else Color.Transparent)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaSec = dragAmount.x / pixelsPerSecond
                        val deltaMs = (deltaSec * 1000f).toLong()
                        onTrimStart(startMs + deltaMs)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }

        // Center Move Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaSec = dragAmount.x / pixelsPerSecond
                        val deltaMs = (deltaSec * 1000f).toLong()
                        onMove(deltaMs)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right Trim Handle
        Box(
            modifier = Modifier
                .width(14.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .background(if (isSelected) GoldAccent.copy(alpha = 0.5f) else Color.Transparent)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaSec = dragAmount.x / pixelsPerSecond
                        val deltaMs = (deltaSec * 1000f).toLong()
                        onTrimEnd(endMs + deltaMs)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun PlayheadLine(
    currentMs: Long,
    pixelsPerSecond: Float,
    onSeekTo: (Long) -> Unit
) {
    val sec = currentMs / 1000f
    val xOffsetDp = (sec * pixelsPerSecond).dp

    Box(
        modifier = Modifier
            .offset(x = xOffsetDp - 8.dp)
            .width(16.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val deltaSec = dragAmount.x / pixelsPerSecond
                    val newTime = (currentMs + (deltaSec * 1000f).toLong()).coerceAtLeast(0L)
                    onSeekTo(newTime)
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Red Playhead Pointer Head
        Surface(
            color = PlayheadRed,
            shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
            modifier = Modifier.size(width = 12.dp, height = 12.dp)
        ) {}

        // Red Vertical Playhead Line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(PlayheadRed)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatSeconds(sec: Int): String {
    val minutes = sec / 60
    val seconds = sec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
