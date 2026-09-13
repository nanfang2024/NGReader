package com.book.ng.feature.reader.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.book.ng.domain.model.ReadMode
import com.book.ng.ui.theme.LocalIsDark
import com.book.ng.ui.theme.jellyGlass
import kotlin.math.max
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun TextReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        val pendingPagination = state.mode == ReadMode.PAGING && state.document == null
        when {
            state.error != null -> ReaderMessage(text = state.error ?: "加载失败")
            state.loading || pendingPagination -> ReaderMessage(text = null)
            state.mode == ReadMode.PAGING -> PagingContent(
                state = state,
                menuVisible = menuVisible,
                onToggleMenu = { menuVisible = !menuVisible },
                onViewportSize = viewModel::onViewportSize,
                onPageChange = viewModel::onPageChange,
            )
            else -> ScrollContent(
                state = state,
                onScrollChapterChange = viewModel::onScrollChapterChange,
            )
        }
        ReaderOverlay(
            state = state,
            menuVisible = menuVisible,
            onBack = onBack,
            onToggleMode = {
                viewModel.onModeChange(
                    if (state.mode == ReadMode.PAGING) ReadMode.SCROLL else ReadMode.PAGING,
                )
            },
            onFontSizeChange = viewModel::onFontSizeChange,
        )
    }
}

@Composable
private fun ReaderMessage(text: String?) {
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

@Composable
private fun PagingContent(
    state: ReaderUiState,
    menuVisible: Boolean,
    onToggleMenu: () -> Unit,
    onViewportSize: (Int, Int) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val doc = state.document ?: return
    val pagerState = rememberPagerState(pageCount = { doc.pageCount })
    val scope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(state.currentPage, doc) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != state.currentPage) {
            onPageChange(pagerState.settledPage)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize = size
                onViewportSize(size.width, size.height)
            }
            .pointerInput(state.mode, doc.pageCount) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    when {
                        offset.x < width / 3f -> {
                            val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                            scope.launch { pagerState.requestScrollPage(target) }
                        }
                        offset.x > width * 2f / 3f -> {
                            val target = (pagerState.currentPage + 1).coerceAtMost(doc.pageCount - 1)
                            scope.launch { pagerState.requestScrollPage(target) }
                        }
                        else -> onToggleMenu()
                    }
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
        ) { index ->
            val spec = doc.pageAt(index)
            StaticPage(
                state = state,
                startOffset = spec.startOffset,
                endOffset = spec.endOffset,
                containerSize = containerSize,
            )
        }
    }
}

private suspend fun PagerState.requestScrollPage(page: Int) {
    if (page in 0 until pageCount) {
        animateScrollToPage(page)
    }
}

@Composable
private fun StaticPage(
    state: ReaderUiState,
    startOffset: Int,
    endOffset: Int,
    containerSize: IntSize,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val text = remember(state.fullText, startOffset, endOffset) {
        state.fullText.substring(startOffset, max(startOffset, endOffset))
    }
    val metrics = context.resources.displayMetrics
    val layout = remember(
        text,
        containerSize,
        state.fontSizeSp,
        textColor,
        density.density,
    ) {
        if (containerSize.width <= 0) {
            null
        } else {
            val widthDp = (containerSize.width / metrics.density).toInt()
            val paddingPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PADDING_DP,
                metrics,
            )
            val contentWidthPx = max(1, (widthDp * metrics.density - 2f * paddingPx).toInt())
            val fontPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                state.fontSizeSp,
                metrics,
            )
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontPx
                color = textColor
            }
            StaticLayout.Builder
                .obtain(text, 0, text.length, paint, contentWidthPx)
                .setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
                .setIncludePad(false)
                .build()
        }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PADDING_DP.dp, vertical = PADDING_DP.dp),
        factory = { ctx -> PageRenderView(ctx) },
        update = { view ->
            view.layout = layout
            view.invalidate()
        },
    )
}

private class PageRenderView(context: Context) : View(context) {
    var layout: StaticLayout? = null

    override fun onDraw(canvas: Canvas) {
        layout?.draw(canvas)
    }
}

@Composable
private fun ScrollContent(
    state: ReaderUiState,
    onScrollChapterChange: (Int) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.currentChapterIndex.coerceIn(
            0,
            (state.chapters.size - 1).coerceAtLeast(0),
        ),
    )
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { onScrollChapterChange(it) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = PADDING_DP.dp,
            vertical = (PADDING_DP * 2).dp,
        ),
    ) {
        items(state.chapters) { chapter ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = state.fullText.substring(
                        chapter.startOffset,
                        max(chapter.startOffset, chapter.endOffset),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = state.fontSizeSp.sp,
                    lineHeight = (state.fontSizeSp * LINE_SPACING_MULTIPLIER).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ReaderOverlay(
    state: ReaderUiState,
    menuVisible: Boolean,
    onBack: () -> Unit,
    onToggleMode: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
) {
    val dark = LocalIsDark.current
    Box(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(
        visible = menuVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(18.dp))
                .jellyGlass(shape = RoundedCornerShape(18.dp), alpha = 0.9f, dark = dark)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(text = "返回", color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = state.currentChapterTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (state.pageCount > 0) {
                    "${state.currentPage + 1}/${state.pageCount}"
                } else {
                    "全 ${state.chapters.size} 章"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    AnimatedVisibility(
        visible = menuVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .jellyGlass(shape = RoundedCornerShape(22.dp), alpha = 0.9f, dark = dark)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onToggleMode) {
                    Text(
                        text = if (state.mode == ReadMode.PAGING) {
                            "切换滚动"
                        } else {
                            "切换翻页"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(onClick = { onFontSizeChange(-2) }) {
                    Text(text = "A-", color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "${state.fontSizeSp.toInt()}sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onFontSizeChange(2) }) {
                    Text(text = "A+", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
    }
}

private const val PADDING_DP = 16f
private const val LINE_SPACING_MULTIPLIER = 1.2f
