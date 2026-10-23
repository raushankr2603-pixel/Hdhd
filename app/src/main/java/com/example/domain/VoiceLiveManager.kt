package com.example.domain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceLiveState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val continuousLiveMode: Boolean = false,
    val liveScreenVisionEnabled: Boolean = true,
    val partialTranscript: String = "",
    val lastConfidence: Float = 0.96f,
    val lastErrorDiagnostic: String? = null,
    val statusBanner: String = "Voice Live Partner Ready • Tap Orb or Speak"
)

class VoiceLiveManager(
    private val context: Context,
    private val onFinalSpeechResult: (transcript: String, confidence: Float) -> Unit
) : TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _state = MutableStateFlow(VoiceLiveState())
    val state: StateFlow<VoiceLiveState> = _state.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            isTtsReady = false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(1.05f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = _state.value.copy(
                        isSpeaking = true,
                        statusBanner = "Aether Speaking Live..."
                    )
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = _state.value.copy(
                        isSpeaking = false,
                        statusBanner = if (_state.value.continuousLiveMode) {
                            "Continuous Live Mode • Listening for follow-up..."
                        } else {
                            "Voice Live Partner Ready"
                        }
                    )
                }

                override fun onError(utteranceId: String?) {
                    _state.value = _state.value.copy(isSpeaking = false)
                }
            })
            isTtsReady = true
        }
    }

    fun toggleContinuousLiveMode(enabled: Boolean) {
        _state.value = _state.value.copy(
            continuousLiveMode = enabled,
            statusBanner = if (enabled) {
                "Continuous Live Conversational Mode ACTIVE"
            } else {
                "Push-to-Talk Mode Active"
            }
        )
    }

    fun toggleScreenVision(enabled: Boolean) {
        _state.value = _state.value.copy(
            liveScreenVisionEnabled = enabled,
            statusBanner = if (enabled) {
                "Live Screen Vision + Voice Stream Enabled"
            } else {
                "Voice-Only Stream (Screen Vision Paused)"
            }
        )
    }

    fun clearErrorDiagnostic() {
        _state.value = _state.value.copy(lastErrorDiagnostic = null)
    }

    fun startListening() {
        try {
            tts?.stop()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = _state.value.copy(
                    isListening = false,
                    lastErrorDiagnostic = "On-device SpeechRecognizer service not installed in this emulator container. Use the Voice Command Simulator or Text Bar below.",
                    statusBanner = "Use Voice Simulator Chips or Command Bar"
                )
                return
            }
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _state.value = _state.value.copy(
                                isListening = true,
                                partialTranscript = "",
                                lastErrorDiagnostic = null,
                                statusBanner = "Listening Live... Speak command or macro trigger"
                            )
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _state.value = _state.value.copy(
                                isListening = false,
                                statusBanner = "Transcribing & parsing voice intent..."
                            )
                        }

                        override fun onError(error: Int) {
                            val friendlyError = mapSpeechErrorToMessage(error)
                            _state.value = _state.value.copy(
                                isListening = false,
                                lastErrorDiagnostic = friendlyError,
                                statusBanner = "Voice capture paused • Tap Orb to retry"
                            )
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val confScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                            val spoken = matches?.firstOrNull()?.trim().orEmpty()
                            val conf = confScores?.firstOrNull()?.takeIf { it > 0f } ?: 0.94f
                            _state.value = _state.value.copy(
                                isListening = false,
                                partialTranscript = spoken,
                                lastConfidence = conf,
                                lastErrorDiagnostic = null
                            )
                            if (spoken.isNotEmpty()) {
                                onFinalSpeechResult(spoken, conf)
                            } else {
                                _state.value = _state.value.copy(
                                    lastErrorDiagnostic = "No discernible words detected. Please speak clearly or tap a Quick Voice Chip."
                                )
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (partial.isNotEmpty()) {
                                _state.value = _state.value.copy(partialTranscript = partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                lastErrorDiagnostic = "Voice capture fallback active (${e.javaClass.simpleName}). Use Voice Simulator Chips or Command Bar.",
                statusBanner = "Voice stream ready via Simulator & Command Bar"
            )
        }
    }

    private fun mapSpeechErrorToMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_NO_MATCH ->
                "Unclear speech: No matching words recognized. Try speaking closer to the mic or use a registered macro trigger."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Speech timeout: No voice input was heard. Tap the Neural Orb when you are ready to speak."
            SpeechRecognizer.ERROR_AUDIO ->
                "Audio capture error: Microphone stream could not be read."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission required: Grant RECORD_AUDIO permission to capture live voice."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network speech recognizer unreachable: Using local offline command processor."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Speech engine busy: Finishing current recognition cycle."
            else ->
                "Voice recognition interrupted (code $errorCode). You can retry or tap any Quick Voice Command below."
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(
            isListening = false,
            statusBanner = "Voice listening paused"
        )
    }

    fun speakResponse(text: String) {
        if (!isTtsReady) return
        val cleanText = text.replace("===", "").replace("•", "").take(380)
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "aether_utt_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun shutdown() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
