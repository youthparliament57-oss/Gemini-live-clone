package com.example.geminilive.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VpnKey
import com.example.geminilive.ui.components.ApiKeyDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.geminilive.camera.CameraLiveView
import com.example.geminilive.ui.components.GeminiLiveOrb
import com.example.geminilive.ui.components.LiveOrbState
import com.example.geminilive.ui.components.LiveTranscriptSheet
import com.example.geminilive.ui.components.SessionHistoryDialog
import com.example.geminilive.ui.components.VoiceSettingsDialog

@Composable
fun GeminiLiveScreen(viewModel: GeminiLiveViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val orbState by viewModel.orbState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val liveCaption by viewModel.liveCaption.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val continuousMode by viewModel.continuousMode.collectAsState()
    val currentVoice by viewModel.currentVoice.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val hasActiveApiKey by viewModel.hasActiveApiKey.collectAsState()

    var showTranscriptSheet by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showTextInputBar by remember { mutableStateOf(false) }
    var textInputQuery by remember { mutableStateOf("") }

    // Audio record permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for Gemini Live voice", Toast.LENGTH_LONG).show()
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleCamera()
        } else {
            Toast.makeText(context, "Camera permission is required for Live Vision", Toast.LENGTH_LONG).show()
        }
    }

    fun handleMicClick() {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPerm) {
            viewModel.toggleListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleCameraClick() {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPerm) {
            viewModel.toggleCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Deep cosmic background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF080B13),
            Color(0xFF0D1220),
            Color(0xFF080B13)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------------- TOP BAR ----------------
            TopBar(
                orbState = orbState,
                messageCount = messages.size,
                currentVoiceName = currentVoice.displayName,
                hasActiveApiKey = hasActiveApiKey,
                onOpenApiKey = { showApiKeyDialog = true },
                onOpenVoice = { showVoiceDialog = true },
                onOpenHistory = { showHistoryDialog = true },
                onNewSession = { viewModel.startNewSession() },
                onOpenTranscript = { showTranscriptSheet = true }
            )

            if (!hasActiveApiKey) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x264285F4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D4285F4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showApiKeyDialog = true }
                        .testTag("api_key_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tap to enter your Gemini API Key for Live AI",
                            color = Color(0xFFC4D5F6),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Set Key",
                            color = Color(0xFF8AB4F8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ---------------- CAMERA VISION VIEW (Optional) ----------------
            AnimatedVisibility(
                visible = isCameraActive,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    CameraLiveView(
                        controller = viewModel.cameraController,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay camera switcher button
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0x99000000),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    // toggle camera
                                    viewModel.toggleCamera()
                                    viewModel.toggleCamera()
                                },
                                modifier = Modifier.testTag("switch_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Live Vision Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xCC1A73E8),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Gemini Live Vision",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ---------------- CENTRAL ORB CANVAS ----------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                GeminiLiveOrb(
                    state = orbState,
                    amplitude = amplitude,
                    modifier = Modifier.size(if (isCameraActive) 240.dp else 310.dp)
                )
            }

            // ---------------- LIVE CAPTION / SUBTITLE ----------------
            CaptionCard(
                orbState = orbState,
                caption = liveCaption,
                onClick = { showTranscriptSheet = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ---------------- QUICK PROMPT SUGGESTIONS ----------------
            PromptSuggestionChips(
                isCameraActive = isCameraActive,
                onPromptClick = { prompt ->
                    viewModel.sendTextQuery(prompt)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---------------- OPTIONAL DIRECT TEXT INPUT BAR ----------------
            AnimatedVisibility(visible = showTextInputBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInputQuery,
                        onValueChange = { textInputQuery = it },
                        placeholder = { Text("Ask Gemini Live...", color = Color(0xFF9AA0A6), fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_query_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF161C2C),
                            unfocusedContainerColor = Color(0xFF161C2C),
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFF28324C)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInputQuery.isNotBlank()) {
                                viewModel.sendTextQuery(textInputQuery)
                                textInputQuery = ""
                                keyboardController?.hide()
                                showTextInputBar = false
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInputQuery.isNotBlank()) {
                                viewModel.sendTextQuery(textInputQuery)
                                textInputQuery = ""
                                keyboardController?.hide()
                                showTextInputBar = false
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4))
                            .testTag("send_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ---------------- BOTTOM ACTIONS (Iconic Gemini Live Controls) ----------------
            BottomControls(
                orbState = orbState,
                isCameraActive = isCameraActive,
                isMuted = isMuted,
                showTextInputBar = showTextInputBar,
                onMicClick = { handleMicClick() },
                onCameraClick = { handleCameraClick() },
                onInterruptClick = { viewModel.interruptSpeaking() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleTextInput = { showTextInputBar = !showTextInputBar }
            )

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Dialogs & Sheets
        if (showTranscriptSheet) {
            LiveTranscriptSheet(
                messages = messages,
                onDismiss = { showTranscriptSheet = false },
                onSpeakMessage = { text -> viewModel.speakMessage(text) }
            )
        }

        if (showVoiceDialog) {
            VoiceSettingsDialog(
                currentVoice = currentVoice,
                onVoiceSelected = { voice -> viewModel.setVoice(voice) },
                continuousConversation = continuousMode,
                onContinuousConversationChanged = { viewModel.toggleContinuousMode(it) },
                onTestVoice = { voice -> viewModel.testVoice(voice) },
                onDismiss = { showVoiceDialog = false }
            )
        }

        if (showHistoryDialog) {
            SessionHistoryDialog(
                sessions = allSessions,
                onSelectSession = { session ->
                    viewModel.loadPastSession(session)
                    showHistoryDialog = false
                },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onDismiss = { showHistoryDialog = false }
            )
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                currentApiKey = customApiKey,
                onSaveKey = { key -> viewModel.saveApiKey(key) },
                onClearKey = { viewModel.clearApiKey() },
                onTestKey = { key -> viewModel.testApiKey(key) },
                onDismiss = { showApiKeyDialog = false }
            )
        }
    }
}

// ---------------- SUB-COMPONENTS ----------------

@Composable
private fun TopBar(
    orbState: LiveOrbState,
    messageCount: Int,
    currentVoiceName: String,
    hasActiveApiKey: Boolean,
    onOpenApiKey: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewSession: () -> Unit,
    onOpenTranscript: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo and status pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF24C1E0), Color(0xFF4285F4), Color(0xFF9B72CF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Gemini Logo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Gemini Live",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Live status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (orbState) {
                        LiveOrbState.IDLE -> Color(0xFF34A853)
                        LiveOrbState.LISTENING -> Color(0xFF24C1E0)
                        LiveOrbState.THINKING -> Color(0xFF9B72CF)
                        LiveOrbState.SPEAKING -> Color(0xFFFA5D75)
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when (orbState) {
                            LiveOrbState.IDLE -> "Connected • $currentVoiceName"
                            LiveOrbState.LISTENING -> "Listening..."
                            LiveOrbState.THINKING -> "Thinking..."
                            LiveOrbState.SPEAKING -> "Speaking..."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF9AA0A6)
                    )
                }
            }
        }

        // Top actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            // API Key button
            IconButton(
                onClick = onOpenApiKey,
                modifier = Modifier.testTag("api_key_button")
            ) {
                BadgedBox(
                    badge = {
                        if (hasActiveApiKey) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34A853))
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Gemini API Key Settings",
                        tint = if (hasActiveApiKey) Color(0xFF81C995) else Color(0xFF8AB4F8)
                    )
                }
            }

            // Voice selector
            IconButton(
                onClick = onOpenVoice,
                modifier = Modifier.testTag("voice_selector_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Change Voice",
                    tint = Color(0xFF8AB4F8)
                )
            }

            // History
            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.testTag("history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Conversations History",
                    tint = Color(0xFF8AB4F8)
                )
            }

            // New session
            IconButton(
                onClick = onNewSession,
                modifier = Modifier.testTag("new_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Session",
                    tint = Color(0xFF8AB4F8)
                )
            }

            // Transcript drawer
            IconButton(
                onClick = onOpenTranscript,
                modifier = Modifier.testTag("transcript_button")
            ) {
                BadgedBox(
                    badge = {
                        if (messageCount > 0) {
                            Badge(
                                containerColor = Color(0xFF4285F4),
                                contentColor = Color.White
                            ) {
                                Text(messageCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "View Transcript",
                        tint = Color(0xFF8AB4F8)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionCard(
    orbState: LiveOrbState,
    caption: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .clickable(onClick = onClick)
            .testTag("caption_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131728).copy(alpha = 0.85f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF222B44), Color(0xFF323F62), Color(0xFF222B44))
            ),
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (orbState) {
                    LiveOrbState.IDLE -> "READY"
                    LiveOrbState.LISTENING -> "LISTENING"
                    LiveOrbState.THINKING -> "THINKING"
                    LiveOrbState.SPEAKING -> "GEMINI LIVE"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = when (orbState) {
                    LiveOrbState.IDLE -> Color(0xFF8AB4F8)
                    LiveOrbState.LISTENING -> Color(0xFF24C1E0)
                    LiveOrbState.THINKING -> Color(0xFF9B72CF)
                    LiveOrbState.SPEAKING -> Color(0xFFFA5D75)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = caption,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = Color(0xFFF1F3F4),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PromptSuggestionChips(
    isCameraActive: Boolean,
    onPromptClick: (String) -> Unit
) {
    val suggestions = if (isCameraActive) {
        listOf(
            "What is this object in front of me?",
            "Can you read or translate this text?",
            "Give me fun facts about what you see",
            "Suggest ideas based on this scene"
        )
    } else {
        listOf(
            "Explain quantum physics in simple words",
            "Help me practice a job interview",
            "Tell me an intriguing mystery riddle",
            "Suggest 3 high-impact startup concepts",
            "Roleplay as a tour guide in Tokyo"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF161D2E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25304C)),
                modifier = Modifier.clickable { onPromptClick(prompt) }
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    color = Color(0xFFC4D5F6),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    orbState: LiveOrbState,
    isCameraActive: Boolean,
    isMuted: Boolean,
    showTextInputBar: Boolean,
    onMicClick: () -> Unit,
    onCameraClick: () -> Unit,
    onInterruptClick: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleTextInput: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Camera Vision Toggle
        Surface(
            shape = CircleShape,
            color = if (isCameraActive) Color(0xFF1A73E8) else Color(0xFF1A2136),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCameraActive) Color(0xFF4285F4) else Color(0xFF2A3654)),
            modifier = Modifier.size(54.dp)
        ) {
            IconButton(
                onClick = onCameraClick,
                modifier = Modifier.testTag("camera_toggle_button")
            ) {
                Icon(
                    imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = "Toggle Camera",
                    tint = if (isCameraActive) Color.White else Color(0xFF8AB4F8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Center: Primary Tactile Glowing Microphone Button
        val isListening = orbState == LiveOrbState.LISTENING
        val isSpeaking = orbState == LiveOrbState.SPEAKING

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(86.dp)
        ) {
            // Radiant animated outer pulse ring when active
            if (isListening || isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Color(0x3324C1E0) else Color(0x33FA5D75)
                        )
                )
            }

            Surface(
                shape = CircleShape,
                color = when {
                    isListening -> Color(0xFF24C1E0)
                    isSpeaking -> Color(0xFFFA5D75)
                    isMuted -> Color(0xFF5F6368)
                    else -> Color(0xFF4285F4)
                },
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clickable(onClick = onMicClick)
                    .testTag("main_mic_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when {
                            isMuted -> Icons.Default.MicOff
                            isSpeaking -> Icons.Default.GraphicEq
                            isListening -> Icons.Default.Mic
                            else -> Icons.Default.Mic
                        },
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Right: Context-Aware Action (Interrupt if Speaking, or Keyboard Input Toggle)
        if (isSpeaking) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFD93025),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA4335)),
                modifier = Modifier.size(54.dp)
            ) {
                IconButton(
                    onClick = onInterruptClick,
                    modifier = Modifier.testTag("interrupt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt Gemini",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            Surface(
                shape = CircleShape,
                color = if (showTextInputBar) Color(0xFF4285F4) else Color(0xFF1A2136),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3654)),
                modifier = Modifier.size(54.dp)
            ) {
                IconButton(
                    onClick = onToggleTextInput,
                    modifier = Modifier.testTag("keyboard_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Text Input",
                        tint = if (showTextInputBar) Color.White else Color(0xFF8AB4F8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
