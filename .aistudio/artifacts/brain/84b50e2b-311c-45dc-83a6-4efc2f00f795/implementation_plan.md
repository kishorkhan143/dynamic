# Fixed Dynamic Island Biometric Unlock Experience

Centralize all Face ID biometric scanning and unlock animations into an authentic, fixed top-center Dynamic Island anchored at the camera notch. On app launch or unlock simulation, the top island automatically springs into an animated Face ID biometric HUD, verifies with a glowing green checkmark, and unlocks the app interface upon completion.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction below reflects your confirmed choices:
> - **Single Fixed Top Island**: Only one true Dynamic Island is displayed, permanently pinned at the top center of the screen at the status bar / camera cutout level.
> - **App Open & Phone Unlock Flow**: When the application opens or when an unlock simulation is triggered, the Face ID biometric sequence executes from the top island. The app interface displays an authentic unlocking state until biometric verification completes, then cleanly reveals the app.
> - **Centralized Scan Animation Trigger**: Every biometric action across the app ("Run Scan Demo", re-scans, preset testing, and island taps) animates directly through this fixed top-center island.

- **Confirmed Decision 1**: Single fixed top-center Dynamic Island as the master interface for all Face ID visual feedback.
- **Confirmed Decision 2**: Automatic Face ID unlock sequence upon opening the app, smoothly transitioning to the unlocked main screen when the animation finishes.
- **Confirmed Decision 3**: Spring morph physics from resting pill (160×36dp) into the iOS-style Face ID square (84×84dp) with wireframe face scanning laser sweep and morphing green tick checkmark.

---

## 1. Overview & Core Concept

### What It Does
The app positions a permanent, fluid Dynamic Island at the top-center apex of the screen (in edge-to-edge status bar space). Rather than showing separate or conflicting in-card island graphics, all biometric events originate from this fixed island:
1. **App Open Face ID Authentication**: Upon launching the app, a stealth lock curtain is active with the top island in resting state. It instantly springs expand into the Face ID biometric square, sweeps an electric laser over the wireframe face glyph, morphs into a vibrant neon green checkmark with haptic rhythm, and smoothly dismisses while fading into the main dashboard.
2. **Global Trigger Routing**: Tapping the top Dynamic Island directly, clicking "RUN SCAN DEMO" on the Home screen, changing themes in Studio, or hitting "Retry" routes directly to the top island's spring-expansion physics.
3. **Lock/Unlock Re-simulation**: A dedicated "Lock & Authenticate" control lets users re-experience the authentic cold-boot unlock flow anytime without restarting the process.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[App Cold Boot / Open]
       │
       ▼
┌──────────────────────────────────────────────┐
│ Stealth Lock Screen + Fixed Top Island       │
│ Status: "AUTHENTICATING FACE ID..."          │
└──────────────────────┬───────────────────────┘
                       │ (Spring Expands to 84x84dp)
                       ▼
┌──────────────────────────────────────────────┐
│ Top Island: Face ID Wireframe + Laser Sweep │
│ Sweeping Aqua / Cyan Biometric Reticle       │
└──────────────────────┬───────────────────────┘
                       │ (Biometrics Verified)
                       ▼
┌──────────────────────────────────────────────┐
│ Top Island: Morph into Neon Green Checkmark  │
│ Padlock Icon Pops Open (Unlocked)            │
└──────────────────────┬───────────────────────┘
                       │ (Smooth Spring Retraction)
                       ▼
┌──────────────────────────────────────────────┐
│ Island Retracts to 160x36dp Resting Capsule  │
│ Full App UI Reveals with Staggered Entrance  │
└──────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Dynamic Island Resting State**: Sleek OLED black pill (160dp width × 36dp height, 18dp corner radius) with a luminous rim light brush, integrated camera lens optic, and minimalist Face ID icon.
- **Dynamic Island Active State**: Bouncy spring morph expansion to 84dp × 84dp rounded square (24dp corner radius) featuring classic iPhone corner brackets, wireframe face geometry, animated laser scanline, and animated path-drawn green checkmark.
- **Color Tokens**:
  - `OLED Black` (`#000000`): Island core and contrast base.
  - `AuraElectricAqua` (`#35F5E5`): Scanline laser, biometric HUD telemetry, and lock status text.
  - `AuraSuccess` (`#00F59B`): Crisp green checkmark stroke, success glow, and unlocked badge.
  - `AuraSurface` / `AuraBackground` (`#05070B`, `#0B0F17`): Deep cyber glass backdrops.
- **Motion & Physics**: Low-bouncy damping (`Spring.DampingRatioLowBouncy`) and medium-low stiffness (`Spring.StiffnessMediumLow`) for tactile, Apple-grade fluidity.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Single Master Top-Center Island vs. Duplicate Widgets**
  - *Chosen Approach*: Maintain a single, permanently visible Dynamic Island fixed at `Alignment.TopCenter` with `WindowInsets.statusBars` awareness.
  - *Why*: Eliminates visual clutter and confusion. Keeps all biometric animations anchored where physical device camera cutouts exist.
  - *Alternatives Considered*: Having both a top island and a secondary floating pill caused redundancy and broke the sense of device hardware integration.

- **Decision 2: App Startup Unlock Orchestration**
  - *Chosen Approach*: Manage an `isAppUnlocked` state in `AuraUnlockViewModel`. On app start (or simulation reset), the app launches into a locked presentation where the top island initiates the scan, finishes with the green tick, sets `isAppUnlocked = true`, and retracts.
  - *Why*: Directly addresses the user requirement: *"if the user unlocks phone or open application by face id our face id animation will work after our app opens when our animation end."*

- **Decision 3: Seamless Integration with Studio and Animations Presets**
  - *Chosen Approach*: When users switch presets or customize scan speed/accent colors in the Studio, the top Dynamic Island dynamically adapts its glow, particle cadence, and accent hue while keeping the iconic iPhone Face ID bracket & tick geometry.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               Fixed Top-Center DynamicIslandBar                │   │
│   │   [Resting Capsule (160x36dp) <---> Face ID Square (84x84dp)]   │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│   ┌───────────────────────────────▼────────────────────────────────┐   │
│   │                    App Content Container                       │   │
│   │                                                                │   │
│   │   ┌────────────────────────┐      ┌────────────────────────┐   │   │
│   │   │  Locked Boot Curtain   │  or  │  Unlocked Application  │   │   │
│   │   │  (Fades out on unlock) │      │  (Home / Studio / Nav) │   │   │
│   │   └────────────────────────┘      └────────────────────────┘   │   │
│   └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                         State & Event Observer
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                         AuraUnlockViewModel                            │
│                                                                        │
│   - scanState: StateFlow<ScanState> (IDLE, SCANNING, SUCCESS, RETRY)   │
│   - isAppUnlocked: StateFlow<Boolean> (controls startup unlock reveal) │
│   - studioConfig: StateFlow<StudioConfig> (colors, speed, presets)     │
│   - startUnlockSequence() / triggerAppOpenAuthentication()             │
└────────────────────────────────────────────────────────────────────────┘
```

### State Transitions & Sequences
1. **`triggerAppOpenAuthentication()`**: Triggered on `onCreate` / first composition. `isAppUnlocked = false`, `scanState = SCANNING`.
2. **Biometric Acquisition**: Sweeping scanline animates across the top island for 1.2s.
3. **Verification Transition**: `scanState = SUCCESS`. Face wireframe cross-fades into the animated green tick checkmark for 600ms.
4. **Unlock Complete**: `isAppUnlocked = true`, island springs back to resting dimensions (160×36dp), locked overlay fades out with `fadeOut(tween(350))`.
5. **Interactive Controls**: Home screen buttons ("RUN SCAN DEMO", "LOCK & TEST FACE ID", "RESCAN") call into `viewModel.startScanSimulation()` which directly expands the top island.
