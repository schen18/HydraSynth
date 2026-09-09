package com.dissonance.wfarer.hydra.db

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<ScriptEntity>> = scriptDao.getAllScripts()

    suspend fun insert(script: ScriptEntity) = scriptDao.insertScript(script)

    suspend fun delete(script: ScriptEntity) = scriptDao.deleteScript(script)

    suspend fun deleteById(id: Int) = scriptDao.deleteScriptById(id)
}
