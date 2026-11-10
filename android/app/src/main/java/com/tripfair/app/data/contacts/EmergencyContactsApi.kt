package com.tripfair.app.data.contacts

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class EmergencyContactListDto(
    val items: List<EmergencyContact>,
    /** Most contacts the account can keep. */
    val limit: Int = 5,
)

/** Only the fields that are set are changed (nulls are left out of the JSON). */
@Serializable
data class ContactPatch(
    val name: String? = null,
    val relation: String? = null,
    val phone: String? = null,
    val receivesSos: Boolean? = null,
)

/** Routes under `me/emergency-contacts`, all with the access token. */
interface EmergencyContactsApi {
    @GET("me/emergency-contacts")
    suspend fun list(): EmergencyContactListDto

    @POST("me/emergency-contacts")
    suspend fun create(@Body body: ContactInput): EmergencyContact

    @PATCH("me/emergency-contacts/{id}")
    suspend fun update(@Path("id") id: Int, @Body body: ContactPatch): EmergencyContact

    @DELETE("me/emergency-contacts/{id}")
    suspend fun delete(@Path("id") id: Int)
}
