package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Style
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ScriptBrandFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Flashcard
import com.example.data.model.FolderTone
import com.example.data.model.ReviewResult
import com.example.data.model.StudyFolder
import com.example.data.model.UiState
import com.example.ui.components.PinyinTogglePill
import com.example.ui.components.RegencyCard
import com.example.ui.components.RegencyDivider
import com.example.ui.components.toneAccentColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHomeScreen(
    foldersState: UiState<List<StudyFolder>>,
    flashcardsState: UiState<List<Flashcard>>,
    selectedReviewFolderIds: Set<String>,
    showPinyin: Boolean,
    hasGeminiKey: Boolean,
    isBusy: Boolean,
    onTogglePinyin: () -> Unit,
    onOpenGeminiSetup: () -> Unit,
    onToggleFolderSelection: (String) -> Unit,
    onSelectAllFolders: (List<StudyFolder>) -> Unit,
    onClearFolderSelection: () -> Unit,
    onOpenFolder: (StudyFolder) -> Unit,
    onCreateFolderClick: () -> Unit,
    onEditFolderClick: (StudyFolder) -> Unit,
    onDeleteFolderConfirm: (StudyFolder) -> Unit,
    onAddCardClick: () -> Unit,
    onStartReviewSelected: (onlyNeedsReview: Boolean) -> Unit,
    onQuickReviewFolder: (StudyFolder) -> Unit,
    onSeedStarterDeck: () -> Unit,
    onSignOut: () -> Unit
) {
    var onlyNeedsReview by remember { mutableStateOf(false) }
    var folderPendingDelete by remember { mutableStateOf<StudyFolder?>(null) }

    val folders = (foldersState as? UiState.Success)?.data.orEmpty()
    val flashcards = (flashcardsState as? UiState.Success)?.data.orEmpty()
    val cardsByFolder = remember(flashcards) { flashcards.groupBy { it.folderId } }

    val selectedCardsCount = remember(flashcards, selectedReviewFolderIds, onlyNeedsReview) {
        flashcards.count { card ->
            card.folderId in selectedReviewFolderIds &&
                (!onlyNeedsReview || card.lastResult != ReviewResult.REMEMBERED.key)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "Vocabuddy",
                        fontFamily = ScriptBrandFamily,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenGeminiSetup,
                        modifier = Modifier.testTag("gemini_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = if (hasGeminiKey) {
                                "Đã kết nối Gemini của bạn"
                            } else {
                                "Kết nối Gemini API của bạn"
                            },
                            tint = if (hasGeminiKey) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    PinyinTogglePill(
                        showPinyin = showPinyin,
                        onToggle = onTogglePinyin
                    )
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedReviewFolderIds.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddCardClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("home_add_card_fab")
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thêm thẻ")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedReviewFolderIds.isNotEmpty(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Đã chọn ${selectedReviewFolderIds.size} thư mục",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$selectedCardsCount thẻ sẵn sàng ôn tập",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = onlyNeedsReview,
                                    onClick = { onlyNeedsReview = !onlyNeedsReview },
                                    label = { Text("Chỉ thẻ cần ôn") }
                                )
                                TextButton(onClick = onClearFolderSelection) {
                                    Text("Bỏ chọn")
                                }
                            }
                        }

                        Button(
                            onClick = { onStartReviewSelected(onlyNeedsReview) },
                            enabled = selectedCardsCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("start_multi_folder_review_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Style,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bắt đầu lướt thẻ ($selectedCardsCount thẻ)")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                foldersState is UiState.Loading || flashcardsState is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                foldersState is UiState.Error -> {
                    ErrorNoticeCard(
                        message = foldersState.message,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                flashcardsState is UiState.Error -> {
                    ErrorNoticeCard(
                        message = flashcardsState.message,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                else -> {
                    val masteredTotal = flashcards.count {
                        it.lastResult == ReviewResult.REMEMBERED.key
                    }

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = if (selectedReviewFolderIds.isNotEmpty()) 156.dp else 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Regency Overview Header Card
                        item {
                            RegencyCard(
                                containerColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Tủ thẻ chữ Hán",
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${folders.size} thư mục  •  ${flashcards.size} thẻ  •  $masteredTotal đã nhớ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onCreateFolderClick,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("create_folder_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CreateNewFolder,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Thư mục")
                                        }
                                    }

                                    if (folders.isNotEmpty()) {
                                        RegencyDivider()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tick chọn 1 hoặc nhiều thư mục để ôn bài cũ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(
                                                onClick = {
                                                    if (selectedReviewFolderIds.size == folders.size) {
                                                        onClearFolderSelection()
                                                    } else {
                                                        onSelectAllFolders(folders)
                                                    }
                                                },
                                                modifier = Modifier.testTag("select_all_folders_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DoneAll,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (selectedReviewFolderIds.size == folders.size) {
                                                        "Bỏ tất cả"
                                                    } else {
                                                        "Chọn tất cả"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (folders.isEmpty()) {
                            item {
                                RegencyCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = "Thư viện đang chờ những thẻ đầu tiên",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Hãy tạo thư mục chủ đề (vd: Giao tiếp, HSK, Thành ngữ) rồi nhập tiếng Trung để AI tự động định nghĩa.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = onCreateFolderClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.CreateNewFolder,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Tạo thư mục đầu tiên")
                                        }
                                        OutlinedButton(
                                            onClick = onSeedStarterDeck,
                                            enabled = !isBusy,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("seed_starter_deck_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Tạo bộ thẻ mẫu \"Trà thất & Giao tiếp\"")
                                        }
                                    }
                                }
                            }
                        } else {
                            items(folders, key = { it.id }) { folder ->
                                val folderCards = cardsByFolder[folder.id].orEmpty()
                                FolderRowCard(
                                    folder = folder,
                                    cards = folderCards,
                                    isSelected = folder.id in selectedReviewFolderIds,
                                    onToggleSelect = { onToggleFolderSelection(folder.id) },
                                    onOpen = { onOpenFolder(folder) },
                                    onQuickReview = { onQuickReviewFolder(folder) },
                                    onEdit = { onEditFolderClick(folder) },
                                    onDelete = { folderPendingDelete = folder }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (folderPendingDelete != null) {
        val target = folderPendingDelete!!
        val count = cardsByFolder[target.id]?.size ?: 0
        AlertDialog(
            onDismissRequest = { folderPendingDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Xóa thư mục?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Thư mục \"${target.name}\" và toàn bộ $count thẻ bên trong sẽ bị xóa vĩnh viễn.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        folderPendingDelete = null
                        onDeleteFolderConfirm(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun FolderRowCard(
    folder: StudyFolder,
    cards: List<Flashcard>,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpen: () -> Unit,
    onQuickReview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = toneAccentColor(FolderTone.fromKey(folder.accentTone))
    val rememberedCount = cards.count { it.lastResult == ReviewResult.REMEMBERED.key }
    val progress = if (cards.isEmpty()) 0f else rememberedCount.toFloat() / cards.size.toFloat()
    var menuExpanded by remember { mutableStateOf(false) }

    RegencyCard(
        borderColor = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        containerColor = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("folder_card_${folder.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accent)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = accent),
                modifier = Modifier.testTag("folder_checkbox_${folder.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (folder.description.isNotBlank()) {
                    Text(
                        text = folder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${cards.size} thẻ • $rememberedCount nhớ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (cards.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { progress },
                            color = accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            if (cards.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onQuickReview)
                        .testTag("quick_review_folder_${folder.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = "Ôn thư mục ${folder.name}",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ôn",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Tùy chọn thư mục ${folder.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa thư mục", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorNoticeCard(
    message: String,
    modifier: Modifier = Modifier
) {
    RegencyCard(
        borderColor = MaterialTheme.colorScheme.error,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}
