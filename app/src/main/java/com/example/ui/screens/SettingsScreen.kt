package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.ThemeSettings
import com.example.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.currentTheme
    val isAutoSave by viewModel.isAutoSaveEnabled
    val lastBackupStamp by viewModel.lastBackupTime

    // Dialog state control
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPinText by remember { mutableStateOf("") }
    var isSettingNewPin by remember { mutableStateOf(!viewModel.preferenceManager.pinLockEnabled) }

    // Backup Activity result handlers (Storage Access Framework Standard API)
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = viewModel.backupNotesToUri(context, uri)
            if (success) {
                Toast.makeText(context, "Database backup exported successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to export backup. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val success = viewModel.restoreNotesFromUri(context, uri)
            if (success) {
                Toast.makeText(context, "Notes database restored successfully!", Toast.LENGTH_LONG).show()
                onNavigateBack() // Go back to reload note indexes
            } else {
                Toast.makeText(context, "Failed to parse backup content. Verify file format.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings Console", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Styles section
            SectionHeader(icon = Icons.Default.Palette, title = "Aesthetic Themes")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Tap below to instantly shift the app's fonts and colors:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeSettings.THEMES.forEach { themeName ->
                            val isSelected = currentTheme == themeName
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .height(70.dp)
                                    .clickable { viewModel.changeTheme(themeName) }
                                    .testTag("theme_card_$themeName"),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                ) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = themeName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Security Controls lock PIN
            SectionHeader(icon = Icons.Default.Security, title = "Device Security")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Primary 4-Digit Security PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (viewModel.preferenceManager.pinLockEnabled) "Enabled: App requires your PIN on start." else "Disabled: App starts without secure authentication lock.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                enteredPinText = ""
                                isSettingNewPin = !viewModel.preferenceManager.pinLockEnabled
                                showPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.preferenceManager.pinLockEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (viewModel.preferenceManager.pinLockEnabled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("pin_config_button")
                        ) {
                            Text(if (viewModel.preferenceManager.pinLockEnabled) "Disable" else "Configure")
                        }
                    }
                }
            }

            // Database Backups & Synchronization
            SectionHeader(icon = Icons.Default.CloudSync, title = "Synchronization & Storage")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "NATIVE CLOUD BACKUPS (GDrive & Dropbox Integration)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Secure Notes utilizes Android's native Storage Access Framework (SAF). By selecting the 'Export Backup File' action below, your operating system will open a folder picker where you can choose a directory inside your Google Drive, Dropbox, or OneDrive storage client. The database backup will be saved directly and cloud-synchronized securely without exposing personal details or files to third-parties.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Export Database Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                if (lastBackupStamp > 0) "Last backup: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastBackupStamp))}" else "Never backed up",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                createBackupLauncher.launch("notes_backup_${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export JSON Backup", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Import / Restore Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Replace or expand active notes lists from JSON file.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Import JSON Backup", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            // Compliance & Absolute Privacy Logs Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Privacy Information Indicator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column {
                        Text("100% Privacy Preserved", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "This application operates on a zero-tracking model. Notes are kept exclusively in a secure, encrypted local sandbox database on your hardware. Your notes are never harvested, sold, or shared. Cloud interaction persists strictly at your manual instruction via Android document trees.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Security PIN Locker Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = {
                Text(
                    if (isSettingNewPin) "Configure Secure App PIN" else "Verify App PIN Security",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (isSettingNewPin) "Enter a 4-digit security PIN to restrict app access on launch:" else "Enter your current 4-digit security PIN to disable app locking:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = enteredPinText,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                enteredPinText = it
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_dialog_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPinText.length != 4) {
                            Toast.makeText(context, "PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isSettingNewPin) {
                            viewModel.setAppSecurityPin(enteredPinText)
                            Toast.makeText(context, "App Security PIN established successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            if (viewModel.preferenceManager.verifyPin(enteredPinText)) {
                                viewModel.disableAppSecurityPin()
                                Toast.makeText(context, "App Security PIN removed successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Incorrect PIN. Settings unchanged.", Toast.LENGTH_LONG).show()
                            }
                        }
                        showPinDialog = false
                    },
                    modifier = Modifier.testTag("pin_dialog_submit")
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
