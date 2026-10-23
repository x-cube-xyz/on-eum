package com.selfspotify.oneum.search

import org.junit.Assert.*
import org.junit.Test

class SearchResultsTest {
    @Test fun `parses flat results and excludes non videos live streams and duplicate entries`() {
        val results = SearchResults.parse("""{"entries":[
            {"id":"gHKT4uU8Zng","title":"테스트 영상","channel":"테스트 채널","duration":65.5,
             "thumbnails":[{"url":"https://i.ytimg.com/one.jpg"},{"url":"https://i.ytimg.com/two.jpg"}]},
            {"id":"gHKT4uU8Zng","title":"중복 영상"},
            {"id":"UCchannel","title":"채널"},
            {"id":"BaW_jenozKc","title":"라이브","live_status":"is_live"},
            {"id":"aqz-KE-bpKQ","title":"공개 예정","live_status":"is_upcoming"},
            null
        ]}""")
        assertEquals(1, results.size)
        val video = results.single()
        assertEquals("https://www.youtube.com/watch?v=gHKT4uU8Zng", video.url)
        assertEquals("테스트 채널", video.channel)
        assertEquals(65, video.durationSeconds)
        assertEquals("https://i.ytimg.com/two.jpg", video.thumbnail)
    }

    @Test fun `missing duration and thumbnails remain usable with uploader fallback`() {
        val video = SearchResults.parse("""{"entries":[{"id":"gHKT4uU8Zng","title":"음악","uploader":"아티스트","duration":null}]}""").single()
        assertNull(video.durationSeconds)
        assertEquals("아티스트", video.channel)
        assertEquals("https://i.ytimg.com/vi/gHKT4uU8Zng/hqdefault.jpg", video.thumbnail)
        assertTrue(SearchResults.parse("""{"entries":[]}""").isEmpty())
    }
}
