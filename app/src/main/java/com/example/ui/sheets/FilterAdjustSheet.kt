package com.example.ui.sheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.EditorTool
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAdjustSheet(
    tool: EditorTool,
    currentFilter: VideoFilter,
    currentAdjustment: VideoAdjustment,
    currentRatio: CanvasRatio,
    currentSpeed: Float,
    onSetFilter: (VideoFilter) -> Unit,
    onSetAdjustment: (VideoAdjustment) -> Unit,
    onAddEffect: (EffectType) -> Unit,
    onSetRatio: (CanvasRatio) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var adj by remember { mutableStateOf(currentAdjustment) }

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
                Text(
                    text = when (tool) {
                        EditorTool.FILTER -> "Video Filters (فلٹرز)"
                        EditorTool.ADJUST -> "Color Adjustments (رنگ درست کریں)"
                        EditorTool.EFFECTS -> "Visual Effects (اثرات)"
                        EditorTool.CANVAS -> "Canvas Aspect Ratio"
                        EditorTool.SPEED -> "Playback Speed (رفتار)"
                        else -> "Settings"
                    },
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (tool) {
                EditorTool.FILTER -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VideoFilter.values().forEach { f ->
                            FilterChip(
                                selected = currentFilter == f,
                                onClick = { onSetFilter(f) },
                                label = { Text(f.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                EditorTool.ADJUST -> {
                    Text(text = "Brightness: ${(adj.brightness * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = adj.brightness,
                        onValueChange = {
                            adj = adj.copy(brightness = it)
                            onSetAdjustment(adj)
                        },
                        valueRange = -0.5f..0.5f
                    )

                    Text(text = "Contrast: ${(adj.contrast * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = adj.contrast,
                        onValueChange = {
                            adj = adj.copy(contrast = it)
                            onSetAdjustment(adj)
                        },
                        valueRange = 0.5f..2.0f
                    )

                    Text(text = "Saturation: ${(adj.saturation * 100).toInt()}%", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = adj.saturation,
                        onValueChange = {
                            adj = adj.copy(saturation = it)
                            onSetAdjustment(adj)
                        },
                        valueRange = 0f..2.0f
                    )
                }

                EditorTool.EFFECTS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EffectType.values().forEach { eff ->
                            OutlinedButton(
                                onClick = {
                                    onAddEffect(eff)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(eff.name.replace("_", " "), fontSize = 11.sp, color = EffectTrackColor)
                            }
                        }
                    }
                }

                EditorTool.CANVAS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasRatio.values().forEach { r ->
                            FilterChip(
                                selected = currentRatio == r,
                                onClick = { onSetRatio(r) },
                                label = { Text(r.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                EditorTool.SPEED -> {
                    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        speeds.forEach { spd ->
                            FilterChip(
                                selected = currentSpeed == spd,
                                onClick = { onSetSpeed(spd) },
                                label = { Text("${spd}x", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
