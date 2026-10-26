# iPhone-Style Face ID Dynamic Island Popup

This plan refines the Dynamic Island into an authentic iPhone Face ID experience: completely hidden in resting state, popping out smoothly from the top camera cutout only when face scanning is triggered, displaying a square container with the classic iPhone Face ID wireframe animation, morphing into an animated green checkmark (`✓`), and closing completely.

---

### User Review & Critical Decisions

- **Resting Visibility**: Completely hidden by default (0 height / scale), appearing only during face scan simulations or camera unlock events.
- **Removed Elements**: "AURA FACE ID" text and bottom loading bar removed as requested.
- **Visual Structure**: Clean black pill holding a square box with the iconic iPhone Face ID wireframe (corner brackets, eye dots, nose tip, smile arc, moving scan beam).
- **Verification Transition**: Smooth animated morph from the face wireframe into an emerald checkmark tick (`✓`), with haptic satisfaction and automatic shrink-to-dismiss closing animation.

---

## 1. Overview & Core Concept

- **What It Does**: When any face scan demo is initiated (via button tap, home screen demo, or studio), the Dynamic Island expands downward from the top camera notch as a sleek rounded black module. It focuses entirely on a centered square containing the iconic Apple-style Face ID animated glyph. The beam scans the face, turns into an animated green tick mark, and disappears.
- **Authenticity**: Matches the minimalist, non-cluttered motion design of iOS Face ID with bouncy spring physics.

---

## 2. User Experience & Visual Design

### Key Animation Phases
1. **Hidden / Sleep State**:
   - `height = 0.dp`, `alpha = 0f`, positioned exactly at top center behind camera cutout.
2. **Scan Triggered (Pop-Out)**:
   - Bouncy spring expansion to `80.dp × 80.dp` square pill (or `110.dp × 72.dp` compact capsule).
   - Classic iPhone Face ID square:
     - 4 corner brackets (`┌ ┐ └ ┘`) in cool white/electric aqua.
     - Minimalist eye dots, vertical nose tick, and mouth curve.
     - Sweeping horizontal laser bar scanning up and down.
3. **Verified (Tick Symbol)**:
   - Scanning laser line fades out.
   - Wireframe dissolves and morphs into a vibrant emerald green checkmark (`✓`).
   - Slight spring pop on the checkmark for visual celebration.
4. **Dismissal (Auto-Close)**:
   - 600ms hold after tick mark, then bouncy shrink back to camera notch (`0.dp`, scale `0f`).
   - Restores completely hidden posture until next scan.

---

## 3. Technical Architecture & Component Changes

```
┌────────────────────────────────────────────────────────┐
│                      AuraApp                           │
│  (Top-Center: DynamicIslandBar floats over viewport)   │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────▼────────────┐
               │    DynamicIslandBar     │
               │  - AnimatedVisibility   │
               │  - Spring Size & Scale  │
               └────────────┬────────────┘
                            │
              ┌─────────────┴─────────────┐
              ▼                           ▼
    ┌───────────────────┐       ┌───────────────────┐
    │  State: SCANNING  │       │  State: SUCCESS   │
    │  - Corner Brackets│       │  - Green Tick     │
    │  - Face Wireframe │       │  - Spring Pop     │
    │  - Sweeping Beam  │       │  - Auto-Dismiss   │
    └───────────────────┘       └───────────────────┘
```

- **`DynamicIslandBar.kt`**:
  - Strip text labels and bottom loading bar.
  - Implement full hide/show animation with `AnimatedVisibility(visible = scanState != ScanState.IDLE)`.
  - Draw custom Canvas path for the classic iPhone Face ID wireframe and animated green checkmark.
- **`AuraUnlockViewModel.kt`**:
  - Maintain scan lifecycle: `SCANNING` -> `SUCCESS` -> auto reset to `IDLE` which triggers island dismissal.
