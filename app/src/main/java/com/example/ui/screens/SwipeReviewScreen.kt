package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Flashcard
import com.example.data.model.StudyFolder
import com.example.ui.components.PinyinTogglePill
import com.example.ui.components.RegencyCard
import com.example.ui.components.RegencyDivider
import com.example.ui.theme.ForgotCrimson
import com.example.ui.theme.ForgotCrimsonBg
import com.example.ui.theme.HanziFontFamily
import com.example.ui.theme.RememberedSage
import com.example.ui.theme.RememberedSageBg
import com.example.ui.viewmodel.ReviewSessionState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeReviewScreen(
    session: ReviewSessionState,
    folders: List<StudyFolder>,
    showPinyin: Boolean,
    onTogglePinyin: () -> Unit,
    onFlipCard: () -> Unit,
    onSwipeDecision: (remembered: Boolean) -> Unit,
    onRetryForgotten: () -> Unit,
    onRestartAll: () -> Unit,
    onSpeakHanzi: (String) -> Unit,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val folderMap = remember(folders) { folders.associateBy { it.id } }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentCard = session.currentCard
    val nextCard = session.nextCard

    val offsetX = remember(currentCard?.id) { Animatable(0f) }
    val offsetY = remember(currentCard?.id) { Animatable(0f) }

    LaunchedEffect(currentCard?.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    fun animateAndCommitSwipe(remembered: Boolean) {
        if (currentCard == null) return
        scope.launch {
            val targetX = if (remembered) 1300f else -1300f
            offsetX.animateTo(targetX, animationSpec = tween(durationMillis = 220))
            onSwipeDecision(remembered)
        }
    }

    val progress = if (session.deck.isEmpty()) {
        0f
    } else {
        (session.currentIndex.toFloat() / session.deck.size.toFloat()).coerceIn(0f, 1f)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier.testTag("review_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Thoát chế độ ôn tập"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = session.folderNamesSummary,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!session.isFinished && session.deck.isNotEmpty()) {
                                Text(
                                    text = "Thẻ ${session.currentIndex + 1} / ${session.deck.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        PinyinTogglePill(
                            showPinyin = showPinyin,
                            onToggle = onTogglePinyin,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            if (session.isFinished || currentCard == null) {
                ReviewSummaryView(
                    session = session,
                    onRetryForgotten = onRetryForgotten,
                    onRestartAll = onRestartAll,
                    onExit = onExit
                )
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Subtle direction legend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "← Quẹt trái: Chưa nhớ",
                            style = MaterialTheme.typography.labelMedium,
                            color = ForgotCrimson
                        )
                        Text(
                            text = "Quẹt phải: Đã nhớ →",
                            style = MaterialTheme.typography.labelMedium,
                            color = RememberedSage
                        )
                    }

                    // Card stack area
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val swipeThresholdPx = with(density) {
                            min(120.dp.toPx(), maxWidth.toPx() * 0.30f)
                        }
                        val dragProgress = (abs(offsetX.value) / swipeThresholdPx).coerceIn(0f, 1f)

                        // Next card peeking behind
                        if (nextCard != null) {
                            val scale = 0.94f + (0.06f * dragProgress)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        alpha = 0.65f + (0.35f * dragProgress)
                                    }
                            ) {
                                FlashcardFace(
                                    card = nextCard,
                                    folderName = folderMap[nextCard.folderId]?.name.orEmpty(),
                                    showPinyin = showPinyin,
                                    isFlipped = false,
                                    dragOffsetX = 0f,
                                    swipeThresholdPx = swipeThresholdPx,
                                    onSpeak = {},
                                    onFlip = {}
                                )
                            }
                        }

                        // Active top card
                        key(currentCard.id) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        IntOffset(
                                            offsetX.value.roundToInt(),
                                            (offsetY.value * 0.35f).roundToInt()
                                        )
                                    }
                                    .graphicsLayer {
                                        rotationZ = (offsetX.value / 32f).coerceIn(-14f, 14f)
                                    }
                                    .pointerInput(currentCard.id) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val x = offsetX.value
                                                if (x > swipeThresholdPx) {
                                                    animateAndCommitSwipe(remembered = true)
                                                } else if (x < -swipeThresholdPx) {
                                                    animateAndCommitSwipe(remembered = false)
                                                } else {
                                                    scope.launch {
                                                        launch {
                                                            offsetX.animateTo(
                                                                0f,
                                                                spring(
                                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                    stiffness = Spring.StiffnessMedium
                                                                )
                                                            )
                                                        }
                                                        launch {
                                                            offsetY.animateTo(
                                                                0f,
                                                                spring(stiffness = Spring.StiffnessMedium)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch {
                                                    offsetX.animateTo(0f)
                                                    offsetY.animateTo(0f)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                scope.launch {
                                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                                    offsetY.snapTo(offsetY.value + dragAmount.y)
                                                }
                                            }
                                        )
                                    }
                                    .testTag("swipeable_flashcard")
                            ) {
                                FlashcardFace(
                                    card = currentCard,
                                    folderName = folderMap[currentCard.folderId]?.name.orEmpty(),
                                    showPinyin = showPinyin,
                                    isFlipped = session.isCurrentCardFlipped,
                                    dragOffsetX = offsetX.value,
                                    swipeThresholdPx = swipeThresholdPx,
                                    onSpeak = { onSpeakHanzi(currentCard.hanzi) },
                                    onFlip = onFlipCard
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tactile Regency action controls (accessible alternative to swiping)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            OutlinedIconButton(
                                onClick = { animateAndCommitSwipe(remembered = false) },
                                border = BorderStroke(1.5.dp, ForgotCrimson),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    containerColor = ForgotCrimsonBg,
                                    contentColor = ForgotCrimson
                                ),
                                modifier = Modifier
                                    .size(58.dp)
                                    .testTag("swipe_left_forgot_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Chưa nhớ",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chưa nhớ",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForgotCrimson
                            )
                        }

                        OutlinedButton(
                            onClick = onFlipCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .heightIn(min = 50.dp)
                                .testTag("flip_card_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flip,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (session.isCurrentCardFlipped) "Mặt chữ" else "Xem nghĩa")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = { animateAndCommitSwipe(remembered = true) },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = RememberedSage,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(58.dp)
                                    .testTag("swipe_right_remembered_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Đã nhớ",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Đã nhớ",
                                style = MaterialTheme.typography.labelSmall,
                                color = RememberedSage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardFace(
    card: Flashcard,
    folderName: String,
    showPinyin: Boolean,
    isFlipped: Boolean,
    dragOffsetX: Float,
    swipeThresholdPx: Float,
    onSpeak: () -> Unit,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "card_flip"
    )
    val showBack = rotation > 90f

    val rightAlpha = ((dragOffsetX / swipeThresholdPx)).coerceIn(0f, 1f)
    val leftAlpha = ((-dragOffsetX / swipeThresholdPx)).coerceIn(0f, 1f)

    val dynamicBorderColor = when {
        rightAlpha > 0.25f -> RememberedSage
        leftAlpha > 0.25f -> ForgotCrimson
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (rightAlpha > 0.25f || leftAlpha > 0.25f) 2.dp else 1.2.dp, dynamicBorderColor),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onFlip)
    ) {
        // Inner Regency matte frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .border(
                    width = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(18.dp)
        ) {
            if (!showBack) {
                // FRONT: Chinese Characters + optional Pinyin
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            if (folderName.isNotBlank()) {
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("card_speak_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Phát âm ${card.hanzi}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        val hanziFontSize = when {
                            card.hanzi.length <= 2 -> 58.sp
                            card.hanzi.length <= 5 -> 44.sp
                            card.hanzi.length <= 12 -> 32.sp
                            else -> 24.sp
                        }
                        Text(
                            text = card.hanzi,
                            fontFamily = HanziFontFamily,
                            fontSize = hanziFontSize,
                            lineHeight = hanziFontSize * 1.25f,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        AnimatedVisibility(visible = showPinyin && card.pinyin.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = card.pinyin,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("card_pinyin_text")
                                )
                            }
                        }
                    }

                    Text(
                        text = "Chạm vào thẻ để lật xem nghĩa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // BACK (un-mirrored with graphicsLayer { rotationY = 180f })
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.hanzi,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = HanziFontFamily),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showPinyin && card.pinyin.isNotBlank()) {
                                Text(
                                    text = card.pinyin,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Phát âm ${card.hanzi}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ĐỊNH NGHĨA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = card.meaning,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        if (card.exampleHanzi.isNotBlank()) {
                            RegencyDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = card.exampleHanzi,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = HanziFontFamily),
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

                    Text(
                        text = "Quẹt trái: Chưa nhớ  •  Quẹt phải: Đã nhớ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Regency Wax-Seal Swipe Stamps
            if (rightAlpha > 0.1f) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RememberedSageBg,
                    border = BorderStroke(2.dp, RememberedSage),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .alpha(rightAlpha)
                        .graphicsLayer { rotationZ = -12f }
                ) {
                    Text(
                        text = "ĐÃ NHỚ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = RememberedSage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (leftAlpha > 0.1f) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ForgotCrimsonBg,
                    border = BorderStroke(2.dp, ForgotCrimson),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .alpha(leftAlpha)
                        .graphicsLayer { rotationZ = 12f }
                ) {
                    Text(
                        text = "CHƯA NHỚ",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForgotCrimson,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSummaryView(
    session: ReviewSessionState,
    onRetryForgotten: () -> Unit,
    onRestartAll: () -> Unit,
    onExit: () -> Unit
) {
    val total = session.deck.size
    val remembered = session.rememberedIds.size
    val forgotten = session.forgottenCards.size
    val rate = if (total == 0) 0 else ((remembered * 100f) / total).roundToInt()

    RegencyCard(
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Hoàn thành phiên ôn",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = session.folderNamesSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            RegencyDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStatColumn(label = "Đã nhớ", value = "$remembered", color = RememberedSage)
                SummaryStatColumn(label = "Chưa nhớ", value = "$forgotten", color = ForgotCrimson)
                SummaryStatColumn(
                    label = "Tỷ lệ",
                    value = "$rate%",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (forgotten > 0) {
                Button(
                    onClick = onRetryForgotten,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("retry_forgotten_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ôn lại $forgotten thẻ chưa nhớ")
                }
            }

            OutlinedButton(
                onClick = onRestartAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Lướt lại toàn bộ ($total thẻ)")
            }

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("finish_review_button")
            ) {
                Text("Quay về tủ thẻ")
            }
        }
    }
}

@Composable
private fun SummaryStatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
