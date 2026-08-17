package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.RouteStep
import java.util.Locale

class VoiceNavigationManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isMuted = false
    private var lastSpokenInstruction: String = ""
    private var lastSpokenDistanceBand: Int = -1

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set Spanish language (Cuba / Universal Spanish)
                val spanishLocale = Locale("es", "ES")
                val result = tts?.setLanguage(spanishLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default Spanish or available locale
                    tts?.setLanguage(Locale("es"))
                }
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.05f) // Natural crisp pace for GPS voice
                isInitialized = true
                Log.d("VoiceNav", "TTS Initialized successfully for Spanish navigation")
            } else {
                Log.e("VoiceNav", "TTS Initialization failed")
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
    }

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        if (muted) {
            stop()
        }
    }

    fun isMuted(): Boolean = isMuted

    fun speak(text: String, flush: Boolean = true) {
        if (isMuted || !isInitialized || text.isBlank()) return
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "TTS_NAV_${System.currentTimeMillis()}")
    }

    /**
     * Context-aware turn announcement based on proximity bands (500m, 200m, 50m, now)
     */
    fun announceStep(step: RouteStep, distanceToStepMeters: Double) {
        if (isMuted || !isInitialized) return

        val band = when {
            distanceToStepMeters > 400.0 && distanceToStepMeters <= 600.0 -> 500
            distanceToStepMeters > 150.0 && distanceToStepMeters <= 250.0 -> 200
            distanceToStepMeters in 20.0..60.0 -> 50
            distanceToStepMeters < 20.0 -> 0
            else -> -1
        }

        // Only announce if crossing a distance threshold or instruction changed
        if (band != -1 && (band != lastSpokenDistanceBand || step.instruction != lastSpokenInstruction)) {
            lastSpokenDistanceBand = band
            lastSpokenInstruction = step.instruction

            val phrase = when (band) {
                500 -> "En 500 metros, ${step.instruction}"
                200 -> "En 200 metros, ${step.instruction}"
                50 -> "En 50 metros, ${step.instruction}"
                0 -> "Ahora, ${step.instruction}"
                else -> step.instruction
            }
            speak(phrase, flush = true)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
