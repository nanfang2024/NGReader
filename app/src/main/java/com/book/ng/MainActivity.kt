package com.book.ng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.book.ng.feature.shelf.ShelfViewModel
import com.book.ng.ui.nav.NGBookNavHost
import com.book.ng.ui.settings.JellySettings
import com.book.ng.ui.settings.JellySettingsRepository
import com.book.ng.ui.theme.NGBookTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsRepository: JellySettingsRepository

    private val shelfViewModel: ShelfViewModel by viewModels()

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            shelfViewModel.import(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = JellySettings(),
            )
            NGBookTheme(
                themeMode = settings.themeMode,
                palette = settings.palette,
            ) {
                NGBookNavHost(
                    shelfViewModel = shelfViewModel,
                    onImport = { importLauncher.launch(arrayOf("*/*")) },
                )
            }
        }
    }
}
