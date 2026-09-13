package com.book.ng.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.book.ng.feature.reader.epub.EpubReaderScreen
import com.book.ng.feature.reader.text.TextReaderScreen
import com.book.ng.feature.shelf.ShelfScreen
import com.book.ng.feature.shelf.ShelfViewModel

object Routes {
    const val SHELF = "shelf"
    const val ARG_BOOK_ID = "bookId"
    const val READER_TXT = "reader_txt/{$ARG_BOOK_ID}"
    const val READER_EPUB = "reader_epub/{$ARG_BOOK_ID}"

    fun readerTxt(bookId: Long): String = "reader_txt/$bookId"

    fun readerEpub(bookId: Long): String = "reader_epub/$bookId"
}

@Composable
fun NGBookNavHost(
    shelfViewModel: ShelfViewModel,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.SHELF,
        modifier = modifier,
    ) {
        composable(Routes.SHELF) {
            ShelfScreen(
                viewModel = shelfViewModel,
                onImport = onImport,
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable(
            route = Routes.READER_TXT,
            arguments = listOf(navArgument(Routes.ARG_BOOK_ID) { type = NavType.LongType }),
        ) {
            TextReaderScreen(onBack = { navController.navigateUp() })
        }
        composable(
            route = Routes.READER_EPUB,
            arguments = listOf(navArgument(Routes.ARG_BOOK_ID) { type = NavType.LongType }),
        ) {
            EpubReaderScreen(onBack = { navController.navigateUp() })
        }
    }
}
