package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class ChineseSpeaker(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val result = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val fallback = engine.setLanguage(Locale.CHINESE)
                    isReady = fallback != TextToSpeech.LANG_MISSING_DATA &&
                        fallback != TextToSpeech.LANG_NOT_SUPPORTED
                    if (!isReady) {
                        Log.w(TAG, "Chinese TTS language data unavailable on device.")
                    }
                } else {
                    isReady = true
                }
                engine.setSpeechRate(0.9f)
            } else {
                Log.w(TAG, "TextToSpeech init failed with status $status")
            }
        }
    }

    fun speak(text: String): Boolean {
        if (!isReady || text.isBlank()) return false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hanzi_${System.currentTimeMillis()}")
        return true
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private companion object {
        const val TAG = "ChineseSpeaker"
    }
}
