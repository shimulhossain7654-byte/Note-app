package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: Int): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteByIdDirect(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteMultipleNotes(ids: List<Int>)

    @Query("SELECT * FROM notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR folder LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchAllNotes(query: String): Flow<List<Note>>

    @Query("SELECT DISTINCT folder FROM notes WHERE isArchived = 0")
    fun getUniqueFolders(): Flow<List<String>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesRaw(): List<Note> // Needed for robust backup generation
}
