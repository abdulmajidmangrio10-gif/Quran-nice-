package com.example.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioClip
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSheet(
    activeAudio: AudioClip?,
    onPickAudioFile: () -> Unit,
    onRecordVoice: () -> Unit,
    onSetVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onDismiss: () -> Unit
) {
    var volume by remember { mutableFloatStateOf(activeAudio?.volume ?: 1f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = AudioTrackColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audio & Recitation (آڈیو اور تلاوت)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Add Audio / Record Voice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onPickAudioFile()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AudioTrackColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Audio", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onRecordVoice()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Voice", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeAudio != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = DarkBorder)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Selected: ${activeAudio.name}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Volume: ${(volume * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (activeAudio.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (activeAudio.isMuted) ErrorRed else EmeraldLight
                        )
                    }
                }

                Slider(
                    value = volume,
                    onValueChange = {
                        volume = it
                        onSetVolume(it)
                    },
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(thumbColor = AudioTrackColor, activeTrackColor = AudioTrackColor)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
