package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.blackbox.BlackBoxCore
import com.example.blackbox.model.VirtualEngineStats
import com.example.data.database.CloneDatabase
import com.example.data.database.entity.CloneInstanceEntity
import com.example.data.repository.CloneRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class AppClonerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CloneDatabase.getInstance(application)
    private val repository = CloneRepository(application, db.cloneDao())

    val clones: StateFlow<List<CloneInstanceEntity>> = repository.allClones
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _engineStats = MutableStateFlow(repository.getEngineStats())
    val engineStats: StateFlow<VirtualEngineStats> = _engineStats.asStateFlow()

    private val _runningProcesses = MutableStateFlow<List<BlackBoxCore.RunningProcessInfo>>(emptyList())
    val runningProcesses: StateFlow<List<BlackBoxCore.RunningProcessInfo>> = _runningProcesses.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        _engineStats.value = repository.getEngineStats()
        _runningProcesses.value = BlackBoxCore.get().getRunningProcesses()
    }

    fun launchClone(context: Context, clone: CloneInstanceEntity) {
        viewModelScope.launch {
            val result = repository.launchClone(clone)
            if (result.isSuccess) {
                _toastEvents.emit("Launched ${clone.customName} in isolated sandbox")
            } else {
                _toastEvents.emit("Launch failed: ${result.exceptionOrNull()?.message}")
            }
            refreshStats()
        }
    }

    fun cloneAgain(clone: CloneInstanceEntity) {
        viewModelScope.launch {
            val result = repository.createClone(
                sourcePackage = clone.packageName,
                appName = clone.appName,
                isSystem = clone.isSystemApp,
                versionName = clone.versionName,
                versionCode = clone.versionCode,
                category = clone.category
            )
            if (result.isSuccess) {
                _toastEvents.emit("Created ${result.getOrNull()?.customName}")
            } else {
                _toastEvents.emit("Failed to create another clone: ${result.exceptionOrNull()?.message}")
            }
            refreshStats()
        }
    }

    fun renameClone(cloneId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameClone(cloneId, newName)
            _toastEvents.emit("Clone renamed to $newName")
        }
    }

    fun clearCache(clone: CloneInstanceEntity) {
        viewModelScope.launch {
            val ok = repository.clearCloneCache(clone)
            if (ok) {
                _toastEvents.emit("Cache cleared for ${clone.customName}")
            } else {
                _toastEvents.emit("Failed to clear cache")
            }
            refreshStats()
        }
    }

    fun clearData(clone: CloneInstanceEntity) {
        viewModelScope.launch {
            val ok = repository.clearCloneData(clone)
            if (ok) {
                _toastEvents.emit("Isolated data wiped for ${clone.customName}")
            } else {
                _toastEvents.emit("Failed to clear data")
            }
            refreshStats()
        }
    }

    fun deleteClone(clone: CloneInstanceEntity) {
        viewModelScope.launch {
            val ok = repository.deleteClone(clone)
            if (ok) {
                _toastEvents.emit("Deleted ${clone.customName}")
            }
            refreshStats()
        }
    }

    fun importApk(file: File) {
        viewModelScope.launch {
            val result = repository.createCloneFromApkFile(file)
            if (result.isSuccess) {
                _toastEvents.emit("Imported APK into virtual sandbox: ${result.getOrNull()?.appName}")
            } else {
                _toastEvents.emit("Import error: ${result.exceptionOrNull()?.message}")
            }
            refreshStats()
        }
    }
}
