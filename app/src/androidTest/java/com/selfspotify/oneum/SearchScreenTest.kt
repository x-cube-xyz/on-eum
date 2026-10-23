package com.selfspotify.oneum

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.selfspotify.oneum.search.SearchState
import com.selfspotify.oneum.search.SearchVideo
import com.selfspotify.oneum.ui.OneumTheme
import com.selfspotify.oneum.ui.SearchScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun queryResultSelectionAndClear() {
        val video = SearchVideo("gHKT4uU8Zng", "검색 결과 테스트", "테스트 채널", 65, null)
        val state = mutableStateOf(SearchState())
        var selected: SearchVideo? = null
        rule.setContent {
            OneumTheme {
                SearchScreen(state.value, emptySet(), false,
                    onQuery = { state.value = state.value.copy(query = it) },
                    onSearch = { state.value = state.value.copy(submittedQuery = state.value.query, results = listOf(video)) },
                    onCancel = { state.value = SearchState(query = state.value.query) }, onSelect = { selected = it })
            }
        }
        rule.onNodeWithTag("youtube-search-button").assertIsNotEnabled()
        rule.onNodeWithTag("youtube-search-query").performTextInput("테스트 음악")
        rule.onNodeWithTag("youtube-search-query").performImeAction()
        rule.onNodeWithText("테스트 채널").assertIsDisplayed()
        rule.onNodeWithTag("search-result-${video.id}").performClick()
        assertEquals(video.url, selected?.url)
        rule.onNodeWithContentDescription("검색어 지우기").performClick()
        rule.onNodeWithText("어떤 음악을 찾고 있나요?").assertIsDisplayed()
    }

    @Test fun loadingCanBeCancelledAndFailureRetried() {
        val state = mutableStateOf(SearchState(query = "음악", submittedQuery = "음악", loading = true))
        rule.setContent {
            OneumTheme {
                SearchScreen(state.value, emptySet(), false, onQuery = {},
                    onSearch = { state.value = state.value.copy(error = null, loading = true) },
                    onCancel = { state.value = state.value.copy(loading = false, error = "네트워크를 확인해 주세요.") }, onSelect = {})
            }
        }
        rule.onNodeWithText("검색 취소").performClick()
        rule.onNodeWithText("검색을 완료하지 못했어요").assertIsDisplayed()
        rule.onNodeWithText("다시 검색").performClick()
        rule.onNodeWithText("유튜브에서 음악을 찾고 있습니다").assertIsDisplayed()
    }
}
