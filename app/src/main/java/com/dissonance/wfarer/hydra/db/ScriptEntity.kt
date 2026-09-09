package com.dissonance.wfarer.hydra.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)
