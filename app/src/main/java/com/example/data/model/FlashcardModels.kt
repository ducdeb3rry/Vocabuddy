package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class FolderTone(val key: String, val label: String) {
    SAGE("SAGE", "Xanh xô thơm"),
    BRONZE("BRONZE", "Đồng cổ"),
    ROSE("ROSE", "Hồng trà"),
    SLATE("SLATE", "Xám đá");

    companion object {
        fun fromKey(raw: String?): FolderTone =
            entries.firstOrNull { it.key == raw } ?: SAGE
    }
}

enum class CardCategory(val label: String) {
    WORD("Từ vựng"),
    PHRASE("Cụm từ"),
    SENTENCE("Câu"),
    IDIOM("Thành ngữ");

    companion object {
        fun fromLabel(raw: String?): CardCategory =
            entries.firstOrNull { it.label == raw } ?: WORD
    }
}

enum class ReviewResult(val key: String) {
    NEW("NEW"),
    REMEMBERED("REMEMBERED"),
    FORGOT("FORGOT");

    companion object {
        fun fromKey(raw: String?): ReviewResult =
            entries.firstOrNull { it.key == raw } ?: NEW
    }
}

@Keep
@IgnoreExtraProperties
data class StudyFolder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val accentTone: String = FolderTone.SAGE.key,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Flashcard(
    @DocumentId val id: String = "",
    val userId: String = "",
    val folderId: String = "",
    val hanzi: String = "",
    val pinyin: String = "",
    val meaning: String = "",
    val category: String = CardCategory.WORD.label,
    val exampleHanzi: String = "",
    val exampleMeaning: String = "",
    val lastResult: String = ReviewResult.NEW.key,
    val masteryCount: Long = 0L,
    val lapseCount: Long = 0L,
    val lastReviewedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GeneratedDefinition(
    val hanzi: String,
    val pinyin: String,
    val meaning: String,
    val category: String,
    val exampleHanzi: String,
    val exampleMeaning: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
