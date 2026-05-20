package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val folder: String = "Uncategorized", // Folder categorizer
    val tags: String = "",               // Comma-separated tags (e.g. "Work,Idea")
    val colorHex: String = "#2D3748",    // Default charcoal hex
    val isLocked: Boolean = false,       // Locked note indicator
    val notePinHash: String = "",        // Custom secure digest for note-level lock
    val reminderTime: Long? = null,      // Reminder/Alert Unix-timestamp
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val isArchived: Boolean = false      // Archive indicator
) : Serializable
