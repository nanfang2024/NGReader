package com.book.ng.ui.theme

import androidx.compose.ui.graphics.Color

enum class JellyPalette {
    MATCHA,
    GRAPE,
    SAKURA,
    OCEAN,
}

data class JellyColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
    val background: Color,
    val surface: Color,
)

internal val MatchaLight = JellyColors(
    primary = Color(0xFF4E7A46),
    secondary = Color(0xFF6B8F71),
    tertiary = Color(0xFF8A9B4F),
    primaryContainer = Color(0xFFD8EBCD),
    secondaryContainer = Color(0xFFDCEBE0),
    tertiaryContainer = Color(0xFFE9F0CE),
    background = Color(0xFFF4F7EF),
    surface = Color(0xFFFAFCF6),
)

internal val MatchaDark = JellyColors(
    primary = Color(0xFFA9D6A0),
    secondary = Color(0xFFB8CCB6),
    tertiary = Color(0xFFD3DC9E),
    primaryContainer = Color(0xFF33592E),
    secondaryContainer = Color(0xFF43604A),
    tertiaryContainer = Color(0xFF5E6B2F),
    background = Color(0xFF10160E),
    surface = Color(0xFF181F16),
)

internal val GrapeLight = JellyColors(
    primary = Color(0xFF6F5B9E),
    secondary = Color(0xFF8B7BB8),
    tertiary = Color(0xFFB48ED0),
    primaryContainer = Color(0xFFE4DDF5),
    secondaryContainer = Color(0xFFE8E2F3),
    tertiaryContainer = Color(0xFFF2E3FA),
    background = Color(0xFFF6F3FB),
    surface = Color(0xFFFBF9FE),
)

internal val GrapeDark = JellyColors(
    primary = Color(0xFFC7B8EA),
    secondary = Color(0xFFC0B4DE),
    tertiary = Color(0xFFDCC0F0),
    primaryContainer = Color(0xFF4A3B78),
    secondaryContainer = Color(0xFF54486F),
    tertiaryContainer = Color(0xFF6E4E88),
    background = Color(0xFF131019),
    surface = Color(0xFF1A1622),
)

internal val SakuraLight = JellyColors(
    primary = Color(0xFFC06C85),
    secondary = Color(0xFFD08BA0),
    tertiary = Color(0xFFD4917D),
    primaryContainer = Color(0xFFF8DCE4),
    secondaryContainer = Color(0xFFFBE2EB),
    tertiaryContainer = Color(0xFFFBE5DC),
    background = Color(0xFFFDF4F6),
    surface = Color(0xFFFEFAFB),
)

internal val SakuraDark = JellyColors(
    primary = Color(0xFFF2B6C6),
    secondary = Color(0xFFEAB8C6),
    tertiary = Color(0xFFEEC0AB),
    primaryContainer = Color(0xFF7C3A50),
    secondaryContainer = Color(0xFF6E4356),
    tertiaryContainer = Color(0xFF7A4834),
    background = Color(0xFF1A1114),
    surface = Color(0xFF221519),
)

internal val OceanLight = JellyColors(
    primary = Color(0xFF3E7CB1),
    secondary = Color(0xFF5E93B8),
    tertiary = Color(0xFF4FA3A0),
    primaryContainer = Color(0xFFD5E6F5),
    secondaryContainer = Color(0xFFDDEAF4),
    tertiaryContainer = Color(0xFFD7EFEA),
    background = Color(0xFFF1F6FA),
    surface = Color(0xFFF8FBFD),
)

internal val OceanDark = JellyColors(
    primary = Color(0xFF9CC6E8),
    secondary = Color(0xFFA9C9DD),
    tertiary = Color(0xFFA2D8D0),
    primaryContainer = Color(0xFF28556F),
    secondaryContainer = Color(0xFF3C5D72),
    tertiaryContainer = Color(0xFF2E6B64),
    background = Color(0xFF0E151B),
    surface = Color(0xFF141D25),
)

internal fun jellyColors(palette: JellyPalette, dark: Boolean): JellyColors = when (palette) {
    JellyPalette.MATCHA -> if (dark) MatchaDark else MatchaLight
    JellyPalette.GRAPE -> if (dark) GrapeDark else GrapeLight
    JellyPalette.SAKURA -> if (dark) SakuraDark else SakuraLight
    JellyPalette.OCEAN -> if (dark) OceanDark else OceanLight
}
