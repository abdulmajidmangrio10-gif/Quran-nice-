package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class EditorTool {
    CANVAS,
    VIDEO,
    AUDIO,
    QURAN,
    AI_AUTO_CAPTION,
    TEXT,
    IMAGE,
    STICKER,
    EFFECTS,
    FILTER,
    ADJUST,
    SPEED
}

@Composable
fun BottomToolBar(
    activeTool: EditorTool?,
    onSelectTool: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkBackground,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolItem(
                title = "Canvas",
                icon = Icons.Default.AspectRatio,
                isSelected = activeTool == EditorTool.CANVAS,
                onClick = { onSelectTool(EditorTool.CANVAS) }
            )
            ToolItem(
                title = "Video",
                icon = Icons.Default.Movie,
                isSelected = activeTool == EditorTool.VIDEO,
                onClick = { onSelectTool(EditorTool.VIDEO) }
            )
            ToolItem(
                title = "Audio",
                icon = Icons.Default.Mic,
                isSelected = activeTool == EditorTool.AUDIO,
                onClick = { onSelectTool(EditorTool.AUDIO) }
            )

            // Prominent Quran tool
            ToolItem(
                title = "Quran",
                icon = Icons.Default.MenuBook,
                isSelected = activeTool == EditorTool.QURAN,
                accentColor = EmeraldPrimary,
                onClick = { onSelectTool(EditorTool.QURAN) }
            )

            // Special highlighted AI Auto Caption tool
            ToolItem(
                title = "AI Auto Caption",
                icon = Icons.Default.AutoAwesome,
                isSelected = activeTool == EditorTool.AI_AUTO_CAPTION,
                accentColor = GoldAccent,
                isSpecial = true,
                onClick = { onSelectTool(EditorTool.AI_AUTO_CAPTION) }
            )

            ToolItem(
                title = "Text",
                icon = Icons.Default.Title,
                isSelected = activeTool == EditorTool.TEXT,
                onClick = { onSelectTool(EditorTool.TEXT) }
            )
            ToolItem(
                title = "Image",
                icon = Icons.Default.Photo,
                isSelected = activeTool == EditorTool.IMAGE,
                onClick = { onSelectTool(EditorTool.IMAGE) }
            )
            ToolItem(
                title = "Sticker",
                icon = Icons.Default.Mood,
                isSelected = activeTool == EditorTool.STICKER,
                onClick = { onSelectTool(EditorTool.STICKER) }
            )
            ToolItem(
                title = "Effects",
                icon = Icons.Default.AutoFixHigh,
                isSelected = activeTool == EditorTool.EFFECTS,
                onClick = { onSelectTool(EditorTool.EFFECTS) }
            )
            ToolItem(
                title = "Filter",
                icon = Icons.Default.ColorLens,
                isSelected = activeTool == EditorTool.FILTER,
                onClick = { onSelectTool(EditorTool.FILTER) }
            )
            ToolItem(
                title = "Adjust",
                icon = Icons.Default.Tune,
                isSelected = activeTool == EditorTool.ADJUST,
                onClick = { onSelectTool(EditorTool.ADJUST) }
            )
            ToolItem(
                title = "Speed",
                icon = Icons.Default.Speed,
                isSelected = activeTool == EditorTool.SPEED,
                onClick = { onSelectTool(EditorTool.SPEED) }
            )
        }
    }
}

@Composable
private fun ToolItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color = TextPrimary,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        isSpecial -> EmeraldDark.copy(alpha = 0.85f)
        isSelected -> DarkSurfaceVariant
        else -> Color.Transparent
    }
    val contentColor = when {
        isSpecial -> GoldLight
        isSelected -> EmeraldLight
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("tool_${title.lowercase().replace(" ", "_")}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected || isSpecial) FontWeight.Bold else FontWeight.Normal
        )
    }
}
