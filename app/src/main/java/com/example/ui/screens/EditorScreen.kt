package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.dialogs.ExportDialog
import com.example.ui.sheets.*
import com.example.ui.theme.*
import com.example.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val projectState by viewModel.projectState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val autoCaptionStatus by viewModel.autoCaptionStatus.collectAsState()
    val detectedCaptions by viewModel.detectedCaptions.collectAsState()

    var activeTool by remember { mutableStateOf<EditorTool?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showQuranStyleSheet by remember { mutableStateOf(false) }
    var showTextSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }

    BackHandler {
        onNavigateBack()
    }

    // Media picker for Image overlay
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addImage(uri.toString())
        }
    }

    // Media picker for Audio
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addAudio(uri.toString(), "Custom Audio", 20000L)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            EditorTopBar(
                canUndo = canUndo,
                canRedo = canRedo,
                canvasRatio = projectState.canvasRatio,
                onBack = onNavigateBack,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSelectRatio = { viewModel.setCanvasRatio(it) },
                onExport = { showExportDialog = true }
            )
        },
        bottomBar = {
            BottomToolBar(
                activeTool = activeTool,
                onSelectTool = { tool ->
                    when (tool) {
                        EditorTool.QURAN -> activeTool = EditorTool.QURAN
                        EditorTool.AI_AUTO_CAPTION -> activeTool = EditorTool.AI_AUTO_CAPTION
                        EditorTool.TEXT -> showTextSheet = true
                        EditorTool.AUDIO -> showAudioSheet = true
                        EditorTool.IMAGE -> {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        EditorTool.STICKER -> showStickerSheet = true
                        else -> activeTool = tool
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Video Preview Area (Top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
            ) {
                VideoPreviewView(
                    projectState = projectState,
                    onSeekTo = { viewModel.seekTo(it) },
                    onUpdateQuranPosition = { id, x, y -> viewModel.updateQuranPosition(id, x, y) },
                    onUpdateTextPosition = { id, x, y, s, r -> viewModel.updateTextPosition(id, x, y, s, r) },
                    onUpdateStickerPosition = { id, x, y, s -> viewModel.updateStickerPosition(id, x, y, s) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 2. Playback Control Bar
            PlaybackControlsBar(
                isPlaying = projectState.isPlaying,
                currentTimeMs = projectState.currentTimeMs,
                durationMs = projectState.durationMs,
                isMuted = projectState.isMuted,
                speed = projectState.speed,
                onTogglePlay = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleMute = { viewModel.setMute(!projectState.isMuted) }
            )

            // 3. Quick Clip Action Bar (Split, Duplicate, Delete, Style)
            if (projectState.selectedClipId != null) {
                ClipActionToolbar(
                    selectedType = projectState.selectedClipType,
                    onSplit = { viewModel.splitClipAtPlayhead() },
                    onDuplicate = { viewModel.duplicateSelectedClip() },
                    onDelete = { viewModel.deleteSelectedClip() },
                    onStyle = {
                        if (projectState.selectedClipType == ClipType.QURAN) {
                            showQuranStyleSheet = true
                        }
                    }
                )
            }

            // 4. Multi-track Interactive Timeline
            TimelineView(
                projectState = projectState,
                onSeekTo = { viewModel.seekTo(it) },
                onSelectClip = { id, type -> viewModel.selectClip(id, type) },
                onMoveClip = { id, type, delta -> viewModel.moveClip(id, type, delta) },
                onTrimClipStart = { id, type, start -> viewModel.trimClipStart(id, type, start) },
                onTrimClipEnd = { id, type, end -> viewModel.trimClipEnd(id, type, end) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.95f)
            )
        }
    }

    // Modals and Sheets
    if (activeTool == EditorTool.QURAN) {
        QuranSelectionSheet(
            onDismiss = { activeTool = null },
            onAddAyahToVideo = { ayah -> viewModel.addQuranAyah(ayah) }
        )
    }

    if (activeTool == EditorTool.AI_AUTO_CAPTION) {
        AutoCaptionSheet(
            autoCaptionStatus = autoCaptionStatus,
            detectedCaptions = detectedCaptions,
            onStartAutoCaption = { targetSurah, query -> viewModel.startAutoCaption(targetSurah, query) },
            onConfirmAndApply = { captions -> viewModel.confirmAndApplyAutoCaptions(captions) },
            onDismiss = {
                viewModel.cancelAutoCaption()
                activeTool = null
            }
        )
    }

    if (showQuranStyleSheet) {
        val selectedClip = projectState.quranClips.find { it.id == projectState.selectedClipId }
        if (selectedClip != null) {
            QuranStyleSheet(
                clip = selectedClip,
                onUpdateStyle = { newStyle -> viewModel.updateQuranStyle(selectedClip.id, newStyle) },
                onDismiss = { showQuranStyleSheet = false }
            )
        }
    }

    if (showTextSheet) {
        TextEditSheet(
            onAddOrUpdateText = { text, size, color, bold, italic, anim ->
                viewModel.addText(text)
            },
            onDismiss = { showTextSheet = false }
        )
    }

    if (showAudioSheet) {
        val activeAudio = projectState.audioClips.find { it.id == projectState.selectedClipId }
            ?: projectState.audioClips.firstOrNull()
        AudioSheet(
            activeAudio = activeAudio,
            onPickAudioFile = {
                audioPickerLauncher.launch("audio/*")
            },
            onRecordVoice = {
                viewModel.addAudio(null, "Voice Recording", 10000L)
            },
            onSetVolume = { vol ->
                activeAudio?.let { viewModel.setAudioVolume(it.id, vol) }
            },
            onToggleMute = {
                activeAudio?.let { viewModel.toggleAudioMute(it.id) }
            },
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showStickerSheet) {
        StickerSheet(
            onSelectSticker = { symbol, label -> viewModel.addSticker(symbol, label) },
            onDismiss = { showStickerSheet = false }
        )
    }

    if (activeTool in listOf(EditorTool.FILTER, EditorTool.ADJUST, EditorTool.EFFECTS, EditorTool.CANVAS, EditorTool.SPEED)) {
        FilterAdjustSheet(
            tool = activeTool!!,
            currentFilter = projectState.filter,
            currentAdjustment = projectState.adjustment,
            currentRatio = projectState.canvasRatio,
            currentSpeed = projectState.speed,
            onSetFilter = { viewModel.setFilter(it) },
            onSetAdjustment = { viewModel.setAdjustment(it) },
            onAddEffect = { viewModel.addEffect(it) },
            onSetRatio = { viewModel.setCanvasRatio(it) },
            onSetSpeed = { viewModel.setSpeed(it) },
            onDismiss = { activeTool = null }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            projectState = projectState,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    canvasRatio: CanvasRatio,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSelectRatio: (CanvasRatio) -> Unit,
    onExport: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Quran Editor",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Center: Undo & Redo & Ratio
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(34.dp).testTag("undo_button")
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) TextPrimary else TextMuted
                    )
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(34.dp).testTag("redo_button")
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) TextPrimary else TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = canvasRatio.displayName,
                        color = EmeraldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // Right: EXPORT button
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("export_button")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EXPORT", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlaybackControlsBar(
    isPlaying: Boolean,
    currentTimeMs: Long,
    durationMs: Long,
    isMuted: Boolean,
    speed: Float,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleMute: () -> Unit
) {
    Surface(
        color = Color(0xFF11141B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(36.dp).testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Timestamps
            Text(
                text = "${formatTime(currentTimeMs)} / ${formatTime(durationMs)}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            // Scrubber Slider
            Slider(
                value = (currentTimeMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f),
                onValueChange = { norm ->
                    onSeekTo((norm * durationMs).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = PlayheadRed,
                    activeTrackColor = PlayheadRed,
                    inactiveTrackColor = DarkBorder
                )
            )

            // Mute Button
            IconButton(onClick = onToggleMute, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (isMuted) ErrorRed else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (speed != 1f) {
                Text(
                    text = "${speed}x",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ClipActionToolbar(
    selectedType: ClipType?,
    onSplit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onStyle: () -> Unit
) {
    Surface(
        color = DarkSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SELECTED: ${selectedType?.name ?: "CLIP"}",
                color = EmeraldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Split button
            Button(
                onClick = onSplit,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp).testTag("split_button")
            ) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SPLIT", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Duplicate button
            Button(
                onClick = onDuplicate,
                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp).testTag("duplicate_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("COPY", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Delete button
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp).testTag("delete_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DEL", color = ErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Style button (for Quran clips)
            if (selectedType == ClipType.QURAN) {
                Button(
                    onClick = onStyle,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp).testTag("style_button")
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = GoldLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STYLE", color = GoldLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
