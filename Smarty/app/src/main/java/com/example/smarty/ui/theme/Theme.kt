package com.example.smarty.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SmartyColors = lightColorScheme(
    primary = Sage, onPrimary = Color.White,
    primaryContainer = PaleSage, onPrimaryContainer = Forest,
    secondary = Forest, onSecondary = Color.White,
    secondaryContainer = Peach, onSecondaryContainer = Forest,
    tertiary = Color(0xFF80582E), tertiaryContainer = AmberSoft,
    background = Cream, onBackground = Forest,
    surface = Cream, onSurface = Forest,
    surfaceVariant = PaleSage, onSurfaceVariant = InkMuted,
    outline = Color(0xFF858C7E), outlineVariant = WarmOutline,
    error = Color(0xFFAB3B39), onError = Color.White,
    errorContainer = Color(0xFFFFE5DE), onErrorContainer = Color(0xFF722723)
)

// A deliberately consistent light theme for the hackathon prototype.
// Dynamic wallpaper colors are disabled so the app always matches its branding.
@Composable
fun SmartyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartyColors,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
