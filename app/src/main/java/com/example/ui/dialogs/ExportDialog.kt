package com.example.ui.dialogs

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.export.ExportProgress
import com.example.export.ExportQuality
import com.example.export.ExportResolution
import com.example.export.ExportResult
import com.example.export.VideoExportEngine
import com.example.model.EditorProjectState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ExportDialog(
    projectState: EditorProjectState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var resolution by remember { mutableStateOf(ExportResolution.RES_1080P) }
    var quality by remember { mutableStateOf(ExportQuality.HIGH) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }
    var exportResult by remember { mutableStateOf<ExportResult?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exportResult != null) {
                    // Export Completed Successfully!
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Video Exported Successfully!",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Size: ${exportResult?.fileSizeFormatted} • ${exportResult?.resolution?.label}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = exportResult?.outputFile ?: return@Button
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Quran Video"))
                                } catch (e: Exception) {
                                    // Fallback share intent
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Created with Quran Video Editor by Hafiz Abdul Majid Mangrew")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Video"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                } else if (isExporting) {
                    // Export in Progress
                    Text(
                        text = "Exporting Quran Video...",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { exportProgress },
                        color = EmeraldPrimary,
                        trackColor = DarkBackground,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${(exportProgress * 100).toInt()}% • $exportStatusText",
                        color = GoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                } else {
                    // Export Settings Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Export Settings",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Resolution: 720p / 1080p
                    Text(
                        text = "Resolution (ریزولیوشن):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = resolution == ExportResolution.RES_720P,
                            onClick = { resolution = ExportResolution.RES_720P },
                            label = { Text("720p HD", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = resolution == ExportResolution.RES_1080P,
                            onClick = { resolution = ExportResolution.RES_1080P },
                            label = { Text("1080p Full HD", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quality: Standard / High
                    Text(
                        text = "Quality (معیار):",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = quality == ExportQuality.STANDARD,
                            onClick = { quality = ExportQuality.STANDARD },
                            label = { Text("Standard", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                        FilterChip(
                            selected = quality == ExportQuality.HIGH,
                            onClick = { quality = ExportQuality.HIGH },
                            label = { Text("High Quality", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    if (exportError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = exportError ?: "", color = ErrorRed, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            isExporting = true
                            exportError = null
                            scope.launch {
                                VideoExportEngine.exportVideo(context, projectState, resolution, quality)
                                    .collect { prog ->
                                        when (prog) {
                                            is ExportProgress.Encoding -> {
                                                exportProgress = prog.progress
                                                exportStatusText = prog.currentStep
                                            }
                                            is ExportProgress.Success -> {
                                                isExporting = false
                                                exportResult = prog.result
                                            }
                                            is ExportProgress.Failure -> {
                                                isExporting = false
                                                exportError = prog.error
                                            }
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT VIDEO", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
