package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.CloneDatabase
import com.example.data.database.entity.InstalledAppEntity
import com.example.data.repository.AppDiscoveryRepository
import com.example.data.repository.CloneRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CloneDatabase.getInstance(application)
    private val discoveryRepo = AppDiscoveryRepository(application, db.installedAppDao())
    private val cloneRepo = CloneRepository(application, db.cloneDao())

    val allInstalledApps: StateFlow<List<InstalledAppEntity>> = discoveryRepo.installedApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _cloningPackage = MutableStateFlow<String?>(null)
    val cloningPackage: StateFlow<String?> = _cloningPackage.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val filteredApps: StateFlow<List<InstalledAppEntity>> = combine(
        allInstalledApps,
        searchQuery,
        selectedCategory
    ) { apps, query, category ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)

            val matchesCategory = when (category) {
                "Social & Messaging" -> app.category == "Social & Messaging"
                "Productivity" -> app.category == "Productivity"
                "User Apps" -> !app.isSystemApp
                "System Apps" -> app.isSystemApp
                else -> true
            }

            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        refreshApps()
    }

    fun refreshApps() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val res = discoveryRepo.refreshInstalledApps()
            _isRefreshing.value = false
            if (res.isFailure) {
                _messages.emit("Failed to discover apps: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleSelectPackage(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
    }

    fun selectAllVisible() {
        val currentVisible = filteredApps.value.map { it.packageName }.toSet()
        _selectedPackages.value = currentVisible
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
    }

    fun cloneApp(app: InstalledAppEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            _cloningPackage.value = app.packageName
            val result = cloneRepo.createClone(
                sourcePackage = app.packageName,
                appName = app.appName,
                isSystem = app.isSystemApp,
                versionName = app.versionName,
                versionCode = app.versionCode,
                category = app.category
            )
            _cloningPackage.value = null
            if (result.isSuccess) {
                _messages.emit("Cloned ${app.appName} successfully!")
                onComplete()
            } else {
                _messages.emit("Clone failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun cloneSelected(onComplete: () -> Unit) {
        val toClone = _selectedPackages.value.toList()
        if (toClone.isEmpty()) return

        viewModelScope.launch {
            val allAppsMap = allInstalledApps.value.associateBy { it.packageName }
            var successCount = 0
            for (pkg in toClone) {
                _cloningPackage.value = pkg
                val app = allAppsMap[pkg]
                if (app != null) {
                    val res = cloneRepo.createClone(
                        sourcePackage = app.packageName,
                        appName = app.appName,
                        isSystem = app.isSystemApp,
                        versionName = app.versionName,
                        versionCode = app.versionCode,
                        category = app.category
                    )
                    if (res.isSuccess) successCount++
                }
            }
            _cloningPackage.value = null
            _selectedPackages.value = emptySet()
            _messages.emit("Cloned $successCount of ${toClone.size} applications")
            onComplete()
        }
    }
}
