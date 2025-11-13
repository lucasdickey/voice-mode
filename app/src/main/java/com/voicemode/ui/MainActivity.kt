package com.voicemode.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicemode.ui.theme.VoiceModeTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("Microphone permission granted: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onRequestMicrophone = {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onRequestOverlay = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            startActivity(intent)
                        },
                        onRequestAccessibility = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onRequestMicrophone: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Mode") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Voice Mode",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "AI-Powered Voice Dictation",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Setup Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isFullySetup) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (state.isFullySetup) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.isFullySetup) {
                            "All Set!"
                        } else {
                            "Setup Required"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Permission Cards
            PermissionCard(
                title = "Accessibility Service",
                description = "Required to detect text fields and insert text",
                isGranted = state.isAccessibilityEnabled,
                icon = Icons.Default.Accessibility,
                onRequest = onRequestAccessibility
            )

            PermissionCard(
                title = "Microphone",
                description = "Required to record your voice",
                isGranted = state.hasMicrophonePermission,
                icon = Icons.Default.Mic,
                onRequest = onRequestMicrophone
            )

            PermissionCard(
                title = "Display Overlay",
                description = "Required to show recording interface",
                isGranted = state.hasOverlayPermission,
                icon = Icons.Default.Window,
                onRequest = onRequestOverlay
            )

            Spacer(modifier = Modifier.weight(1f))

            // Instructions
            if (state.isFullySetup) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "How to use:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Open any app with a text field\n" +
                                    "2. Long-press in the text field (500ms)\n" +
                                    "3. Speak your message\n" +
                                    "4. Release to process and insert text",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onRequest) {
                    Text("Grant")
                }
            }
        }
    }
}
