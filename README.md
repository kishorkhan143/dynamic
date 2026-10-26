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

3. **System-Wide Face ID Dynamic Island Overlay**:
   - **Dual Presence**: Permanent top-center capsule inside Aura Unlock, and an optional system-wide floating overlay (`DynamicIslandOverlayService`) outside the app.
   - **Hidden While Idle**: When running outside the app, the floating overlay stays completely hidden (`View.GONE`) and non-intrusive.
   - **Touch-Through Window**: Configured with `FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE` so touches seamlessly pass through to whatever app is running underneath.
   - **Automatic System Triggers**:
     1. **Device Unlock**: Automatically plays when the phone is unlocked via a runtime `BroadcastReceiver` listening for `Intent.ACTION_USER_PRESENT`.
     2. **App-Open Detection**: Plays whenever the user launches another third-party app via `AppOpenAccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`).
     3. **Manual Preview**: Testable immediately from the Settings screen via "PREVIEW SYSTEM OVERLAY".
   - **Precise Timing Sequence**:
     - Appears as resting capsule (`160 × 36 dp`)
     - Waits `150 ms`
     - Spring-expands to classic Face ID square (`84 × 84 dp`) with animated sweeping laser line for `1100 ms`
     - Morphs to emerald green checkmark (`✓`) for `1200 ms`
     - Collapses back to resting pill, holds for `600 ms`
     - Hides completely (`View.GONE`)
   - **Busy-Drop Event Bus (`OverlayTrigger`)**: SharedFlow with `replay = 0, extraBufferCapacity = 0`. Any triggers arriving while the animation is currently active are dropped rather than queued.

4. **Privacy-Preserving App-Open Accessibility Service**:
   - `canRetrieveWindowContent="false"` ensures zero access to screen contents or keystrokes.
   - Only reads the `packageName` on `TYPE_WINDOW_STATE_CHANGED`.
   - Filters out Aura Unlock, `com.android.systemui`, `android` core framework, the active soft keyboard (`Settings.Secure.DEFAULT_INPUT_METHOD`), and the default Home launcher.
   - Ignores repeat events from the same package and enforces a `2.5 s` debounce interval.

5. **Real-Time Calibration Studio**:
   - Live sliders for Scan Velocity (0.5x – 2.5x), Glow Intensity (20% – 100%), Line Thickness (1.0dp – 6.0dp), and Particle Density.
   - Cyber color picker: Electric Aqua, Neon Violet, Cyber Cyan, Matrix Amber, Crimson Core, and Ice White.
   - Full transport controls: Play, Pause, Scan Simulation, and Reset.

---

## 🛡️ Honesty, Privacy & Operating System Limitations

1. **Visual Simulation Only**:
   - Android broadcasts only a generic "device unlocked" signal (`ACTION_USER_PRESENT`). The operating system does NOT disclose whether the phone was unlocked via Face recognition, Fingerprint sensor, or PIN/pattern.
   - Aura Unlock does not claim that the animation verifies or validates actual facial identity upon unlock. It is a futuristic visual animation designed for style and aesthetic delight.

2. **Secure Lock Screen Isolation**:
   - Android OS executes lock-screen biometric authentication inside an isolated Trusted Execution Environment (TEE) hardware enclave.
   - System security strictly blocks all third-party window overlays from drawing over the secure lock screen / keyguard.
   - Consequently, the Dynamic Island Face ID animation appears immediately after the device is unlocked and onto the home screen / active app.

3. **Zero Background Camera Access**:
   - Aura Unlock **never** accesses, turns on, or streams from the camera sensor in the background or during system overlays.
   - Camera permissions are strictly used for in-app live silhouette previews when requested by the user.

4. **Zero Cloud / Local Persistence**:
   - All configurations and favorites are stored locally in device `SharedPreferences`. No user telemetry or images are sent to external servers.

---

## 🧪 Verification & Permissions Guide

- **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**: Required to display the Dynamic Island overlay over other applications.
- **App-Open Detection (`AccessibilityService`)**: Enables triggering the animation when switching into new applications.
- **Notification Permission (`POST_NOTIFICATIONS`)**: Keeps the background foreground service alive on Android 13+.
- **Camera Permission (`CAMERA`)**: Optional for in-app live camera HUD feed.
