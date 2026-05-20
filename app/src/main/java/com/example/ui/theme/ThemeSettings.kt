package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ThemeSettings {
    
    // Theme Names
    val THEMES = listOf(
        "Slate Dark",
        "Midnight Onyx",
        "Solarized Sepia",
        "Nordic Frost",
        "Velvet Mint",
        "Cyberpunk Amber"
    )

    // Slate Dark Colors
    private val SlateDarkPrimary = Color(0xFF14B8A6) // Teal
    private val SlateDarkBackground = Color(0xFF0F172A) // Dark Slate
    private val SlateDarkSurface = Color(0xFF1E293B) // Card Slate
    private val SlateDarkText = Color(0xFFF8FAFC)

    // Midnight Onyx Colors
    private val MidnightOnyxPrimary = Color(0xFF3B82F6) // Electric Blue
    private val MidnightOnyxBackground = Color(0xFF000000) // True black
    private val MidnightOnyxSurface = Color(0xFF111111) // Crisp dark gray
    private val MidnightOnyxText = Color(0xFFFFFFFF)

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

    fun getColorScheme(themeName: String): ColorScheme {
        return when (themeName) {
            "Midnight Onyx" -> darkColorScheme(
                primary = MidnightOnyxPrimary,
                onPrimary = Color.White,
                secondary = MidnightOnyxPrimary.copy(alpha = 0.8f),
                background = MidnightOnyxBackground,
                onBackground = MidnightOnyxText,
                surface = MidnightOnyxSurface,
                onSurface = MidnightOnyxText,
                surfaceVariant = MidnightOnyxSurface.copy(alpha = 0.7f),
                onSurfaceVariant = MidnightOnyxText.copy(alpha = 0.7f)
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
            else -> { // Default: Slate Dark
                darkColorScheme(
                    primary = SlateDarkPrimary,
                    onPrimary = Color.Black,
                    secondary = SlateDarkPrimary.copy(alpha = 0.8f),
                    background = SlateDarkBackground,
                    onBackground = SlateDarkText,
                    surface = SlateDarkSurface,
                    onSurface = SlateDarkText,
                    surfaceVariant = SlateDarkSurface.copy(alpha = 0.8f),
                    onSurfaceVariant = SlateDarkText.copy(alpha = 0.7f)
                )
            }
        }
    }
}
