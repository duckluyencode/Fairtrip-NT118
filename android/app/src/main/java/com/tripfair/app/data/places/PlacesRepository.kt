package com.tripfair.app.data.places

import com.tripfair.app.data.location.LocationProvider
import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.apiCall

/**
 * Where the next page of results starts. The sample catalog pages by number; Google
 * results page with a token. Callers only pass back what they got.
 */
sealed interface PageCursor {
    data class Number(val page: Int) : PageCursor

    data class Token(val token: String) : PageCursor
}

data class PlacePage(val items: List<PlaceSummaryDto>, val next: PageCursor?)

/** Places, search history and favorites on the TripFair backend. */
interface PlacesRepository {
    /** [category] is FOOD, FUN or SCENIC. A search with [query] is saved to the history. */
    suspend fun search(query: String?, category: String?, cursor: PageCursor? = null): ApiResult<PlacePage>

    /** Pass the same [sessionToken] until a place is opened, then pass it to [detail]. */
    suspend fun suggestions(text: String, sessionToken: String?): ApiResult<List<SuggestionDto>>

    suspend fun detail(id: Int, sessionToken: String? = null): ApiResult<PlaceDetailDto>

    suspend fun history(): ApiResult<List<HistoryEntryDto>>

    suspend fun deleteHistoryEntry(id: Int): ApiResult<Unit>

    suspend fun clearHistory(): ApiResult<Unit>

    suspend fun favorites(category: String? = null): ApiResult<List<PlaceSummaryDto>>

    /** Saving twice or removing twice is fine (the backend ignores repeats). */
    suspend fun setFavorite(id: Int, favorite: Boolean): ApiResult<Unit>
}

/**
 * With a [location] provider, searches and suggestions are sent with the user's position
 * (when they allowed it), so results come back nearest first and with distances.
 */
class RemotePlacesRepository(
    private val api: PlacesApi,
    private val location: LocationProvider? = null,
) : PlacesRepository {
    private val json = ApiClient.json

    override suspend fun search(query: String?, category: String?, cursor: PageCursor?): ApiResult<PlacePage> {
        val near = location?.current()
        val result = apiCall(json) {
            api.search(
                q = query?.trim()?.takeIf { it.isNotEmpty() },
                category = category,
                lat = near?.lat,
                lng = near?.lng,
                page = (cursor as? PageCursor.Number)?.page,
                pageToken = (cursor as? PageCursor.Token)?.token,
            )
        }
        return when (result) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(PlacePage(result.value.items, nextCursor(result.value)))
        }
    }

    private fun nextCursor(page: PlacePageDto): PageCursor? = when {
        page.nextPageToken != null -> PageCursor.Token(page.nextPageToken)
        page.total != null && page.page * page.limit < page.total -> PageCursor.Number(page.page + 1)
        else -> null
    }

    override suspend fun suggestions(text: String, sessionToken: String?) =
        apiCall(json) {
            val near = location?.current()
            api.suggestions(text, sessionToken, near?.lat, near?.lng)
        }

    override suspend fun detail(id: Int, sessionToken: String?) =
        apiCall(json) { api.detail(id, sessionToken) }

    override suspend fun history(): ApiResult<List<HistoryEntryDto>> =
        when (val result = apiCall(json) { api.history(limit = 50) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.value.items)
        }

    override suspend fun deleteHistoryEntry(id: Int) = apiCall(json) { api.deleteHistoryEntry(id) }

    override suspend fun clearHistory() = apiCall(json) { api.clearHistory() }

    override suspend fun favorites(category: String?): ApiResult<List<PlaceSummaryDto>> =
        when (val result = apiCall(json) { api.favorites(category) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(result.value.items)
        }

    override suspend fun setFavorite(id: Int, favorite: Boolean) =
        apiCall(json) { if (favorite) api.addFavorite(id) else api.removeFavorite(id) }
}
