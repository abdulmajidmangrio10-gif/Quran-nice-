package com.example.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autocaption.AutoCaptionProgress
import com.example.autocaption.ConfidenceLevel
import com.example.autocaption.DetectedAyahCaption
import com.example.quran.AyahItem
import com.example.quran.QuranDatabase
import com.example.quran.SurahMeta
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCaptionSheet(
    autoCaptionStatus: AutoCaptionProgress?,
    detectedCaptions: List<DetectedAyahCaption>,
    onStartAutoCaption: (Int?, String?) -> Unit,
    onConfirmAndApply: (List<DetectedAyahCaption>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedScopeIndex by remember { mutableIntStateOf(0) } // 0: All Quran, 1: Select Surah
    var selectedSurah by remember { mutableStateOf<SurahMeta?>(QuranDatabase.surahs.first()) }
    var userHintQuery by remember { mutableStateOf("") }

    // Editable copy of detected captions so user can correct or pick alternatives
    var workingCaptions by remember { mutableStateOf(detectedCaptions) }

    LaunchedEffect(detectedCaptions) {
        workingCaptions = detectedCaptions
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AI Quran Auto Caption",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% Local & Offline • No Paid API Needed",
                            color = EmeraldLight,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Processing State
            if (autoCaptionStatus != null && autoCaptionStatus is AutoCaptionProgress.Status) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { autoCaptionStatus.progress },
                            color = EmeraldPrimary,
                            trackColor = DarkBorder,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Professional status in Urdu and English
                        Text(
                            text = autoCaptionStatus.messageUrdu,
                            color = GoldLight,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = autoCaptionStatus.messageEnglish,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else if (workingCaptions.isEmpty()) {
                // Setup / Scope Selection before running
                Text(
                    text = "Recitation Recognition Scope (تلاوت دائرہ کار):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedScopeIndex == 0,
                        onClick = { selectedScopeIndex = 0 },
                        label = { Text("All Quran (تمام قرآن)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedScopeIndex == 1,
                        onClick = { selectedScopeIndex = 1 },
                        label = { Text("Select Surah (مخصوص سورۃ)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                if (selectedScopeIndex == 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Choose Surah for High-Priority Matching:", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(DarkBackground, RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(QuranDatabase.surahs) { surah ->
                            val isSelected = selectedSurah?.number == surah.number
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) EmeraldDark else DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSurah = surah }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${surah.number}. ${surah.nameEnglish}",
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${surah.nameArabic} (${surah.nameUrdu})",
                                        color = GoldLight,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userHintQuery,
                    onValueChange = { userHintQuery = it },
                    placeholder = { Text("Optional: Enter starting word or Ayah hint (مثلاً: الحمد)", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val targetSurahNum = if (selectedScopeIndex == 1) selectedSurah?.number else null
                        onStartAutoCaption(targetSurahNum, userHintQuery.ifBlank { null })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("start_auto_caption_button")
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ANALYZE AUDIO & AUTO CAPTION",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            } else {
                // Captions Review & Wrong Ayah Protection Screen
                val hasLowConfidence = workingCaptions.any { it.confidenceLevel == ConfidenceLevel.LOW }

                // Wrong Ayah Protection Warning
                if (hasLowConfidence) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33EF4444),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "آیت کی شناخت مکمل طور پر واضح نہیں ہے۔ براہ کرم درست آیت منتخب کریں۔",
                                    color = Color(0xFFFFB4AB),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                                Text(
                                    text = "Some verses have low confidence. Please verify or pick correct Ayah below.",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Text(
                    text = "Detected Captions with Timing (${workingCaptions.size} Blocks):",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(workingCaptions) { item ->
                        val badgeColor = when (item.confidenceLevel) {
                            ConfidenceLevel.HIGH -> SuccessGreen
                            ConfidenceLevel.MEDIUM -> WarningAmber
                            ConfidenceLevel.LOW -> ErrorRed
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (item.confidenceLevel == ConfidenceLevel.LOW) ErrorRed else DarkBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Timing badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkBackground
                                    ) {
                                        Text(
                                            text = "${formatTime(item.startTimeMs)} → ${formatTime(item.endTimeMs)}",
                                            color = GoldLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Confidence Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${item.confidenceLevel.name} (${(item.confidence * 100).toInt()}%)",
                                            color = badgeColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Arabic Ayah
                                Text(
                                    text = item.ayah.arabicText,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Urdu
                                Text(
                                    text = item.ayah.urduTranslation,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // If Low confidence, show alternative choices
                                if (item.alternatives.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Alternative Matches (متبادل آیات):",
                                        color = GoldLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item.alternatives.forEach { alt ->
                                            OutlinedButton(
                                                onClick = {
                                                    // Replace with alternative
                                                    workingCaptions = workingCaptions.map { current ->
                                                        if (current.id == item.id) {
                                                            current.copy(
                                                                ayah = alt,
                                                                confidence = 0.90f,
                                                                confidenceLevel = ConfidenceLevel.HIGH,
                                                                requiresUserConfirmation = false
                                                            )
                                                        } else current
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text(text = "Ayah ${alt.ayahNumber}", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Apply to Timeline Button
                Button(
                    onClick = {
                        onConfirmAndApply(workingCaptions)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_captions_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INSERT ALL CAPTIONS TO TIMELINE",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d", minutes, seconds)
}
