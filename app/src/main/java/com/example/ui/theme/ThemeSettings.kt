package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ThemeSettings {
    
    // Theme Names
    val THEMES = listOf(
        "System Default",
        "Clean White",
        "AMOLED Black",
        "Solarized Sepia",
        "Nordic Frost",
        "Velvet Mint",
        "Cyberpunk Amber"
    )

    // Solarized Sepia Colors
    private val SepiaPrimary = Color(0xFFB45309) // Warm Amber Brown
    private val SepiaBackground = Color(0xFFFDF6E3) // Cream Sepia
    private val SepiaSurface = Color(0xFFEEE8D5) // Warm Gray
    private val SepiaText = Color(0xFF586E75) // Charcoal

    // Nordic Frost Colors
    private val FrostPrimary = Color(0xFF0284C7) // Clear sky blue
    private val FrostBackground = Color(0xFFF0F9FF) // Light ice white
    private val FrostSurface = Color(0xFFE0F2FE) // Sky blue card
    private val FrostText = Color(0xFF0F172A)

    // Velvet Mint Colors
    private val MintPrimary = Color(0xFF059669) // Forest Mint
    private val MintBackground = Color(0xFFF0FDF4) // Mint cream
    private val MintSurface = Color(0xFFDCFCE7) // Mint gray card
    private val MintText = Color(0xFF14532D)

    // Cyberpunk Amber Colors
    private val AmberPrimary = Color(0xFFF59E0B) // High contrast neon amber
    private val AmberBackground = Color(0xFF0D0D0D) // Cyberpunk deep block
    private val AmberSurface = Color(0xFF1A1A1A) // Cyber card
    private val AmberText = Color(0xFFF59E0B)

    fun getColorScheme(themeName: String, isSystemDark: Boolean): ColorScheme {
        val actualThemeName = if (themeName == "System Default") {
            if (isSystemDark) "AMOLED Black" else "Clean White"
        } else {
            themeName
        }

        return when (actualThemeName) {
            "AMOLED Black" -> darkColorScheme(
                primary = Color(0xFF38BDF8), // Radiant Sky Blue
                onPrimary = Color.Black,
                secondary = Color(0xFF0EA5E9),
                background = Color(0xFF000000), // AMOLED Pure Black
                onBackground = Color(0xFFFFFFFF),
                surface = Color(0xFF0A0B0D), // Polished Dark Glass Container
                onSurface = Color(0xFFE2E8F0),
                surfaceVariant = Color(0xFF15181E),
                onSurfaceVariant = Color(0xFF94A3B8),
                error = Color(0xFFF87171),
                onError = Color.Black
            )
            "Clean White" -> lightColorScheme(
                primary = Color(0xFF1E3A8A), // Regal Dark Blue (Xiaomi notes style)
                onPrimary = Color.White,
                secondary = Color(0xFF3B82F6),
                background = Color(0xFFFFFFFF), // Pure White default!
                onBackground = Color(0xFF000000),
                surface = Color(0xFFF8FAFC), // Ultra soft slate surface
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = Color(0xFF475569),
                error = Color(0xFFEF4444),
                onError = Color.White
            )
            "Solarized Sepia" -> lightColorScheme(
                primary = SepiaPrimary,
                onPrimary = Color.White,
                secondary = SepiaPrimary.copy(alpha = 0.8f),
                background = SepiaBackground,
                onBackground = SepiaText,
                surface = SepiaSurface,
                onSurface = SepiaText,
                surfaceVariant = SepiaSurface.copy(alpha = 0.9f),
                onSurfaceVariant = SepiaText.copy(alpha = 0.8f)
            )
            "Nordic Frost" -> lightColorScheme(
                primary = FrostPrimary,
                onPrimary = Color.White,
                secondary = FrostPrimary.copy(alpha = 0.8f),
                background = FrostBackground,
                onBackground = FrostText,
                surface = FrostSurface,
                onSurface = FrostText,
                surfaceVariant = FrostSurface.copy(alpha = 0.8f),
                onSurfaceVariant = FrostText.copy(alpha = 0.7f)
            )
            "Velvet Mint" -> lightColorScheme(
                primary = MintPrimary,
                onPrimary = Color.White,
                secondary = MintPrimary.copy(alpha = 0.8f),
                background = MintBackground,
                onBackground = MintText,
                surface = MintSurface,
                onSurface = MintText,
                surfaceVariant = MintSurface.copy(alpha = 0.8f),
                onSurfaceVariant = MintText.copy(alpha = 0.7f)
            )
            "Cyberpunk Amber" -> darkColorScheme(
                primary = AmberPrimary,
                onPrimary = Color.Black,
                secondary = AmberPrimary,
                background = AmberBackground,
                onBackground = AmberText,
                surface = AmberSurface,
                onSurface = AmberText,
                surfaceVariant = AmberSurface.copy(alpha = 0.8f),
                onSurfaceVariant = AmberText.copy(alpha = 0.8f)
            )
            else -> { // Default Fallback
                lightColorScheme(
                    primary = Color(0xFF1E3A8A),
                    onPrimary = Color.White,
                    background = Color(0xFFFFFFFF),
                    onBackground = Color(0xFF000000),
                    surface = Color(0xFFF1F5F9),
                    onSurface = Color(0xFF0F172A)
                )
            }
        }
    }
}
