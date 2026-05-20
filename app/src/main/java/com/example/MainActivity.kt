package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NoteListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel

enum class NavScreen {
    LIST,
    EDITOR,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Read active visual theme preference real-time
            val themeName by viewModel.currentTheme
            val isLocked by viewModel.isAppLocked

            MyApplicationTheme(selectedThemeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked) {
                        // Display beautiful PIN auth gate before showing app content
                        PinAuthenticationGate(viewModel = viewModel)
                    } else {
                        // App Core screens with routing
                        AppNavigationContainer(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavigationContainer(viewModel: NotesViewModel) {
    var screenState by remember { mutableStateOf(NavScreen.LIST) }
    var selectedNoteId by remember { mutableStateOf<Int?>(null) }

    // Clean back button gestures handlers
    when (screenState) {
        NavScreen.LIST -> {
            // Exit warning or native action handled by OS
        }
        NavScreen.EDITOR -> {
            BackHandler(enabled = true) {
                viewModel.saveNote {
                    screenState = NavScreen.LIST
                }
            }
        }
        NavScreen.SETTINGS -> {
            BackHandler(enabled = true) {
                screenState = NavScreen.LIST
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screenState) {
            NavScreen.LIST -> NoteListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { id ->
                    selectedNoteId = id
                    screenState = NavScreen.EDITOR
                },
                onNavigateToSettings = {
                    screenState = NavScreen.SETTINGS
                }
            )
            NavScreen.EDITOR -> NoteEditorScreen(
                viewModel = viewModel,
                noteId = selectedNoteId,
                onNavigateBack = {
                    screenState = NavScreen.LIST
                }
            )
            NavScreen.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    screenState = NavScreen.LIST
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinAuthenticationGate(viewModel: NotesViewModel) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    val isSetup = viewModel.isPinSetup

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "App Locked Toggle",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "ACCESS RESTRICTED",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Enter your secure 4-digit PIN password to decrypt offline database container:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Large PIN Entry dots visualizer
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0..3) {
                val entered = pinText.length > i
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (entered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (entered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        // Sleek numeric keypad
        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("Clear", "0", "OK")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (key in row) {
                        Button(
                            onClick = {
                                when (key) {
                                    "Clear" -> {
                                        if (pinText.isNotEmpty()) {
                                            pinText = pinText.substring(0, pinText.length - 1)
                                        }
                                    }
                                    "OK" -> {
                                        if (pinText.length != 4) {
                                            Toast.makeText(context, "PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val authorized = viewModel.unlockApp(pinText)
                                            if (!authorized) {
                                                Toast.makeText(context, "Incorrect PIN. Attempts logged.", Toast.LENGTH_SHORT).show()
                                                pinText = ""
                                            }
                                        }
                                    }
                                    else -> {
                                        if (pinText.length < 4) {
                                            pinText += key
                                            // Auto trigger verification on reaching 4 numbers for frictionless speed
                                            if (pinText.length == 4) {
                                                val authorized = viewModel.unlockApp(pinText)
                                                if (!authorized) {
                                                    Toast.makeText(context, "Incorrect PIN. Attempts logged.", Toast.LENGTH_SHORT).show()
                                                    pinText = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "Clear" || key == "OK") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (key == "Clear" || key == "OK") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("keypad_$key")
                        ) {
                            Text(
                                text = key,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
