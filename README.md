# Aura Unlock — Futuristic Android Face Scan Animation App

Aura Unlock delivers a futuristic, iPhone-inspired face-scanning animation suite and customization studio for Android. Built natively in Jetpack Compose with hardware-accelerated 60 FPS Canvas rendering and optional CameraX live optical overlay, the app enables users to explore, customize, and preview 8 cyber biometric HUD scan styles.

---

## ⚡ Core Features

1. **8 Biometric HUD Animation Presets**:
   - **Aqua Pulse**: Expanding sonar ripples from nasal bridge, bright electric aqua scanline with glowing vertical trail.
   - **Neural Scan**: Animated node network connecting biometric landmarks (eyes, jaw, nose, forehead) with pulsing synaptic currents.
   - **Holographic Face**: 3D wireframe mesh with shifting holographic chromatic aberration and depth slicing.
   - **Cyber Grid**: Isometric laser coordinate grid traversing biometric anchors with telemetry readouts.
   - **Minimal Orbit**: Dual concentric counter-rotating reticles with corner brackets and clean target-lock indicator.
   - **Neon Contour**: Fluid continuous neon stroke progressively illuminating facial silhouette boundaries.
   - **Digital Particles**: Disintegrating and re-assembling cyber-dust floating upwards through a scanning plane.
   - **Aurora Scan**: Multi-spectral gradient wave sweeping across facial landmarks with celestial aura.

2. **Dual-Mode Viewport**:
   - **Stylized Biometric Silhouette**: 17-point anatomical facial landmark wireframe with triangulation mesh and ocular reticles.
   - **Live Front Camera Feed**: Smooth CameraX front-camera integration with dark vignette gradient and HUD overlay.

3. **iPhone-Style Dynamic Island (Permanent Camera Cutout & System Overlay)**:
   - **Elongated Capsule Proportions (`160 × 36 dp`)**: Restores the authentic iPhone-style pill silhouette spanning across the front camera area, featuring a subtle horizontal glass rim light reflection along its length.
   - **Resting Visual Balance**: Inside the elongated black capsule sits a minimalist Face ID wireframe glyph on the left, an optical light indicator bar in the center, and a dual-element camera lens module on the right.
   - **Spring Expansion to Face ID Square**: Morphs smoothly using iOS bouncy spring physics into an `82 × 82 dp` square featuring the classic Apple Face ID wireframe with an animated sweeping laser beam.
   - **Verification Tick (`✓`) & Smooth Collapse**: Morphs into the animated emerald green checkmark upon completion, holds for ~1.2s, and smoothly shrinks back into the resting camera notch capsule.
   - **System-Wide Overlay When App Is Closed**: Built with Android's `SYSTEM_ALERT_WINDOW` and `DynamicIslandOverlayService`, allowing the Dynamic Island to optionally float at the top of the Android screen even when the user minimizes or closes the app.

4. **Real-Time Calibration Studio**:
   - Interactive live adjustments for **Scan Velocity** (0.5x to 2.5x), **Glow Intensity** (20% to 100%), **Line Thickness** (1.0dp to 6.0dp), and **Particle Density** (Low, Med, Dense).
   - Cyber color picker: Electric Aqua, Neon Violet, Cyber Cyan, Matrix Amber, Crimson Core, and Cool Ice White.
   - Transport controls: Play, Pause, Simulated Success Trigger, Re-align / Retry Simulation, and Reset to Defaults.

4. **Full-Screen Simulation Mode**:
   - Immersive lock-screen style demonstration featuring animated padlock transitions, progress telemetry, and simulated verification status.

5. **Privacy & On-Device Security**:
   - **Simulation Only**: Aura Unlock is a visual animation simulation. It does not perform actual biometric facial recognition or identity verification.
   - **Volatile Processing**: Camera frames are processed strictly in GPU memory for real-time display and are never stored, cached, or transmitted.
   - **Zero Cloud Accounts**: All presets, favorites, and studio configurations persist locally on-device.

---

## 🛡️ Android Native Face-Unlock Limitations

- **Hardware Isolation (TEE)**: Android executes lock-screen biometric authentication inside an isolated Trusted Execution Environment (TEE) hardware enclave. Third-party applications cannot hook into, modify, or replace native system lock-screen animations.
- **Overlay Restrictions**: While floating system-alert overlays can be displayed over normal applications with specialized user permission, Android security intentionally prevents overlays from rendering on top of the secure system lock screen.
- **Biometric Prompt API**: Standard Android BiometricPrompt APIs only display system-provided biometric dialogs; customizing or replacing this dialog with custom animations is prohibited by Android security guidelines.

---

## 🧪 Testing Checklist

- [x] **App Launch & Edge-to-Edge**: App launches with obsidian background `#050608` and Orbitron typography.
- [x] **Bottom Navigation**: Seamless switching between Home, Animations, Studio, and Settings tabs with Android back gesture handling.
- [x] **8 Presets**: Each preset displays distinct live miniature animations in the gallery and updates the studio canvas immediately.
- [x] **Studio Customization**: Sliders for speed, glow, line width, and color react instantly without frame drops.
- [x] **Camera Toggle**: Graceful fallback to vector silhouette when camera permission is denied or unavailable; live front preview when granted.
- [x] **Scan Simulation**: Progress advances from 0% to 100%, displaying simulated "VERIFIED" state.
- [x] **Persistence**: Active preset and favorites persist across app restarts via `SettingsRepository`.
