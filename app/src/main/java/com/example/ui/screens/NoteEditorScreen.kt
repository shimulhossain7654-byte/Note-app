package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MarkdownRenderer
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: Int?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Trigger loading once on render
    LaunchedEffect(noteId) {
        viewModel.loadNoteIntoEditor(noteId)
    }

    // Bind state from View Model
    val editorTitle by viewModel.edTitle
    val editorContent by viewModel.edContent
    val editorFolder by viewModel.edFolder
    val editorTags by viewModel.edTags
    val editorColorHex by viewModel.edColorHex
    val editorIsPinned by viewModel.edIsPinned
    val editorIsLocked by viewModel.edIsLocked
    val editorIsArchived by viewModel.edIsArchived

    val wordCount = viewModel.edWordCount
    val charCount = viewModel.edCharCount
    val readingTime = viewModel.edReadingTimeMinutes

    // Toggle Tab states (Write vs Preview)
    var activeTab by remember { mutableStateOf(0) } // 0 = Write, 1 = Markdown Visualizer

    // Controls dialogs
    var showAttributesDialog by remember { mutableStateOf(false) }

    // Master editor colors
    val editorColors = listOf(
        "#2D3748" to "Default",
        "#B91C1C" to "Crimson",
        "#15803D" to "Forest",
        "#1D4ED8" to "Royal Blue",
        "#7C3AED" to "Violet",
        "#B45309" to "Burnt Amber",
        "#D97706" to "Copper"
    )

    val currentBgColor = remember(editorColorHex) {
        try {
            Color(android.graphics.Color.parseColor(editorColorHex))
        } catch (e: Exception) {
            Color(0xFF2D3748)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNote {
                            onNavigateBack()
                        }
                    }, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Save and Go Back")
                    }
                },
                actions = {
                    // Lock protection notification
                    IconButton(onClick = { viewModel.edIsPinned.value = !editorIsPinned }) {
                        Icon(
                            imageVector = if (editorIsPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (editorIsPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showAttributesDialog = true }, modifier = Modifier.testTag("note_settings_button")) {
                        Icon(Icons.Default.Tune, contentDescription = "Edit Attributes")
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteCurrentNote {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("delete_note_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.error)
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
        ) {
            // Write / Visual Markdown Preview Top Tabs
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editor")
                        }
                    },
                    modifier = Modifier.testTag("tab_editor")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = "Preview", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Visual Preview")
                        }
                    },
                    modifier = Modifier.testTag("tab_preview")
                )
            }

            // Tabs Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeTab == 0) {
                    // Writing Workspace Screen
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Title Area
                        TextField(
                            value = editorTitle,
                            onValueChange = { viewModel.onEditorChange(it, editorContent) },
                            placeholder = { Text("Note Title", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_title_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Live Editor Markdown helper toolbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Formatting action injection buttons
                            MarkdownActionButton(label = "H1", desc = "H1 Header") {
                                viewModel.onEditorChange(editorTitle, "$editorContent\n# ")
                            }
                            MarkdownActionButton(label = "H2", desc = "H2 Header") {
                                viewModel.onEditorChange(editorTitle, "$editorContent\n## ")
                            }
                            MarkdownActionButton(imageVector = Icons.Default.FormatBold, desc = "Bold Text") {
                                viewModel.onEditorChange(editorTitle, "$editorContent**bold**")
                            }
                            MarkdownActionButton(imageVector = Icons.Default.FormatItalic, desc = "Italic Text") {
                                viewModel.onEditorChange(editorTitle, "$editorContent*italic*")
                            }
                            MarkdownActionButton(imageVector = Icons.Default.CheckBox, desc = "Checklist Row") {
                                viewModel.onEditorChange(editorTitle, "$editorContent\n- [ ] ")
                            }
                            MarkdownActionButton(imageVector = Icons.Default.List, desc = "Bullet List") {
                                viewModel.onEditorChange(editorTitle, "$editorContent\n- ")
                            }
                            MarkdownActionButton(imageVector = Icons.Default.Code, desc = "Code Block") {
                                viewModel.onEditorChange(editorTitle, "$editorContent\n```\ncode\n```")
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            // Undo action button
                            IconButton(
                                onClick = { viewModel.performUndo() },
                                enabled = viewModel.canUndo,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Undo,
                                    contentDescription = "Undo Edit",
                                    tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Redo action button
                            IconButton(
                                onClick = { viewModel.performRedo() },
                                enabled = viewModel.canRedo,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Redo,
                                    contentDescription = "Redo Edit",
                                    tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Code writing Text Field Area
                        TextField(
                            value = editorContent,
                            onValueChange = { viewModel.onEditorChange(editorTitle, it) },
                            placeholder = { Text("Write Markdown here... Use the markdown action bars or visual previews for rich-text designs.", fontSize = 15.sp) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontFamily = FontFamily.SansSerif, lineHeight = 22.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .testTag("note_content_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }
                } else {
                    // Preview Markdown rendered view Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        if (editorTitle.isNotBlank()) {
                            Text(
                                text = editorTitle,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        if (editorContent.isBlank()) {
                            Text(
                                "Document preview is currently empty. Start typing standard markdown formatting inside the Editor Tab to preview formatting instantly.",
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        } else {
                            MarkdownRenderer(markdownText = editorContent)
                        }
                    }
                }
            }

            // Bottom Metrics Bar of Note Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Words: $wordCount  |  Chars: $charCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = "~$readingTime min read",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Interactive Attributes Modal Setup Drawer Dialogue
    if (showAttributesDialog) {
        AlertDialog(
            onDismissRequest = { showAttributesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = "Config", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Note Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Folder attributes field
                    OutlinedTextField(
                        value = editorFolder,
                        onValueChange = { viewModel.edFolder.value = it },
                        label = { Text("Note Folder", fontSize = 13.sp) },
                        placeholder = { Text("Uncategorized") },
                        modifier = Modifier.fillMaxWidth().testTag("folder_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = "Folder") }
                    )

                    // Tags fields
                    OutlinedTextField(
                        value = editorTags,
                        onValueChange = { viewModel.edTags.value = it },
                        label = { Text("Tags (comma separated)", fontSize = 13.sp) },
                        placeholder = { Text("Work, Private, Brainstorm") },
                        modifier = Modifier.fillMaxWidth().testTag("tags_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = "Tags") }
                    )

                    // Note Locks configurations settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (viewModel.preferenceManager.appSecurityPin.isEmpty()) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Please set up your security PIN inside App Settings screen first.",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                } else {
                                    viewModel.edIsLocked.value = !editorIsLocked
                                }
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (editorIsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock State",
                                tint = if (editorIsLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PIN Secure Lock", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Encrypt contents inside index screens", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = editorIsLocked,
                            onCheckedChange = {
                                if (viewModel.preferenceManager.appSecurityPin.isEmpty()) {
                                    Toast.makeText(context, "Please set up your security PIN inside App Settings screen first.", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.edIsLocked.value = it
                                }
                            }
                        )
                    }

                    // Color choice Picker list row
                    Column {
                        Text("Note Palette Color", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            editorColors.forEach { (hex, name) ->
                                val isSelected = editorColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable { viewModel.edColorHex.value = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = name,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAttributesDialog = false }, modifier = Modifier.testTag("submit_attributes")) {
                    Text("Apply & Save")
                }
            }
        )
    }
}

// Compact helper Composable for quick text button triggers
@Composable
fun MarkdownActionButton(
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(30.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

// Overloaded compact helper Composable for vector formatting icons triggers
@Composable
fun MarkdownActionButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(30.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = desc,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
    }
}
