package com.selfspotify.oneum.search

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SearchControllerTest {
    private val video = SearchVideo("gHKT4uU8Zng", "음악", "채널", 5, null)

    @Test fun `search trims query and ignores blank input`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val queries = mutableListOf<String>()
            val controller = SearchController(YouTubeSearchSource { queries += it; listOf(video) }, scope)
            controller.setQuery("   "); controller.search()
            assertTrue(queries.isEmpty())
            controller.setQuery("  노래 검색  "); controller.search()
            assertEquals(listOf("노래 검색"), queries)
            assertEquals(listOf(video), controller.state.value.results)
            assertFalse(controller.state.value.loading)
        } finally { scope.cancel() }
    }

    @Test fun `late cancelled result cannot replace the newer query`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val old = CompletableDeferred<List<SearchVideo>>()
            val controller = SearchController(YouTubeSearchSource { query ->
                if (query == "old") withContext(NonCancellable) { old.await() } else listOf(video)
            }, scope)
            controller.setQuery("old"); controller.search()
            controller.setQuery("new"); controller.search()
            old.complete(emptyList())
            yield()
            assertEquals("new", controller.state.value.submittedQuery)
            assertEquals(listOf(video), controller.state.value.results)
            assertFalse(controller.state.value.loading)
        } finally { scope.cancel() }
    }

    @Test fun `failure can be retried and cancellation clears pending state`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var fail = true
            val controller = SearchController(YouTubeSearchSource { if (fail) error("network") else listOf(video) }, scope)
            controller.setQuery("음악"); controller.search()
            assertNotNull(controller.state.value.error)
            fail = false; controller.search()
            assertNull(controller.state.value.error)
            assertEquals(listOf(video), controller.state.value.results)
            controller.cancel()
            assertNull(controller.state.value.submittedQuery)
            assertTrue(controller.state.value.results.isEmpty())
        } finally { scope.cancel() }
    }
}
