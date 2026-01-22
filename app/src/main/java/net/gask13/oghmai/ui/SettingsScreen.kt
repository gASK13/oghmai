package net.gask13.oghmai.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import net.gask13.oghmai.auth.AuthManager
import net.gask13.oghmai.notifications.NotificationHelper
import net.gask13.oghmai.notifications.NotificationScheduler
import net.gask13.oghmai.preferences.PreferencesManager
import net.gask13.oghmai.services.TextToSpeechWrapper
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    version: String = "v0.1-dev",
    language: String = "Italian"
) {
    val context = LocalContext.current
    val username = AuthManager.getCurrentUsername() ?: "Not logged in"
    val coroutineScope = rememberCoroutineScope()
    var isLoggingOut by remember { mutableStateOf(false) }

    val preferencesManager = remember { PreferencesManager(context) }
    var notificationsEnabled by remember { mutableStateOf(preferencesManager.isNotificationsEnabled()) }
    val notificationHour = remember { mutableStateOf(preferencesManager.getNotificationHour()) }
    val notificationMinute = remember { mutableStateOf(preferencesManager.getNotificationMinute()) }

    // Voice settings
    var speechRate by remember { mutableStateOf(preferencesManager.getTtsSpeechRate()) }
    var pitch by remember { mutableStateOf(preferencesManager.getTtsPitch()) }
    var selectedVoiceName by remember { mutableStateOf(preferencesManager.getTtsVoice()) }
    var availableVoices by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showVoiceDropdown by remember { mutableStateOf(false) }
    val ttsWrapper = remember { TextToSpeechWrapper() }

    // Italian pop-culture phrases for TTS testing
    val italianTestPhrases = remember {
        listOf(
            "Mamma mia!",
            "Squadra azzurra",
            "Bella Italia",
            "Dolce vita",
            "Arrivederci",
            "Che bella giornata!",
            "Ciao bella!",
            "Molto bene"
        )
    }

    // Snackbar state for showing the selected phrase
    val snackbarHostState = remember { SnackbarHostState() }

    // Expandable section states
    var accountExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var notificationsExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        ttsWrapper.initializeTextToSpeech(context) {
            // Once TTS is initialized, get available voices
            availableVoices = ttsWrapper.getAvailableVoices()
        }
        onDispose {
            ttsWrapper.shutdown()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            preferencesManager.setNotificationsEnabled(true)
            NotificationScheduler.scheduleDailyNotification(
                context,
                notificationHour.value,
                notificationMinute.value
            )
        } else {
            notificationsEnabled = false
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
    }

    ScaffoldWithTopBar(
        title = "Settings",
        isMainScreen = false,
        onBackClick = { navController.popBackStack() },
        showOptionsMenu = false,
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Account Section
            ExpandableSection(
                title = "Account",
                expanded = accountExpanded,
                onToggle = { accountExpanded = !accountExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Language Section
            ExpandableSection(
                title = "Language",
                expanded = languageExpanded,
                onToggle = { languageExpanded = !languageExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Current Language",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = language,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Notifications Section
            ExpandableSection(
                title = "Notifications",
                expanded = notificationsExpanded,
                onToggle = { notificationsExpanded = !notificationsExpanded }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable daily reminders")
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (!hasPermission) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            notificationsEnabled = true
                                            preferencesManager.setNotificationsEnabled(true)
                                            NotificationScheduler.scheduleDailyNotification(
                                                context,
                                                notificationHour.value,
                                                notificationMinute.value
                                            )
                                        }
                                    } else {
                                        notificationsEnabled = true
                                        preferencesManager.setNotificationsEnabled(true)
                                        NotificationScheduler.scheduleDailyNotification(
                                            context,
                                            notificationHour.value,
                                            notificationMinute.value
                                        )
                                    }
                                } else {
                                    notificationsEnabled = false
                                    preferencesManager.setNotificationsEnabled(false)
                                    NotificationScheduler.cancelDailyNotification(context)
                                }
                            }
                        )
                    }

                    if (notificationsEnabled) {
                        Button(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, selectedHour, selectedMinute ->
                                        notificationHour.value = selectedHour
                                        notificationMinute.value = selectedMinute
                                        preferencesManager.setNotificationTime(selectedHour, selectedMinute)
                                        NotificationScheduler.scheduleDailyNotification(
                                            context,
                                            selectedHour,
                                            selectedMinute
                                        )
                                    },
                                    notificationHour.value,
                                    notificationMinute.value,
                                    true
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Notification Time: ${String.format("%02d:%02d", notificationHour.value, notificationMinute.value)}"
                            )
                        }
                    }
                }
            }

            // Voice Section
            ExpandableSection(
                title = "Voice",
                expanded = voiceExpanded,
                onToggle = { voiceExpanded = !voiceExpanded }
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Voice Selection
                    if (availableVoices.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Voice",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Choose which voice to use for Italian pronunciation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Voice dropdown
                            ExposedDropdownMenuBox(
                                expanded = showVoiceDropdown,
                                onExpandedChange = { showVoiceDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = availableVoices.find { it.first == selectedVoiceName }?.second ?: selectedVoiceName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Selected Voice") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showVoiceDropdown)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = showVoiceDropdown,
                                    onDismissRequest = { showVoiceDropdown = false }
                                ) {
                                    availableVoices.forEach { (voiceName, displayName) ->
                                        DropdownMenuItem(
                                            text = { Text(displayName) },
                                            onClick = {
                                                selectedVoiceName = voiceName
                                                preferencesManager.setTtsVoice(voiceName)
                                                ttsWrapper.setVoice(voiceName)
                                                showVoiceDropdown = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Speech Rate Slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Speech Rate",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = String.format("%.1fx", speechRate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "How fast the voice speaks (0.5x = slower, 2.0x = faster)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = speechRate,
                            onValueChange = { newRate ->
                                speechRate = newRate
                            },
                            onValueChangeFinished = {
                                preferencesManager.setTtsSpeechRate(speechRate)
                                ttsWrapper.setSpeechRate(speechRate)
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 29,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    // Pitch Slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pitch",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = String.format("%.1fx", pitch),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "How high or low the voice sounds (0.5x = lower, 2.0x = higher)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = pitch,
                            onValueChange = { newPitch ->
                                pitch = newPitch
                            },
                            onValueChangeFinished = {
                                preferencesManager.setTtsPitch(pitch)
                                ttsWrapper.setPitch(pitch)
                            },
                            valueRange = 0.5f..2.0f,
                            steps = 29,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    // Test Button
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Test Voice Settings",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Button(
                            onClick = {
                                val randomPhrase = italianTestPhrases.random()
                                ttsWrapper.speak(randomPhrase, "preview_utterance")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Voice with Current Settings")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoggingOut = true
                        try {
                            AuthManager.signOut()
                            // Navigate to login screen
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Error signing out", e)
                        } finally {
                            isLoggingOut = false
                        }
                    }
                },
                enabled = !isLoggingOut && username != "Not logged in",
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onToggle) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Content
            if (expanded) {
                HorizontalDivider()
                content()
            }
        }
    }
}