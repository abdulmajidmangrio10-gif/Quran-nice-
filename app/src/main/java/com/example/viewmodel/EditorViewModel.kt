package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autocaption.AutoCaptionEngine
import com.example.autocaption.AutoCaptionProgress
import com.example.autocaption.DetectedAyahCaption
import com.example.model.*
import com.example.quran.AyahItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _projectState = MutableStateFlow(EditorProjectState())
    val projectState: StateFlow<EditorProjectState> = _projectState.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<EditorProjectState>()
    private val redoStack = mutableListOf<EditorProjectState>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Auto-caption state
    private val _autoCaptionStatus = MutableStateFlow<AutoCaptionProgress?>(null)
    val autoCaptionStatus: StateFlow<AutoCaptionProgress?> = _autoCaptionStatus.asStateFlow()

    private val _detectedCaptions = MutableStateFlow<List<DetectedAyahCaption>>(emptyList())
    val detectedCaptions: StateFlow<List<DetectedAyahCaption>> = _detectedCaptions.asStateFlow()

    private var playbackJob: Job? = null

    init {
        // Start with a clean blank canvas project
        createBlankProject()
    }

    private fun pushHistory() {
        val current = _projectState.value
        undoStack.add(current.copy())
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_projectState.value.copy())
            _projectState.value = previous
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_projectState.value.copy())
            _projectState.value = next
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun setInitialVideo(uri: Uri?, durationMs: Long, name: String) {
        val dur = if (durationMs > 0) durationMs else 15000L
        val videoClip = VideoClip(
            id = UUID.randomUUID().toString(),
            uri = uri?.toString(),
            name = name,
            startTrimMs = 0L,
            endTrimMs = dur,
            timelineStartMs = 0L,
            timelineEndMs = dur,
            originalDurationMs = dur
        )
        _projectState.value = EditorProjectState(
            videoClips = listOf(videoClip),
            durationMs = dur,
            currentTimeMs = 0L,
            selectedClipId = videoClip.id,
            selectedClipType = ClipType.VIDEO
        )
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    fun createBlankProject() {
        val dur = 15000L
        val videoClip = VideoClip(
            id = UUID.randomUUID().toString(),
            uri = null,
            name = "Blank Canvas",
            startTrimMs = 0L,
            endTrimMs = dur,
            timelineStartMs = 0L,
            timelineEndMs = dur,
            originalDurationMs = dur
        )
        _projectState.value = EditorProjectState(
            videoClips = listOf(videoClip),
            durationMs = dur,
            currentTimeMs = 0L,
            selectedClipId = videoClip.id,
            selectedClipType = ClipType.VIDEO
        )
    }

    fun togglePlayPause() {
        val playing = !_projectState.value.isPlaying
        _projectState.value = _projectState.value.copy(isPlaying = playing)
        if (playing) {
            startPlaybackLoop()
        } else {
            playbackJob?.cancel()
        }
    }

    fun seekTo(timeMs: Long) {
        val clamped = timeMs.coerceIn(0L, _projectState.value.durationMs)
        _projectState.value = _projectState.value.copy(currentTimeMs = clamped)
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_projectState.value.isPlaying) {
                delay(33) // ~30fps loop
                val state = _projectState.value
                val step = (33 * state.speed).toLong()
                val nextTime = state.currentTimeMs + step
                if (nextTime >= state.durationMs) {
                    _projectState.value = state.copy(currentTimeMs = 0L, isPlaying = false)
                    break
                } else {
                    _projectState.value = state.copy(currentTimeMs = nextTime)
                }
            }
        }
    }

    fun selectClip(id: String?, type: ClipType?) {
        _projectState.value = _projectState.value.copy(
            selectedClipId = id,
            selectedClipType = type
        )
    }

    // ==========================================
    // TIMELINE EDITING: MOVE, TRIM, SPLIT, DELETE, DUPLICATE
    // ==========================================

    fun moveClip(id: String, type: ClipType, deltaMs: Long) {
        pushHistory()
        val state = _projectState.value
        when (type) {
            ClipType.VIDEO -> {
                val updated = state.videoClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(videoClips = updated)
            }
            ClipType.QURAN -> {
                val updated = state.quranClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(quranClips = updated)
            }
            ClipType.TEXT -> {
                val updated = state.textClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(textClips = updated)
            }
            ClipType.AUDIO -> {
                val updated = state.audioClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(audioClips = updated)
            }
            ClipType.IMAGE -> {
                val updated = state.imageClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(imageClips = updated)
            }
            ClipType.STICKER -> {
                val updated = state.stickerClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(stickerClips = updated)
            }
            ClipType.EFFECT -> {
                val updated = state.effectClips.map { clip ->
                    if (clip.id == id) {
                        val dur = clip.durationMs
                        val newStart = (clip.timelineStartMs + deltaMs).coerceAtLeast(0L)
                        clip.copy(timelineStartMs = newStart, timelineEndMs = newStart + dur)
                    } else clip
                }
                updateProjectClips(effectClips = updated)
            }
        }
    }

    fun trimClipStart(id: String, type: ClipType, newStartMs: Long) {
        pushHistory()
        val state = _projectState.value
        when (type) {
            ClipType.VIDEO -> {
                val updated = state.videoClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(videoClips = updated)
            }
            ClipType.QURAN -> {
                val updated = state.quranClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(quranClips = updated)
            }
            ClipType.TEXT -> {
                val updated = state.textClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(textClips = updated)
            }
            ClipType.AUDIO -> {
                val updated = state.audioClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(audioClips = updated)
            }
            ClipType.IMAGE -> {
                val updated = state.imageClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(imageClips = updated)
            }
            ClipType.STICKER -> {
                val updated = state.stickerClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(stickerClips = updated)
            }
            ClipType.EFFECT -> {
                val updated = state.effectClips.map { clip ->
                    if (clip.id == id) {
                        val validStart = newStartMs.coerceIn(0L, clip.timelineEndMs - 500L)
                        clip.copy(timelineStartMs = validStart)
                    } else clip
                }
                updateProjectClips(effectClips = updated)
            }
        }
    }

    fun trimClipEnd(id: String, type: ClipType, newEndMs: Long) {
        pushHistory()
        val state = _projectState.value
        when (type) {
            ClipType.VIDEO -> {
                val updated = state.videoClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(videoClips = updated)
            }
            ClipType.QURAN -> {
                val updated = state.quranClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(quranClips = updated)
            }
            ClipType.TEXT -> {
                val updated = state.textClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(textClips = updated)
            }
            ClipType.AUDIO -> {
                val updated = state.audioClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(audioClips = updated)
            }
            ClipType.IMAGE -> {
                val updated = state.imageClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(imageClips = updated)
            }
            ClipType.STICKER -> {
                val updated = state.stickerClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(stickerClips = updated)
            }
            ClipType.EFFECT -> {
                val updated = state.effectClips.map { clip ->
                    if (clip.id == id) {
                        val validEnd = newEndMs.coerceAtLeast(clip.timelineStartMs + 500L)
                        clip.copy(timelineEndMs = validEnd)
                    } else clip
                }
                updateProjectClips(effectClips = updated)
            }
        }
    }

    fun splitClipAtPlayhead() {
        val state = _projectState.value
        val playhead = state.currentTimeMs
        val id = state.selectedClipId ?: return
        val type = state.selectedClipType ?: return
        pushHistory()

        when (type) {
            ClipType.VIDEO -> {
                val clip = state.videoClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.videoClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(videoClips = newClips)
                }
            }
            ClipType.QURAN -> {
                val clip = state.quranClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.quranClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(quranClips = newClips)
                }
            }
            ClipType.TEXT -> {
                val clip = state.textClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.textClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(textClips = newClips)
                }
            }
            ClipType.AUDIO -> {
                val clip = state.audioClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.audioClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(audioClips = newClips)
                }
            }
            ClipType.IMAGE -> {
                val clip = state.imageClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.imageClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(imageClips = newClips)
                }
            }
            ClipType.STICKER -> {
                val clip = state.stickerClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.stickerClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(stickerClips = newClips)
                }
            }
            ClipType.EFFECT -> {
                val clip = state.effectClips.find { it.id == id } ?: return
                if (playhead in (clip.timelineStartMs + 300L)..(clip.timelineEndMs - 300L)) {
                    val piece1 = clip.copy(timelineEndMs = playhead)
                    val piece2 = clip.copy(id = UUID.randomUUID().toString(), timelineStartMs = playhead)
                    val newClips = state.effectClips.filter { it.id != id } + listOf(piece1, piece2)
                    updateProjectClips(effectClips = newClips)
                }
            }
        }
    }

    fun deleteSelectedClip() {
        val state = _projectState.value
        val id = state.selectedClipId ?: return
        val type = state.selectedClipType ?: return
        pushHistory()

        when (type) {
            ClipType.VIDEO -> {
                val updated = state.videoClips.filter { it.id != id }
                updateProjectClips(videoClips = updated)
            }
            ClipType.QURAN -> {
                val updated = state.quranClips.filter { it.id != id }
                updateProjectClips(quranClips = updated)
            }
            ClipType.TEXT -> {
                val updated = state.textClips.filter { it.id != id }
                updateProjectClips(textClips = updated)
            }
            ClipType.AUDIO -> {
                val updated = state.audioClips.filter { it.id != id }
                updateProjectClips(audioClips = updated)
            }
            ClipType.IMAGE -> {
                val updated = state.imageClips.filter { it.id != id }
                updateProjectClips(imageClips = updated)
            }
            ClipType.STICKER -> {
                val updated = state.stickerClips.filter { it.id != id }
                updateProjectClips(stickerClips = updated)
            }
            ClipType.EFFECT -> {
                val updated = state.effectClips.filter { it.id != id }
                updateProjectClips(effectClips = updated)
            }
        }
        _projectState.value = _projectState.value.copy(
            selectedClipId = null,
            selectedClipType = null
        )
    }

    fun duplicateSelectedClip() {
        val state = _projectState.value
        val id = state.selectedClipId ?: return
        val type = state.selectedClipType ?: return
        pushHistory()

        when (type) {
            ClipType.VIDEO -> {
                val orig = state.videoClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(videoClips = state.videoClips + copy)
            }
            ClipType.QURAN -> {
                val orig = state.quranClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(quranClips = state.quranClips + copy)
            }
            ClipType.TEXT -> {
                val orig = state.textClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(textClips = state.textClips + copy)
            }
            ClipType.AUDIO -> {
                val orig = state.audioClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(audioClips = state.audioClips + copy)
            }
            ClipType.IMAGE -> {
                val orig = state.imageClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(imageClips = state.imageClips + copy)
            }
            ClipType.STICKER -> {
                val orig = state.stickerClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(stickerClips = state.stickerClips + copy)
            }
            ClipType.EFFECT -> {
                val orig = state.effectClips.find { it.id == id } ?: return
                val copy = orig.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = orig.timelineEndMs,
                    timelineEndMs = orig.timelineEndMs + orig.durationMs
                )
                updateProjectClips(effectClips = state.effectClips + copy)
            }
        }
    }

    // ==========================================
    // QURAN ACTIONS
    // ==========================================

    fun addQuranAyah(ayah: AyahItem, startMs: Long = _projectState.value.currentTimeMs, durationMs: Long = 6000L) {
        pushHistory()
        val clip = QuranClip(
            id = UUID.randomUUID().toString(),
            surahNumber = ayah.surahNumber,
            surahName = ayah.surahName,
            ayahNumber = ayah.ayahNumber,
            arabicText = ayah.arabicText,
            urduTranslation = ayah.urduTranslation,
            timelineStartMs = startMs,
            timelineEndMs = startMs + durationMs
        )
        updateProjectClips(quranClips = _projectState.value.quranClips + clip)
        selectClip(clip.id, ClipType.QURAN)
    }

    fun updateQuranStyle(id: String, style: QuranStyle) {
        pushHistory()
        val updated = _projectState.value.quranClips.map {
            if (it.id == id) it.copy(style = style) else it
        }
        _projectState.value = _projectState.value.copy(quranClips = updated)
    }

    fun updateQuranPosition(id: String, posX: Float, posY: Float) {
        val updated = _projectState.value.quranClips.map {
            if (it.id == id) it.copy(posX = posX, posY = posY) else it
        }
        _projectState.value = _projectState.value.copy(quranClips = updated)
    }

    fun replaceQuranAyah(id: String, newAyah: AyahItem) {
        pushHistory()
        val updated = _projectState.value.quranClips.map {
            if (it.id == id) {
                it.copy(
                    surahNumber = newAyah.surahNumber,
                    surahName = newAyah.surahName,
                    ayahNumber = newAyah.ayahNumber,
                    arabicText = newAyah.arabicText,
                    urduTranslation = newAyah.urduTranslation
                )
            } else it
        }
        _projectState.value = _projectState.value.copy(quranClips = updated)
    }

    // ==========================================
    // TEXT ACTIONS
    // ==========================================

    fun addText(text: String, startMs: Long = _projectState.value.currentTimeMs, durationMs: Long = 5000L) {
        pushHistory()
        val clip = TextClip(
            id = UUID.randomUUID().toString(),
            text = text,
            timelineStartMs = startMs,
            timelineEndMs = startMs + durationMs
        )
        updateProjectClips(textClips = _projectState.value.textClips + clip)
        selectClip(clip.id, ClipType.TEXT)
    }

    fun updateText(id: String, text: String, fontSize: Float, colorHex: Long, isBold: Boolean, isItalic: Boolean, anim: TextAnimation) {
        pushHistory()
        val updated = _projectState.value.textClips.map {
            if (it.id == id) it.copy(
                text = text,
                fontSize = fontSize,
                colorHex = colorHex,
                isBold = isBold,
                isItalic = isItalic,
                animation = anim
            ) else it
        }
        _projectState.value = _projectState.value.copy(textClips = updated)
    }

    fun updateTextPosition(id: String, posX: Float, posY: Float, scale: Float, rotation: Float) {
        val updated = _projectState.value.textClips.map {
            if (it.id == id) it.copy(posX = posX, posY = posY, scale = scale, rotation = rotation) else it
        }
        _projectState.value = _projectState.value.copy(textClips = updated)
    }

    // ==========================================
    // AUDIO ACTIONS
    // ==========================================

    fun addAudio(uri: String?, name: String, durationMs: Long) {
        pushHistory()
        val startMs = _projectState.value.currentTimeMs
        val clip = AudioClip(
            id = UUID.randomUUID().toString(),
            uri = uri,
            name = name,
            timelineStartMs = startMs,
            timelineEndMs = startMs + durationMs.coerceAtLeast(5000L)
        )
        updateProjectClips(audioClips = _projectState.value.audioClips + clip)
        selectClip(clip.id, ClipType.AUDIO)
    }

    fun setAudioVolume(id: String, volume: Float) {
        pushHistory()
        val updated = _projectState.value.audioClips.map {
            if (it.id == id) it.copy(volume = volume) else it
        }
        _projectState.value = _projectState.value.copy(audioClips = updated)
    }

    fun toggleAudioMute(id: String) {
        pushHistory()
        val updated = _projectState.value.audioClips.map {
            if (it.id == id) it.copy(isMuted = !it.isMuted) else it
        }
        _projectState.value = _projectState.value.copy(audioClips = updated)
    }

    // ==========================================
    // IMAGE & STICKER ACTIONS
    // ==========================================

    fun addImage(uri: String) {
        pushHistory()
        val startMs = _projectState.value.currentTimeMs
        val clip = ImageClip(
            id = UUID.randomUUID().toString(),
            uri = uri,
            timelineStartMs = startMs,
            timelineEndMs = startMs + 6000L
        )
        updateProjectClips(imageClips = _projectState.value.imageClips + clip)
        selectClip(clip.id, ClipType.IMAGE)
    }

    fun addSticker(symbol: String, label: String) {
        pushHistory()
        val startMs = _projectState.value.currentTimeMs
        val clip = StickerClip(
            id = UUID.randomUUID().toString(),
            stickerId = UUID.randomUUID().toString(),
            symbol = symbol,
            label = label,
            timelineStartMs = startMs,
            timelineEndMs = startMs + 6000L
        )
        updateProjectClips(stickerClips = _projectState.value.stickerClips + clip)
        selectClip(clip.id, ClipType.STICKER)
    }

    fun updateImagePosition(id: String, posX: Float, posY: Float, scale: Float, rotation: Float) {
        val updated = _projectState.value.imageClips.map {
            if (it.id == id) it.copy(posX = posX, posY = posY, scale = scale, rotation = rotation) else it
        }
        _projectState.value = _projectState.value.copy(imageClips = updated)
    }

    fun updateStickerPosition(id: String, posX: Float, posY: Float, scale: Float) {
        val updated = _projectState.value.stickerClips.map {
            if (it.id == id) it.copy(posX = posX, posY = posY, scale = scale) else it
        }
        _projectState.value = _projectState.value.copy(stickerClips = updated)
    }

    // ==========================================
    // EFFECTS, FILTERS, ADJUST, CANVAS, SPEED
    // ==========================================

    fun addEffect(type: EffectType) {
        pushHistory()
        val startMs = _projectState.value.currentTimeMs
        val clip = EffectClip(
            id = UUID.randomUUID().toString(),
            type = type,
            timelineStartMs = startMs,
            timelineEndMs = startMs + 6000L
        )
        updateProjectClips(effectClips = _projectState.value.effectClips + clip)
        selectClip(clip.id, ClipType.EFFECT)
    }

    fun setFilter(filter: VideoFilter) {
        pushHistory()
        _projectState.value = _projectState.value.copy(filter = filter)
    }

    fun setAdjustment(adjustment: VideoAdjustment) {
        pushHistory()
        _projectState.value = _projectState.value.copy(adjustment = adjustment)
    }

    fun setCanvasRatio(ratio: CanvasRatio) {
        pushHistory()
        _projectState.value = _projectState.value.copy(canvasRatio = ratio)
    }

    fun setSpeed(speed: Float) {
        pushHistory()
        _projectState.value = _projectState.value.copy(speed = speed)
    }

    fun setMute(isMuted: Boolean) {
        _projectState.value = _projectState.value.copy(isMuted = isMuted)
    }

    fun setVolume(volume: Float) {
        _projectState.value = _projectState.value.copy(volume = volume)
    }

    // ==========================================
    // AUTO CAPTION
    // ==========================================

    fun startAutoCaption(targetSurahNumber: Int? = null, userQuery: String? = null) {
        viewModelScope.launch {
            val videoUri = _projectState.value.videoClips.firstOrNull()?.uri?.let { Uri.parse(it) }
            val duration = _projectState.value.durationMs

            AutoCaptionEngine.processVideoAudio(
                context = getApplication(),
                videoUri = videoUri,
                totalDurationMs = duration,
                targetSurahNumber = targetSurahNumber,
                userProvidedQuery = userQuery
            ).collect { progress ->
                _autoCaptionStatus.value = progress
                if (progress is AutoCaptionProgress.Completed) {
                    _detectedCaptions.value = progress.captions
                }
            }
        }
    }

    fun confirmAndApplyAutoCaptions(captions: List<DetectedAyahCaption>) {
        pushHistory()
        val newClips = captions.map { item ->
            QuranClip(
                id = UUID.randomUUID().toString(),
                surahNumber = item.ayah.surahNumber,
                surahName = item.ayah.surahName,
                ayahNumber = item.ayah.ayahNumber,
                arabicText = item.ayah.arabicText,
                urduTranslation = item.ayah.urduTranslation,
                timelineStartMs = item.startTimeMs,
                timelineEndMs = item.endTimeMs,
                confidence = item.confidence,
                isAutoGenerated = true
            )
        }
        updateProjectClips(quranClips = _projectState.value.quranClips + newClips)
        _autoCaptionStatus.value = null
        _detectedCaptions.value = emptyList()
    }

    fun cancelAutoCaption() {
        _autoCaptionStatus.value = null
        _detectedCaptions.value = emptyList()
    }

    private fun updateProjectClips(
        videoClips: List<VideoClip> = _projectState.value.videoClips,
        quranClips: List<QuranClip> = _projectState.value.quranClips,
        textClips: List<TextClip> = _projectState.value.textClips,
        audioClips: List<AudioClip> = _projectState.value.audioClips,
        imageClips: List<ImageClip> = _projectState.value.imageClips,
        stickerClips: List<StickerClip> = _projectState.value.stickerClips,
        effectClips: List<EffectClip> = _projectState.value.effectClips
    ) {
        // Calculate max project duration from all clips
        val maxEndMs = maxOf(
            videoClips.maxOfOrNull { it.timelineEndMs } ?: 15000L,
            quranClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            textClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            audioClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            imageClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            stickerClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            effectClips.maxOfOrNull { it.timelineEndMs } ?: 0L,
            15000L
        )

        _projectState.value = _projectState.value.copy(
            videoClips = videoClips,
            quranClips = quranClips,
            textClips = textClips,
            audioClips = audioClips,
            imageClips = imageClips,
            stickerClips = stickerClips,
            effectClips = effectClips,
            durationMs = maxEndMs
        )
    }
}
