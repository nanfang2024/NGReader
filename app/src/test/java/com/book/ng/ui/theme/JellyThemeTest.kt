package com.book.ng.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JellyThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun matchaAndGrapePalettesProduceDifferentPrimary() {
        var matchaPrimary: Color? = null
        var grapePrimary: Color? = null
        composeRule.setContent {
            NGBookTheme(themeMode = JellyThemeMode.LIGHT, palette = JellyPalette.MATCHA) {
                matchaPrimary = MaterialTheme.colorScheme.primary
            }
            NGBookTheme(themeMode = JellyThemeMode.LIGHT, palette = JellyPalette.GRAPE) {
                grapePrimary = MaterialTheme.colorScheme.primary
            }
        }
        assertEquals(jellyColors(JellyPalette.MATCHA, dark = false).primary, matchaPrimary)
        assertEquals(jellyColors(JellyPalette.GRAPE, dark = false).primary, grapePrimary)
        assertNotEquals(matchaPrimary, grapePrimary)
    }

    @Test
    fun paletteSwitchUpdatesTokensInSameComposition() {
        val palette = mutableStateOf(JellyPalette.MATCHA)
        var currentPrimary: Color? = null
        composeRule.setContent {
            NGBookTheme(themeMode = JellyThemeMode.LIGHT, palette = palette.value) {
                currentPrimary = MaterialTheme.colorScheme.primary
            }
        }
        val before = currentPrimary
        assertEquals(jellyColors(JellyPalette.MATCHA, dark = false).primary, before)
        composeRule.runOnIdle { palette.value = JellyPalette.GRAPE }
        composeRule.waitForIdle()
        val after = currentPrimary
        assertNotEquals(before, after)
        assertEquals(jellyColors(JellyPalette.GRAPE, dark = false).primary, after)
    }

    @Test
    fun themeModeSelectsLightOrDarkVariant() {
        var lightPrimary: Color? = null
        var darkPrimary: Color? = null
        composeRule.setContent {
            NGBookTheme(themeMode = JellyThemeMode.LIGHT, palette = JellyPalette.SAKURA) {
                lightPrimary = MaterialTheme.colorScheme.primary
            }
            NGBookTheme(themeMode = JellyThemeMode.DARK, palette = JellyPalette.SAKURA) {
                darkPrimary = MaterialTheme.colorScheme.primary
            }
        }
        assertEquals(jellyColors(JellyPalette.SAKURA, dark = false).primary, lightPrimary)
        assertEquals(jellyColors(JellyPalette.SAKURA, dark = true).primary, darkPrimary)
        assertNotEquals(lightPrimary, darkPrimary)
    }
}
