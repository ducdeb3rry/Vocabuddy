package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CardCategory
import com.example.data.model.Flashcard
import com.example.data.model.StudyFolder
import com.example.ui.components.RegencyDivider
import com.example.ui.viewmodel.AiLookupState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditCardSheet(
    folders: List<StudyFolder>,
    initialFolderId: String?,
    editingCard: Flashcard?,
    hasGeminiKey: Boolean,
    aiLookupState: AiLookupState,
    isBusy: Boolean,
    onDefineWithAi: (String) -> Unit,
    onOpenGeminiSetup: () -> Unit,
    onResetAi: () -> Unit,
    onRequestNewFolder: () -> Unit,
    onSave: (
        folderId: String,
        hanzi: String,
        pinyin: String,
        meaning: String,
        category: String,
        exampleHanzi: String,
        exampleMeaning: String,
        addAnother: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedFolderId by remember(editingCard, initialFolderId, folders) {
        mutableStateOf(
            editingCard?.folderId
                ?: initialFolderId?.takeIf { id -> folders.any { it.id == id } }
                ?: folders.firstOrNull()?.id.orEmpty()
        )
    }

    LaunchedEffect(folders) {
        if (selectedFolderId.isBlank() || folders.none { it.id == selectedFolderId }) {
            selectedFolderId = folders.firstOrNull()?.id.orEmpty()
        }
    }

    var hanzi by remember(editingCard) { mutableStateOf(editingCard?.hanzi.orEmpty()) }
    var pinyin by remember(editingCard) { mutableStateOf(editingCard?.pinyin.orEmpty()) }
    var meaning by remember(editingCard) { mutableStateOf(editingCard?.meaning.orEmpty()) }
    var category by remember(editingCard) {
        mutableStateOf(editingCard?.category ?: CardCategory.WORD.label)
    }
    var exampleHanzi by remember(editingCard) { mutableStateOf(editingCard?.exampleHanzi.orEmpty()) }
    var exampleMeaning by remember(editingCard) { mutableStateOf(editingCard?.exampleMeaning.orEmpty()) }

    var showDetailsFields by remember(editingCard) { mutableStateOf(editingCard != null) }

    LaunchedEffect(aiLookupState) {
        if (aiLookupState is AiLookupState.Success) {
            val def = aiLookupState.definition
            pinyin = def.pinyin
            meaning = def.meaning
            category = def.category
            exampleHanzi = def.exampleHanzi
            exampleMeaning = def.exampleMeaning
            showDetailsFields = true
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onResetAi()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingCard == null) "Thêm thẻ tiếng Trung" else "Chỉnh sửa thẻ",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        onResetAi()
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Đóng"
                    )
                }
            }

            // Folder selector
            Text(
                text = "Thư mục lưu trữ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                folders.forEach { folder ->
                    val selected = folder.id == selectedFolderId
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
                OutlinedButton(
                    onClick = onRequestNewFolder,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("sheet_create_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thư mục mới")
                }
            }

            // Chinese-only input
            OutlinedTextField(
                value = hanzi,
                onValueChange = { if (it.length <= 300) hanzi = it },
                label = { Text("Nhập tiếng Trung (từ, cụm từ, thành ngữ hoặc câu)") },
                placeholder = { Text("Ví dụ: 茶, 风和日丽, 今天天气真好") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hanzi_input_field")
            )

            if (!hasGeminiKey) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Kết nối Gemini của bạn để dịch tự động",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ứng dụng sử dụng Gemini API Key của chính bạn để tự động tra Pinyin, nghĩa và câu ví dụ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onOpenGeminiSetup,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("sheet_connect_gemini_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nhập Gemini API Key")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onDefineWithAi(hanzi) },
                        enabled = hanzi.trim().isNotEmpty() && aiLookupState !is AiLookupState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("auto_define_ai_button")
                    ) {
                        if (aiLookupState is AiLookupState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Đang dịch bằng Gemini...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dịch & đính nghĩa (Gemini)")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenGeminiSetup,
                        modifier = Modifier.heightIn(min = 50.dp)
                    ) {
                        Text("API Key")
                    }
                }
            }

            if (aiLookupState is AiLookupState.Error) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiLookupState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (!showDetailsFields && editingCard == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showDetailsFields = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Hoặc tự nhập nghĩa thủ công")
                    }
                }
            }

            AnimatedVisibility(visible = showDetailsFields) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RegencyDivider()

                    OutlinedTextField(
                        value = pinyin,
                        onValueChange = { if (it.length <= 500) pinyin = it },
                        label = { Text("Pinyin") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pinyin_input_field")
                    )

                    OutlinedTextField(
                        value = meaning,
                        onValueChange = { if (it.length <= 500) meaning = it },
                        label = { Text("Nghĩa tiếng Việt *") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meaning_input_field")
                    )

                    Text(
                        text = "Phân loại",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardCategory.entries.forEach { item ->
                            FilterChip(
                                selected = category == item.label,
                                onClick = { category = item.label },
                                label = { Text(item.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = exampleHanzi,
                        onValueChange = { if (it.length <= 400) exampleHanzi = it },
                        label = { Text("Câu ví dụ tiếng Trung (tùy chọn)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = exampleMeaning,
                        onValueChange = { if (it.length <= 500) exampleMeaning = it },
                        label = { Text("Dịch câu ví dụ (tùy chọn)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val canSave = selectedFolderId.isNotBlank() &&
                hanzi.trim().isNotEmpty() &&
                meaning.trim().isNotEmpty() &&
                !isBusy

            if (editingCard == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onSave(
                                selectedFolderId,
                                hanzi,
                                pinyin,
                                meaning,
                                category,
                                exampleHanzi,
                                exampleMeaning,
                                true
                            )
                            hanzi = ""
                            pinyin = ""
                            meaning = ""
                            exampleHanzi = ""
                            exampleMeaning = ""
                            showDetailsFields = false
                            onResetAi()
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("save_and_next_card_button")
                    ) {
                        Text("Lưu & Thêm tiếp", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            onSave(
                                selectedFolderId,
                                hanzi,
                                pinyin,
                                meaning,
                                category,
                                exampleHanzi,
                                exampleMeaning,
                                false
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .testTag("save_card_button")
                    ) {
                        Text("Lưu thẻ")
                    }
                }
            } else {
                Button(
                    onClick = {
                        onSave(
                            selectedFolderId,
                            hanzi,
                            pinyin,
                            meaning,
                            category,
                            exampleHanzi,
                            exampleMeaning,
                            false
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("update_card_button")
                ) {
                    Text("Cập nhật thẻ")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
