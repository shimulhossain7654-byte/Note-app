package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import com.example.ui.viewmodel.NotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Int?) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val notes by viewModel.filteredNotesState.collectAsState()
    val folders by viewModel.uniqueFolders.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()

    val selectedFolder by viewModel.selectedFolderFilter.collectAsState()
    val selectedTag by viewModel.selectedTagFilter.collectAsState()
    val selectedColor by viewModel.selectedColorFilter.collectAsState()

    val isGridLayout by viewModel.isGridLayout

    // Multi-selection management state
    val selectedNoteIds = remember { mutableStateListOf<Int>() }
    val isMultiSelectMode = selectedNoteIds.isNotEmpty()

    // Aggregate tags across all notes to build a dynamic tag cloud filter
    val allTags = remember(notes) {
        notes.flatMap { it.tags.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    // Advanced search parameters drawer
    var showFilterSheet by remember { mutableStateOf(false) }

    // Colors list to pick filters from
    val filterColors = listOf(
        "#2D3748" to "Charcoal",
        "#B91C1C" to "Crimson",
        "#15803D" to "Forest",
        "#1D4ED8" to "Royal Blue",
        "#7C3AED" to "Violet",
        "#B45309" to "Burnt Amber",
        "#D97706" to "Copper"
    )

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                // Multi-select actions bar
                TopAppBar(
                    title = { Text("${selectedNoteIds.size} selected", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { selectedNoteIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.deleteNotesBulk(selectedNoteIds.toList())
                                selectedNoteIds.clear()
                            },
                            modifier = Modifier.testTag("bulk_delete_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Bulk Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                )
            } else {
                // Standard app header
                TopAppBar(
                    title = {
                        Text(
                            "Secure Notes",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("app_title")
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleGridAndList() }) {
                            Icon(
                                imageVector = if (isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle Grid/List Views"
                            )
                        }
                        IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_button")) {
                            Icon(Icons.Default.Settings, contentDescription = "App Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("New Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_note_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Rounded search bar element
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_field"),
                placeholder = { Text("Search title, content, folders...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    } else {
                        IconButton(onClick = { showFilterSheet = !showFilterSheet }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = if (selectedColor != null || selectedTag != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                singleLine = true
            )

            // Live filter drawer sheet (expandable top view)
            AnimatedVisibility(
                visible = showFilterSheet,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Filters & Presets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Tags list selection
                        if (allTags.isNotEmpty()) {
                            Text("Filter by Tag", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(allTags) { tag ->
                                    val isSelected = selectedTag == tag
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectedTagFilter.value = if (isSelected) null else tag
                                        },
                                        label = { Text("#$tag", fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Colors list selection
                        Text("Filter by Note Color", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(filterColors) { (hex, name) ->
                                val isSelected = selectedColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.selectedColorFilter.value = if (isSelected) null else hex
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Reset button
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                viewModel.selectedTagFilter.value = null
                                viewModel.selectedColorFilter.value = null
                                showFilterSheet = false
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear All Filters", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Quick Horizontal Folder Selector Tabs
            val displayFolders = listOf("All", "Pinned", "Reminders") + folders.filter { it != "Uncategorized" && it.isNotEmpty() }
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(displayFolders) { folder ->
                    val isSelected = selectedFolder == folder
                    InputChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedFolderFilter.value = folder },
                        label = { Text(folder) },
                        leadingIcon = {
                            val icon = when (folder) {
                                "All" -> Icons.Default.FolderOpen
                                "Pinned" -> Icons.Default.PushPin
                                "Reminders" -> Icons.Default.NotificationsActive
                                else -> Icons.Default.Folder
                            }
                            Icon(icon, contentDescription = folder, modifier = Modifier.size(16.dp))
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Notes representation Canvas Staggered Grid/List
            if (notes.isEmpty()) {
                EmptyStateLayout(hasFilters = searchQuery.isNotEmpty() || selectedTag != null || selectedColor != null)
            } else {
                LazyVerticalStaggeredGrid(
                    columns = if (isGridLayout) StaggeredGridCells.Fixed(2) else StaggeredGridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .testTag("notes_grid"),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        val isSelected = selectedNoteIds.contains(note.id)
                        NoteCard(
                            note = note,
                            isSelected = isSelected,
                            isSelectionMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) selectedNoteIds.remove(note.id) else selectedNoteIds.add(note.id)
                                } else {
                                    onNavigateToEditor(note.id)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    selectedNoteIds.add(note.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val noteColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (e: Exception) {
            Color(0xFF2D3748) // Default charcoal fallback
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeString = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else noteColor.copy(alpha = 0.15f)
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
        } else {
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(noteColor.copy(alpha = 0.4f)))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Folder marker badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(noteColor.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = note.folder,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Security locks & Pin Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (note.isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "PIN Secured Note",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note title
            Text(
                text = if (note.isLocked) "••••••••••" else note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Note body snippet content
            Text(
                text = if (note.isLocked) "This note is encrypted and secured behind your local applications access PIN." else note.content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Note metadata footer tag list and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Word count badge metrics
                Text(
                    text = "${note.wordCount} words",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Custom canvas-drawn illustration empty note list screen state helper
@Composable
fun EmptyStateLayout(hasFilters: Boolean) {
    val animPrimaryColor = MaterialTheme.colorScheme.primary
    val animSecondaryColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp)
        ) {
            // Draw a beautiful canvas aesthetic notebook + pencil outline
            val w = size.width
            val h = size.height

            // Notebook body
            drawRoundRect(
                color = animSecondaryColor,
                topLeft = Offset(w * 0.25f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Rings on the side
            for (i in 0..4) {
                val ringY = h * 0.25f + i * (h * 0.12f)
                drawLine(
                    color = animPrimaryColor,
                    start = Offset(w * 0.20f, ringY),
                    end = Offset(w * 0.28f, ringY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Pencil drawing lines
            drawLine(
                color = animPrimaryColor,
                start = Offset(w * 0.35f, h * 0.40f),
                end = Offset(w * 0.65f, h * 0.40f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = animPrimaryColor,
                start = Offset(w * 0.35f, h * 0.52f),
                end = Offset(w * 0.60f, h * 0.52f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = animPrimaryColor,
                start = Offset(w * 0.35f, h * 0.64f),
                end = Offset(w * 0.50f, h * 0.64f),
                strokeWidth = 2.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (hasFilters) "No notes match your filters" else "No notes found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (hasFilters) "Try adjusting some search terms or resetting active tag and color chips." else "Write your first brainstorm, shopping guide, checklist, or secure encrypted markdown document offline!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
