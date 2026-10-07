package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TextAnimation
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditSheet(
    initialText: String = "",
    onAddOrUpdateText: (String, Float, Long, Boolean, Boolean, TextAnimation) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(24f) }
    var isBold by remember { mutableStateOf(true) }
    var isItalic by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableLongStateOf(0xFFFFFFFF) }
    var selectedAnimation by remember { mutableStateOf(TextAnimation.NONE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Title, contentDescription = null, tint = EmeraldLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (initialText.isBlank()) "Add Custom Text" else "Edit Text",
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

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Enter title, translation or notes...", color = TextMuted) },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("text_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Font Size: ${fontSize.toInt()}sp", color = TextPrimary, fontSize = 13.sp)
            Slider(
                value = fontSize,
                onValueChange = { fontSize = it },
                valueRange = 14f..48f,
                colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Styling toggles
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = isBold,
                    onClick = { isBold = !isBold },
                    label = { Text("Bold", fontWeight = FontWeight.Bold) }
                )
                FilterChip(
                    selected = isItalic,
                    onClick = { isItalic = !isItalic },
                    label = { Text("Italic") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animation selection
            Text(text = "Text Animation (اینیمیشن):", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedAnimation == TextAnimation.NONE,
                    onClick = { selectedAnimation = TextAnimation.NONE },
                    label = { Text("None", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedAnimation == TextAnimation.FADE_IN,
                    onClick = { selectedAnimation = TextAnimation.FADE_IN },
                    label = { Text("Fade In", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedAnimation == TextAnimation.ZOOM_IN,
                    onClick = { selectedAnimation = TextAnimation.ZOOM_IN },
                    label = { Text("Zoom In", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedAnimation == TextAnimation.SLIDE_UP,
                    onClick = { selectedAnimation = TextAnimation.SLIDE_UP },
                    label = { Text("Slide Up", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onAddOrUpdateText(textInput, fontSize, selectedColorHex, isBold, isItalic, selectedAnimation)
                        onDismiss()
                    }
                },
                enabled = textInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_text_button")
            ) {
                Text("APPLY TEXT", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
