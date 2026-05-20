package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NoteDatabase.getDatabase(application)
    private val repository = NoteRepository(db.noteDao())
    val preferenceManager = PreferenceManager(application)

    // Current app state values
    var currentTheme = mutableStateOf(preferenceManager.selectedTheme)
    var isGridLayout = mutableStateOf(preferenceManager.isGridLayout)
    var isAutoSaveEnabled = mutableStateOf(preferenceManager.isAutoSaveEnabled)
    var lastBackupTime = mutableStateOf(preferenceManager.lastBackupTimestamp)

    // App Security states
    var isAppLocked = mutableStateOf(preferenceManager.pinLockEnabled)
    val isPinSetup = preferenceManager.appSecurityPin.isNotEmpty()
    var pinErrorMsg = mutableStateOf("")

    // List and Dashboard states
    val allActiveNotes = repository.allActiveNotes
    val archivedNotes = repository.archivedNotes
    val uniqueFolders = repository.uniqueFolders

    // Search and Filter criteria
    val searchQuery = MutableStateFlow("")
    val selectedFolderFilter = MutableStateFlow("All")
    val selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter = MutableStateFlow<String?>(null)

    // Filtered lists emission
    val filteredNotesState: StateFlow<List<Note>> = combine(
        allActiveNotes,
        searchQuery,
        selectedFolderFilter,
        selectedTagFilter,
        selectedColorFilter
    ) { notes, query, folder, tag, color ->
        notes.filter { note ->
            val matchesQuery = query.isEmpty() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.tags.contains(query, ignoreCase = true)

            val matchesFolder = when (folder) {
                "All" -> true
                "Pinned" -> note.isPinned
                "Reminders" -> note.reminderTime != null
                else -> note.folder.equals(folder, ignoreCase = true)
            }

            val matchesTag = tag == null || note.tags.split(",").map { it.trim() }.contains(tag)
            val matchesColor = color == null || note.colorHex.equals(color, ignoreCase = true)

            matchesQuery && matchesFolder && matchesTag && matchesColor
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Note Editor Workspace state
    private val _editorNoteId = MutableStateFlow<Int?>(null)
    val editorNoteId: StateFlow<Int?> = _editorNoteId

    var edTitle = mutableStateOf("")
    var edContent = mutableStateOf("")
    var edFolder = mutableStateOf("Uncategorized")
    var edTags = mutableStateOf("")
    var edColorHex = mutableStateOf("#2D3748")
    var edIsPinned = mutableStateOf(false)
    var edIsLocked = mutableStateOf(false)
    var edIsArchived = mutableStateOf(false)
    var edReminderTime = mutableStateOf<Long?>(null)

    // Metrics/Statistics calculated live
    val edWordCount get() = calculateWordCount(edContent.value)
    val edCharCount get() = edContent.value.length
    val edReadingTimeMinutes get() = Math.max(1, edWordCount / 200) // Assumes standard average user reading rate

    // Undo / Redo engine stacks
    data class EditorHistoryState(val title: String, val content: String)
    private val undoStack = mutableListOf<EditorHistoryState>()
    private val redoStack = mutableListOf<EditorHistoryState>()

    // Set/Load target note into workspace
    fun loadNoteIntoEditor(noteId: Int?) {
        _editorNoteId.value = noteId
        undoStack.clear()
        redoStack.clear()
        if (noteId == null) {
            edTitle.value = ""
            edContent.value = ""
            edFolder.value = "Uncategorized"
            edTags.value = ""
            edColorHex.value = "#1E293B"
            edIsPinned.value = false
            edIsLocked.value = false
            edIsArchived.value = false
            edReminderTime.value = null
        } else {
            viewModelScope.launch {
                val note = repository.getNoteByIdDirect(noteId)
                if (note != null) {
                    edTitle.value = note.title
                    edContent.value = note.content
                    edFolder.value = note.folder
                    edTags.value = note.tags
                    edColorHex.value = note.colorHex
                    edIsPinned.value = note.isPinned
                    edIsLocked.value = note.isLocked
                    edIsArchived.value = note.isArchived
                    edReminderTime.value = note.reminderTime
                    
                    // Push initial state to history (as starting point)
                    undoStack.add(EditorHistoryState(note.title, note.content))
                }
            }
        }
    }

    // Capture editing history states to power undo/redo
    fun onEditorChange(title: String, content: String) {
        val oldTitle = edTitle.value
        val oldContent = edContent.value
        edTitle.value = title
        edContent.value = content

        // To keep editing history light, we save checkpoints on word word additions or deletion boundary
        val contentWordsDiff = Math.abs(calculateWordCount(oldContent) - calculateWordCount(content))
        if (contentWordsDiff > 0 || oldTitle != title) {
            // Push previous state onto Undo Stack, cap stack size to 50 items for memory efficiency
            if (undoStack.isEmpty() || undoStack.last().content != oldContent || undoStack.last().title != oldTitle) {
                undoStack.add(EditorHistoryState(oldTitle, oldContent))
                if (undoStack.size > 50) {
                    undoStack.removeAt(0)
                }
                // When new modifications are typed, Redo history is cleared
                redoStack.clear()
            }
        }
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val prevState = undoStack.removeAt(undoStack.lastIndex)
            // Push the current state to Redo Stack
            redoStack.add(EditorHistoryState(edTitle.value, edContent.value))
            
            // Set current text states (avoiding generating recursive change checkpoints)
            edTitle.value = prevState.title
            edContent.value = prevState.content
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            // Push current to Undo Stack
            undoStack.add(EditorHistoryState(edTitle.value, edContent.value))

            // Set current text states
            edTitle.value = nextState.title
            edContent.value = nextState.content
        }
    }

    // Save note inside loaded workspace database
    fun saveNote(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val titleText = edTitle.value.ifBlank { "Untitled Note" }
            val now = System.currentTimeMillis()

            val currentNoteId = _editorNoteId.value
            if (currentNoteId == null) {
                // Insert New Note
                val newNote = Note(
                    title = titleText,
                    content = edContent.value,
                    createdAt = now,
                    updatedAt = now,
                    isPinned = edIsPinned.value,
                    folder = edFolder.value,
                    tags = edTags.value,
                    colorHex = edColorHex.value,
                    isLocked = edIsLocked.value,
                    wordCount = edWordCount,
                    charCount = edCharCount,
                    isArchived = edIsArchived.value,
                    reminderTime = edReminderTime.value
                )
                val newId = repository.insertNote(newNote)
                _editorNoteId.value = newId.toInt()
            } else {
                // Update Existing Note
                val existingNote = repository.getNoteByIdDirect(currentNoteId)
                if (existingNote != null) {
                    val updatedNote = existingNote.copy(
                        title = titleText,
                        content = edContent.value,
                        updatedAt = now,
                        isPinned = edIsPinned.value,
                        folder = edFolder.value,
                        tags = edTags.value,
                        colorHex = edColorHex.value,
                        isLocked = edIsLocked.value,
                        wordCount = edWordCount,
                        charCount = edCharCount,
                        isArchived = edIsArchived.value,
                        reminderTime = edReminderTime.value
                    )
                    repository.updateNote(updatedNote)
                }
            }
            onComplete()
        }
    }

    fun deleteCurrentNote(onComplete: () -> Unit) {
        val currentNoteId = _editorNoteId.value
        if (currentNoteId != null) {
            viewModelScope.launch {
                val currentNote = repository.getNoteByIdDirect(currentNoteId)
                if (currentNote != null) {
                    repository.deleteNote(currentNote)
                    onComplete()
                }
            }
        } else {
            onComplete()
        }
    }

    // Bulk deletion helper for multiple selection
    fun deleteNotesBulk(noteIds: List<Int>) {
        viewModelScope.launch {
            repository.deleteMultipleNotes(noteIds)
        }
    }

    // App Pin Security Actions
    fun unlockApp(pin: String): Boolean {
        return if (preferenceManager.verifyPin(pin)) {
            isAppLocked.value = false
            pinErrorMsg.value = ""
            true
        } else {
            pinErrorMsg.value = "Incorrect security PIN. Please try again."
            false
        }
    }

    fun setAppSecurityPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            preferenceManager.appSecurityPin = newPin
            preferenceManager.pinLockEnabled = true
            isAppLocked.value = false
        }
    }

    fun disableAppSecurityPin() {
        preferenceManager.clearPin()
        isAppLocked.value = false
    }

    // UI Configuration selectors
    fun changeTheme(themeName: String) {
        preferenceManager.selectedTheme = themeName
        currentTheme.value = themeName
    }

    fun toggleGridAndList() {
        val next = !isGridLayout.value
        preferenceManager.isGridLayout = next
        isGridLayout.value = next
    }

    fun toggleAutoSave() {
        val next = !isAutoSaveEnabled.value
        preferenceManager.isAutoSaveEnabled = next
        isAutoSaveEnabled.value = next
    }

    // Export Notes JSON to a system URI (Storage Access Framework synchronization target)
    fun backupNotesToUri(context: Context, targetUri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(targetUri)?.use { outputStream: OutputStream ->
                val backupData = kotlinx.coroutines.runBlocking { repository.exportDatabaseAsJson() }
                outputStream.write(backupData.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            val now = System.currentTimeMillis()
            preferenceManager.lastBackupTimestamp = now
            lastBackupTime.value = now
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Import Notes JSON from selected storage URI file
    fun restoreNotesFromUri(context: Context, sourceUri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    kotlinx.coroutines.runBlocking {
                        repository.importDatabaseFromJson(sb.toString())
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Auxiliary offline word counter helper class
    private fun calculateWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).size
    }
}
