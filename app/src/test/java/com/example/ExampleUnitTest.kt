package com.example

import com.example.model.ParticleDensity
import com.example.model.PresetCatalog
import com.example.model.ScanPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testAllPresetsValid() {
    val presets = PresetCatalog.ALL_PRESETS
    assertEquals(8, presets.size)
    presets.forEach { preset ->
      assertNotNull(preset.id)
      assertNotNull(preset.name)
      assertNotNull(preset.category)
      assertNotNull(preset.description)
    }
  }

  @Test
  fun testParticleDensityValues() {
    assertEquals(20, ParticleDensity.LOW.count)
    assertEquals(45, ParticleDensity.MEDIUM.count)
    assertEquals(80, ParticleDensity.HIGH.count)
  }
}
