package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.PresetCatalog
import org.junit.Assert.assertEquals
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
}
