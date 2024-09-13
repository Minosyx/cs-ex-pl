package com.lagradost

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class EkinoProvider : MainAPI() {
    override var mainUrl = "https://www.ekino-tv.pl/"
    override var name = "Ekino"
    override var lang = "pl"
    override val hasMainPage = true
    override val usesWebView = true
    override val supportedTypes =
        setOf(
            TvType.TvSeries,
            TvType.Movie,
        )
    val imagePrefix = "https:"

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest,
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val lists = document.select(".mostPopular")
        val categories = ArrayList<HomePageList>()
        for (l in lists) {
            var title =
                capitalizeString(
                    l
                        .select("h4")
                        .text()
                        .lowercase()
                        .trim(),
                )
            val subtitle = capitalizeStringNullable(
                l
                    .select(".sm")
                    .text()
                    .lowercase()
                    .trim(),
            )
            if (subtitle != null) title += " $subtitle"
            var isSeries = title.contains("serial")
            val items =
                l.select("li").map { i -> 
                    val leftScope = i.select(".scope_left")
                    val rightScope = i.select(".scope_right")

                    val name = rightScope.select(".title > a").text()
                    val href = mainUrl + leftScope.select("a").attr("href")
                    val poster = imagePrefix + leftScope.select("img[src]").attr("src").replace("/thumb/", "/normal/")
                    val year = rightScope.select(".cates").text().takeUnless { it.isBlank() }?.toIntOrNull()
                    
                    if (isSeries) {
                        TvSeriesSearchResponse(
                            name,
                            href,
                            this.name,
                            TvType.TvSeries,
                            poster,
                            year,
                            null,
                        )
                    } else {
                        MovieSearchResponse(
                            name, 
                            href, 
                            this.name, 
                            TvType.Movie,
                            poster, 
                            year, 
                        )
                    }
                    // there might be needed an option for series
                }   

            categories.add(HomePageList(title, items))
        }
        return HomePageResponse(categories)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/qf/?q=$query"
        val document = app.get(url).document
        val lists = document.select(".movie-wrap > :not(div.menu-wrap)")
        val movies = lists[0].select(".movies-list-item")
        val series = lists[1].select(".movies-list-item")

        if (movies.isEmpty() && series.isEmpty()) return ArrayList()

        fun getVideos(
            type: TvType,
            items: Elements,
        ): List<SearchResponse> {
            return items.mapNotNull { i ->
                var href = i.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                var img = i.selectFirst("a > img[src]")?.attr("src")
                val name = i.selectFirst(".title > a")?.text() ?: return@mapNotNull null
                if (href.isNotEmpty()) href = mainUrl + href 
                if (img != null) {
                    img = mainUrl + img
                    img = img.replace("/thumb/", "/normal/")
                }
                val year = i.select(".cates").text().takeUnless { it.isBlank() }?.toIntOrNull()
                if (type === TvType.TvSeries) {
                    TvSeriesSearchResponse(
                        name,
                        href,
                        this.name,
                        type,
                        img,
                        year,
                        null,
                    )
                } else {
                    MovieSearchResponse(
                        name,
                        href,
                        this.name,
                        type,
                        img,
                        year,
                    )
                }
            }
        }
        return getVideos(TvType.Movie, movies) + getVideos(TvType.TvSeries, series)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val documentTitle = document.select("title").text().trim()

        if (documentTitle.startsWith("Logowanie")) {
            throw RuntimeException(
                "This page seems to be locked behind a login-wall on the website, unable to scrape it. If it is not please report it.",
            )
        }

        val title = document.select("h1.title").text()
        val data = document.select(".playerContainer").outerHtml()
        val posterUrl = mainUrl + document.select(".moviePoster").attr("src")
        val year = 
            document
                .select(".catBox .cat .a").text().toIntOrNull()
        val plot = document.select(".descriptionMovie").text()
        val episodesElements = document.select(".list-series > a[href]")
        if (episodesElements.isEmpty()) {
            return MovieLoadResponse(title, url, name, TvType.Movie, data, posterUrl, year, plot)
        }
        val episodes = episodesElements
            .mapNotNull { episode ->
                val e = episode.text()
                val regex = Regex("""\[\d+\]""").findAll(e)
                val s_e_list = regex.map { it.value }.toList()
                Episode(
                    mainUrl + episode.attr("href"),
                    e.trim(),
                    s_e_list[0].toInt(),
                    s_e_list[1].toInt(),
                )
            }.toMutableList()

        return TvSeriesLoadResponse(
            title,
            url,
            name,
            TvType.TvSeries,
            episodes,
            posterUrl,
            year,
            plot,
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val document =
            if (data.startsWith("http")) {
                app
                    .get(data)
                    .document
                    .select(".playerContainer .tab-content")
                    .first()
            } else {
                Jsoup.parse(data)
            }
        
        // val namesMap = document.select(".players a").map { 
        //     i -> i.text() to i.attr("href")
        // }

        document?.select(".players a")?.apmap { item ->
            val link = document.select(".playerContainer .tab-content > div#" + item.attr("href") + " a.buttonprch").attr("href")
            loadExtractor(link, subtitleCallback, callback)
        }
        return true
    }
}

data class LinkElement(
    @JsonProperty("src") val src: String,
)