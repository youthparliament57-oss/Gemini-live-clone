package com.example.geminilive.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.geminilive.audio.GeminiVoice
import com.example.geminilive.audio.VoiceManager
import com.example.geminilive.camera.CameraController
import com.example.geminilive.camera.toBase64
import com.example.geminilive.data.api.GeminiApiClient
import com.example.geminilive.data.db.LiveDatabase
import com.example.geminilive.data.model.ContentItem
import com.example.geminilive.data.model.GenerateContentRequest
import com.example.geminilive.data.model.GenerationConfig
import com.example.geminilive.data.model.InlineData
import com.example.geminilive.data.model.LiveMessageEntity
import com.example.geminilive.data.model.LiveSessionEntity
import com.example.geminilive.data.model.PartItem
import com.example.geminilive.ui.components.LiveOrbState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeminiLiveViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LiveDatabase.getInstance(application)
    private val dao = db.liveDao()

    val voiceManager = VoiceManager(application)
    val cameraController = CameraController(application)

    val allSessions: StateFlow<List<LiveSessionEntity>> = dao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _orbState = MutableStateFlow(LiveOrbState.IDLE)
    val orbState: StateFlow<LiveOrbState> = _orbState.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _liveCaption = MutableStateFlow("Gemini Live is ready. Tap to talk or ask anything.")
    val liveCaption: StateFlow<String> = _liveCaption.asStateFlow()

    private val _messages = MutableStateFlow<List<LiveMessageEntity>>(emptyList())
    val messages: StateFlow<List<LiveMessageEntity>> = _messages.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _continuousMode = MutableStateFlow(true)
    val continuousMode: StateFlow<Boolean> = _continuousMode.asStateFlow()

    private val _currentVoice = MutableStateFlow(GeminiVoice.CAPELLA)
    val currentVoice: StateFlow<GeminiVoice> = _currentVoice.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // API Key Management (SharedPreferences + BuildConfig fallback)
    private val sharedPrefs = application.getSharedPreferences("gemini_live_prefs", Context.MODE_PRIVATE)
    private val _customApiKey = MutableStateFlow(sharedPrefs.getString("custom_gemini_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value.trim()
        if (custom.isNotBlank()) return custom
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")) key else ""
        } catch (e: Throwable) {
            ""
        }
    }

    private val _hasActiveApiKey = MutableStateFlow(getEffectiveApiKey().isNotBlank())
    val hasActiveApiKey: StateFlow<Boolean> = _hasActiveApiKey.asStateFlow()

    fun saveApiKey(key: String) {
        val clean = key.trim()
        sharedPrefs.edit().putString("custom_gemini_api_key", clean).apply()
        _customApiKey.value = clean
        _hasActiveApiKey.value = getEffectiveApiKey().isNotBlank()
        if (clean.isNotBlank()) {
            _liveCaption.value = "Gemini API Key saved. Ready for real-time live AI."
        }
    }

    fun clearApiKey() {
        sharedPrefs.edit().remove("custom_gemini_api_key").apply()
        _customApiKey.value = ""
        _hasActiveApiKey.value = getEffectiveApiKey().isNotBlank()
        _liveCaption.value = "API key removed. Running in local demo mode."
    }

    suspend fun testApiKey(testKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val clean = testKey.trim()
        if (clean.isBlank()) {
            return@withContext Pair(false, "API Key is empty")
        }
        try {
            val testRequest = GenerateContentRequest(
                contents = listOf(
                    ContentItem(
                        role = "user",
                        parts = listOf(PartItem(text = "Hello! Please reply with exactly one word: OK"))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.1f,
                    maxOutputTokens = 10
                )
            )
            val response = GeminiApiClient.apiService.generateContent(
                model = "gemini-3.5-flash",
                apiKey = clean,
                request = testRequest
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                Pair(true, "API Key verified! Connected to Gemini 3.5 Flash successfully.")
            } else {
                Pair(false, "Received empty response from Gemini API.")
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Network or authorization error"
            Pair(false, "Verification failed: $msg")
        }
    }

    private var currentSessionId: Long = 0
    private var conversationHistory = mutableListOf<ContentItem>()
    private var autoListenJob: Job? = null

    init {
        // Collect voice manager amplitude
        viewModelScope.launch {
            voiceManager.amplitude.collect { amp ->
                _amplitude.value = amp
            }
        }

        // Voice manager callbacks
        voiceManager.onSpeechPartialResult = { partial ->
            _liveCaption.value = partial
        }

        voiceManager.onSpeechFinalResult = { finalSpeech ->
            onUserSpoke(finalSpeech)
        }

        voiceManager.onSpeechError = { errorCode ->
            if (_orbState.value == LiveOrbState.LISTENING) {
                _orbState.value = LiveOrbState.IDLE
                _liveCaption.value = "Tap the mic button to speak with Gemini."
            }
        }

        voiceManager.onGeminiSpeakStarted = {
            _orbState.value = LiveOrbState.SPEAKING
        }

        voiceManager.onGeminiSpeakFinished = {
            _orbState.value = LiveOrbState.IDLE
            if (_continuousMode.value && !_isMuted.value) {
                // In continuous live mode, auto-listen for user's next turn after brief pause
                autoListenJob?.cancel()
                autoListenJob = viewModelScope.launch {
                    delay(500)
                    startListening()
                }
            }
        }

        // Initialize new live conversation session
        createNewSession()
    }

    private fun createNewSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val session = LiveSessionEntity(title = "Live Conversation")
            currentSessionId = dao.insertSession(session)
            conversationHistory.clear()
            _messages.value = emptyList()
        }
    }

    fun startListening() {
        if (_isMuted.value) return
        autoListenJob?.cancel()
        voiceManager.stopSpeaking()
        _orbState.value = LiveOrbState.LISTENING
        _liveCaption.value = "Listening..."
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        if (_orbState.value == LiveOrbState.LISTENING) {
            _orbState.value = LiveOrbState.IDLE
            _liveCaption.value = "Gemini Live is ready."
        }
    }

    fun toggleListening() {
        if (_orbState.value == LiveOrbState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun interruptSpeaking() {
        autoListenJob?.cancel()
        voiceManager.stopSpeaking()
        _orbState.value = LiveOrbState.IDLE
        _liveCaption.value = "Interrupted. Tap mic to speak."
    }

    fun toggleCamera() {
        _isCameraActive.value = !_isCameraActive.value
        if (_isCameraActive.value) {
            _liveCaption.value = "Vision live: Point camera and ask anything."
        }
    }

    fun toggleContinuousMode(enabled: Boolean) {
        _continuousMode.value = enabled
    }

    fun setVoice(voice: GeminiVoice) {
        _currentVoice.value = voice
        voiceManager.setVoice(voice)
    }

    fun testVoice(voice: GeminiVoice) {
        voiceManager.setVoice(voice)
        voiceManager.speak("Hi! I'm ${voice.displayName}, your voice in Gemini Live. How can I help you today?")
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            stopListening()
            voiceManager.stopSpeaking()
            _liveCaption.value = "Microphone muted"
        } else {
            _liveCaption.value = "Microphone unmuted. Ready to talk."
        }
    }

    fun sendTextQuery(query: String) {
        onUserSpoke(query)
    }

    private fun onUserSpoke(userText: String) {
        if (userText.isBlank()) return
        autoListenJob?.cancel()
        voiceManager.stopListening()

        _liveCaption.value = userText
        _orbState.value = LiveOrbState.THINKING

        viewModelScope.launch {
            // Check if vision is enabled and capture frame
            var capturedBitmap: Bitmap? = null
            if (_isCameraActive.value) {
                try {
                    withContext(Dispatchers.Main) {
                        cameraController.captureFrame(
                            onBitmapCaptured = { bitmap -> capturedBitmap = bitmap },
                            onError = { }
                        )
                    }
                    delay(250) // Wait for capture
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Save user message to Room DB
            val userMsg = LiveMessageEntity(
                sessionId = currentSessionId,
                role = "user",
                text = userText,
                hasImage = capturedBitmap != null
            )
            val msgId = withContext(Dispatchers.IO) {
                dao.insertMessage(userMsg)
            }
            _messages.value = _messages.value + userMsg.copy(id = msgId)

            // Update session title if first message
            if (_messages.value.size == 1) {
                withContext(Dispatchers.IO) {
                    val previewTitle = if (userText.length > 32) userText.take(32) + "..." else userText
                    dao.updateSession(LiveSessionEntity(id = currentSessionId, title = previewTitle, messageCount = 1))
                }
            } else {
                withContext(Dispatchers.IO) {
                    val s = dao.getSessionById(currentSessionId)
                    if (s != null) {
                        dao.updateSession(s.copy(messageCount = _messages.value.size))
                    }
                }
            }

            // Execute Gemini AI Call
            executeGeminiTurn(userText, capturedBitmap)
        }
    }

    private suspend fun executeGeminiTurn(userPrompt: String, bitmap: Bitmap?) {
        val apiKey = getEffectiveApiKey()
        val hasValidKey = apiKey.isNotBlank() && !apiKey.contains("MY_GEMINI_API_KEY")

        if (hasValidKey) {
            try {
                // Build multi-turn content with optional image
                val parts = mutableListOf<PartItem>()
                parts.add(PartItem(text = userPrompt))
                if (bitmap != null) {
                    val base64 = bitmap.toBase64()
                    parts.add(PartItem(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
                }

                val userContent = ContentItem(role = "user", parts = parts)
                conversationHistory.add(userContent)

                // Limit conversation history to latest 12 turns for low latency
                val recentHistory = conversationHistory.takeLast(12)

                val request = GenerateContentRequest(
                    contents = recentHistory,
                    systemInstruction = ContentItem(
                        parts = listOf(
                            PartItem(
                                text = "You are Gemini Live, a warm, intelligent, real-time voice assistant built by Google. " +
                                        "Speak naturally, engagingly, and concisely—in 1 to 3 spoken sentences. " +
                                        "Never use markdown formatting, bullets, asterisks, or code blocks because your response is read aloud. " +
                                        "If the user shared an image with camera vision, describe and answer questions about what you see accurately."
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f,
                        maxOutputTokens = 300
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiApiClient.apiService.generateContent(
                        model = "gemini-3.5-flash",
                        apiKey = apiKey,
                        request = request
                    )
                }

                val geminiReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?.trim() ?: "I heard you, but couldn't generate a response. Could you rephrase that?"

                conversationHistory.add(
                    ContentItem(role = "model", parts = listOf(PartItem(text = geminiReply)))
                )

                handleGeminiResponse(geminiReply)
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackAnswer = "I ran into a network hiccup: ${e.localizedMessage ?: "Unknown error"}. Please check your connection or API key."
                handleGeminiResponse(fallbackAnswer)
            }
        } else {
            // Intelligent interactive fallback response
            delay(700)
            val fallbackReply = generateOfflineDemoReply(userPrompt, bitmap != null)
            conversationHistory.add(ContentItem(role = "user", parts = listOf(PartItem(text = userPrompt))))
            conversationHistory.add(ContentItem(role = "model", parts = listOf(PartItem(text = fallbackReply))))
            handleGeminiResponse(fallbackReply)
        }
    }

    private suspend fun handleGeminiResponse(replyText: String) {
        // Save to DB
        val geminiMsg = LiveMessageEntity(
            sessionId = currentSessionId,
            role = "gemini",
            text = replyText
        )
        val msgId = withContext(Dispatchers.IO) {
            dao.insertMessage(geminiMsg)
        }
        _messages.value = _messages.value + geminiMsg.copy(id = msgId)
        withContext(Dispatchers.IO) {
            val s = dao.getSessionById(currentSessionId)
            if (s != null) {
                dao.updateSession(s.copy(messageCount = _messages.value.size))
            }
        }

        _liveCaption.value = replyText

        // Speak aloud via TTS
        withContext(Dispatchers.Main) {
            voiceManager.speak(replyText)
        }
    }

    private fun generateOfflineDemoReply(query: String, hasImage: Boolean): String {
        val q = query.lowercase()
        return when {
            hasImage ->
                "I see what you're pointing at! To unlock live vision analysis with Gemini 3.5 Flash, make sure to add your Gemini API Key in the AI Studio Secrets panel."
            q.contains("hello") || q.contains("hi") || q.contains("hey") ->
                "Hello! I am Gemini Live, your real-time conversational assistant. You can speak to me naturally, explore ideas, or turn on the camera for visual answers."
            q.contains("who are you") || q.contains("what is this") ->
                "I am Gemini Live, Google's real-time multimodal voice assistant. You can chat with me out loud, change voices, and share your camera."
            q.contains("weather") ->
                "I can discuss the weather, draft emails, roleplay, or brainstorm! For real-time live data, configure your Gemini API Key in the AI Studio Secrets menu."
            q.contains("voice") || q.contains("sound") ->
                "You can change my voice anytime using the sound icon at the top! I feature Capella, Nova, Ursa, Vega, and Orion voice profiles."
            else ->
                "That's a fascinating topic! I am listening and ready to talk. For full neural intelligence, ensure your Gemini API Key is set in AI Studio Secrets."
        }
    }

    fun loadPastSession(session: LiveSessionEntity) {
        viewModelScope.launch {
            currentSessionId = session.id
            val pastMessages = withContext(Dispatchers.IO) {
                dao.getMessagesListForSession(session.id)
            }
            _messages.value = pastMessages
            conversationHistory.clear()
            pastMessages.forEach { msg ->
                conversationHistory.add(
                    ContentItem(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(PartItem(text = msg.text))
                    )
                )
            }
            _liveCaption.value = "Loaded conversation: ${session.title}"
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteSession(sessionId)
            if (currentSessionId == sessionId) {
                createNewSession()
            }
        }
    }

    fun startNewSession() {
        voiceManager.stopSpeaking()
        stopListening()
        createNewSession()
        _liveCaption.value = "Started new Gemini Live session."
        _orbState.value = LiveOrbState.IDLE
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.cleanup()
    }
}
