package com.example.geminilive.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun ApiKeyDialog(
    currentApiKey: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTestKey: suspend (String) -> Pair<Boolean, String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputKey by remember { mutableStateOf(currentApiKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131726))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E284A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Gemini Key",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini API Key",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Configure Gemini 3.5 Flash",
                                fontSize = 12.sp,
                                color = Color(0xFF9AA0A6)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_api_key_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9AA0A6)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Status Pill
                val hasKey = currentApiKey.isNotBlank()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasKey) Color(0x2634A853) else Color(0x26FABB05),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasKey) Color(0xFF34A853) else Color(0xFFFABB05)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasKey) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (hasKey) Color(0xFF34A853) else Color(0xFFFABB05),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasKey) "Active API Key Configured" else "No Custom Key Set (Using Local/Demo)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasKey) Color(0xFF81C995) else Color(0xFFFDD663)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Text Field
                Text(
                    text = "Enter your Google Gemini API Key:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE8EAED)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = {
                        inputKey = it
                        testResult = null
                    },
                    placeholder = {
                        Text("AIzaSy...", color = Color(0xFF5F6368), fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (inputKey.isNotBlank()) {
                            onSaveKey(inputKey.trim())
                            Toast.makeText(context, "Gemini API Key saved", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputKey.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        inputKey = ""
                                        testResult = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color(0xFF9AA0A6),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide Key" else "Show Key",
                                    tint = Color(0xFF9AA0A6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF171C2E),
                        unfocusedContainerColor = Color(0xFF171C2E),
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFF28324C)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Paste from Clipboard Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    inputKey = text.trim()
                                    testResult = null
                                    Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("paste_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste from Clipboard",
                            color = Color(0xFF8AB4F8),
                            fontSize = 13.sp
                        )
                    }
                }

                // Test Connection Status Banner
                AnimatedVisibility(visible = testResult != null) {
                    testResult?.let { (success, message) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (success) Color(0x2234A853) else Color(0x22EA4335),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (success) Color(0xFF34A853) else Color(0xFFEA4335)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (success) Color(0xFF34A853) else Color(0xFFEA4335),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = message,
                                    color = if (success) Color(0xFF81C995) else Color(0xFFF28B82),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test & Save Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Test Button
                    OutlinedButton(
                        onClick = {
                            if (inputKey.isBlank()) {
                                Toast.makeText(context, "Please enter an API key to test", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            isTesting = true
                            testResult = null
                            scope.launch {
                                val result = onTestKey(inputKey.trim())
                                isTesting = false
                                testResult = result
                            }
                        },
                        enabled = !isTesting && inputKey.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_api_key_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8AB4F8)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C486B))
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF8AB4F8)
                            )
                        } else {
                            Text("Test Key", fontSize = 14.sp)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            if (inputKey.isBlank()) {
                                Toast.makeText(context, "Please enter a key or cancel", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSaveKey(inputKey.trim())
                            Toast.makeText(context, "Gemini API Key saved successfully!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        enabled = !isTesting && inputKey.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_api_key_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Key", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (hasKey) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onClearKey()
                            inputKey = ""
                            testResult = null
                            Toast.makeText(context, "Custom API Key removed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_api_key_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF28B82)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C2B2B))
                    ) {
                        Text("Remove Saved Key", fontSize = 13.sp)
                    }
                }

                HorizontalDivider(
                    color = Color(0xFF222B45),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // How to get key guidance section
                Text(
                    text = "How to get a free API Key:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC4D5F6)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Open Google AI Studio (aistudio.google.com)\n" +
                            "2. Sign in and tap \"Get API key\"\n" +
                            "3. Create a free key and paste it here to unlock real-time Gemini Live AI voice & vision.",
                    fontSize = 12.sp,
                    color = Color(0xFF9AA0A6),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF181E32),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("AI Studio URL", "https://aistudio.google.com/app/apikey")
                            )
                            Toast.makeText(context, "AI Studio link copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("copy_aistudio_link_button")
                        .padding(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "https://aistudio.google.com",
                            fontSize = 12.sp,
                            color = Color(0xFF8AB4F8)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
