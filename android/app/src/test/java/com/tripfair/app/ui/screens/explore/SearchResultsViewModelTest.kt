package com.tripfair.app.ui.screens.explore

import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.places.HistoryEntryDto
import com.tripfair.app.data.places.PageCursor
import com.tripfair.app.data.places.PlaceDetailDto
import com.tripfair.app.data.places.PlacePage
import com.tripfair.app.data.places.PlaceSummaryDto
import com.tripfair.app.data.places.PlacesRepository
import com.tripfair.app.data.places.SuggestionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

private fun place(id: Int, favorite: Boolean = false) =
    PlaceSummaryDto(id, "GOOGLE", "Place $id", category = "SCENIC", isFavorite = favorite)

/** Two pages of results; favorites can be told to fail. */
private class FakePlaces : PlacesRepository {
    var failSearch = false
    var failFavorite = false
    val categories = mutableListOf<String?>()

    override suspend fun search(query: String?, category: String?, cursor: PageCursor?): ApiResult<PlacePage> {
        categories += category
        if (failSearch) return ApiResult.Failure(ApiError(ApiError.NETWORK))
        return if (cursor == null) {
            ApiResult.Success(PlacePage(listOf(place(1), place(2)), PageCursor.Token("next")))
        } else {
            ApiResult.Success(PlacePage(listOf(place(2), place(3)), null))
        }
    }

    override suspend fun setFavorite(id: Int, favorite: Boolean): ApiResult<Unit> =
        if (failFavorite) ApiResult.Failure(ApiError(ApiError.NETWORK)) else ApiResult.Success(Unit)

    override suspend fun suggestions(text: String, sessionToken: String?): ApiResult<List<SuggestionDto>> = TODO()
    override suspend fun detail(id: Int, sessionToken: String?): ApiResult<PlaceDetailDto> = TODO()
    override suspend fun history(): ApiResult<List<HistoryEntryDto>> = TODO()
    override suspend fun deleteHistoryEntry(id: Int): ApiResult<Unit> = TODO()
    override suspend fun clearHistory(): ApiResult<Unit> = TODO()
    override suspend fun favorites(category: String?): ApiResult<List<PlaceSummaryDto>> = TODO()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchResultsViewModelTest {
    private val places = FakePlaces()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads the first page, then appends the next without duplicates`() {
        val viewModel = SearchResultsViewModel(places)
        viewModel.start("Đà Nẵng")
        assertEquals(listOf(1, 2), viewModel.state.items.map { it.id })

        viewModel.loadMore()

        assertEquals(listOf(1, 2, 3), viewModel.state.items.map { it.id })
        assertEquals(null, viewModel.state.next)
    }

    @Test
    fun `a failed search shows an error and a category change searches again`() {
        places.failSearch = true
        val viewModel = SearchResultsViewModel(places)
        viewModel.start("Đà Nẵng")
        assertEquals(R.string.error_network, viewModel.state.error)

        places.failSearch = false
        viewModel.onCategoryChange("FOOD")

        assertEquals(null, viewModel.state.error)
        assertEquals(listOf(null, "FOOD"), places.categories)
    }

    @Test
    fun `a refused favorite is undone`() {
        val viewModel = SearchResultsViewModel(places)
        viewModel.start("Đà Nẵng")
        places.failFavorite = true

        viewModel.toggleFavorite(1)

        assertFalse(viewModel.state.items.first { it.id == 1 }.isFavorite)
    }
}
