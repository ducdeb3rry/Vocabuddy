package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyDataStore by preferencesDataStore(name = "vocabuddy_prefs")

class StudyPreferences(private val context: Context) {
    private val showPinyinKey = booleanPreferencesKey("show_pinyin")
    private val userGeminiApiKey = stringPreferencesKey("user_gemini_api_key")
    private val userGeminiModelKey = stringPreferencesKey("user_gemini_model")

    val showPinyin: Flow<Boolean> = context.studyDataStore.data.map { prefs ->
        prefs[showPinyinKey] ?: true
    }

    val geminiApiKey: Flow<String> = context.studyDataStore.data.map { prefs ->
        prefs[userGeminiApiKey] ?: ""
    }

    val geminiModel: Flow<String> = context.studyDataStore.data.map { prefs ->
        prefs[userGeminiModelKey]?.takeIf { it.isNotBlank() } ?: DEFAULT_GEMINI_MODEL
    }

    suspend fun setShowPinyin(show: Boolean) {
        context.studyDataStore.edit { prefs ->
            prefs[showPinyinKey] = show
        }
    }

    suspend fun setGeminiConfig(apiKey: String, model: String) {
        context.studyDataStore.edit { prefs ->
            prefs[userGeminiApiKey] = apiKey.trim()
            prefs[userGeminiModelKey] = model.trim().ifBlank { DEFAULT_GEMINI_MODEL }
        }
    }

    suspend fun clearGeminiApiKey() {
        context.studyDataStore.edit { prefs ->
            prefs.remove(userGeminiApiKey)
        }
    }

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-3.8-flash"
        val SUPPORTED_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-flash-latest",
            "gemini-3.5-flash-lite"
        )
    }
}
