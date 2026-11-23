package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.FolderTone
import com.example.data.model.ReviewResult
import com.example.data.model.StudyFolder
import com.example.ui.components.PinyinTogglePill
import com.example.ui.components.RegencyCard
import com.example.ui.components.toneAccentColor
import com.example.ui.theme.ForgotCrimson
import com.example.ui.theme.ForgotCrimsonBg
import com.example.ui.theme.HanziFontFamily
import com.example.ui.theme.RememberedSage
import com.example.ui.theme.RememberedSageBg

private enum class CardFilter(val label: String) {
    ALL("Tất cả"),
    NEEDS_REVIEW("Cần ôn"),
    REMEMBERED("Đã nhớ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder: StudyFolder,
    cards: List<Flashcard>,
    showPinyin: Boolean,
    onTogglePinyin: () -> Unit,
    onBack: () -> Unit,
    onStartReview: (onlyNeedsReview: Boolean) -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (Flashcard) -> Unit,
    onDeleteCard: (Flashcard) -> Unit,
    onSpeakHanzi: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedFilter by remember { mutableStateOf(CardFilter.ALL) }
    val toneColor = toneAccentColor(FolderTone.fromKey(folder.accentTone))

    val filteredCards = remember(cards, selectedFilter) {
        when (selectedFilter) {
            CardFilter.ALL -> cards
            CardFilter.NEEDS_REVIEW -> cards.filter { it.lastResult != ReviewResult.REMEMBERED.key }
            CardFilter.REMEMBERED -> cards.filter { it.lastResult == ReviewResult.REMEMBERED.key }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("folder_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại thư viện"
                        )
                    }
                },
                title = {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    PinyinTogglePill(
                        showPinyin = showPinyin,
                        onToggle = onTogglePinyin,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCard,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("folder_add_card_fab")
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm thẻ")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RegencyCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(toneColor)
                        )
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (folder.description.isNotBlank()) {
                                Text(
                                    text = folder.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val rememberedCount = cards.count {
                                it.lastResult == ReviewResult.REMEMBERED.key
                            }
                            Text(
                                text = "${cards.size} thẻ  •  $rememberedCount đã nhớ",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = { onStartReview(false) },
                                enabled = cards.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("review_this_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Style,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ôn tập thư mục này (${cards.size} thẻ)")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                if (filteredCards.isEmpty()) {
                    item {
                        RegencyCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Chưa có thẻ nào",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "Nhấn \"Thêm thẻ\" và nhập chữ Hán để AI tự động tra Pinyin và nghĩa cho bạn.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCards, key = { it.id }) { card ->
                        FlashcardListItem(
                            card = card,
                            showPinyin = showPinyin,
                            onSpeak = { onSpeakHanzi(card.hanzi) },
                            onEdit = { onEditCard(card) },
                            onDelete = { onDeleteCard(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardListItem(
    card: Flashcard,
    showPinyin: Boolean,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val result = ReviewResult.fromKey(card.lastResult)
    val badgeBg = when (result) {
        ReviewResult.REMEMBERED -> RememberedSageBg
        ReviewResult.FORGOT -> ForgotCrimsonBg
        ReviewResult.NEW -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeText = when (result) {
        ReviewResult.REMEMBERED -> RememberedSage
        ReviewResult.FORGOT -> ForgotCrimson
        ReviewResult.NEW -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val badgeLabel = when (result) {
        ReviewResult.REMEMBERED -> "Đã nhớ"
        ReviewResult.FORGOT -> "Chưa nhớ"
        ReviewResult.NEW -> "Mới"
    }

    RegencyCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("flashcard_item_${card.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = card.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onSpeak) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Nghe phát âm ${card.hanzi}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Sửa thẻ ${card.hanzi}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Xóa thẻ ${card.hanzi}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = card.hanzi,
                fontFamily = HanziFontFamily,
                fontSize = if (card.hanzi.length <= 6) 28.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (showPinyin && card.pinyin.isNotBlank()) {
                Text(
                    text = card.pinyin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = card.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (card.exampleHanzi.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = card.exampleHanzi,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = HanziFontFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (card.exampleMeaning.isNotBlank()) {
                            Text(
                                text = card.exampleMeaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
