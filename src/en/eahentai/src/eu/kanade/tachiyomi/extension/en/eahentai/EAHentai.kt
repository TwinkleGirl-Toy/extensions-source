package eu.kanade.tachiyomi.extension.en.eahentai

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import keiyoushi.utils.parseAs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.seconds

@Source
abstract class EAHentai : KeiSource() {

    private val galleryIdPattern = Regex("/a/(\\d+)(?:/|$)")
    private val preflightSemaphore = Semaphore(2)

    override fun OkHttpClient.Builder.configureClient(): OkHttpClient.Builder =
        rateLimit(permits = 2, period = 1.seconds)

    override suspend fun getPopularManga(page: Int): MangasPage =
        fetchListing(listingUrl("/", page), page)

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        fetchListing(listingUrl("/latest", page), page)

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        if (query.isBlank()) return MangasPage(emptyList(), false)

        val url = "$baseUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .apply { if (page > 1) addQueryParameter("page", page.toString()) }
            .build()
        return fetchListing(url.toString(), page)
    }

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/a/${manga.url}"

    override fun getChapterUrl(chapter: SChapter): String = "$baseUrl/a/${chapter.url}"

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.host != baseUrl.toHttpUrl().host || url.pathSegments.firstOrNull() != "a") return null
        val id = url.pathSegments.getOrNull(1)?.toLongOrNull()?.toString() ?: return null
        return createManga(id, loadVerifiedGallery(id))
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        // Details and the single gallery chapter share one API response.
        val gallery = loadVerifiedGallery(manga.url)
        manga.apply {
            title = gallery.title
            thumbnail_url = gallery.thumbnailUri?.takeIf(String::isNotBlank)?.let(::imageUrl)
                ?: thumbnail_url
            genre = gallery.allTags.joinToString()
            author = gallery.author.orEmpty()
            artist = gallery.author.orEmpty()
            update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        }

        val chapter = SChapter.create().apply {
            url = manga.url
            name = "Gallery"
        }
        return SMangaUpdate(manga, listOf(chapter))
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val gallery = loadVerifiedGallery(chapter.url)
        return gallery.images
            .sortedBy { it.sort }
            .mapIndexed { index, image ->
                Page(index, imageUrl = imageUrl(image.imageUri))
            }
    }

    private suspend fun fetchListing(url: String, page: Int): MangasPage {
        val document = client.get(url, Fetch.DOCUMENT).asJsoup()
        val candidates = document.select(".grid > a[aria-label]").mapNotNull { card ->
            val href = card.absUrl("href")
            val id = galleryIdPattern.find(href)?.groupValues?.getOrNull(1) ?: return@mapNotNull null
            val title = card.attr("aria-label").ifBlank {
                card.selectFirst("img")?.attr("alt").orEmpty()
            }
            if (title.isBlank() || ContentBlocklist.blocks(title, emptyList())) return@mapNotNull null

            val image = card.selectFirst("img")
            val thumbnail = image?.absUrl("src").orEmpty().ifBlank {
                image?.absUrl("data-src").orEmpty()
            }
            SManga.create().apply {
                this.url = id
                this.title = title
                thumbnail_url = thumbnail
            }
        }

        // Cards do not expose full tags, so verify every result against the album API.
        val verified = coroutineScope {
            candidates.map { manga ->
                async {
                    try {
                        preflightSemaphore.withPermit {
                            createManga(manga.url, loadVerifiedGallery(manga.url)).apply {
                                if (thumbnail_url.isBlank()) thumbnail_url = manga.thumbnail_url
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        val nextPage = page + 1
        val hasNextPage = document.selectFirst(
            "a[rel=next], a[aria-label*=next i], button[aria-label*=next i], " +
                "button[data-next-page], a[href*='page=$nextPage']",
        ) != null || document.select("button").any {
            it.text().contains("load more", ignoreCase = true)
        }

        return MangasPage(verified, hasNextPage)
    }

    private suspend fun loadVerifiedGallery(id: String): GalleryDto {
        if (id.toLongOrNull() == null) throw IOException("Invalid gallery ID")

        val galleries = client.get(
            "$baseUrl/api/image/album/$id",
            Fetch.XHR.from("$baseUrl/a/$id"),
        ).parseAs<List<GalleryDto>>()
        val gallery = galleries.firstOrNull() ?: throw IOException("Gallery data was not returned")

        if (gallery.tags.isBlank()) {
            throw IOException("Gallery tags could not be verified")
        }
        if (ContentBlocklist.blocks(gallery.title, gallery.allTags)) {
            throw IOException("Gallery blocked by the safety tag filter")
        }
        if (gallery.images.isEmpty()) throw IOException("Gallery has no reader pages")

        return gallery
    }

    private fun createManga(id: String, gallery: GalleryDto): SManga = SManga.create().apply {
        url = id
        title = gallery.title
        thumbnail_url = gallery.thumbnailUri?.takeIf(String::isNotBlank)?.let(::imageUrl).orEmpty()
        genre = gallery.allTags.joinToString()
        author = gallery.author.orEmpty()
        artist = gallery.author.orEmpty()
        update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
    }

    private fun listingUrl(path: String, page: Int): String = "$baseUrl$path".toHttpUrl().newBuilder()
        .apply { if (page > 1) addQueryParameter("page", page.toString()) }
        .build()
        .toString()

    private fun imageUrl(uri: String): String {
        val encoded = uri.split('/').joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("%20", "+")
        }
        return "https://i.eahentai.com/file/ea-gallery/$encoded"
    }

    @Serializable
    private class GalleryDto(
        val albumID: Long,
        val title: String,
        val tags: String,
        val albumType: String,
        val images: List<GalleryImageDto>,
        val thumbnailUri: String? = null,
        val addDt: String? = null,
        val from: String? = null,
        val characters: String? = null,
        val author: String? = null,
    ) {
        val allTags: List<String>
            get() = buildList {
                addAll(tags.split('|'))
                addAll(albumType.split('|'))
                addAll(characters.orEmpty().split('|'))
            }.map(String::trim).filter(String::isNotBlank).distinct()
    }

    @Serializable
    private class GalleryImageDto(
        val imageUri: String,
        val sort: Int = 0,
        val thumbnailUri: String? = null,
        val title: String? = null,
    )
}
