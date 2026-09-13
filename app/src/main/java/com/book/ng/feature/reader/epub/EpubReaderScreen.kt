package com.book.ng.feature.reader.epub

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.book.ng.domain.parser.EpubChapter
import com.book.ng.ui.theme.LocalIsDark
import com.book.ng.ui.theme.jellyGlass

@Composable
fun EpubReaderScreen(
    onBack: () -> Unit,
    viewModel: EpubReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = LocalIsDark.current
    var tocOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        EpubTopBar(
            title = state.title,
            dark = dark,
            onBack = onBack,
            onToggleToc = { tocOpen = !tocOpen },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val ready = !state.loading && state.error == null && state.chapters.isNotEmpty()
            when {
                ready -> EpubWebViewContent(
                    html = state.chapterHtml,
                    baseUrl = state.chapterBaseUrl,
                    contentVersion = state.contentVersion,
                    restoreScrollY = state.restoreScrollY,
                    dark = dark,
                    onScrollReported = viewModel::onScrollReported,
                )
                state.error != null -> EpubMessage(text = state.error ?: "加载失败")
                state.loading -> EpubMessage(text = null)
                else -> EpubMessage(text = "这本书没有可阅读的章节")
            }
            if (tocOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { tocOpen = false },
                )
            }
            Box(
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                TocDrawer(
                    open = tocOpen,
                    title = state.title,
                    chapters = state.chapters,
                    currentIndex = state.currentChapterIndex,
                    dark = dark,
                    onSelect = { index ->
                        viewModel.selectChapter(index)
                        tocOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EpubTopBar(
    title: String,
    dark: Boolean,
    onBack: () -> Unit,
    onToggleToc: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .jellyGlass(shape = shape, alpha = 0.9f, dark = dark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text(text = "返回", color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToggleToc) {
            Text(text = "目录", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EpubWebViewContent(
    html: String,
    baseUrl: String,
    contentVersion: Int,
    restoreScrollY: Int,
    dark: Boolean,
    onScrollReported: (Int) -> Unit,
) {
    val backgroundArgb = MaterialTheme.colorScheme.background.toArgb()
    val filter = if (dark) INVERT_FILTER else "none"
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            EpubWebView(ctx).apply {
                setBackgroundColor(backgroundArgb)
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        val epub = view as? EpubWebView ?: return
                        epub.evaluateJavascript(
                            "document.documentElement.style.filter='" + epub.themeFilter + "';",
                            null,
                        )
                        if (epub.pendingScrollY > 0) {
                            epub.evaluateJavascript(
                                "window.scrollTo(0, ${epub.pendingScrollY});",
                                null,
                            )
                            epub.pendingScrollY = 0
                        }
                    }
                }
            }
        },
        update = { view ->
            view.onScrollY = onScrollReported
            if (view.themeFilter != filter) {
                view.themeFilter = filter
                view.evaluateJavascript(
                    "document.documentElement.style.filter='$filter';",
                    null,
                )
            }
            if (view.loadedVersion != contentVersion) {
                view.loadedVersion = contentVersion
                view.pendingScrollY = restoreScrollY
                view.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null)
            }
        },
    )
}

private class EpubWebView(context: Context) : WebView(context) {
    var onScrollY: ((Int) -> Unit)? = null
    var themeFilter: String = "none"
    var loadedVersion: Int = -1
    var pendingScrollY: Int = 0

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollY?.invoke(t)
    }
}

@Composable
private fun TocDrawer(
    open: Boolean,
    title: String,
    chapters: List<EpubChapter>,
    currentIndex: Int,
    dark: Boolean,
    onSelect: (Int) -> Unit,
) {
    AnimatedVisibility(
        visible = open,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { -it }),
    ) {
        TocPanel(
            title = title,
            chapters = chapters,
            currentIndex = currentIndex,
            dark = dark,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun TocPanel(
    title: String,
    chapters: List<EpubChapter>,
    currentIndex: Int,
    dark: Boolean,
    onSelect: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (chapters.isNotEmpty()) {
            listState.scrollToItem(currentIndex.coerceIn(0, chapters.lastIndex))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .clip(shape)
            .jellyGlass(shape = shape, alpha = 0.95f, dark = dark)
            .padding(12.dp),
    ) {
        Text(
            text = "目录 · $title",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(chapters) { chapter ->
                val selected = chapter.index == currentIndex
                Text(
                    text = chapter.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { onSelect(chapter.index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun EpubMessage(text: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (text == null) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private const val INVERT_FILTER = "invert(1) hue-rotate(180deg)"
