# Implementation Plan: Always-On Top-Center Dynamic Island System Overlay

Transition the Dynamic Island from an inside-the-app composable to a persistent, 24/7 system overlay centered at the top camera notch across the lock screen, home screen, and all applications.

## Proposed Changes

### 1. Overlay Service Persistence (`DynamicIslandOverlayService.kt`)
- **Always-On Visibility**: Change the default/idle state of the overlay from `View.GONE` / `HIDDEN` to `View.VISIBLE` in `SystemOverlayPhase.RESTING`.
- **Top-Center Alignment**: Anchor the overlay window directly at `Gravity.TOP or Gravity.CENTER_HORIZONTAL` with exact notch calibration (`FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`, `y = 0dp - 4dp`).
- **Touch Transparency**: Maintain `FLAG_NOT_TOUCHABLE` and `FLAG_NOT_FOCUSABLE` so the persistent island never impedes pulling down the notification shade, tapping status bar icons, or interacting with underlying apps.
- **Continuous Lifecycle Animation**:
  - **Idle**: Sits in sleek resting capsule mode (160×36dp) directly over the camera notch.
  - **Trigger (Unlock / App Launch / Preview)**: Expands dynamically to the 84×84dp Face ID scanning square with laser animation (1100ms) $\rightarrow$ Displays verified green checkmark (1200ms) $\rightarrow$ Smoothly collapses back to the resting capsule and remains visible at the notch apex.

### 2. Remove Duplicate In-App Island (`MainActivity.kt`)
- Remove the in-app `DynamicIslandBar` component and related in-app notch padding from `MainActivity.kt`.
- Because the system overlay is always visible on top of everything, Aura Unlock will now naturally show the exact same floating system Dynamic Island whether the app is open, minimized, or closed.
- Ensure the overlay service is started automatically when the app opens if overlay permission is already granted.

### 3. Overlay Permission UX & Diagnostics (`SettingsScreen.kt` & `HomeScreen.kt`)
- Prominently guide the user to grant the `SYSTEM_ALERT_WINDOW` ("Appear on top") permission if not yet enabled so the persistent island can attach to the screen immediately.
- Update UI labels and status badges to clearly indicate "Always-On Camera Notch Island" is active.

## Verification Plan

### Automated Build & Unit Tests
- Execute `compile_applet` to verify clean compilation with no missing imports or broken layout references.
- Run Robolectric unit tests via `run_command` (`gradle :app:testDebugUnitTest`) to verify overlay trigger event delivery.

### Manual Verification Scenarios
1. **Always-On Camera Alignment**: Launch the app, grant "Appear on top" permission, and observe the Dynamic Island positioned directly at the top center camera apex.
2. **Outside the App**: Press Home to exit Aura Unlock; verify the island stays pinned at the camera notch across the launcher and other apps.
3. **Face ID Unlock & App Launch Triggers**: Lock and unlock the device, or launch another app, and verify the island expands into the Face ID square, scans, verifies, and collapses cleanly back to the resting notch capsule.
4. **No In-App Duplication**: Return to Aura Unlock and confirm there is only one unified island sitting at the top.
