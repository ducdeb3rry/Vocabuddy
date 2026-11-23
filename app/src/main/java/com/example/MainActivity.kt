package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.ChineseSpeaker
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.model.Flashcard
import com.example.data.model.StudyFolder
import com.example.data.model.UiState
import com.example.data.preferences.StudyPreferences
import com.example.data.repository.ChineseAiRepository
import com.example.data.repository.FlashcardRepository
import com.example.firebase.configureAppCheck
import com.example.ui.components.CreateEditFolderDialog
import com.example.ui.components.GeminiConnectDialog
import com.example.ui.screens.AddEditCardSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.LibraryHomeScreen
import com.example.ui.screens.SwipeReviewScreen
import com.example.ui.theme.HanziSalonTheme
import com.example.ui.viewmodel.HanziSalonViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure App Check with launch intent before rendering UI or creating ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            HanziSalonTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedSalonApp(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedSalonApp(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: HanziSalonViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HanziSalonViewModel(
                    repository = FlashcardRepository(db),
                    aiRepository = ChineseAiRepository(),
                    preferences = StudyPreferences(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val speaker = remember(context) { ChineseSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.release() }
    }

    val foldersState by viewModel.foldersState.collectAsStateWithLifecycle()
    val flashcardsState by viewModel.flashcardsState.collectAsStateWithLifecycle()
    val showPinyin by viewModel.showPinyin.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val geminiModel by viewModel.geminiModel.collectAsStateWithLifecycle()
    val geminiKeyCheckState by viewModel.geminiKeyCheckState.collectAsStateWithLifecycle()
    val selectedReviewFolderIds by viewModel.selectedReviewFolderIds.collectAsStateWithLifecycle()
    val aiLookupState by viewModel.aiLookupState.collectAsStateWithLifecycle()
    val reviewSession by viewModel.reviewSession.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val hasGeminiKey = geminiApiKey.isNotBlank()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        val msg = actionMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    var openedFolderId by remember { mutableStateOf<String?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showGeminiDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<StudyFolder?>(null) }
    var showCardSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Flashcard?>(null) }
    var sheetDefaultFolderId by remember { mutableStateOf<String?>(null) }

    val folders = (foldersState as? UiState.Success)?.data.orEmpty()
    val flashcards = (flashcardsState as? UiState.Success)?.data.orEmpty()
    val openedFolder = folders.firstOrNull { it.id == openedFolderId }

    LaunchedEffect(openedFolderId, foldersState) {
        if (openedFolderId != null && foldersState is UiState.Success && openedFolder == null) {
            openedFolderId = null
        }
    }

    fun speakChinese(text: String) {
        if (!speaker.speak(text)) {
            scope.launch {
                snackbarHostState.showSnackbar("Thiết bị chưa sẵn sàng giọng đọc tiếng Trung.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            reviewSession.active -> {
                SwipeReviewScreen(
                    session = reviewSession,
                    folders = folders,
                    showPinyin = showPinyin,
                    onTogglePinyin = { viewModel.toggleShowPinyin() },
                    onFlipCard = { viewModel.flipCurrentCard() },
                    onSwipeDecision = { remembered -> viewModel.swipeCurrentCard(remembered) },
                    onRetryForgotten = { viewModel.retryForgottenCards() },
                    onRestartAll = { viewModel.restartFullDeck() },
                    onSpeakHanzi = ::speakChinese,
                    onExit = { viewModel.endReviewSession() }
                )
            }

            openedFolder != null -> {
                val folderCards = remember(flashcards, openedFolder.id) {
                    flashcards.filter { it.folderId == openedFolder.id }
                }
                FolderDetailScreen(
                    folder = openedFolder,
                    cards = folderCards,
                    showPinyin = showPinyin,
                    onTogglePinyin = { viewModel.toggleShowPinyin() },
                    onBack = { openedFolderId = null },
                    onStartReview = { onlyNeedsReview ->
                        viewModel.startReviewSession(
                            folderIds = setOf(openedFolder.id),
                            folders = folders,
                            allCards = flashcards,
                            onlyNeedsReview = onlyNeedsReview
                        )
                    },
                    onAddCard = {
                        editingCard = null
                        sheetDefaultFolderId = openedFolder.id
                        viewModel.resetAiLookup()
                        showCardSheet = true
                    },
                    onEditCard = { card ->
                        editingCard = card
                        sheetDefaultFolderId = card.folderId
                        viewModel.resetAiLookup()
                        showCardSheet = true
                    },
                    onDeleteCard = { card -> viewModel.deleteFlashcard(card.id) },
                    onSpeakHanzi = ::speakChinese
                )
            }

            else -> {
                LibraryHomeScreen(
                    foldersState = foldersState,
                    flashcardsState = flashcardsState,
                    selectedReviewFolderIds = selectedReviewFolderIds,
                    showPinyin = showPinyin,
                    hasGeminiKey = hasGeminiKey,
                    isBusy = isBusy,
                    onTogglePinyin = { viewModel.toggleShowPinyin() },
                    onOpenGeminiSetup = {
                        viewModel.resetGeminiKeyCheckState()
                        showGeminiDialog = true
                    },
                    onToggleFolderSelection = { id -> viewModel.toggleFolderSelectionForReview(id) },
                    onSelectAllFolders = { list -> viewModel.selectAllFoldersForReview(list) },
                    onClearFolderSelection = { viewModel.clearFolderSelectionForReview() },
                    onOpenFolder = { folder -> openedFolderId = folder.id },
                    onCreateFolderClick = {
                        editingFolder = null
                        showFolderDialog = true
                    },
                    onEditFolderClick = { folder ->
                        editingFolder = folder
                        showFolderDialog = true
                    },
                    onDeleteFolderConfirm = { folder -> viewModel.deleteFolder(folder.id) },
                    onAddCardClick = {
                        if (folders.isEmpty()) {
                            editingFolder = null
                            showFolderDialog = true
                            scope.launch {
                                snackbarHostState.showSnackbar("Hãy tạo một thư mục trước khi thêm thẻ nhé.")
                            }
                        } else {
                            editingCard = null
                            sheetDefaultFolderId = folders.firstOrNull()?.id
                            viewModel.resetAiLookup()
                            showCardSheet = true
                        }
                    },
                    onStartReviewSelected = { onlyNeedsReview ->
                        viewModel.startReviewSession(
                            folderIds = selectedReviewFolderIds,
                            folders = folders,
                            allCards = flashcards,
                            onlyNeedsReview = onlyNeedsReview
                        )
                    },
                    onQuickReviewFolder = { folder ->
                        viewModel.startReviewSession(
                            folderIds = setOf(folder.id),
                            folders = folders,
                            allCards = flashcards,
                            onlyNeedsReview = false
                        )
                    },
                    onSeedStarterDeck = { viewModel.seedStarterDeck() },
                    onSignOut = {
                        signOut(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
        )
    }

    if (showFolderDialog) {
        CreateEditFolderDialog(
            editingFolder = editingFolder,
            isBusy = isBusy,
            onDismiss = { showFolderDialog = false },
            onConfirm = { name, desc, tone ->
                val target = editingFolder
                if (target == null) {
                    viewModel.createFolder(name, desc, tone) { newFolderId ->
                        showFolderDialog = false
                        if (showCardSheet) {
                            sheetDefaultFolderId = newFolderId
                        }
                    }
                } else {
                    viewModel.updateFolder(target.id, name, desc, tone) {
                        showFolderDialog = false
                    }
                }
            }
        )
    }

    if (showCardSheet) {
        AddEditCardSheet(
            folders = folders,
            initialFolderId = sheetDefaultFolderId,
            editingCard = editingCard,
            hasGeminiKey = hasGeminiKey,
            aiLookupState = aiLookupState,
            isBusy = isBusy,
            onDefineWithAi = { hanzi -> viewModel.defineChineseWithAi(hanzi) },
            onOpenGeminiSetup = {
                viewModel.resetGeminiKeyCheckState()
                showGeminiDialog = true
            },
            onResetAi = { viewModel.resetAiLookup() },
            onRequestNewFolder = {
                editingFolder = null
                showFolderDialog = true
            },
            onSave = { folderId, hanzi, pinyin, meaning, category, exHanzi, exMeaning, addAnother ->
                viewModel.saveFlashcard(
                    editingCardId = editingCard?.id,
                    folderId = folderId,
                    hanzi = hanzi,
                    pinyin = pinyin,
                    meaning = meaning,
                    category = category,
                    exampleHanzi = exHanzi,
                    exampleMeaning = exMeaning
                ) {
                    if (!addAnother) {
                        showCardSheet = false
                        viewModel.resetAiLookup()
                    }
                }
            },
            onDismiss = {
                showCardSheet = false
                viewModel.resetAiLookup()
            }
        )
    }

    if (showGeminiDialog) {
        GeminiConnectDialog(
            currentApiKey = geminiApiKey,
            currentModel = geminiModel,
            checkState = geminiKeyCheckState,
            onVerifyAndSave = { key, model ->
                viewModel.verifyAndSaveGeminiKey(key, model) {
                    showGeminiDialog = false
                }
            },
            onClearKey = { viewModel.clearGeminiKey() },
            onDismiss = {
                viewModel.resetGeminiKeyCheckState()
                showGeminiDialog = false
            }
        )
    }
}
