package com.selfspotify.oneum.search

import android.content.Context
import com.selfspotify.oneum.download.DownloaderRuntime
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

fun interface YouTubeSearchSource { suspend fun search(query: String): List<SearchVideo> }

class YouTubeSearchRepository(private val context: Context) : YouTubeSearchSource {
    override suspend fun search(query: String): List<SearchVideo> {
        val response = DownloaderRuntime.execute(context, request = {
            YoutubeDLRequest("ytsearch20:$query").apply {
                addOption("--flat-playlist"); addOption("--dump-single-json"); addOption("--skip-download")
                addOption("--socket-timeout", "20"); addOption("--retries", "1")
            }
        }, processId = "search-${UUID.randomUUID()}")
        return SearchResults.parse(response.out)
    }
}

data class SearchState(
    val query: String = "", val submittedQuery: String? = null, val results: List<SearchVideo> = emptyList(),
    val loading: Boolean = false, val error: String? = null,
)

class SearchController(private val source: YouTubeSearchSource, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(SearchState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null
    private var generation = 0

    fun setQuery(query: String) { mutableState.update { it.copy(query = query.take(200)) } }
    fun search() {
        val query = state.value.query.trim()
        if (query.isEmpty()) return
        request?.cancel()
        val current = ++generation
        mutableState.update { it.copy(submittedQuery = query, results = emptyList(), loading = true, error = null) }
        request = scope.launch {
            try {
                val results = source.search(query)
                if (current == generation) mutableState.update { it.copy(results = results, loading = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (current == generation) mutableState.update { it.copy(loading = false,
                    error = "검색하지 못했습니다. 네트워크를 확인한 뒤 다시 시도해 주세요.") }
            }
        }
    }

    fun cancel() {
        generation++
        request?.cancel()
        mutableState.update { it.copy(loading = false, submittedQuery = null, results = emptyList(), error = null) }
    }
}
