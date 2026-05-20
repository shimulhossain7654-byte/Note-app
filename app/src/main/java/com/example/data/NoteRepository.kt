package com.example.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class NoteRepository(private val noteDao: NoteDao) {

    val allActiveNotes: Flow<List<Note>> = noteDao.getAllActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val uniqueFolders: Flow<List<String>> = noteDao.getUniqueFolders()

    fun getNoteById(id: Int): Flow<Note?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdDirect(id: Int): Note? = noteDao.getNoteByIdDirect(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteMultipleNotes(ids: List<Int>) = noteDao.deleteMultipleNotes(ids)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchAllNotes(query)

    // Export entire database as a highly polished, formatted JSON String
    suspend fun exportDatabaseAsJson(): String {
        val notes = noteDao.getAllNotesRaw()
        val jsonArray = JSONArray()
        for (note in notes) {
            val noteObj = JSONObject().apply {
                put("title", note.title)
                put("content", note.content)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
                put("isPinned", note.isPinned)
                put("folder", note.folder)
                put("tags", note.tags)
                put("colorHex", note.colorHex)
                put("isLocked", note.isLocked)
                put("notePinHash", note.notePinHash)
                put("reminderTime", note.reminderTime ?: JSONObject.NULL)
                put("wordCount", note.wordCount)
                put("charCount", note.charCount)
                put("isArchived", note.isArchived)
            }
            jsonArray.put(noteObj)
        }
        val envelope = JSONObject().apply {
            put("backupVersion", 1)
            put("timestamp", System.currentTimeMillis())
            put("appId", "secure_notes_offline")
            put("notes", jsonArray)
        }
        return envelope.toString(4) // pretty printed with indentation
    }

    // Import notes from a backup JSON string with automatic de-duplication
    suspend fun importDatabaseFromJson(jsonString: String): Boolean {
        return try {
            val envelope = JSONObject(jsonString)
            if (!envelope.has("notes")) return false
            val jsonArray = envelope.getJSONArray("notes")
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Untitled Note")
                val content = obj.optString("content", "")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val isPinned = obj.optBoolean("isPinned", false)
                val folder = obj.optString("folder", "Uncategorized")
                val tags = obj.optString("tags", "")
                val colorHex = obj.optString("colorHex", "#2D3748")
                val isLocked = obj.optBoolean("isLocked", false)
                val notePinHash = obj.optString("notePinHash", "")
                val reminderObj = obj.opt("reminderTime")
                val reminderTime = if (reminderObj != null && reminderObj != JSONObject.NULL) {
                    obj.optLong("reminderTime")
                } else {
                    null
                }
                val wordCount = obj.optInt("wordCount", 0)
                val charCount = obj.optInt("charCount", 0)
                val isArchived = obj.optBoolean("isArchived", false)

                val note = Note(
                    title = title,
                    content = content,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    isPinned = isPinned,
                    folder = folder,
                    tags = tags,
                    colorHex = colorHex,
                    isLocked = isLocked,
                    notePinHash = notePinHash,
                    reminderTime = reminderTime,
                    wordCount = wordCount,
                    charCount = charCount,
                    isArchived = isArchived
                )
                noteDao.insertNote(note)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
