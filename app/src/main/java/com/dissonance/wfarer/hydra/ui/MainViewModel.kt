package com.dissonance.wfarer.hydra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dissonance.wfarer.hydra.db.ScriptEntity
import com.dissonance.wfarer.hydra.db.ScriptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ScriptRepository) : ViewModel() {

    val savedScripts: StateFlow<List<ScriptEntity>> = repository.allScripts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveScript(title: String, code: String) {
        viewModelScope.launch {
            repository.insert(ScriptEntity(title = title.trim(), code = code))
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            repository.delete(script)
        }
    }
}

class MainViewModelFactory(private val repository: ScriptRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
