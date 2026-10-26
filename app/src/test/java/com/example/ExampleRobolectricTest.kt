package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.PresetCatalog
import com.example.service.AppOpenAccessibilityService
import com.example.service.OverlayTrigger
import com.example.service.getIslandBackgroundColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Unlock", appName)
  }

  @Test
  fun `verify preset catalog has 8 distinct presets`() {
    assertEquals(8, PresetCatalog.ALL_PRESETS.size)
    val ids = PresetCatalog.ALL_PRESETS.map { it.id }.toSet()
    assertEquals(8, ids.size)
    assertTrue(ids.contains("aqua_pulse"))
    assertTrue(ids.contains("neural_scan"))
    assertTrue(ids.contains("holographic_face"))
    assertTrue(ids.contains("cyber_grid"))
    assertTrue(ids.contains("minimal_orbit"))
    assertTrue(ids.contains("neon_contour"))
    assertTrue(ids.contains("digital_particles"))
    assertTrue(ids.contains("aurora_scan"))
  }

  @Test
  fun `verify settings repository persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SettingsRepository(context)

    repo.saveActivePresetId("neural_scan")
    assertEquals("neural_scan", repo.getActivePresetId())

    val favs = repo.toggleFavorite("holographic_face")
    assertTrue(favs.contains("holographic_face"))
  }

  @Test
  fun `verify island darkness and animation velocity customization and overlay sync`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = SettingsRepository(context)
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AuraUnlockViewModel(application)

    // Default values
    assertEquals(1.0f, repo.getIslandDarkness(), 0.01f)
    assertEquals(1.0f, repo.getAnimationVelocity(), 0.01f)

    // Update island tone (lighter)
    viewModel.updateIslandDarkness(0.35f)
    assertEquals(0.35f, viewModel.islandDarkness.value, 0.01f)
    assertEquals(0.35f, repo.getIslandDarkness(), 0.01f)
    assertEquals(0.35f, OverlayTrigger.islandDarkness.value, 0.01f)

    // Update velocity
    viewModel.updateAnimationVelocity(1.8f)
    assertEquals(1.8f, viewModel.animationVelocity.value, 0.01f)
    assertEquals(1.8f, repo.getAnimationVelocity(), 0.01f)
    assertEquals(1.8f, OverlayTrigger.animationVelocity.value, 0.01f)

    // Verify background color helper calculation
    val lightBg = getIslandBackgroundColor(0.0f)
    assertTrue(lightBg.alpha in 0.89f..0.91f)

    val darkBg = getIslandBackgroundColor(1.0f)
    assertEquals(1.0f, darkBg.alpha, 0.01f)
    assertEquals(0f, darkBg.red, 0.01f)
    assertEquals(0f, darkBg.green, 0.01f)
    assertEquals(0f, darkBg.blue, 0.01f)
  }

  @Test
  fun `verify realtime face recognition synchronizes with scan state and dynamic island`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AuraUnlockViewModel(application)

    // Initial state
    assertEquals(com.example.model.ScanState.IDLE, viewModel.scanState.value)
    assertEquals(0f, viewModel.scanProgress.value)

    // Simulate acquiring face
    val trackingData = com.example.detection.RealtimeFaceData(
      isFaceDetected = true,
      lockProgress = 0.65f,
      status = com.example.detection.RecognitionStatus.TRACKING
    )
    viewModel.updateRealtimeFaceData(trackingData)

    assertEquals(com.example.model.ScanState.SCANNING, viewModel.scanState.value)
    assertEquals(0.65f, viewModel.scanProgress.value)
    assertTrue(viewModel.isDynamicIslandExpanded.value)

    // Simulate verified face
    val verifiedData = com.example.detection.RealtimeFaceData(
      isFaceDetected = true,
      lockProgress = 1.0f,
      status = com.example.detection.RecognitionStatus.VERIFIED
    )
    viewModel.updateRealtimeFaceData(verifiedData)

    assertEquals(com.example.model.ScanState.SUCCESS, viewModel.scanState.value)
    assertEquals(1.0f, viewModel.scanProgress.value)
    assertTrue(viewModel.isDynamicIslandExpanded.value)
  }

  @Test
  fun `verify app unlock state and scan triggers operate from main top dynamic island`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AuraUnlockViewModel(application)

    // Initially locked before authentication completes
    assertFalse(viewModel.isAppUnlocked.value)
    assertEquals(com.example.model.ScanState.IDLE, viewModel.scanState.value)
    assertTrue(viewModel.isDynamicIslandEnabled.value)

    // Trigger simulation
    viewModel.startScanSimulation()
    assertEquals(com.example.model.ScanState.SCANNING, viewModel.scanState.value)
    assertTrue(viewModel.isDynamicIslandExpanded.value)
  }

  @Test
  fun `verify island position Y and X offset customization and synchronization`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AuraUnlockViewModel(application)
    val repo = SettingsRepository(application.applicationContext)

    // Default position: Y=12dp (notch offset), X=0dp (centered)
    assertEquals(12, repo.getIslandOffsetY())
    assertEquals(0, repo.getIslandOffsetX())

    // Update position
    viewModel.updateIslandOffsetY(24)
    viewModel.updateIslandOffsetX(-15)

    assertEquals(24, viewModel.islandOffsetY.value)
    assertEquals(-15, viewModel.islandOffsetX.value)
    assertEquals(24, repo.getIslandOffsetY())
    assertEquals(-15, repo.getIslandOffsetX())
    assertEquals(24, OverlayTrigger.islandOffsetY.value)
    assertEquals(-15, OverlayTrigger.islandOffsetX.value)
  }

  @Test
  fun `verify island size reduction scale and green light indicator toggle`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.AuraUnlockViewModel(application)
    val repo = SettingsRepository(application.applicationContext)

    // Default scale: 1.0f (100%), green light: false (off when resting)
    assertEquals(1.0f, repo.getIslandScale(), 0.01f)
    assertFalse(repo.getGreenLightEnabled())
    assertFalse(OverlayTrigger.greenLightEnabled.value)

    // Reduce island size to 60% (mini compact for small pinhole camera notches)
    viewModel.updateIslandScale(0.60f)
    assertEquals(0.60f, viewModel.islandScale.value, 0.01f)
    assertEquals(0.60f, repo.getIslandScale(), 0.01f)
    assertEquals(0.60f, OverlayTrigger.islandScale.value, 0.01f)

    // Toggle green light on and off
    viewModel.updateGreenLightEnabled(true)
    assertTrue(viewModel.greenLightEnabled.value)
    assertTrue(repo.getGreenLightEnabled())
    assertTrue(OverlayTrigger.greenLightEnabled.value)

    viewModel.updateGreenLightEnabled(false)
    assertFalse(viewModel.greenLightEnabled.value)
    assertFalse(repo.getGreenLightEnabled())
    assertFalse(OverlayTrigger.greenLightEnabled.value)
  }
}
