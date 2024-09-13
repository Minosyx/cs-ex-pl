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

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest,
    ): HomePageResponse {
        val document = app.get(mainUrl).document
        val lists = document.select(".mostPopular")
        val categories = ArrayList<HomePageList>()
        for (l in lists) {
            val title =
                capitalizeString(
                    l
                        .select("h4")
                        .text()
                        .lowercase()
                        .trim(),
                )
            val items =
                l.select("li").map { i -> 
                    val leftScope = i.select(".scope_left")
                    val rightScope = i.select(".scope_right")

                    val name = rightScope.select(".title a").text()
                    val href = leftScope.select("a").attr("href")
                    val poster = leftScope.select("img[src]").attr("src")
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
        return ArrayList();
    }
}