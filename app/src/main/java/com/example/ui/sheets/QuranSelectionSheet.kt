package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.quran.AyahItem
import com.example.quran.QuranDatabase
import com.example.quran.SurahMeta
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSelectionSheet(
    onDismiss: () -> Unit,
    onAddAyahToVideo: (AyahItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurah by remember { mutableStateOf<SurahMeta?>(QuranDatabase.surahs.first()) }
    var selectedAyah by remember { mutableStateOf<AyahItem?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) } // 0: Browse Surahs, 1: Search All

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quran Database (القرآن الكريم)",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Surah name or Ayah text...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldLight) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs: Surahs vs Ayahs
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = DarkBackground,
                contentColor = EmeraldLight,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = EmeraldPrimary
                    )
                }
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("1. Select Surah (${QuranDatabase.surahs.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("2. Select Ayah", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content List
            Box(modifier = Modifier.weight(1f)) {
                if (tabIndex == 0) {
                    // Surah List
                    val filteredSurahs = remember(searchQuery) {
                        if (searchQuery.isBlank()) QuranDatabase.surahs
                        else QuranDatabase.surahs.filter {
                            it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                            it.nameArabic.contains(searchQuery) ||
                            it.nameUrdu.contains(searchQuery) ||
                            it.number.toString() == searchQuery.trim()
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredSurahs) { surah ->
                            val isSelected = selectedSurah?.number == surah.number
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EmeraldDark.copy(alpha = 0.6f) else DarkSurfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSurah = surah
                                        selectedAyah = null
                                        tabIndex = 1
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkBackground,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${surah.number}",
                                                    color = GoldLight,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = surah.nameEnglish,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${surah.revelation} • ${surah.ayahCount} Verses",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = surah.nameArabic,
                                            color = GoldAccent,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "سورۃ ${surah.nameUrdu}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Ayah List for selected Surah
                    val ayahs = remember(selectedSurah, searchQuery) {
                        if (searchQuery.isNotBlank() && tabIndex == 1) {
                            QuranDatabase.searchAyahs(searchQuery, selectedSurah?.number)
                        } else {
                            QuranDatabase.getAyahsForSurah(selectedSurah?.number ?: 1)
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ayahs) { ayah ->
                            val isSelected = selectedAyah?.ayahNumber == ayah.ayahNumber && selectedAyah?.surahNumber == ayah.surahNumber
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EmeraldDark.copy(alpha = 0.5f) else DarkSurfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAyah = ayah }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Ayah ${ayah.ayahNumber}",
                                                color = EmeraldLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${ayah.surahName}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Arabic Text
                                    Text(
                                        text = ayah.arabicText,
                                        color = TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 28.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Urdu Translation
                                    Text(
                                        text = ayah.urduTranslation,
                                        color = GoldLight,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = {
                    selectedAyah?.let {
                        onAddAyahToVideo(it)
                        onDismiss()
                    }
                },
                enabled = selectedAyah != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    disabledContainerColor = DarkBorder
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_to_video_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedAyah != null) "ADD TO VIDEO (Ayah ${selectedAyah?.ayahNumber})" else "Select an Ayah to Add",
                    color = if (selectedAyah != null) Color.Black else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
