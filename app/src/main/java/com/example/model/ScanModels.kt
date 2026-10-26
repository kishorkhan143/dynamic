package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraNeonViolet
import com.example.ui.theme.AuraCyberCyan
import com.example.ui.theme.AuraMatrixAmber
import com.example.ui.theme.AuraCrimsonCore
import com.example.ui.theme.AuraCoolWhite

enum class ScanState {
    IDLE,
    SCANNING,
    SUCCESS,
    RETRY,
    ERROR
}

enum class BackgroundMode {
    SILHOUETTE,
    CAMERA
}

enum class ParticleDensity(val label: String, val count: Int) {
    LOW("Low", 20),
    MEDIUM("Med", 45),
    HIGH("Dense", 80)
}

data class ScanPreset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val accentColor: Color,
    val defaultSpeed: Float = 1.0f,
    val defaultGlow: Float = 0.7f,
    val defaultLineWidth: Float = 2.5f,
    val defaultDensity: ParticleDensity = ParticleDensity.MEDIUM
)

object PresetCatalog {
    val AQUA_PULSE = ScanPreset(
        id = "aqua_pulse",
        name = "Aqua Pulse",
        category = "Cyber HUD",
        description = "Expanding sonar ripples from nasal bridge with high-intensity aqua laser scanline",
        accentColor = AuraElectricAqua,
        defaultSpeed = 1.0f,
        defaultGlow = 0.8f,
        defaultLineWidth = 2.5f,
        defaultDensity = ParticleDensity.MEDIUM
    )

    val NEURAL_SCAN = ScanPreset(
        id = "neural_scan",
        name = "Neural Scan",
        category = "Synaptic",
        description = "Animated biometric nodes with pulsing neural axons and landmark targeting clusters",
        accentColor = AuraNeonViolet,
        defaultSpeed = 1.2f,
        defaultGlow = 0.75f,
        defaultLineWidth = 2.0f,
        defaultDensity = ParticleDensity.HIGH
    )

    val HOLOGRAPHIC_FACE = ScanPreset(
        id = "holographic_face",
        name = "Holographic Face",
        category = "Hologram",
        description = "3D depth contour lines with chromatic shifting telemetry and dimensional scan slicing",
        accentColor = AuraCyberCyan,
        defaultSpeed = 0.9f,
        defaultGlow = 0.85f,
        defaultLineWidth = 2.2f,
        defaultDensity = ParticleDensity.MEDIUM
    )

    val CYBER_GRID = ScanPreset(
        id = "cyber_grid",
        name = "Cyber Grid",
        category = "Cyber HUD",
        description = "Isometric coordinate laser grid traversing biometric anchors with telemetry readouts",
        accentColor = AuraElectricAqua,
        defaultSpeed = 1.1f,
        defaultGlow = 0.65f,
        defaultLineWidth = 1.8f,
        defaultDensity = ParticleDensity.LOW
    )

    val MINIMAL_ORBIT = ScanPreset(
        id = "minimal_orbit",
        name = "Minimal Orbit",
        category = "Minimal",
        description = "Dual counter-rotating reticles with corner brackets and clean target-lock indicator",
        accentColor = AuraCoolWhite,
        defaultSpeed = 1.0f,
        defaultGlow = 0.5f,
        defaultLineWidth = 2.0f,
        defaultDensity = ParticleDensity.LOW
    )

    val NEON_CONTOUR = ScanPreset(
        id = "neon_contour",
        name = "Neon Contour",
        category = "Vibrant",
        description = "Fluid continuous neon stroke progressively illuminating facial silhouette boundaries",
        accentColor = AuraCrimsonCore,
        defaultSpeed = 1.3f,
        defaultGlow = 0.9f,
        defaultLineWidth = 3.5f,
        defaultDensity = ParticleDensity.MEDIUM
    )

    val DIGITAL_PARTICLES = ScanPreset(
        id = "digital_particles",
        name = "Digital Particles",
        category = "Quantum",
        description = "Floating cyber-dust floating upwards and crystallizing through an active scan plane",
        accentColor = AuraMatrixAmber,
        defaultSpeed = 0.8f,
        defaultGlow = 0.8f,
        defaultLineWidth = 2.0f,
        defaultDensity = ParticleDensity.HIGH
    )

    val AURORA_SCAN = ScanPreset(
        id = "aurora_scan",
        name = "Aurora Scan",
        category = "Spectral",
        description = "Multi-spectral gradient wave sweeping across facial landmarks with celestial aura",
        accentColor = AuraElectricAqua,
        defaultSpeed = 0.95f,
        defaultGlow = 0.85f,
        defaultLineWidth = 3.0f,
        defaultDensity = ParticleDensity.MEDIUM
    )

    val ALL_PRESETS = listOf(
        AQUA_PULSE,
        NEURAL_SCAN,
        HOLOGRAPHIC_FACE,
        CYBER_GRID,
        MINIMAL_ORBIT,
        NEON_CONTOUR,
        DIGITAL_PARTICLES,
        AURORA_SCAN
    )

    fun getById(id: String): ScanPreset {
        return ALL_PRESETS.find { it.id == id } ?: AQUA_PULSE
    }
}

data class StudioConfig(
    val preset: ScanPreset = PresetCatalog.AQUA_PULSE,
    val accentColor: Color = PresetCatalog.AQUA_PULSE.accentColor,
    val speed: Float = 1.0f,
    val glowIntensity: Float = 0.75f,
    val scanLineWidth: Float = 2.5f,
    val particleDensity: ParticleDensity = ParticleDensity.MEDIUM,
    val backgroundMode: BackgroundMode = BackgroundMode.SILHOUETTE,
    val isPlaying: Boolean = true,
    val scanState: ScanState = ScanState.IDLE
)
