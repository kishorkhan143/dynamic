package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.model.BackgroundMode
import com.example.model.ParticleDensity
import com.example.model.PresetCatalog
import com.example.model.StudioConfig
import com.example.service.OverlayTrigger

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_unlock_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_PRESET_ID = "active_preset_id"
        private const val KEY_DEFAULT_PRESET_ID = "default_preset_id"
        private const val KEY_FAVORITES = "favorite_preset_ids"
        private const val KEY_SPEED = "studio_speed"
        private const val KEY_GLOW = "studio_glow"
        private const val KEY_LINE_WIDTH = "studio_line_width"
        private const val KEY_ACCENT_COLOR = "studio_accent_color"
        private const val KEY_PARTICLE_DENSITY = "studio_particle_density"
        private const val KEY_BG_MODE = "studio_bg_mode"
        private const val KEY_CAMERA_PREVIEW_ENABLED = "camera_preview_enabled"
        private const val KEY_ISLAND_DARKNESS = "island_darkness"
        private const val KEY_ANIMATION_VELOCITY = "animation_velocity"
        private const val KEY_ISLAND_OFFSET_Y = "island_offset_y"
        private const val KEY_ISLAND_OFFSET_X = "island_offset_x"
        private const val KEY_ISLAND_SCALE = "island_scale"
        private const val KEY_GREEN_LIGHT_ENABLED = "green_light_enabled"
    }

    init {
        // Sync persisted values to OverlayTrigger singleton
        OverlayTrigger.updateIslandDarkness(getIslandDarkness())
        OverlayTrigger.updateAnimationVelocity(getAnimationVelocity())
        OverlayTrigger.updateIslandOffsetY(getIslandOffsetY())
        OverlayTrigger.updateIslandOffsetX(getIslandOffsetX())
        OverlayTrigger.updateIslandScale(getIslandScale())
        OverlayTrigger.updateGreenLightEnabled(getGreenLightEnabled())
    }

    fun getIslandOffsetY(): Int {
        return prefs.getInt(KEY_ISLAND_OFFSET_Y, 12)
    }

    fun saveIslandOffsetY(offsetY: Int) {
        val clamped = offsetY.coerceIn(0, 100)
        prefs.edit().putInt(KEY_ISLAND_OFFSET_Y, clamped).apply()
        OverlayTrigger.updateIslandOffsetY(clamped)
    }

    fun getIslandOffsetX(): Int {
        return prefs.getInt(KEY_ISLAND_OFFSET_X, 0)
    }

    fun saveIslandOffsetX(offsetX: Int) {
        val clamped = offsetX.coerceIn(-100, 100)
        prefs.edit().putInt(KEY_ISLAND_OFFSET_X, clamped).apply()
        OverlayTrigger.updateIslandOffsetX(clamped)
    }

    fun getIslandScale(): Float {
        return prefs.getFloat(KEY_ISLAND_SCALE, 1.0f)
    }

    fun saveIslandScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 1.4f)
        prefs.edit().putFloat(KEY_ISLAND_SCALE, clamped).apply()
        OverlayTrigger.updateIslandScale(clamped)
    }

    fun getGreenLightEnabled(): Boolean {
        // Default true: persistent green color dot light on the dynamic island
        return prefs.getBoolean(KEY_GREEN_LIGHT_ENABLED, true)
    }

    fun saveGreenLightEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GREEN_LIGHT_ENABLED, enabled).apply()
        OverlayTrigger.updateGreenLightEnabled(enabled)
    }

    fun getIslandDarkness(): Float {
        return prefs.getFloat(KEY_ISLAND_DARKNESS, 1.0f)
    }

    fun saveIslandDarkness(darkness: Float) {
        val clamped = darkness.coerceIn(0.0f, 1.0f)
        prefs.edit().putFloat(KEY_ISLAND_DARKNESS, clamped).apply()
        OverlayTrigger.updateIslandDarkness(clamped)
    }

    fun getAnimationVelocity(): Float {
        return prefs.getFloat(KEY_ANIMATION_VELOCITY, 1.0f)
    }

    fun saveAnimationVelocity(velocity: Float) {
        val clamped = velocity.coerceIn(0.5f, 3.0f)
        prefs.edit().putFloat(KEY_ANIMATION_VELOCITY, clamped).apply()
        OverlayTrigger.updateAnimationVelocity(clamped)
    }

    fun getActivePresetId(): String {
        return prefs.getString(KEY_ACTIVE_PRESET_ID, PresetCatalog.AQUA_PULSE.id) ?: PresetCatalog.AQUA_PULSE.id
    }

    fun saveActivePresetId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_PRESET_ID, id).apply()
    }

    fun getDefaultPresetId(): String {
        return prefs.getString(KEY_DEFAULT_PRESET_ID, PresetCatalog.AQUA_PULSE.id) ?: PresetCatalog.AQUA_PULSE.id
    }

    fun saveDefaultPresetId(id: String) {
        prefs.edit().putString(KEY_DEFAULT_PRESET_ID, id).apply()
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, setOf(PresetCatalog.AQUA_PULSE.id, PresetCatalog.NEURAL_SCAN.id)) ?: emptySet()
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun toggleFavorite(presetId: String): Set<String> {
        val current = getFavorites().toMutableSet()
        if (current.contains(presetId)) {
            current.remove(presetId)
        } else {
            current.add(presetId)
        }
        saveFavorites(current)
        return current
    }

    fun getCameraPreviewEnabled(): Boolean {
        return prefs.getBoolean(KEY_CAMERA_PREVIEW_ENABLED, false)
    }

    fun saveCameraPreviewEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CAMERA_PREVIEW_ENABLED, enabled).apply()
    }

    fun loadStudioConfig(): StudioConfig {
        val activePresetId = getActivePresetId()
        val preset = PresetCatalog.getById(activePresetId)
        val speed = prefs.getFloat(KEY_SPEED, preset.defaultSpeed)
        val glow = prefs.getFloat(KEY_GLOW, preset.defaultGlow)
        val lineWidth = prefs.getFloat(KEY_LINE_WIDTH, preset.defaultLineWidth)
        val densityName = prefs.getString(KEY_PARTICLE_DENSITY, preset.defaultDensity.name) ?: preset.defaultDensity.name
        val density = try { ParticleDensity.valueOf(densityName) } catch (_: Exception) { preset.defaultDensity }
        val bgModeName = prefs.getString(KEY_BG_MODE, BackgroundMode.SILHOUETTE.name) ?: BackgroundMode.SILHOUETTE.name
        val bgMode = try { BackgroundMode.valueOf(bgModeName) } catch (_: Exception) { BackgroundMode.SILHOUETTE }
        val colorInt = prefs.getInt(KEY_ACCENT_COLOR, preset.accentColor.toArgb())
        val color = Color(colorInt)

        return StudioConfig(
            preset = preset,
            accentColor = color,
            speed = speed,
            glowIntensity = glow,
            scanLineWidth = lineWidth,
            particleDensity = density,
            backgroundMode = bgMode,
            isPlaying = true
        )
    }

    fun saveStudioConfig(config: StudioConfig) {
        prefs.edit()
            .putString(KEY_ACTIVE_PRESET_ID, config.preset.id)
            .putFloat(KEY_SPEED, config.speed)
            .putFloat(KEY_GLOW, config.glowIntensity)
            .putFloat(KEY_LINE_WIDTH, config.scanLineWidth)
            .putInt(KEY_ACCENT_COLOR, config.accentColor.toArgb())
            .putString(KEY_PARTICLE_DENSITY, config.particleDensity.name)
            .putString(KEY_BG_MODE, config.backgroundMode.name)
            .apply()
    }

    fun resetAllSettings(): StudioConfig {
        prefs.edit().clear().apply()
        OverlayTrigger.updateIslandDarkness(1.0f)
        OverlayTrigger.updateAnimationVelocity(1.0f)
        OverlayTrigger.updateIslandOffsetY(12)
        OverlayTrigger.updateIslandOffsetX(0)
        OverlayTrigger.updateIslandScale(1.0f)
        OverlayTrigger.updateGreenLightEnabled(false)
        return loadStudioConfig()
    }
}
