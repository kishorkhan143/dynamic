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
import com.example.service.OverlayTrigger
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

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _isDynamicIslandExpanded = MutableStateFlow(false)
    val isDynamicIslandExpanded: StateFlow<Boolean> = _isDynamicIslandExpanded.asStateFlow()

    private val _isDynamicIslandEnabled = MutableStateFlow(true)
    val isDynamicIslandEnabled: StateFlow<Boolean> = _isDynamicIslandEnabled.asStateFlow()

    // Dynamic Island Tone (Lightness/Darkness): 0.0f (lighter frosted slate) to 1.0f (pitch black OLED)
    private val _islandDarkness = MutableStateFlow(repository.getIslandDarkness())
    val islandDarkness: StateFlow<Float> = _islandDarkness.asStateFlow()

    // Face ID Animation Velocity: 0.5x to 3.0x speed multiplier
    private val _animationVelocity = MutableStateFlow(repository.getAnimationVelocity())
    val animationVelocity: StateFlow<Float> = _animationVelocity.asStateFlow()

    // Dynamic Island Position Y Offset (Distance from top camera / status bar)
    private val _islandOffsetY = MutableStateFlow(repository.getIslandOffsetY())
    val islandOffsetY: StateFlow<Int> = _islandOffsetY.asStateFlow()

    // Dynamic Island Position X Offset (Horizontal alignment)
    private val _islandOffsetX = MutableStateFlow(repository.getIslandOffsetX())
    val islandOffsetX: StateFlow<Int> = _islandOffsetX.asStateFlow()

    // Dynamic Island Size Scaling (0.5x to 1.4x)
    private val _islandScale = MutableStateFlow(repository.getIslandScale())
    val islandScale: StateFlow<Float> = _islandScale.asStateFlow()

    // Green indicator light toggle
    private val _greenLightEnabled = MutableStateFlow(repository.getGreenLightEnabled())
    val greenLightEnabled: StateFlow<Boolean> = _greenLightEnabled.asStateFlow()

    private var scanSimulationJob: Job? = null

    fun updateIslandDarkness(darkness: Float) {
        val clamped = darkness.coerceIn(0.0f, 1.0f)
        _islandDarkness.value = clamped
        repository.saveIslandDarkness(clamped)
    }

    fun updateAnimationVelocity(velocity: Float) {
        val clamped = velocity.coerceIn(0.5f, 3.0f)
        _animationVelocity.value = clamped
        repository.saveAnimationVelocity(clamped)
    }

    fun updateIslandOffsetY(offsetY: Int) {
        val clamped = offsetY.coerceIn(0, 100)
        _islandOffsetY.value = clamped
        repository.saveIslandOffsetY(clamped)
    }

    fun updateIslandOffsetX(offsetX: Int) {
        val clamped = offsetX.coerceIn(-100, 100)
        _islandOffsetX.value = clamped
        repository.saveIslandOffsetX(clamped)
    }

    fun updateIslandScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 1.4f)
        _islandScale.value = clamped
        repository.saveIslandScale(clamped)
    }

    fun updateGreenLightEnabled(enabled: Boolean) {
        _greenLightEnabled.value = enabled
        repository.saveGreenLightEnabled(enabled)
    }

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
        val velocity = _animationVelocity.value.coerceIn(0.5f, 3.0f)
        if (data.status == RecognitionStatus.VERIFIED && _scanState.value != ScanState.SUCCESS) {
            _scanState.value = ScanState.SUCCESS
            _scanProgress.value = 1f
            _isDynamicIslandExpanded.value = true
            viewModelScope.launch {
                delay((1200L / velocity).toLong())
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

    fun triggerAppOpenUnlock() {
        OverlayTrigger.fire(OverlayTrigger.Source.APP_OPENED)
        scanSimulationJob?.cancel()
        scanSimulationJob = viewModelScope.launch {
            val velocity = _animationVelocity.value.coerceIn(0.5f, 3.0f)
            _isAppUnlocked.value = false
            _isDynamicIslandExpanded.value = true
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            val totalSteps = 28
            val stepDelay = ((35L) / velocity).toLong().coerceAtLeast(8L)

            for (i in 1..totalSteps) {
                delay(stepDelay)
                _scanProgress.value = (i.toFloat() / totalSteps)
            }

            // Morph into green checkmark
            _scanState.value = ScanState.SUCCESS
            delay((750L / velocity).toLong())

            // Unlock the application now that animation completed
            _isAppUnlocked.value = true

            // Smoothly retract the dynamic island
            delay((450L / velocity).toLong())
            _scanState.value = ScanState.IDLE
            _scanProgress.value = 0f
            _isDynamicIslandExpanded.value = false
        }
    }

    fun lockAndAuthenticate() {
        triggerAppOpenUnlock()
    }

    fun startScanSimulation() {
        OverlayTrigger.fire(OverlayTrigger.Source.MANUAL)
        scanSimulationJob?.cancel()
        scanSimulationJob = viewModelScope.launch {
            val velocity = _animationVelocity.value.coerceIn(0.5f, 3.0f)
            if (_isDynamicIslandEnabled.value) {
                _isDynamicIslandExpanded.value = true
            }
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            val totalSteps = 28
            val stepDelay = ((35L) / velocity).toLong().coerceAtLeast(8L)

            for (i in 1..totalSteps) {
                delay(stepDelay)
                _scanProgress.value = (i.toFloat() / totalSteps)
            }

            _scanState.value = ScanState.SUCCESS
            delay((850L / velocity).toLong())
            _scanState.value = ScanState.IDLE
            _scanProgress.value = 0f
            _isDynamicIslandExpanded.value = false
        }
    }

    fun triggerRetrySimulation() {
        scanSimulationJob?.cancel()
        scanSimulationJob = viewModelScope.launch {
            val velocity = _animationVelocity.value.coerceIn(0.5f, 3.0f)
            if (_isDynamicIslandEnabled.value) {
                _isDynamicIslandExpanded.value = true
            }
            _scanState.value = ScanState.SCANNING
            _scanProgress.value = 0f

            val stepDelay = ((35L) / velocity).toLong().coerceAtLeast(8L)
            for (i in 1..16) {
                delay(stepDelay)
                _scanProgress.value = (i.toFloat() / 28)
            }

            _scanState.value = ScanState.RETRY
            delay((1200L / velocity).toLong())
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
        _islandDarkness.value = repository.getIslandDarkness()
        _animationVelocity.value = repository.getAnimationVelocity()
        _islandOffsetY.value = repository.getIslandOffsetY()
        _islandOffsetX.value = repository.getIslandOffsetX()
        _islandScale.value = repository.getIslandScale()
        _greenLightEnabled.value = repository.getGreenLightEnabled()
    }
}
