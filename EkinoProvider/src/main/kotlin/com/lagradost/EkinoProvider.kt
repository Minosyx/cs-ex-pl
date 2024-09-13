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
            
            val items =
                l.select("li").map { i -> 
                    val leftScope = i.select(".scope_left")
                    val rightScope = i.select(".scope_right")

                    val name = rightScope.select(".title > a").text()
                    val href = mainUrl + leftScope.select("a").attr("href")
                    val poster = imagePrefix + leftScope.select("img[src]").attr("src")
                    val year = rightScope.select(".cates").text().takeUnless { it.isBlank() }?.toIntOrNull()
                    MovieSearchResponse(
                        name, 
                        href, 
                        this.name, 
                        TvType.Movie, 
                        poster, 
                        year, 
                    )
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
                if (img != null) img = mainUrl + img
                if (type === TvType.TvSeries) {
                    TvSeriesSearchResponse(
                        name,
                        href,
                        this.name,
                        type,
                        img,
                        null,
                    )
                } else {
                    MovieSearchResponse(
                        name,
                        href,
                        this.name,
                        type,
                        img,
                        null,
                    )
                }
            }
        }
        return getVideos(TvType.Movie, movies) + getVideos(TvType.TvSeries, series)
    }
}