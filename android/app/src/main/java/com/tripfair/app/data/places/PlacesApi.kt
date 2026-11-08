package com.tripfair.app.data.places

import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Responses of backend/src/places, search-history and favorites (field names match the API).

@Serializable
data class LocationDto(val lat: Double, val lng: Double)

/** A place in a list. Lists never carry prices. */
@Serializable
data class PlaceSummaryDto(
    val id: Int,
    val source: String,
    val name: String,
    val area: String? = null,
    val city: String? = null,
    /** FOOD, FUN or SCENIC. */
    val category: String,
    val categories: List<String> = emptyList(),
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val imageUrl: String? = null,
    val location: LocationDto? = null,
    val distanceKm: Double? = null,
    val isFavorite: Boolean = false,
    /** "Google Maps" when the data comes from Google and must be credited. */
    val attribution: String? = null,
)

@Serializable
data class OpeningHoursDto(
    val allDay: Boolean,
    val opensAt: String? = null,
    val closesAt: String? = null,
    val closedToday: Boolean = false,
)

@Serializable
data class IncludedInDto(val id: Int, val name: String)

@Serializable
data class PriceDto(
    val min: Int,
    val max: Int,
    val currency: String = "VND",
    /** PARTNER_API, GOOGLE_API or ESTIMATED. */
    val source: String,
    val provider: String,
    val isEstimate: Boolean,
    val includedIn: IncludedInDto? = null,
)

@Serializable
data class PlaceDetailDto(
    val id: Int,
    val source: String,
    val name: String,
    val area: String? = null,
    val category: String,
    val categories: List<String> = emptyList(),
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val imageUrl: String? = null,
    val location: LocationDto? = null,
    val distanceKm: Double? = null,
    val isFavorite: Boolean = false,
    val attribution: String? = null,
    val description: String? = null,
    val visitMinutes: Int,
    val openingHours: OpeningHoursDto? = null,
    val price: PriceDto,
    val mapsUri: String? = null,
)

@Serializable
data class PlacePageDto(
    val items: List<PlaceSummaryDto>,
    val page: Int,
    val limit: Int,
    /** Null for Google results: follow nextPageToken instead. */
    val total: Int? = null,
    val nextPageToken: String? = null,
)

@Serializable
data class PlaceListDto(val items: List<PlaceSummaryDto>)

@Serializable
data class SuggestionDto(val id: Int, val name: String, val area: String? = null)

@Serializable
data class HistoryEntryDto(val id: Int, val query: String, val category: String? = null, val searchedAt: String)

@Serializable
data class HistoryListDto(val items: List<HistoryEntryDto>)

interface PlacesApi {
    /** Search (saved to the history when [q] is set), or browse around [lat]/[lng]. */
    @GET("places")
    suspend fun search(
        @Query("q") q: String?,
        @Query("category") category: String?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("page") page: Int?,
        @Query("pageToken") pageToken: String?,
        @Query("limit") limit: Int? = null,
    ): PlacePageDto

    @GET("places/suggestions")
    suspend fun suggestions(
        @Query("q") q: String,
        @Query("sessionToken") sessionToken: String?,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
    ): List<SuggestionDto>

    @GET("places/{id}")
    suspend fun detail(
        @Path("id") id: Int,
        @Query("sessionToken") sessionToken: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
    ): PlaceDetailDto

    @GET("me/search-history")
    suspend fun history(@Query("limit") limit: Int? = null): HistoryListDto

    @DELETE("me/search-history/{id}")
    suspend fun deleteHistoryEntry(@Path("id") id: Int)

    @DELETE("me/search-history")
    suspend fun clearHistory()

    @GET("me/favorites")
    suspend fun favorites(@Query("category") category: String? = null): PlaceListDto

    @PUT("me/favorites/{placeId}")
    suspend fun addFavorite(@Path("placeId") placeId: Int)

    @DELETE("me/favorites/{placeId}")
    suspend fun removeFavorite(@Path("placeId") placeId: Int)
}
