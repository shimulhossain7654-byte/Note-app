package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    // General app state updates tracking
    var isSavingState by remember { mutableStateOf(false) }

    // Live Metrics calculations
    val wordCount = viewModel.edWordCount
    val charCount = viewModel.edCharCount
    val charNoSpaces = editorContent.replace(" ", "").replace("\n", "").length
    val paragraphCount = if (editorContent.isBlank()) 0 else editorContent.split("\n").filter { it.isNotBlank() }.size
    val estimatedLines = if (editorContent.isBlank()) 0 else editorContent.split("\n").size
    val readingTime = viewModel.edReadingTimeMinutes
    val speakingTime = Math.max(1, wordCount / 130) // average standard speech pace is 130 wpm

    // Simple Sentiment / Vibe Analyzer live evaluation (Checks frequency of happy vs sad words)
    val joyIntensity = remember(editorContent) {
        val happyWords = listOf("happy", "glad", "joy", "great", "nice", "awesome", "wonderful", "success", "love", "smile", "laugh", "good", "beautiful", "blessed", "creative")
        val sadWords = listOf("sad", "sorry", "tear", "bad", "awful", "terrible", "worst", "unhappy", "cry", "pain", "hurt", "fail", "broken", "worried", "stressed")
        val contentLower = editorContent.lowercase()
        val hCount = happyWords.count { contentLower.contains(it) }
        val sCount = sadWords.count { contentLower.contains(it) }
        if (hCount == 0 && sCount == 0) 50 // Neutral score
        else (hCount * 100) / (hCount + sCount)
    }

    // Coleman-Liau Readability index simulation
    val readDifficulty = remember(editorContent, wordCount) {
        if (wordCount < 10) "Simple"
        else {
            val sentences = Math.max(1, editorContent.split(Regex("[.!?]")).filter { it.trim().isNotEmpty() }.size)
            val l = (charNoSpaces.toFloat() / wordCount.toFloat()) * 100
            val s = (sentences.toFloat() / wordCount.toFloat()) * 100
            val index = 0.0588 * l - 0.296 * s - 15.8
            when {
                index < 5.0 -> "Elementary"
                index < 9.0 -> "Intermediate"
                index < 12.0 -> "Advanced (High School)"
                else -> "Academic (College)"
            }
        }
    }

    // Dynamic WYSIWYG properties
    var editorFontSize by remember { mutableStateOf(16.sp) }
    var editorFontFamily by remember { mutableStateOf(FontFamily.SansSerif) }
    var editorFontWeight by remember { mutableStateOf(FontWeight.Normal) }
    var editorFontStyle by remember { mutableStateOf(FontStyle.Normal) }

    // Toggle drawers & menus states
    var focusMode by remember { mutableStateOf(false) }
    var showFindReplace by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showMetricsDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    // Search and Replace values state
    var searchWord by remember { mutableStateOf("") }
    var replaceWord by remember { mutableStateOf("") }

    // Folder input state
    var newFolderNameInput by remember { mutableStateOf("") }

    // Color definitions
    val editorColors = listOf(
        "#2D3748" to "Default Slate",
        "#B91C1C" to "Crimson Peach",
        "#15803D" to "Emerald Forest",
        "#1D4ED8" to "Ocean Royal",
        "#7C3AED" to "Orchid Lavender",
        "#B45309" to "Burnt Tangerine",
        "#D97706" to "Copper Gold"
    )

    // AMOLED pure dark card adaptivity matching Keep & Redmi
    val isSystemDark = isSystemInDarkTheme()
    val pageBgColor = remember(editorColorHex, isSystemDark) {
        if (isSystemDark) {
            when (editorColorHex) {
                "#B91C1C" -> Color(0xFF2B0B0B) // Deep Coral amoled 
                "#15803D" -> Color(0xFF062312) // Deep Emerald amoled
                "#1D4ED8" -> Color(0xFF061430) // Deep Sapphire amoled
                "#7C3AED" -> Color(0xFF1E0A36) // Deep Velvet amoled
                "#B45309" -> Color(0xFF281102) // Deep Tangerine amoled
                "#D97706" -> Color(0xFF261202) // Deep Gold amoled
                else -> Color(0xFF000000) // Pure AMOLED black!
            }
        } else {
            when (editorColorHex) {
                "#B91C1C" -> Color(0xFFFFECEC) // Pastel Crimson 
                "#15803D" -> Color(0xFFE8FDF0) // Pastel Mint
                "#1D4ED8" -> Color(0xFFECF3FF) // Pastel Sky
                "#7C3AED" -> Color(0xFFF7F3FF) // Pastel Lavender
                "#B45309" -> Color(0xFFFFF7E6) // Pastel Peach
                "#D97706" -> Color(0xFFFFF9EC) // Pastel Cream
                else -> Color(0xFFFFFFFF) // Pure Clean White default!
            }
        }
    }

    val pageOnColor = if (isSystemDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val pageMutedColor = if (isSystemDark) Color(0xFF94A3B8) else Color(0xFF475569)

    // Simple custom find-and-replace algorithm
    val textMatchesCount = remember(editorContent, searchWord) {
        if (searchWord.isEmpty()) 0
        else {
            var index = 0
            var count = 0
            while (true) {
                index = editorContent.indexOf(searchWord, index, ignoreCase = true)
                if (index == -1) break
                count++
                index += searchWord.length
            }
            count
        }
    }

    Scaffold(
        topBar = {
            if (!focusMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (noteId == null) "Create Note" else "Edit Note",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = pageOnColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSavingState = true
                            viewModel.saveNote {
                                isSavingState = false
                                onNavigateBack()
                            }
                        }, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Save and Go Back", tint = pageOnColor)
                        }
                    },
                    actions = {
                        // Quick Pin visual toggle
                        IconButton(onClick = { 
                            viewModel.edIsPinned.value = !editorIsPinned
                            Toast.makeText(context, if (!editorIsPinned) "Note Pinned" else "Note Unpinned", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (editorIsPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                                contentDescription = "Pin Status",
                                tint = if (editorIsPinned) MaterialTheme.colorScheme.primary else pageMutedColor
                            )
                        }

                        // Search and Replace workspace activator
                        IconButton(onClick = { showFindReplace = !showFindReplace }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Find and Replace Toggle",
                                tint = if (showFindReplace) MaterialTheme.colorScheme.primary else pageMutedColor
                            )
                        }

                        // Direct categorize folders selection trigger
                        IconButton(onClick = { showFolderDialog = true }, modifier = Modifier.testTag("note_folder_button")) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Folder Categorize", tint = pageOnColor)
                        }

                        // Metadata categories configuration trigger dialog
                        IconButton(onClick = { showTagDialog = true }) {
                            Icon(Icons.Default.LocalOffer, contentDescription = "Tag Clouds", tint = pageOnColor)
                        }

                        // Delete notes click direct
                        IconButton(
                            onClick = {
                                viewModel.deleteCurrentNote {
                                    Toast.makeText(context, "Note Removed", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash Draft", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = pageBgColor
                    )
                )
            }
        },
        containerColor = pageBgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (focusMode) PaddingValues(top = 40.dp, bottom = 12.dp) else innerPadding)
                .background(pageBgColor)
        ) {
            // Find and Replace Workspace drawer (Active visual editor support)
            AnimatedVisibility(visible = showFindReplace && !focusMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSystemDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Find target field
                            OutlinedTextField(
                                value = searchWord,
                                onValueChange = { searchWord = it },
                                placeholder = { Text("Find Text", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp)
                            )

                            // Replace target field
                            OutlinedTextField(
                                value = replaceWord,
                                onValueChange = { replaceWord = it },
                                placeholder = { Text("Replace With", fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp)
                            )

                            IconButton(onClick = { showFindReplace = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search workspace")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (searchWord.isEmpty()) "Type word to find matches" else "$textMatchesCount matches found",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (textMatchesCount > 0) MaterialTheme.colorScheme.primary else pageMutedColor
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        if (searchWord.isNotEmpty()) {
                                            val updated = editorContent.replaceFirst(searchWord, replaceWord, ignoreCase = true)
                                            viewModel.onEditorChange(editorTitle, updated)
                                        }
                                    },
                                    enabled = textMatchesCount > 0
                                ) {
                                    Text("Replace Match", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (searchWord.isNotEmpty()) {
                                            val updated = editorContent.replace(searchWord, replaceWord, ignoreCase = true)
                                            viewModel.onEditorChange(editorTitle, updated)
                                            Toast.makeText(context, "All occurrence replacements updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = textMatchesCount > 0,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Replace All", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Folder and Tags info panel strip (Redmi notes header style)
            if (!focusMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showFolderDialog = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Folder",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = editorFolder.ifBlank { "Uncategorized" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (editorTags.isNotBlank()) {
                        editorTags.split(",").map { it.trim() }.forEach { tag ->
                            if (tag.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = pageMutedColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "#$tag", fontSize = 10.sp, color = pageMutedColor, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            // Distraction free Exit prompt if in Focus mode
            if (focusMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { focusMode = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Exit Distraction-Free Focus Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Core Writing Canvas Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Title Field
                TextField(
                    value = editorTitle,
                    onValueChange = { viewModel.onEditorChange(it, editorContent) },
                    placeholder = { Text("Title", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = pageMutedColor) },
                    textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = pageOnColor),
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

                // Separator line (Keep style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(pageMutedColor.copy(alpha = 0.15f))
                )

                // Editable Note Content Canvas
                TextField(
                    value = editorContent,
                    onValueChange = { viewModel.onEditorChange(editorTitle, it) },
                    placeholder = { Text("Start typing... Click on the formatting tray below to customize fonts, cases, convert sizes, or trigger templates.", fontSize = editorFontSize, color = pageMutedColor.copy(alpha = 0.7f)) },
                    textStyle = TextStyle(
                        fontSize = editorFontSize,
                        fontFamily = editorFontFamily,
                        fontWeight = editorFontWeight,
                        fontStyle = editorFontStyle,
                        color = pageOnColor,
                        lineHeight = (editorFontSize.value * 1.5f).sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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

            // Dynamic XML Formatting Tool Tray / Assistant Bar (Docks bottom!)
            if (!focusMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSystemDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                        .padding(vertical = 4.dp)
                ) {
                    // Soft background palette bar (Quick background selector Keep Notes style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Theme Color Palette:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pageMutedColor)
                        
                        editorColors.forEach { (hex, name) ->
                            val isSelected = editorColorHex == hex
                            val hexColor = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(hexColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSystemDark) Color.White else Color.Black,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.edColorHex.value = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Separator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(pageMutedColor.copy(alpha = 0.08f))
                    )

                    // Core Redmi Formatting horizontal row commands
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Font Style modifiers (Cursive, Serif, Sans, Mono, Bold, normal, italic)
                        EditorGroupTitle("FONT")
                        
                        TrayAction(label = "Bold", icon = Icons.Default.FormatBold, desc = "Toggle bold") {
                            editorFontWeight = if (editorFontWeight == FontWeight.Bold) FontWeight.Normal else FontWeight.Bold
                        }

                        TrayAction(label = "Italic", icon = Icons.Default.FormatItalic, desc = "Toggle italic") {
                            editorFontStyle = if (editorFontStyle == FontStyle.Italic) FontStyle.Normal else FontStyle.Italic
                        }

                        TrayAction(label = "Sans", desc = "Sans-Serif Font") {
                            editorFontFamily = FontFamily.SansSerif
                        }
                        TrayAction(label = "Serif", desc = "Serif Font") {
                            editorFontFamily = FontFamily.Serif
                        }
                        TrayAction(label = "Mono", desc = "Monospace Font") {
                            editorFontFamily = FontFamily.Monospace
                        }

                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(pageMutedColor.copy(alpha = 0.2f)))

                        // Convert Sizes Preset
                        EditorGroupTitle("SIZE")
                        TrayAction(label = "Huge", desc = "XL Headings") {
                            editorFontSize = 24.sp
                        }
                        TrayAction(label = "Title", desc = "Title size") {
                            editorFontSize = 20.sp
                        }
                        TrayAction(label = "Body", desc = "Body regular size") {
                            editorFontSize = 16.sp
                        }
                        TrayAction(label = "Small", desc = "Minor annotations") {
                            editorFontSize = 13.sp
                        }

                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(pageMutedColor.copy(alpha = 0.2f)))

                        // Cases conversions presets (UPPER, lower, Title, Sentence, Invert, Space-strip)
                        EditorGroupTitle("CASE")
                        TrayAction(label = "UPPER", desc = "All Capital") {
                            viewModel.onEditorChange(editorTitle, editorContent.uppercase())
                        }
                        TrayAction(label = "lower", desc = "All minor case") {
                            viewModel.onEditorChange(editorTitle, editorContent.lowercase())
                        }
                        TrayAction(label = "TitleCase", desc = "Word First Capitalized") {
                            val converted = editorContent.split(" ").joinToString(" ") { wd ->
                                wd.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            }
                            viewModel.onEditorChange(editorTitle, converted)
                        }
                        TrayAction(label = "Sentence", desc = "Sentence Initial caps") {
                            if (editorContent.isNotEmpty()) {
                                val sentences = editorContent.split(". ").joinToString(". ") { sc ->
                                    sc.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                }
                                viewModel.onEditorChange(editorTitle, sentences)
                            }
                        }
                        TrayAction(label = "Swap", desc = "Invert characters capitals") {
                            val swapped = editorContent.map { c ->
                                if (c.isUpperCase()) c.lowercaseChar() else c.uppercaseChar()
                            }.joinToString("")
                            viewModel.onEditorChange(editorTitle, swapped)
                        }
                        TrayAction(label = "Strip", desc = "Strip redundant spaces") {
                            viewModel.onEditorChange(editorTitle, editorContent.replace(Regex("\\s+"), " ").trim())
                            Toast.makeText(context, "Double spaces stripped cleanly!", Toast.LENGTH_SHORT).show()
                        }

                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(pageMutedColor.copy(alpha = 0.2f)))

                        // Custom insert triggers templates
                        EditorGroupTitle("INSERT")
                        TrayAction(label = "Datetime", icon = Icons.Default.Event, desc = "Insert timestamp") {
                            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            viewModel.onEditorChange(editorTitle, "$editorContent\n[$stamp] ")
                        }
                        TrayAction(label = "Checklist", icon = Icons.Default.CheckBox, desc = "Insert todo checklist line") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n- [ ] ")
                        }
                        TrayAction(label = "Bullet", icon = Icons.Default.FormatListBulleted, desc = "Insert bullet points") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n• ")
                        }
                        TrayAction(label = "Divider", icon = Icons.Default.HorizontalRule, desc = "Insert division stroke") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n---\n")
                        }
                        TrayAction(label = "Quote", icon = Icons.Default.FormatQuote, desc = "Insert quotes templates block") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n> ")
                        }
                        TrayAction(label = "Signature", icon = Icons.Default.DriveFileRenameOutline, desc = "Insert signature standard") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n\nBest regards,\n[Your Name]\n")
                        }
                        TrayAction(label = "Table", icon = Icons.Default.GridOn, desc = "Insert table markdown layout") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n| Header 1 | Header 2 |\n|---|---|\n| Cell 1 | Cell 2 |")
                        }
                        TrayAction(label = "CodeBlock", icon = Icons.Default.Code, desc = "Insert monospace code brackets") {
                            viewModel.onEditorChange(editorTitle, "$editorContent\n```\nCode block here\n```")
                        }

                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(pageMutedColor.copy(alpha = 0.2f)))

                        // Commands, Metrics details drawer, focus mode, lock options
                        EditorGroupTitle("UTILITY")
                        TrayAction(label = "Copy Text", icon = Icons.Default.ContentCopy, desc = "Copy note to clipboard") {
                            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("note_clipboard", editorContent)
                            manager.setPrimaryClip(clip)
                            Toast.makeText(context, "Note copied to clipboards!", Toast.LENGTH_SHORT).show()
                        }
                        TrayAction(label = "Share", icon = Icons.Default.Share, desc = "Send note text") {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, editorTitle)
                                putExtra(Intent.EXTRA_TEXT, "$editorTitle\n\n$editorContent")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Note content"))
                        }
                        TrayAction(label = "Metrics 📊", desc = "See full counters analytics") {
                            showMetricsDialog = true
                        }
                        TrayAction(label = "Focus Mode", icon = Icons.Default.FilterCenterFocus, desc = "Hide formatting tools") {
                            focusMode = true
                            Toast.makeText(context, "Distraction-Free Focus enabled. Backbutton restores.", Toast.LENGTH_SHORT).show()
                        }
                        TrayAction(label = "Clear Canvas", icon = Icons.Default.RestartAlt, desc = "Clean entire document") {
                            viewModel.onEditorChange(editorTitle, "")
                        }
                    }

                    // Underline footer actions (Undo, Redo, Auto save pulse draft indicators)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Undo triggers
                            IconButton(
                                onClick = { viewModel.performUndo() },
                                enabled = viewModel.canUndo,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("undo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Undo,
                                    contentDescription = "Undo changes",
                                    tint = if (viewModel.canUndo) MaterialTheme.colorScheme.primary else pageMutedColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Redo triggers
                            IconButton(
                                onClick = { viewModel.performRedo() },
                                enabled = viewModel.canRedo,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("redo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.Redo,
                                    contentDescription = "Redo changes",
                                    tint = if (viewModel.canRedo) MaterialTheme.colorScheme.primary else pageMutedColor.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Auto-Save pulsing indication or status bar
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)) // Pulsing soft green
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSavingState) "Auto-Saving Draft..." else "Draft Saved",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = pageMutedColor
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Tag Cloud configuration module dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalOffer, contentDescription = "Tags editor", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tag Management Clouds", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configure comma-separated tags grouping below to filter from note index list:", fontSize = 12.sp, color = pageMutedColor)
                    
                    OutlinedTextField(
                        value = editorTags,
                        onValueChange = { viewModel.edTags.value = it },
                        placeholder = { Text("Personal, Ideas, Work, Priority", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("tags_input"),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )

                    Text("Quick tags recommendation select on click:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pageMutedColor)
                    
                    val tagsRecommends = listOf("Work", "Idea", "Personal", "Draft", "Shopping", "Urgent")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagsRecommends.forEach { tagR ->
                            Card(
                                onClick = {
                                    val currentOnTags = editorTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                    if (!currentOnTags.contains(tagR)) {
                                        currentOnTags.add(tagR)
                                        viewModel.edTags.value = currentOnTags.joinToString(", ")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(tagR, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTagDialog = false }) {
                    Text("Apply Clouds")
                }
            }
        )
    }

    // 2. Redmi and Google Keep Notes - Category Folder dialog selector
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = "Folders categorization", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Note Category Folder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Place note into folder drawer directories on save:", fontSize = 12.sp, color = pageMutedColor)
                    
                    // Folder name list representation
                    val standardFolders = listOf("Uncategorized", "Work", "Personal", "Travel", "Diary", "Study")
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        standardFolders.forEach { folderItem ->
                            val isSelected = editorFolder == folderItem
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.edFolder.value = folderItem },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.FolderSpecial else Icons.Default.Folder,
                                            contentDescription = folderItem,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else pageMutedColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(folderItem, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Custom input category addition block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newFolderNameInput,
                            onValueChange = { newFolderNameInput = it },
                            placeholder = { Text("Custom Custom Folder", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("folder_input"),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp)
                        )

                        Button(
                            onClick = {
                                if (newFolderNameInput.isNotBlank()) {
                                    viewModel.edFolder.value = newFolderNameInput.trim()
                                    newFolderNameInput = ""
                                    Toast.makeText(context, "Custom Folder applied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("Add", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFolderDialog = false }) {
                    Text("Apply Folder Settings")
                }
            }
        )
    }

    // 3. Complete metrics detailed analytics dialog (Over 10 counts analyzed live!)
    if (showMetricsDialog) {
        AlertDialog(
            onDismissRequest = { showMetricsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = "Analytics dashboard detail", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Note Metrics & Analytics", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    MetricLabelValue(label = "Words Count", value = "$wordCount words")
                    MetricLabelValue(label = "Total Key Characters", value = "$charCount chars")
                    MetricLabelValue(label = "Characters (excl. spaces)", value = "$charNoSpaces chars")
                    MetricLabelValue(label = "Paragraph Blocks Count", value = "$paragraphCount blocks")
                    MetricLabelValue(label = "Estimated Text Lines", value = "$estimatedLines lines")
                    
                    HorizontalDivider(color = pageMutedColor.copy(alpha = 0.15f))

                    MetricLabelValue(label = "Reading Time", value = "~$readingTime min read (200 WPM)")
                    MetricLabelValue(label = "Speaking Delivery duration", value = "~$speakingTime min speech (130 WPM)")
                    MetricLabelValue(label = "Coleman-Liau Readability Grade", value = readDifficulty)
                    
                    HorizontalDivider(color = pageMutedColor.copy(alpha = 0.15f))

                    // Live Sentiment / Vibe Index display representation
                    Text("Text Sentiment Score Evaluation (Vibe):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pageMutedColor)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { joyIntensity.toFloat() / 100f },
                            modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (joyIntensity >= 50) Color(0xFF22C55E) else Color(0xFFEF4444)
                        )
                        Text(
                            text = when {
                                joyIntensity > 60 -> "Positive Vibe 😊 ($joyIntensity%)"
                                joyIntensity < 40 -> "Expressive / Serious Vibe 😢 ($joyIntensity%)"
                                else -> "Objective/Neutral Vibe 😐 ($joyIntensity%)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = pageOnColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showMetricsDialog = false }) {
                    Text("Close Panel")
                }
            }
        )
    }
}

// Action label Composable helper trigger
@Composable
fun TrayAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = desc,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EditorGroupTitle(title: String) {
    Text(
        text = title,
        fontSize = 8.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
fun MetricLabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
