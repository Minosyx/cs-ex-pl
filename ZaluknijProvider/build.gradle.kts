// use an integer for version numbers
version = 2

android {
    namespace = "com.lagradost"
}

cloudstream {
    language = "pl"
    // All of these properties are optional, you can safely remove them

    // description = "Lorem Ipsum"
    authors = listOf("Cloudburst")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1
    tvTypes =
        listOf(
            "Movie",
            "TvSeries",
        )

    iconUrl = "https://www.google.com/s2/favicons?domain=zaluknij.xyz&sz=%size%"
}
