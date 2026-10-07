package com.example.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun VideoPreviewView(
    projectState: EditorProjectState,
    onSeekTo: (Long) -> Unit,
    onUpdateQuranPosition: (String, Float, Float) -> Unit,
    onUpdateTextPosition: (String, Float, Float, Float, Float) -> Unit,
    onUpdateStickerPosition: (String, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMs = projectState.currentTimeMs
    val activeVideo = projectState.videoClips.firstOrNull()

    // Setup color matrix for real Filter and Adjustments
    val colorMatrix = remember(projectState.filter, projectState.adjustment) {
        buildCombinedColorMatrix(projectState.filter, projectState.adjustment)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Framing according to canvas aspect ratio
        Box(
            modifier = Modifier
                .fillMaxHeight(0.96f)
                .aspectRatio(projectState.canvasRatio.aspectRatio)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Video / Blank Canvas View
            if (activeVideo?.uri != null) {
                RealVideoPlayer(
                    uri = Uri.parse(activeVideo.uri),
                    currentMs = currentMs,
                    isPlaying = projectState.isPlaying,
                    isMuted = projectState.isMuted,
                    volume = projectState.volume,
                    speed = projectState.speed,
                    colorMatrix = colorMatrix,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Blank Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF0A0F1D), Color(0xFF1E293B), Color(0xFF064E3B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "﷽",
                            color = GoldLight.copy(alpha = 0.4f),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Quran Video Editor",
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Active Effect Layer
            val activeEffects = projectState.effectClips.filter {
                currentMs in it.timelineStartMs..it.timelineEndMs
            }
            for (effect in activeEffects) {
                EffectOverlay(effect = effect)
            }

            // Active Stickers Layer
            val activeStickers = projectState.stickerClips.filter {
                currentMs in it.timelineStartMs..it.timelineEndMs
            }
            for (sticker in activeStickers) {
                StickerOverlay(
                    sticker = sticker,
                    onPositionChange = { dx, dy ->
                        onUpdateStickerPosition(sticker.id, sticker.posX + dx, sticker.posY + dy, sticker.scale)
                    }
                )
            }

            // Active Text Layer
            val activeTexts = projectState.textClips.filter {
                currentMs in it.timelineStartMs..it.timelineEndMs
            }
            for (textClip in activeTexts) {
                TextOverlay(
                    textClip = textClip,
                    currentMs = currentMs,
                    onPositionChange = { dx, dy ->
                        onUpdateTextPosition(
                            textClip.id,
                            textClip.posX + dx,
                            textClip.posY + dy,
                            textClip.scale,
                            textClip.rotation
                        )
                    }
                )
            }

            // Active Quran Caption Layer (Arabic + Urdu)
            val activeQuran = projectState.quranClips.filter {
                currentMs in it.timelineStartMs..it.timelineEndMs
            }
            for (quranClip in activeQuran) {
                QuranCaptionOverlay(
                    clip = quranClip,
                    onPositionChange = { dx, dy ->
                        onUpdateQuranPosition(quranClip.id, quranClip.posX + dx, quranClip.posY + dy)
                    }
                )
            }
        }
    }
}

@Composable
fun RealVideoPlayer(
    uri: Uri,
    currentMs: Long,
    isPlaying: Boolean,
    isMuted: Boolean,
    volume: Float,
    speed: Float,
    colorMatrix: ColorMatrix,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSurfaceReady by remember { mutableStateOf(false) }

    DisposableEffect(uri) {
        val mp = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                isLooping = true
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = mp

        onDispose {
            try {
                mp.stop()
                mp.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying, isSurfaceReady) {
        mediaPlayer?.let { mp ->
            if (isSurfaceReady) {
                try {
                    if (isPlaying && !mp.isPlaying) {
                        mp.start()
                    } else if (!isPlaying && mp.isPlaying) {
                        mp.pause()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(currentMs) {
        mediaPlayer?.let { mp ->
            try {
                val diff = kotlin.math.abs(mp.currentPosition - currentMs)
                if (diff > 500) {
                    mp.seekTo(currentMs.toInt())
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isMuted, volume) {
        mediaPlayer?.let { mp ->
            val vol = if (isMuted) 0f else volume.coerceIn(0f, 1f)
            mp.setVolume(vol, vol)
        }
    }

    LaunchedEffect(speed) {
        mediaPlayer?.let { mp ->
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    mp.playbackParams = mp.playbackParams.setSpeed(speed.coerceIn(0.25f, 2.0f))
                }
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        mediaPlayer?.setDisplay(holder)
                        isSurfaceReady = true
                    }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        isSurfaceReady = false
                        mediaPlayer?.setDisplay(null)
                    }
                })
            }
        },
        modifier = modifier
    )
}

@Composable
fun QuranCaptionOverlay(
    clip: QuranClip,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val baseX = (clip.posX * (widthPx / 3f))
        val baseY = (clip.posY * (heightPx / 3f))

        Box(
            modifier = Modifier
                .offset { IntOffset((baseX + offsetX).roundToInt(), (baseY + offsetY).roundToInt()) }
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .pointerInput(clip.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val normDx = offsetX / (widthPx / 3f).coerceAtLeast(1f)
                            val normDy = offsetY / (heightPx / 3f).coerceAtLeast(1f)
                            onPositionChange(normDx, normDy)
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(Color(clip.style.backgroundColorHex).copy(alpha = clip.style.backgroundOpacity))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Surah Name Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDark.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "${clip.surahName} (${clip.surahNumber}:${clip.ayahNumber})",
                        color = GoldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Arabic Text
                if (clip.style.displayMode != QuranDisplayMode.URDU_ONLY) {
                    Text(
                        text = clip.arabicText,
                        color = Color(clip.style.arabicColorHex),
                        fontSize = clip.style.arabicFontSize.sp,
                        fontWeight = if (clip.style.isBold) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = (clip.style.arabicFontSize * clip.style.lineSpacing).sp,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .shadow(2.dp, RoundedCornerShape(4.dp), ambientColor = Color.Black)
                    )
                }

                // Urdu Translation
                if (clip.style.displayMode != QuranDisplayMode.ARABIC_ONLY) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = clip.urduTranslation,
                        color = Color(clip.style.urduColorHex),
                        fontSize = clip.style.urduFontSize.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = (clip.style.urduFontSize * 1.3f).sp,
                        modifier = Modifier.fillMaxWidth(0.95f)
                    )
                }
            }
        }
    }
}

@Composable
fun TextOverlay(
    textClip: TextClip,
    currentMs: Long,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Animations calculation
    val progress = ((currentMs - textClip.timelineStartMs).toFloat() / textClip.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val animAlpha = when (textClip.animation) {
        TextAnimation.FADE_IN -> (progress * 4f).coerceIn(0f, 1f)
        TextAnimation.FADE_OUT -> (1f - ((progress - 0.75f) * 4f)).coerceIn(0f, 1f)
        else -> 1f
    }
    val animScale = when (textClip.animation) {
        TextAnimation.ZOOM_IN -> (0.5f + progress * 0.5f).coerceIn(0.5f, 1f)
        TextAnimation.ZOOM_OUT -> (1.5f - progress * 0.5f).coerceIn(1f, 1.5f)
        else -> textClip.scale
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val baseX = (textClip.posX * (widthPx / 3f))
        val baseY = (textClip.posY * (heightPx / 3f))

        Box(
            modifier = Modifier
                .offset { IntOffset((baseX + offsetX).roundToInt(), (baseY + offsetY).roundToInt()) }
                .align(Alignment.Center)
                .alpha(animAlpha * textClip.opacity)
                .scale(animScale)
                .rotate(textClip.rotation)
                .pointerInput(textClip.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val normDx = offsetX / (widthPx / 3f).coerceAtLeast(1f)
                            val normDy = offsetY / (heightPx / 3f).coerceAtLeast(1f)
                            onPositionChange(normDx, normDy)
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clip(RoundedCornerShape(8.dp))
                .background(Color(textClip.backgroundColorHex))
                .padding(8.dp)
        ) {
            Text(
                text = textClip.text,
                color = Color(textClip.colorHex),
                fontSize = textClip.fontSize.sp,
                fontWeight = if (textClip.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (textClip.isItalic) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StickerOverlay(
    sticker: StickerClip,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val baseX = (sticker.posX * (widthPx / 3f))
        val baseY = (sticker.posY * (heightPx / 3f))

        Box(
            modifier = Modifier
                .offset { IntOffset((baseX + offsetX).roundToInt(), (baseY + offsetY).roundToInt()) }
                .align(Alignment.Center)
                .scale(sticker.scale)
                .alpha(sticker.opacity)
                .pointerInput(sticker.id) {
                    detectDragGestures(
                        onDragEnd = {
                            val normDx = offsetX / (widthPx / 3f).coerceAtLeast(1f)
                            val normDy = offsetY / (heightPx / 3f).coerceAtLeast(1f)
                            onPositionChange(normDx, normDy)
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x88000000))
                .padding(12.dp)
        ) {
            Text(
                text = sticker.symbol,
                fontSize = 36.sp,
                color = GoldAccent
            )
        }
    }
}

@Composable
fun EffectOverlay(effect: EffectClip) {
    when (effect.type) {
        EffectType.BLUR -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur((12 * effect.intensity).dp)
                    .background(Color.Transparent)
            )
        }
        EffectType.CINEMATIC_BARS -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((36 * effect.intensity).dp)
                        .background(Color.Black)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((36 * effect.intensity).dp)
                        .background(Color.Black)
                )
            }
        }
        EffectType.GLITCH -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x3300FFFF))
            )
        }
        EffectType.FADE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f * effect.intensity))
            )
        }
        EffectType.GLOW -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GoldAccent.copy(alpha = 0.15f * effect.intensity))
            )
        }
    }
}

private fun buildCombinedColorMatrix(filter: VideoFilter, adj: VideoAdjustment): ColorMatrix {
    val cm = ColorMatrix()

    when (filter) {
        VideoFilter.WARM -> {
            cm.set(floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        VideoFilter.VINTAGE -> {
            cm.set(floatArrayOf(
                0.9f, 0.2f, 0.1f, 0f, 20f,
                0.1f, 0.8f, 0.1f, 0f, 10f,
                0.1f, 0.1f, 0.6f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        VideoFilter.CINEMATIC -> {
            cm.set(floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 1.1f, 0f, 0f, 10f,
                0f, 0f, 1.3f, 0f, 25f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        VideoFilter.BLACK_AND_WHITE -> {
            cm.setSaturation(0f)
        }
        VideoFilter.NATURAL -> {}
    }

    if (adj.saturation != 1f) {
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(adj.saturation)
        cm.postConcat(satMatrix)
    }

    return cm
}
