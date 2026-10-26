package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundMode
import com.example.model.PresetCatalog
import com.example.model.ScanPreset
import com.example.ui.components.FaceScanRenderer
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraBorder
import com.example.ui.theme.AuraBorderBright
import com.example.ui.theme.AuraElectricAqua
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.OrbitronFontFamily
import com.example.viewmodel.AuraUnlockViewModel

@Composable
fun AnimationsScreen(
    viewModel: AuraUnlockViewModel,
    onNavigateToStudio: () -> Unit
) {
    val studioConfig by viewModel.studioConfig.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val filterOptions = listOf("All", "Favorites", "Cyber HUD", "Synaptic", "Hologram", "Minimal", "Vibrant", "Quantum", "Spectral")

    val filteredPresets = PresetCatalog.ALL_PRESETS.filter { preset ->
        when (selectedFilter) {
            "All" -> true
            "Favorites" -> favorites.contains(preset.id)
            else -> preset.category.equals(selectedFilter, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .padding(top = 20.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "ANIMATION SUITE",
                fontFamily = OrbitronFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTextPrimary,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "8 HIGH-FIDELITY BIOMETRIC PRESETS",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                color = AuraElectricAqua,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Category Filter Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AuraElectricAqua.copy(alpha = 0.2f) else Color(0xFF10151E))
                        .border(
                            1.dp,
                            if (isSelected) AuraElectricAqua else AuraBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setFilter(filter) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter.uppercase(),
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AuraElectricAqua else AuraTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Cards List
        if (filteredPresets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NO PRESETS FOUND",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 14.sp,
                        color = AuraTextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try clearing filters or adding favorites.",
                        fontSize = 12.sp,
                        color = AuraTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredPresets) { preset ->
                    PresetCardItem(
                        preset = preset,
                        isSelected = preset.id == studioConfig.preset.id,
                        isFavorite = favorites.contains(preset.id),
                        onSelect = { viewModel.selectPreset(preset) },
                        onToggleFavorite = { viewModel.toggleFavorite(preset.id) },
                        onCustomize = {
                            viewModel.selectPreset(preset)
                            onNavigateToStudio()
                        },
                        onRunDemo = {
                            viewModel.selectPreset(preset)
                            viewModel.startScanSimulation()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCardItem(
    preset: ScanPreset,
    isSelected: Boolean,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCustomize: () -> Unit,
    onRunDemo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AuraSurfaceCard)
            .border(
                1.2.dp,
                if (isSelected) preset.accentColor else AuraBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Miniature Live Animation Thumbnail
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF050608))
                ) {
                    FaceScanRenderer(
                        preset = preset,
                        modifier = Modifier.fillMaxSize(),
                        accentColor = preset.accentColor,
                        speedMultiplier = preset.defaultSpeed,
                        glowIntensity = preset.defaultGlow,
                        scanLineWidth = preset.defaultLineWidth,
                        particleDensity = preset.defaultDensity,
                        backgroundMode = BackgroundMode.SILHOUETTE,
                        isPlaying = true,
                        showTelemetryOverlay = false
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = preset.accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = preset.category.uppercase(),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 9.sp,
                                color = preset.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) preset.accentColor else AuraTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = preset.name,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = AuraTextSecondary,
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) preset.accentColor else Color(0xFF161F2C),
                        contentColor = if (isSelected) Color(0xFF003733) else AuraTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isSelected) "ACTIVE" else "SELECT",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onCustomize,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AuraTextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "STUDIO",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = AuraTextPrimary
                    )
                }

                IconButton(
                    onClick = onRunDemo,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF141A24))
                        .border(1.dp, AuraBorder, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Demo",
                        tint = preset.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
