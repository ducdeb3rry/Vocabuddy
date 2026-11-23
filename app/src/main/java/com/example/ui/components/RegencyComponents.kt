package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.data.preferences.StudyPreferences
import com.example.ui.viewmodel.GeminiKeyCheckState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FolderTone
import com.example.data.model.StudyFolder
import com.example.ui.theme.AntiqueWalnut
import com.example.ui.theme.DustyRose
import com.example.ui.theme.RegencySage
import com.example.ui.theme.RegencySlate
import com.example.ui.theme.WarmBronze

fun toneAccentColor(tone: FolderTone): Color = when (tone) {
    FolderTone.SAGE -> RegencySage
    FolderTone.BRONZE -> WarmBronze
    FolderTone.ROSE -> DustyRose
    FolderTone.SLATE -> RegencySlate
}

@Composable
fun RegencyDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "✦",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.8.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun RegencyCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun PinyinTogglePill(
    showPinyin: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (showPinyin) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (showPinyin) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onToggle)
            .testTag("pinyin_toggle_button"),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (showPinyin) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                contentDescription = if (showPinyin) "Đang bật Pinyin" else "Đang ẩn Pinyin",
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (showPinyin) "Pinyin: Bật" else "Pinyin: Ẩn",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CreateEditFolderDialog(
    editingFolder: StudyFolder?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, tone: FolderTone) -> Unit
) {
    var name by remember(editingFolder) { mutableStateOf(editingFolder?.name.orEmpty()) }
    var description by remember(editingFolder) { mutableStateOf(editingFolder?.description.orEmpty()) }
    var selectedTone by remember(editingFolder) {
        mutableStateOf(FolderTone.fromKey(editingFolder?.accentTone))
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (editingFolder == null) "Thư mục mới" else "Chỉnh sửa thư mục",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 60) name = it },
                    label = { Text("Tên thư mục (vd: HSK 2, Trà thất)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 200) description = it },
                    label = { Text("Mô tả ngắn (không bắt buộc)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("folder_description_input")
                )
                Text(
                    text = "Sắc thái nhãn",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FolderTone.entries.forEach { tone ->
                        val color = toneAccentColor(tone)
                        val isSelected = selectedTone == tone
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.2f))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { selectedTone = tone }
                                .testTag("folder_tone_${tone.key.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = tone.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), description.trim(), selectedTone) },
                enabled = name.trim().isNotEmpty() && !isBusy,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_folder_button")
            ) {
                Text(if (editingFolder == null) "Tạo thư mục" else "Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Hủy", color = AntiqueWalnut)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeminiConnectDialog(
    currentApiKey: String,
    currentModel: String,
    checkState: GeminiKeyCheckState,
    onVerifyAndSave: (apiKey: String, model: String) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
    var showKey by remember { mutableStateOf(false) }
    val isChecking = checkState is GeminiKeyCheckState.Checking

    AlertDialog(
        onDismissRequest = { if (!isChecking) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Kết nối Gemini của bạn",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Nhập Gemini API Key từ tài khoản Google AI Studio (aistudio.google.com) của riêng bạn. Key chỉ được lưu cục bộ trên máy để dùng tính năng dịch & định nghĩa tự động.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = if (showKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showKey) "Ẩn API Key" else "Hiện API Key"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_api_key_input")
                )

                Text(
                    text = "Mô hình Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StudyPreferences.SUPPORTED_MODELS.forEach { modelId ->
                        FilterChip(
                            selected = selectedModel == modelId,
                            onClick = { selectedModel = modelId },
                            label = { Text(modelId, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (checkState is GeminiKeyCheckState.Error) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = checkState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (currentApiKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onClearKey()
                            onDismiss()
                        },
                        enabled = !isChecking,
                        modifier = Modifier.testTag("clear_gemini_key_button")
                    ) {
                        Text(
                            text = "Xóa API Key đã lưu",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifyAndSave(apiKeyInput.trim(), selectedModel) },
                enabled = apiKeyInput.trim().isNotEmpty() && !isChecking,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_gemini_key_button")
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang kiểm tra...")
                } else {
                    Text("Kiểm tra & Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isChecking,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Đóng", color = AntiqueWalnut)
            }
        }
    )
}
