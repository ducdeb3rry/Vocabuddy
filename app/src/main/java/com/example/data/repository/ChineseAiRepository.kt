package com.example.data.repository

import android.util.Log
import com.example.data.model.CardCategory
import com.example.data.model.GeneratedDefinition
import com.example.data.preferences.StudyPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ChineseAiRepository {

    suspend fun verifyApiKey(apiKey: String, modelId: String): Result<Unit> {
        val cleanKey = apiKey.trim()
        val cleanModel = modelId.trim().ifBlank { StudyPreferences.DEFAULT_GEMINI_MODEL }
        if (cleanKey.isEmpty()) {
            return Result.failure(IllegalArgumentException("Vui lòng nhập Gemini API Key của bạn."))
        }
        return try {
            withTimeout(30_000L) {
                withContext(Dispatchers.IO) {
                    val body = JSONObject().apply {
                        put(
                            "contents",
                            JSONArray().put(
                                JSONObject().put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", "Reply with OK"))
                                )
                            )
                        )
                    }
                    postGeminiGenerateContent(cleanKey, cleanModel, body)
                }
            }
            Result.success(Unit)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini API key verification timed out", e)
            Result.failure(Exception("Kiểm tra kết nối quá thời gian. Vui lòng thử lại."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API key verification failed", e)
            Result.failure(Exception(e.message ?: "Không thể kết nối với Gemini API Key này."))
        }
    }

    suspend fun defineChineseInput(
        hanziInput: String,
        apiKey: String,
        modelId: String = StudyPreferences.DEFAULT_GEMINI_MODEL
    ): Result<GeneratedDefinition> {
        val cleanInput = hanziInput.trim()
        val cleanKey = apiKey.trim()
        val cleanModel = modelId.trim().ifBlank { StudyPreferences.DEFAULT_GEMINI_MODEL }

        if (cleanInput.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Vui lòng nhập từ, cụm từ hoặc câu tiếng Trung.")
            )
        }
        if (cleanKey.isEmpty()) {
            return Result.failure(
                IllegalStateException("Bạn chưa kết nối Gemini API Key cá nhân.")
            )
        }

        return try {
            val prompt = """
                Bạn là một học giả ngôn ngữ Trung - Việt tinh tế và chuẩn xác.
                Người dùng nhập đoạn tiếng Trung sau: "$cleanInput"
                (Có thể là một chữ đơn, từ vựng, cụm từ, thành ngữ, hoặc cả câu tiếng Trung giản thể/phồn thể.)

                Hãy trả về JSON chuẩn xác với:
                - pinyin: phiên âm Pinyin có dấu thanh điệu đầy đủ, tách từ tự nhiên.
                - meaning: nghĩa tiếng Việt ngắn gọn, rõ ràng, dễ nhớ (nếu có sắc thái dùng từ hoặc từ loại đặc biệt, ghi chú ngắn trong ngoặc).
                - category: chọn đúng 1 trong 4 giá trị: "Từ vựng", "Cụm từ", "Câu", "Thành ngữ".
                - exampleHanzi: 1 câu ví dụ tiếng Trung ngắn gọn, tự nhiên có chứa nội dung trên (nếu đầu vào đã là 1 câu hoàn chỉnh, hãy đưa 1 câu hồi đáp hoặc câu tương tự ngắn gọn).
                - exampleMeaning: bản dịch tiếng Việt tự nhiên của exampleHanzi.
            """.trimIndent()

            val responseJson = withTimeout(60_000L) {
                withContext(Dispatchers.IO) {
                    val schema = JSONObject().apply {
                        put("type", "OBJECT")
                        put(
                            "properties",
                            JSONObject().apply {
                                put("pinyin", JSONObject().put("type", "STRING"))
                                put("meaning", JSONObject().put("type", "STRING"))
                                put(
                                    "category",
                                    JSONObject().apply {
                                        put("type", "STRING")
                                        put(
                                            "enum",
                                            JSONArray(
                                                listOf("Từ vựng", "Cụm từ", "Câu", "Thành ngữ")
                                            )
                                        )
                                    }
                                )
                                put("exampleHanzi", JSONObject().put("type", "STRING"))
                                put("exampleMeaning", JSONObject().put("type", "STRING"))
                            }
                        )
                        put(
                            "required",
                            JSONArray(
                                listOf(
                                    "pinyin",
                                    "meaning",
                                    "category",
                                    "exampleHanzi",
                                    "exampleMeaning"
                                )
                            )
                        )
                    }

                    val requestBody = JSONObject().apply {
                        put(
                            "contents",
                            JSONArray().put(
                                JSONObject().put(
                                    "parts",
                                    JSONArray().put(JSONObject().put("text", prompt))
                                )
                            )
                        )
                        put(
                            "generationConfig",
                            JSONObject().apply {
                                put("responseMimeType", "application/json")
                                put("responseSchema", schema)
                            }
                        )
                    }

                    val rawResponse = postGeminiGenerateContent(cleanKey, cleanModel, requestBody)
                    extractTextFromGeminiResponse(rawResponse)
                }
            }

            val json = JSONObject(responseJson)
            val pinyin = json.optString("pinyin", "").trim()
            val meaning = json.optString("meaning", "").trim()
            val rawCategory = json.optString("category", CardCategory.WORD.label).trim()
            val category = CardCategory.fromLabel(rawCategory).label
            val exampleHanzi = json.optString("exampleHanzi", "").trim()
            val exampleMeaning = json.optString("exampleMeaning", "").trim()

            if (meaning.isBlank()) {
                return Result.failure(IllegalStateException("Không thể phân tích nghĩa từ nội dung này."))
            }

            Result.success(
                GeneratedDefinition(
                    hanzi = cleanInput,
                    pinyin = pinyin,
                    meaning = meaning,
                    category = category,
                    exampleHanzi = exampleHanzi,
                    exampleMeaning = exampleMeaning
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI definition timed out", e)
            Result.failure(Exception("Tra nghĩa quá thời gian (60 giây). Vui lòng thử lại."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate Chinese definition via user's Gemini API", e)
            Result.failure(
                Exception(e.message ?: "Không thể kết nối Gemini API để định nghĩa tự động.")
            )
        }
    }

    private fun postGeminiGenerateContent(
        apiKey: String,
        modelId: String,
        payload: JSONObject
    ): JSONObject {
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val endpoint =
            "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent?key=$encodedKey"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        return try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }

            if (status !in 200..299) {
                val apiErrorMsg = try {
                    JSONObject(responseText).optJSONObject("error")?.optString("message")
                } catch (_: Exception) {
                    null
                }
                throw IllegalStateException(
                    apiErrorMsg?.takeIf { it.isNotBlank() }
                        ?: "Gemini API trả về lỗi HTTP $status."
                )
            }

            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractTextFromGeminiResponse(response: JSONObject): String {
        val candidates = response.optJSONArray("candidates")
            ?: throw IllegalStateException("Gemini không trả về kết quả.")
        if (candidates.length() == 0) {
            throw IllegalStateException("Gemini không trả về nội dung.")
        }
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw IllegalStateException("Phản hồi Gemini bị trống.")
        val textBuilder = StringBuilder()
        for (i in 0 until parts.length()) {
            val partText = parts.getJSONObject(i).optString("text", "")
            textBuilder.append(partText)
        }
        val text = textBuilder.toString().trim()
        if (text.isBlank()) {
            throw IllegalStateException("Gemini trả về văn bản rỗng.")
        }
        return text
    }

    private companion object {
        const val TAG = "ChineseAiRepo"
    }
}
