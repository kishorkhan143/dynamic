package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.detection.RealtimeFaceData
import com.example.detection.RecognitionStatus
import com.example.model.BackgroundMode
import com.example.model.ParticleDensity
import com.example.model.PresetCatalog
import com.example.model.ScanPreset
import com.example.model.ScanState
import com.example.model.StudioConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuraUnlockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application.applicationContext)

    private val _studioConfig = MutableStateFlow(repository.loadStudioConfig())
    val studioConfig: StateFlow<StudioConfig> = _studioConfig.asStateFlow()

    private val _realtimeFaceData = MutableStateFlow(RealtimeFaceData())
    val realtimeFaceData: StateFlow<RealtimeFaceData> = _realtimeFaceData.asStateFlow()

    private val _favorites = MutableStateFlow(repository.getFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    private val _showFullScreenPreview = MutableStateFlow(false)
    val showFullScreenPreview: StateFlow<Boolean> = _showFullScreenPreview.asStateFlow()

    private val _isDynamicIslandExpanded = MutableStateFlow(false)
    val isDynamicIslandExpanded: StateFlow<Boolean> = _isDynamicIslandExpanded.asStateFlow()

    private val _isDynamicIslandEnabled = MutableStateFlow(true)
    val isDynamicIslandEnabled: StateFlow<Boolean> = _isDynamicIslandEnabled.asStateFlow()

    private var scanSimulationJob: Job? = null

    fun selectPreset(preset: ScanPreset) {
        val current = _studioConfig.value
        val updated = current.copy(
            preset = preset,
            accentColor = preset.accentColor,
            speed = preset.defaultSpeed,
            glowIntensity = preset.defaultGlow,
            scanLineWidth = preset.defaultLineWidth,
            particleDensity = preset.defaultDensity
        )
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun toggleFavorite(presetId: String) {
        val updated = repository.toggleFavorite(presetId)
        _favorites.value = updated
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun updateSpeed(speed: Float) {
        val updated = _studioConfig.value.copy(speed = speed)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun updateGlow(glow: Float) {
        val updated = _studioConfig.value.copy(glowIntensity = glow)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun updateLineWidth(width: Float) {
        val updated = _studioConfig.value.copy(scanLineWidth = width)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun updateAccentColor(color: Color) {
        val updated = _studioConfig.value.copy(accentColor = color)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun updateParticleDensity(density: ParticleDensity) {
        val updated = _studioConfig.value.copy(particleDensity = density)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun updateBackgroundMode(mode: BackgroundMode) {
        val updated = _studioConfig.value.copy(backgroundMode = mode)
        _studioConfig.value = updated
        repository.saveStudioConfig(updated)
    }

    fun togglePlayPause() {
        val updated = _studioConfig.value.copy(isPlaying = !_studioConfig.value.isPlaying)
        _studioConfig.value = updated
    }

    fun setCameraPermission(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun openFullScreenPreview() {
        _showFullScreenPreview.value = true
        startScanSimulation()
    }

    fun closeFullScreenPreview() {
        _showFullScreenPreview.value = false
        _scanState.value = ScanState.IDLE
        _scanProgress.value = 0f
        scanSimulationJob?.cancel()
    }

    fun setDynamicIslandExpanded(expanded: Boolean) {
        _isDynamicIslandExpanded.value = expanded
    }

    fun toggleDynamicIslandExpanded() {
        _isDynamicIslandExpanded.value = !_isDynamicIslandExpanded.value
    }

    fun setDynamicIslandEnabled(enabled: Boolean) {
        _isDynamicIslandEnabled.value = enabled
    }

    fun updateRealtimeFaceData(data: RealtimeFaceData) {
        _realtimeFaceData.value = data

        // Synchronize animation states seamlessly with real-time face detection
        if (data.status == RecognitionStatus.VERIFIED && _scanState.value != ScanState.SUCCESS) {
            _scanState.value = ScanState.SUCCESS
            _scanProgress.value = 1f
            _isDynamicIslandExpanded.value = true
            viewModelScope.launch {
                delay(1500)
                _scanState.value = ScanState.IDLE
                _scanProgress.value = 0f
                _isDynamicIslandExpanded.value = false
            }
        } else if (data.status == RecognitionStatus.TRACKING || data.status == RecognitionStatus.FACE_ACQUIRED) {
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = data.lockProgress
            _isDynamicIslandExpanded.value = true
        } else if (data.status == RecognitionStatus.LOST && _scanState.value == ScanState.SCANNING) {
            _scanState.value = ScanState.RETRY
        } else if (data.status == RecognitionStatus.SEARCHING && _scanState.value == ScanState.RETRY) {
            _scanState.value = ScanState.IDLE
            _scanProgress.value = 0f
            _isDynamicIslandExpanded.value = false
        }
    }

    fun startScanSimulation() {
        scanSimulationJob?.cancel()
        scanSimulationJob = viewModelScope.launch {
            if (_isDynamicIslandEnabled.value) {
                _isDynamicIslandExpanded.value = true
            }
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            val totalSteps = 40
            val stepDelay = (2000L / totalSteps)

            for (i in 1..totalSteps) {
                delay(stepDelay)
                _scanProgress.value = (i.toFloat() / totalSteps)
            }

            _scanState.value = ScanState.SUCCESS
            delay(1200)
            _scanState.value = ScanState.IDLE
            _scanProgress.value = 0f
            _isDynamicIslandExpanded.value = false
        }
    }

    fun triggerRetrySimulation() {
        scanSimulationJob?.cancel()
        scanSimulationJob = viewModelScope.launch {
            if (_isDynamicIslandEnabled.value) {
                _isDynamicIslandExpanded.value = true
            }
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            for (i in 1..20) {
                delay(40)
                _scanProgress.value = (i.toFloat() / 40)
            }

            _scanState.value = ScanState.RETRY
            delay(1500)
            _scanState.value = ScanState.IDLE
            _scanProgress.value = 0f
            _isDynamicIslandExpanded.value = false
        }
    }

    fun resetStudioDefaults() {
        val preset = _studioConfig.value.preset
        val reset = StudioConfig(
            preset = preset,
            accentColor = preset.accentColor,
            speed = preset.defaultSpeed,
            glowIntensity = preset.defaultGlow,
            scanLineWidth = preset.defaultLineWidth,
            particleDensity = preset.defaultDensity,
            backgroundMode = BackgroundMode.SILHOUETTE,
            isPlaying = true
        )
        _studioConfig.value = reset
        repository.saveStudioConfig(reset)
    }

    fun resetAllSettings() {
        val restored = repository.resetAllSettings()
        _studioConfig.value = restored
        _favorites.value = repository.getFavorites()
        _scanState.value = ScanState.IDLE
        _scanProgress.value = 0f
    }
}
