package com.dissonance.wfarer.hydra.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM saved_scripts ORDER BY timestamp DESC")
    fun getAllScripts(): Flow<List<ScriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptEntity)

    @Delete
    suspend fun deleteScript(script: ScriptEntity)

    @Query("DELETE FROM saved_scripts WHERE id = :id")
    suspend fun deleteScriptById(id: Int)
}
