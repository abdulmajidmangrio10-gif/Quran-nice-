package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class IslamicStickerItem(
    val symbol: String,
    val label: String
)

val IslamicStickersList = listOf(
    IslamicStickerItem("﷽", "Bismillah"),
    IslamicStickerItem("ﷲ", "Allah"),
    IslamicStickerItem("محمد ﷺ", "Muhammad ﷺ"),
    IslamicStickerItem("سُبْحَانَ اللَّهِ", "SubhanAllah"),
    IslamicStickerItem("الْحَمْدُ لِلَّهِ", "Alhamdulillah"),
    IslamicStickerItem("اللَّهُ أَكْبَرُ", "Allahu Akbar"),
    IslamicStickerItem("لَا إِلٰهَ إِلَّا اللّٰهُ", "La ilaha illallah"),
    IslamicStickerItem("أَسْتَغْفِرُ اللّٰهَ", "Astaghfirullah"),
    IslamicStickerItem("مَا شَاءَ اللَّهُ", "MashAllah"),
    IslamicStickerItem("جَزَاكَ اللَّهُ خَيْرًا", "JazakAllah"),
    IslamicStickerItem("🕋", "Kaaba"),
    IslamicStickerItem("🕌", "Masjid"),
    IslamicStickerItem("☪️", "Crescent & Star"),
    IslamicStickerItem("📖", "Holy Quran"),
    IslamicStickerItem("🤲", "Dua")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerSheet(
    onSelectSticker: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.70f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = StickerTrackColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Islamic Stickers & Calligraphy",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(IslamicStickersList) { sticker ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSticker(sticker.symbol, sticker.label)
                                onDismiss()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = sticker.symbol,
                                fontSize = 24.sp,
                                color = GoldAccent,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sticker.label,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
