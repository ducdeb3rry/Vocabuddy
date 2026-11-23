package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CardCategory
import com.example.data.model.Flashcard
import com.example.data.model.FolderTone
import com.example.data.model.ReviewResult
import com.example.data.model.StudyFolder
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FlashcardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Vui lòng đăng nhập Google trước khi truy cập dữ liệu.")
    }

    fun observeFolders(userId: String): Flow<List<StudyFolder>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/folders"
        emitAll(
            db.collection("users").document(userId).collection("folders")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StudyFolder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFlashcards(userId: String): Flow<List<Flashcard>> = flow {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/flashcards"
        emitAll(
            db.collection("users").document(userId).collection("flashcards")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Flashcard::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createFolder(
        name: String,
        description: String,
        accentTone: FolderTone
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/folders"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Tên thư mục không được để trống." }
            val cleanDesc = description.trim().take(200)

            val docRef = db.collection("users").document(uid).collection("folders").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "description" to cleanDesc.takeIf { it.isNotEmpty() },
                "accentTone" to accentTone.key,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFolder(
        folderId: String,
        name: String,
        description: String,
        accentTone: FolderTone
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/folders/$folderId"
        return try {
            val uid = requireUserId()
            require(folderId.isNotBlank()) { "Mã thư mục không hợp lệ." }
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Tên thư mục không được để trống." }
            val cleanDesc = description.trim().take(200)

            val docRef = db.collection("users").document(uid)
                .collection("folders").document(folderId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to cleanName,
                    "description" to cleanDesc,
                    "accentTone" to accentTone.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFolderAndCards(folderId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/folders/$folderId"
        return try {
            val uid = requireUserId()
            require(folderId.isNotBlank()) { "Mã thư mục không hợp lệ." }
            val userDoc = db.collection("users").document(uid)
            val folderRef = userDoc.collection("folders").document(folderId)
            docPath = folderRef.path

            val cardDocs = userDoc.collection("flashcards")
                .whereEqualTo("folderId", folderId)
                .get()
                .await()
                .documents

            val chunks = cardDocs.chunked(400)
            if (chunks.isEmpty()) {
                folderRef.delete().await()
            } else {
                chunks.forEachIndexed { idx, chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    if (idx == chunks.lastIndex) {
                        batch.delete(folderRef)
                    }
                    batch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createFlashcard(
        folderId: String,
        hanzi: String,
        pinyin: String,
        meaning: String,
        category: String,
        exampleHanzi: String,
        exampleMeaning: String
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/flashcards"
        return try {
            val uid = requireUserId()
            require(folderId.isNotBlank()) { "Vui lòng chọn thư mục cho thẻ." }
            val cleanHanzi = hanzi.trim().take(300)
            val cleanPinyin = pinyin.trim().take(500)
            val cleanMeaning = meaning.trim().take(500)
            require(cleanHanzi.isNotEmpty()) { "Nội dung tiếng Trung không được để trống." }
            require(cleanMeaning.isNotEmpty()) { "Nghĩa của thẻ không được để trống." }

            val validCategory = CardCategory.fromLabel(category).label
            val cleanExHanzi = exampleHanzi.trim().take(400)
            val cleanExMeaning = exampleMeaning.trim().take(500)

            val docRef = db.collection("users").document(uid).collection("flashcards").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "folderId" to folderId,
                "hanzi" to cleanHanzi,
                "pinyin" to cleanPinyin,
                "meaning" to cleanMeaning,
                "category" to validCategory,
                "exampleHanzi" to cleanExHanzi.takeIf { it.isNotEmpty() },
                "exampleMeaning" to cleanExMeaning.takeIf { it.isNotEmpty() },
                "lastResult" to ReviewResult.NEW.key,
                "masteryCount" to 0L,
                "lapseCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFlashcard(
        cardId: String,
        folderId: String,
        hanzi: String,
        pinyin: String,
        meaning: String,
        category: String,
        exampleHanzi: String,
        exampleMeaning: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/flashcards/$cardId"
        return try {
            val uid = requireUserId()
            require(cardId.isNotBlank()) { "Mã thẻ không hợp lệ." }
            require(folderId.isNotBlank()) { "Thư mục không hợp lệ." }
            val cleanHanzi = hanzi.trim().take(300)
            val cleanPinyin = pinyin.trim().take(500)
            val cleanMeaning = meaning.trim().take(500)
            require(cleanHanzi.isNotEmpty()) { "Nội dung tiếng Trung không được để trống." }
            require(cleanMeaning.isNotEmpty()) { "Nghĩa của thẻ không được để trống." }

            val validCategory = CardCategory.fromLabel(category).label
            val cleanExHanzi = exampleHanzi.trim().take(400)
            val cleanExMeaning = exampleMeaning.trim().take(500)

            val docRef = db.collection("users").document(uid)
                .collection("flashcards").document(cardId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "folderId" to folderId,
                    "hanzi" to cleanHanzi,
                    "pinyin" to cleanPinyin,
                    "meaning" to cleanMeaning,
                    "category" to validCategory,
                    "exampleHanzi" to cleanExHanzi,
                    "exampleMeaning" to cleanExMeaning,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordSwipeReview(cardId: String, remembered: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/flashcards/$cardId"
        return try {
            val uid = requireUserId()
            require(cardId.isNotBlank()) { "Mã thẻ không hợp lệ." }
            val docRef = db.collection("users").document(uid)
                .collection("flashcards").document(cardId)
            docPath = docRef.path
            val updates = if (remembered) {
                mapOf(
                    "lastResult" to ReviewResult.REMEMBERED.key,
                    "masteryCount" to FieldValue.increment(1L),
                    "lastReviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "lastResult" to ReviewResult.FORGOT.key,
                    "lapseCount" to FieldValue.increment(1L),
                    "lastReviewedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteFlashcard(cardId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/flashcards/$cardId"
        return try {
            val uid = requireUserId()
            require(cardId.isNotBlank()) { "Mã thẻ không hợp lệ." }
            val docRef = db.collection("users").document(uid)
                .collection("flashcards").document(cardId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun seedStarterDeck(): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/folders"
        return try {
            val uid = requireUserId()
            val userDoc = db.collection("users").document(uid)
            val folderRef = userDoc.collection("folders").document()
            docPath = folderRef.path

            folderRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to "Trà thất & Giao tiếp",
                    "description" to "Các từ và câu tao nhã thường dùng khi trò chuyện",
                    "accentTone" to FolderTone.SAGE.key,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val starterCards = listOf(
                Triple("久仰", "jiǔ yǎng", "Ngưỡng mộ đã lâu (lời chào trang trọng)"),
                Triple("请用茶", "qǐng yòng chá", "Mời dùng trà"),
                Triple("风和日丽", "fēng hé rì lì", "Gió hòa nắng đẹp, thời tiết dễ chịu"),
                Triple("请问怎么称呼？", "qǐng wèn zěn me chēng hu?", "Xin hỏi nên xưng hô thế nào?")
            )
            val categories = listOf(
                CardCategory.WORD.label,
                CardCategory.PHRASE.label,
                CardCategory.IDIOM.label,
                CardCategory.SENTENCE.label
            )
            val examples = listOf(
                "久仰大名，今日终于相见。" to "Ngưỡng mộ danh tiếng đã lâu, hôm nay cuối cùng cũng được gặp.",
                "天气微凉，请用茶暖暖身。" to "Trời hơi se lạnh, mời dùng trà cho ấm người.",
                "今天风和日丽，我们去园里散步吧。" to "Hôm nay gió hòa nắng đẹp, chúng ta ra vườn dạo nhé.",
                "初次见面，请问怎么称呼？" to "Lần đầu gặp mặt, xin hỏi quý danh?"
            )

            val batch = db.batch()
            starterCards.forEachIndexed { index, (hanzi, pinyin, meaning) ->
                val cardRef = userDoc.collection("flashcards").document()
                val (exHz, exMn) = examples[index]
                batch.set(
                    cardRef,
                    mapOf(
                        "userId" to uid,
                        "folderId" to folderRef.id,
                        "hanzi" to hanzi,
                        "pinyin" to pinyin,
                        "meaning" to meaning,
                        "category" to categories[index],
                        "exampleHanzi" to exHz,
                        "exampleMeaning" to exMn,
                        "lastResult" to ReviewResult.NEW.key,
                        "masteryCount" to 0L,
                        "lapseCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            docPath = "users/$uid/flashcards"
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
