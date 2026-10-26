package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Event bus singleton coordinating system-wide Face ID Dynamic Island overlay animations.
 * Drops any events that arrive while an animation is already in progress.
 * Also holds observable customization settings for island tone (lightness/darkness) and animation velocity.
 */
object OverlayTrigger {
    enum class Source {
        DEVICE_UNLOCK,
        APP_OPENED,
        MANUAL
    }

    private val _events = MutableSharedFlow<Source>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<Source> = _events.asSharedFlow()

    private val isPlaying = AtomicBoolean(false)

    // Island Darkness: 0.0f (lighter frosted slate) to 1.0f (pitch black OLED)
    private val _islandDarkness = MutableStateFlow(1.0f)
    val islandDarkness: StateFlow<Float> = _islandDarkness.asStateFlow()

    // Face ID Animation Velocity: 0.5x (slow) to 3.0x (rapid/instant)
    private val _animationVelocity = MutableStateFlow(1.0f)
    val animationVelocity: StateFlow<Float> = _animationVelocity.asStateFlow()

    // Vertical Position Y Offset: 0 to 100 dp (default 12 dp)
    private val _islandOffsetY = MutableStateFlow(12)
    val islandOffsetY: StateFlow<Int> = _islandOffsetY.asStateFlow()

    // Horizontal Position X Offset: -100 to 100 dp (default 0 dp)
    private val _islandOffsetX = MutableStateFlow(0)
    val islandOffsetX: StateFlow<Int> = _islandOffsetX.asStateFlow()

    // Dynamic Island Size Scale: 0.5f (compact mini) to 1.4f (expanded)
    private val _islandScale = MutableStateFlow(1.0f)
    val islandScale: StateFlow<Float> = _islandScale.asStateFlow()

    // Green Indicator Light: default true (persistent green dot on Dynamic Island)
    private val _greenLightEnabled = MutableStateFlow(true)
    val greenLightEnabled: StateFlow<Boolean> = _greenLightEnabled.asStateFlow()

    fun updateIslandDarkness(darkness: Float) {
        _islandDarkness.value = darkness.coerceIn(0.0f, 1.0f)
    }

    fun updateAnimationVelocity(velocity: Float) {
        _animationVelocity.value = velocity.coerceIn(0.5f, 3.0f)
    }

    fun updateIslandOffsetY(offsetY: Int) {
        _islandOffsetY.value = offsetY.coerceIn(0, 100)
    }

    fun updateIslandOffsetX(offsetX: Int) {
        _islandOffsetX.value = offsetX.coerceIn(-100, 100)
    }

    fun updateIslandScale(scale: Float) {
        _islandScale.value = scale.coerceIn(0.5f, 1.4f)
    }

    fun updateGreenLightEnabled(enabled: Boolean) {
        _greenLightEnabled.value = enabled
    }

    fun setPlaying(playing: Boolean) {
        isPlaying.set(playing)
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get()

    fun fire(source: Source): Boolean {
        if (isPlaying.get()) {
            return false
        }
        return _events.tryEmit(source)
    }
}
