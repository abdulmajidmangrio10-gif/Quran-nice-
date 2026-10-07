package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuranClip
import com.example.model.QuranDisplayMode
import com.example.model.QuranStyle
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranStyleSheet(
    clip: QuranClip,
    onUpdateStyle: (QuranStyle) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStyle by remember { mutableStateOf(clip.style) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = GoldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quran Caption Styling (کیپشن انداز)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Mode: Arabic + Urdu / Arabic Only / Urdu Only
            Text(text = "Display Mode (ظاہر کرنے کا طریقہ):", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentStyle.displayMode == QuranDisplayMode.ARABIC_AND_URDU,
                    onClick = {
                        currentStyle = currentStyle.copy(displayMode = QuranDisplayMode.ARABIC_AND_URDU)
                        onUpdateStyle(currentStyle)
                    },
                    label = { Text("Arabic + Urdu", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = currentStyle.displayMode == QuranDisplayMode.ARABIC_ONLY,
                    onClick = {
                        currentStyle = currentStyle.copy(displayMode = QuranDisplayMode.ARABIC_ONLY)
                        onUpdateStyle(currentStyle)
                    },
                    label = { Text("Arabic Only", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = currentStyle.displayMode == QuranDisplayMode.URDU_ONLY,
                    onClick = {
                        currentStyle = currentStyle.copy(displayMode = QuranDisplayMode.URDU_ONLY)
                        onUpdateStyle(currentStyle)
                    },
                    label = { Text("Urdu Only", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arabic Font Size Slider
            Text(
                text = "Arabic Font Size: ${currentStyle.arabicFontSize.toInt()}sp",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = currentStyle.arabicFontSize,
                onValueChange = {
                    currentStyle = currentStyle.copy(arabicFontSize = it)
                    onUpdateStyle(currentStyle)
                },
                valueRange = 16f..40f,
                colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
            )

            // Urdu Font Size Slider
            Text(
                text = "Urdu Font Size: ${currentStyle.urduFontSize.toInt()}sp",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = currentStyle.urduFontSize,
                onValueChange = {
                    currentStyle = currentStyle.copy(urduFontSize = it)
                    onUpdateStyle(currentStyle)
                },
                valueRange = 12f..28f,
                colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
            )

            // Background Opacity
            Text(
                text = "Background Box Opacity: ${(currentStyle.backgroundOpacity * 100).toInt()}%",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = currentStyle.backgroundOpacity,
                onValueChange = {
                    currentStyle = currentStyle.copy(backgroundOpacity = it)
                    onUpdateStyle(currentStyle)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = TextPrimary, activeTrackColor = TextPrimary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Presets: Classic, Elegant, Minimal, Modern, Transparent
            Text(text = "Style Presets (تیار شدہ انداز):", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        currentStyle = currentStyle.copy(
                            arabicColorHex = 0xFFFFFFFF,
                            urduColorHex = 0xFFFCD34D,
                            backgroundColorHex = 0x99000000,
                            backgroundOpacity = 0.65f,
                            isBold = true
                        )
                        onUpdateStyle(currentStyle)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Classic", fontSize = 11.sp, color = EmeraldLight)
                }

                OutlinedButton(
                    onClick = {
                        currentStyle = currentStyle.copy(
                            arabicColorHex = 0xFFFBBF24,
                            urduColorHex = 0xFFFFFFFF,
                            backgroundColorHex = 0xAA064E3B,
                            backgroundOpacity = 0.8f,
                            isBold = true
                        )
                        onUpdateStyle(currentStyle)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Elegant", fontSize = 11.sp, color = GoldLight)
                }

                OutlinedButton(
                    onClick = {
                        currentStyle = currentStyle.copy(
                            arabicColorHex = 0xFFFFFFFF,
                            urduColorHex = 0xFFE2E8F0,
                            backgroundColorHex = 0x00000000,
                            backgroundOpacity = 0f,
                            isBold = true
                        )
                        onUpdateStyle(currentStyle)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Transparent", fontSize = 11.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("DONE", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
