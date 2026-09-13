package com.book.ng.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.book.ng.ui.theme.JellyPalette
import com.book.ng.ui.theme.JellyThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class JellySettings(
    val themeMode: JellyThemeMode = JellyThemeMode.SYSTEM,
    val palette: JellyPalette = JellyPalette.MATCHA,
)

private val Context.jellyDataStore: DataStore<Preferences> by preferencesDataStore(name = "jelly_prefs")

@Singleton
class JellySettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<JellySettings> = context.jellyDataStore.data.map { prefs ->
        JellySettings(
            themeMode = prefs[KEY_THEME_MODE].toEnumOrDefault(JellyThemeMode.SYSTEM),
            palette = prefs[KEY_PALETTE].toEnumOrDefault(JellyPalette.MATCHA),
        )
    }

    suspend fun setThemeMode(mode: JellyThemeMode) {
        context.jellyDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setPalette(palette: JellyPalette) {
        context.jellyDataStore.edit { it[KEY_PALETTE] = palette.name }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_PALETTE = stringPreferencesKey("palette")

        inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
            this?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: default
    }
}
