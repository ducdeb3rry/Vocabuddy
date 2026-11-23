package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Flashcard
import com.example.data.model.FolderTone
import com.example.data.model.GeneratedDefinition
import com.example.data.model.ReviewResult
import com.example.data.model.StudyFolder
import com.example.data.model.UiState
import com.example.data.preferences.StudyPreferences
import com.example.data.repository.ChineseAiRepository
import com.example.data.repository.FlashcardRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AiLookupState {
    data object Idle : AiLookupState
    data object Loading : AiLookupState
    data class Success(val definition: GeneratedDefinition) : AiLookupState
    data class Error(val message: String) : AiLookupState
}

sealed interface GeminiKeyCheckState {
    data object Idle : GeminiKeyCheckState
    data object Checking : GeminiKeyCheckState
    data class Error(val message: String) : GeminiKeyCheckState
}

data class ReviewSessionState(
    val active: Boolean = false,
    val folderNamesSummary: String = "",
    val deck: List<Flashcard> = emptyList(),
    val currentIndex: Int = 0,
    val rememberedIds: List<String> = emptyList(),
    val forgottenCards: List<Flashcard> = emptyList(),
    val isCurrentCardFlipped: Boolean = false
) {
    val isFinished: Boolean get() = active && currentIndex >= deck.size
    val currentCard: Flashcard? get() = deck.getOrNull(currentIndex)
    val nextCard: Flashcard? get() = deck.getOrNull(currentIndex + 1)
}

class HanziSalonViewModel(
    private val repository: FlashcardRepository,
    private val aiRepository: ChineseAiRepository,
    private val preferences: StudyPreferences,
    private val currentUserId: String
) : ViewModel() {

    val foldersState: StateFlow<UiState<List<StudyFolder>>> = repository.observeFolders(currentUserId)
        .map<List<StudyFolder>, UiState<List<StudyFolder>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing folders", rootError)
            emit(UiState.Error(rootError.message ?: "Không thể tải danh sách thư mục."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val flashcardsState: StateFlow<UiState<List<Flashcard>>> = repository.observeFlashcards(currentUserId)
        .map<List<Flashcard>, UiState<List<Flashcard>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing flashcards", rootError)
            emit(UiState.Error(rootError.message ?: "Không thể tải thẻ flashcard."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val showPinyin: StateFlow<Boolean> = preferences.showPinyin
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true
        )

    val geminiApiKey: StateFlow<String> = preferences.geminiApiKey
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ""
        )

    val geminiModel: StateFlow<String> = preferences.geminiModel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StudyPreferences.DEFAULT_GEMINI_MODEL
        )

    private val _selectedReviewFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedReviewFolderIds: StateFlow<Set<String>> = _selectedReviewFolderIds.asStateFlow()

    private val _aiLookupState = MutableStateFlow<AiLookupState>(AiLookupState.Idle)
    val aiLookupState: StateFlow<AiLookupState> = _aiLookupState.asStateFlow()

    private val _geminiKeyCheckState = MutableStateFlow<GeminiKeyCheckState>(GeminiKeyCheckState.Idle)
    val geminiKeyCheckState: StateFlow<GeminiKeyCheckState> = _geminiKeyCheckState.asStateFlow()

    private val _reviewSession = MutableStateFlow(ReviewSessionState())
    val reviewSession: StateFlow<ReviewSessionState> = _reviewSession.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun toggleShowPinyin() {
        viewModelScope.launch {
            preferences.setShowPinyin(!showPinyin.value)
        }
    }

    fun setShowPinyin(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setShowPinyin(enabled)
        }
    }

    fun toggleFolderSelectionForReview(folderId: String) {
        _selectedReviewFolderIds.update { current ->
            if (folderId in current) current - folderId else current + folderId
        }
    }

    fun selectAllFoldersForReview(folders: List<StudyFolder>) {
        _selectedReviewFolderIds.value = folders.map { it.id }.toSet()
    }

    fun clearFolderSelectionForReview() {
        _selectedReviewFolderIds.value = emptySet()
    }

    fun defineChineseWithAi(hanziInput: String) {
        viewModelScope.launch {
            _aiLookupState.value = AiLookupState.Loading
            val result = aiRepository.defineChineseInput(
                hanziInput = hanziInput,
                apiKey = geminiApiKey.value,
                modelId = geminiModel.value
            )
            result.fold(
                onSuccess = { def ->
                    _aiLookupState.value = AiLookupState.Success(def)
                },
                onFailure = { error ->
                    _aiLookupState.value = AiLookupState.Error(
                        error.message ?: "Không thể định nghĩa tự động."
                    )
                }
            )
        }
    }

    fun verifyAndSaveGeminiKey(
        apiKey: String,
        modelId: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _geminiKeyCheckState.value = GeminiKeyCheckState.Checking
            val result = aiRepository.verifyApiKey(apiKey, modelId)
            result.fold(
                onSuccess = {
                    preferences.setGeminiConfig(apiKey, modelId)
                    _geminiKeyCheckState.value = GeminiKeyCheckState.Idle
                    _actionMessage.value = "Đã kết nối Gemini của bạn ($modelId)."
                    onSuccess()
                },
                onFailure = { error ->
                    _geminiKeyCheckState.value = GeminiKeyCheckState.Error(
                        error.message ?: "Không thể xác thực Gemini API Key."
                    )
                }
            )
        }
    }

    fun clearGeminiKey() {
        viewModelScope.launch {
            preferences.clearGeminiApiKey()
            _geminiKeyCheckState.value = GeminiKeyCheckState.Idle
            _actionMessage.value = "Đã ngắt kết nối Gemini API Key."
        }
    }

    fun resetGeminiKeyCheckState() {
        _geminiKeyCheckState.value = GeminiKeyCheckState.Idle
    }

    fun resetAiLookup() {
        _aiLookupState.value = AiLookupState.Idle
    }

    fun createFolder(
        name: String,
        description: String,
        accentTone: FolderTone,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createFolder(name, description, accentTone)
            _isBusy.value = false
            result.fold(
                onSuccess = { folderId ->
                    _actionMessage.value = "Đã tạo thư mục \"$name\""
                    onCreated(folderId)
                },
                onFailure = { err ->
                    _actionMessage.value = err.message ?: "Không thể tạo thư mục."
                }
            )
        }
    }

    fun updateFolder(
        folderId: String,
        name: String,
        description: String,
        accentTone: FolderTone,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateFolder(folderId, name, description, accentTone)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = "Đã cập nhật thư mục."
                    onUpdated()
                },
                onFailure = { err ->
                    _actionMessage.value = err.message ?: "Không thể cập nhật thư mục."
                }
            )
        }
    }

    fun deleteFolder(folderId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteFolderAndCards(folderId)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _selectedReviewFolderIds.update { it - folderId }
                    _actionMessage.value = "Đã xóa thư mục và các thẻ liên quan."
                    onDeleted()
                },
                onFailure = { err ->
                    _actionMessage.value = err.message ?: "Không thể xóa thư mục."
                }
            )
        }
    }

    fun saveFlashcard(
        editingCardId: String?,
        folderId: String,
        hanzi: String,
        pinyin: String,
        meaning: String,
        category: String,
        exampleHanzi: String,
        exampleMeaning: String,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = if (editingCardId == null) {
                repository.createFlashcard(
                    folderId = folderId,
                    hanzi = hanzi,
                    pinyin = pinyin,
                    meaning = meaning,
                    category = category,
                    exampleHanzi = exampleHanzi,
                    exampleMeaning = exampleMeaning
                ).map { Unit }
            } else {
                repository.updateFlashcard(
                    cardId = editingCardId,
                    folderId = folderId,
                    hanzi = hanzi,
                    pinyin = pinyin,
                    meaning = meaning,
                    category = category,
                    exampleHanzi = exampleHanzi,
                    exampleMeaning = exampleMeaning
                )
            }
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _actionMessage.value = if (editingCardId == null) {
                        "Đã lưu thẻ \"$hanzi\""
                    } else {
                        "Đã cập nhật thẻ \"$hanzi\""
                    }
                    onSaved()
                },
                onFailure = { err ->
                    _actionMessage.value = err.message ?: "Không thể lưu thẻ flashcard."
                }
            )
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            val result = repository.deleteFlashcard(cardId)
            result.fold(
                onSuccess = { _actionMessage.value = "Đã xóa thẻ." },
                onFailure = { err -> _actionMessage.value = err.message ?: "Không thể xóa thẻ." }
            )
        }
    }

    fun seedStarterDeck() {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.seedStarterDeck()
            _isBusy.value = false
            result.fold(
                onSuccess = { _actionMessage.value = "Đã khởi tạo bộ thẻ mẫu \"Trà thất & Giao tiếp\"." },
                onFailure = { err -> _actionMessage.value = err.message ?: "Không thể tạo bộ thẻ mẫu." }
            )
        }
    }

    fun startReviewSession(
        folderIds: Set<String>,
        folders: List<StudyFolder>,
        allCards: List<Flashcard>,
        onlyNeedsReview: Boolean = false
    ): Boolean {
        val targetCards = allCards
            .filter { it.folderId in folderIds }
            .filter { !onlyNeedsReview || it.lastResult != ReviewResult.REMEMBERED.key }
            .shuffled()

        if (targetCards.isEmpty()) {
            _actionMessage.value = if (onlyNeedsReview) {
                "Các thư mục đã chọn không có thẻ nào cần ôn lại."
            } else {
                "Các thư mục đã chọn chưa có thẻ nào."
            }
            return false
        }

        val selectedNames = folders.filter { it.id in folderIds }.map { it.name }
        val summary = when {
            selectedNames.isEmpty() -> "Ôn tập"
            selectedNames.size == 1 -> selectedNames.first()
            selectedNames.size == 2 -> "${selectedNames[0]} & ${selectedNames[1]}"
            else -> "${selectedNames.first()} +${selectedNames.size - 1} thư mục"
        }

        _reviewSession.value = ReviewSessionState(
            active = true,
            folderNamesSummary = summary,
            deck = targetCards,
            currentIndex = 0,
            rememberedIds = emptyList(),
            forgottenCards = emptyList(),
            isCurrentCardFlipped = false
        )
        return true
    }

    fun flipCurrentCard() {
        _reviewSession.update { state ->
            state.copy(isCurrentCardFlipped = !state.isCurrentCardFlipped)
        }
    }

    fun swipeCurrentCard(remembered: Boolean) {
        val currentState = _reviewSession.value
        val card = currentState.currentCard ?: return

        _reviewSession.update { state ->
            state.copy(
                currentIndex = state.currentIndex + 1,
                rememberedIds = if (remembered) state.rememberedIds + card.id else state.rememberedIds,
                forgottenCards = if (!remembered) state.forgottenCards + card else state.forgottenCards,
                isCurrentCardFlipped = false
            )
        }

        viewModelScope.launch {
            val result = repository.recordSwipeReview(card.id, remembered)
            result.onFailure { err ->
                _actionMessage.value = err.message ?: "Không thể đồng bộ kết quả ôn tập."
            }
        }
    }

    fun retryForgottenCards() {
        val current = _reviewSession.value
        if (current.forgottenCards.isEmpty()) return
        _reviewSession.value = current.copy(
            deck = current.forgottenCards.shuffled(),
            currentIndex = 0,
            rememberedIds = emptyList(),
            forgottenCards = emptyList(),
            isCurrentCardFlipped = false
        )
    }

    fun restartFullDeck() {
        val current = _reviewSession.value
        if (current.deck.isEmpty()) return
        _reviewSession.value = current.copy(
            deck = current.deck.shuffled(),
            currentIndex = 0,
            rememberedIds = emptyList(),
            forgottenCards = emptyList(),
            isCurrentCardFlipped = false
        )
    }

    fun endReviewSession() {
        _reviewSession.value = ReviewSessionState(active = false)
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "HanziSalonVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
