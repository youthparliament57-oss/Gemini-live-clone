package com.example.geminilive.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

enum class GeminiVoice(val displayName: String, val description: String, val pitch: Float, val speechRate: Float) {
    CAPELLA("Capella", "Bright, warm & expressive", 1.12f, 1.02f),
    NOVA("Nova", "Calm, clear & natural", 1.0f, 1.0f),
    URSA("Ursa", "Deep, warm & thoughtful", 0.84f, 0.95f),
    VEGA("Vega", "Crisp, energetic & direct", 1.22f, 1.08f),
    ORION("Orion", "Smooth, authoritative & steady", 0.90f, 1.03f)
}

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val scope = CoroutineScope(Dispatchers.Main)

    // TTS
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var currentVoice: GeminiVoice = GeminiVoice.CAPELLA

    private var speechJob: Job? = null
    private var waveformJob: Job? = null

    // Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // State flows
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    // Callbacks
    var onSpeechPartialResult: ((String) -> Unit)? = null
    var onSpeechFinalResult: ((String) -> Unit)? = null
    var onSpeechError: ((Int) -> Unit)? = null
    var onGeminiSpeakStarted: (() -> Unit)? = null
    var onGeminiSpeakFinished: (() -> Unit)? = null

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                    applyVoiceProfile(currentVoice)
                }
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        startSpeakingWaveformSimulation()
                        onGeminiSpeakStarted?.invoke()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSpeakingWaveformSimulation()
                        _amplitude.value = 0f
                        onGeminiSpeakFinished?.invoke()
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopSpeakingWaveformSimulation()
                        _amplitude.value = 0f
                        onGeminiSpeakFinished?.invoke()
                    }
                })
            }
        }
    }

    fun setVoice(voice: GeminiVoice) {
        currentVoice = voice
        applyVoiceProfile(voice)
    }

    fun getVoice(): GeminiVoice = currentVoice

    private fun applyVoiceProfile(voice: GeminiVoice) {
        textToSpeech?.apply {
            setPitch(voice.pitch)
            setSpeechRate(voice.speechRate)
        }
    }

    fun speak(text: String) {
        stopListening()
        if (!isTtsReady || textToSpeech == null) {
            // If TTS engine is initializing or unavailable, simulate speaking lifecycle
            scope.launch {
                _isSpeaking.value = true
                startSpeakingWaveformSimulation()
                onGeminiSpeakStarted?.invoke()
                val readingDurationMs = (text.length * 55L).coerceIn(1200L, 8000L)
                delay(readingDurationMs)
                _isSpeaking.value = false
                stopSpeakingWaveformSimulation()
                _amplitude.value = 0f
                onGeminiSpeakFinished?.invoke()
            }
            return
        }

        val cleaned = text
            .replace(Regex("\\*\\*|\\*|_|`"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .trim()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "gemini_utterance_${System.currentTimeMillis()}")
        }
        textToSpeech?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, "gemini_utterance_${System.currentTimeMillis()}")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        stopSpeakingWaveformSimulation()
        _isSpeaking.value = false
        _amplitude.value = 0f
    }

    private fun startSpeakingWaveformSimulation() {
        waveformJob?.cancel()
        waveformJob = scope.launch {
            var step = 0f
            while (isActive && _isSpeaking.value) {
                step += 0.2f
                // Organic speech modulation
                val base = 0.35f + 0.35f * sin(step.toDouble()).toFloat()
                val jitter = Random.nextFloat() * 0.25f
                _amplitude.value = (base + jitter).coerceIn(0.1f, 1.0f)
                delay(40L)
            }
            _amplitude.value = 0f
        }
    }

    private fun stopSpeakingWaveformSimulation() {
        waveformJob?.cancel()
        waveformJob = null
    }

    fun startListening() {
        stopSpeaking()
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError?.invoke(-1)
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            _isRecognizing.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            isListening = false
            _isRecognizing.value = false
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isListening = false
            _isRecognizing.value = false
            _amplitude.value = 0f
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isListening = false
            _isRecognizing.value = false
            _amplitude.value = 0f
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                _isRecognizing.value = true
            }

            override fun onBeginningOfSpeech() {
                // User started speaking
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (isListening) {
                    // Normalize typical rmsdB from roughly -2dB to 10dB into 0..1
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                    _amplitude.value = normalized
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isRecognizing.value = false
                _amplitude.value = 0f
            }

            override fun onError(error: Int) {
                isListening = false
                _isRecognizing.value = false
                _amplitude.value = 0f
                onSpeechError?.invoke(error)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                _isRecognizing.value = false
                _amplitude.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim()
                if (!text.isNullOrEmpty()) {
                    onSpeechFinalResult?.invoke(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim()
                if (!text.isNullOrEmpty()) {
                    onSpeechPartialResult?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun cleanup() {
        stopSpeaking()
        cancelListening()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
