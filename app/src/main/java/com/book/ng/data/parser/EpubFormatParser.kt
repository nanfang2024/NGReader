package com.book.ng.data.parser

import android.content.Context
import com.book.ng.domain.model.BookFormat
import com.book.ng.domain.parser.EpubChapter
import com.book.ng.domain.parser.FormatParser
import com.book.ng.domain.parser.ParsedBook
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.parser.epub.EpubParser

@Singleton
class EpubFormatParser @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : FormatParser {

    override fun supports(format: BookFormat): Boolean = format == BookFormat.EPUB

    override suspend fun parse(file: File): ParsedBook {
        val handle = open(file)
        return try {
            handle.parsed
        } finally {
            handle.close()
        }
    }

    suspend fun open(file: File): EpubHandle = withContext(Dispatchers.IO) {
        val assetRetriever = AssetRetriever(context.contentResolver, DefaultHttpClient())
        val asset = assetRetriever.retrieve(file).getOrNull()
            ?: error("无法打开 EPUB 文件：${file.name}")
        val builder = EpubParser().parse(asset, null).getOrNull()
            ?: error("EPUB 解析失败：${file.name}")
        val publication = builder.build()
        try {
            EpubHandle(publication = publication, extractDir = unzipToCache(file))
        } catch (throwable: Throwable) {
            publication.close()
            throw throwable
        }
    }

    private fun unzipToCache(file: File): File {
        val dir = File(context.cacheDir, "epub/${file.name}-${file.length()}")
        if (dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) return dir
        dir.deleteRecursively()
        dir.mkdirs()
        ZipFile(file).use { zip ->
            zip.entries().toList().forEach { entry ->
                if (entry.isDirectory) return@forEach
                val target = File(dir, entry.name)
                if (!target.canonicalPath.startsWith(dir.canonicalPath + File.separator)) return@forEach
                target.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return dir
    }
}

class EpubHandle internal constructor(
    private val publication: Publication,
    private val extractDir: File,
) {
    val parsed: ParsedBook.Epub by lazy {
        val tocTitles = tocTitleByPath()
        val chapters = publication.readingOrder.mapIndexed { index, link ->
            val path = link.pathIn(publication)
            EpubChapter(
                index = index,
                title = link.title?.takeIf { text -> text.isNotBlank() }
                    ?: tocTitles[path]?.takeIf { text -> text.isNotBlank() }
                    ?: "第${index + 1}章",
                href = path,
            )
        }
        ParsedBook.Epub(
            title = publication.metadata.title?.takeIf { text -> text.isNotBlank() } ?: "未命名",
            author = publication.metadata.authors.firstOrNull()?.name,
            chapters = chapters,
        )
    }

    val chapterCount: Int get() = publication.readingOrder.size

    suspend fun chapterHtml(index: Int): String = withContext(Dispatchers.IO) {
        chapterFile(index)?.readText(Charsets.UTF_8) ?: error("章节内容不存在：$index")
    }

    fun chapterBaseUrl(index: Int): String {
        val parent = chapterFile(index)?.parentFile ?: extractDir
        return "file://${parent.absolutePath}/"
    }

    fun close() {
        publication.close()
    }

    private fun chapterFile(index: Int): File? {
        val link = publication.readingOrder.getOrNull(index) ?: return null
        val file = File(extractDir, link.pathIn(publication))
        return file.takeIf { it.isFile }
    }

    private fun Link.pathIn(publication: Publication): String {
        val resolved = runCatching { publication.url(this).path }.getOrNull()
        return (resolved ?: href.toString())
            .substringBefore('#')
            .removePrefix("/")
    }

    private fun tocTitleByPath(): Map<String, String> {
        val acc = mutableMapOf<String, String>()
        fun walk(links: List<Link>) {
            links.forEach { link ->
                val title = link.title?.takeIf { text -> text.isNotBlank() }
                if (title != null) acc.putIfAbsent(link.pathIn(publication), title)
                walk(link.children)
            }
        }
        walk(publication.manifest.tableOfContents)
        return acc
    }
}
