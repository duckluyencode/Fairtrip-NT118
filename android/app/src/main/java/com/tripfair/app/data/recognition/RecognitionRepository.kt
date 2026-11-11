package com.tripfair.app.data.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.network.apiCall
import com.tripfair.app.data.places.PlaceSummaryDto
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@Serializable
data class RecognizedPlaceDto(
    /** What Cloud Vision saw, e.g. "Dragon Bridge". */
    val landmark: String,
    /** 0–1 */
    val confidence: Double,
    /** The matching place (no price, like every list); null when none was found. */
    val place: PlaceSummaryDto? = null,
)

@Serializable
data class RecognitionDto(
    /** Most likely first; empty when no landmark was recognized. */
    val items: List<RecognizedPlaceDto> = emptyList(),
    val attribution: String? = null,
)

interface RecognitionApi {
    @Multipart
    @POST("places/recognize")
    suspend fun recognize(@Part image: MultipartBody.Part): RecognitionDto
}

/** "Nhận diện địa điểm": sends a photo to the backend, which asks Google Cloud Vision. */
interface RecognitionRepository {
    /** [jpeg] is a JPEG of at most 4 MB (see [loadPhoto] and [toJpeg]). */
    suspend fun recognize(jpeg: ByteArray): ApiResult<RecognitionDto>
}

class RemoteRecognitionRepository(private val api: RecognitionApi) : RecognitionRepository {
    override suspend fun recognize(jpeg: ByteArray): ApiResult<RecognitionDto> = apiCall(ApiClient.json) {
        val body = jpeg.toRequestBody("image/jpeg".toMediaType())
        api.recognize(MultipartBody.Part.createFormData("image", "photo.jpg", body))
    }
}

/** Longest side of the photo that is uploaded: enough for Vision, far below the 4 MB limit. */
private const val MAX_SIDE = 1600

/**
 * Reads the photo at [uri] (a camera capture or a picked image), scaled down so its longest
 * side is at most [MAX_SIDE] and turned the way it was taken. Null when it cannot be read.
 */
suspend fun loadPhoto(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies the photo's EXIF rotation.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > MAX_SIDE) decoder.setTargetSampleSize((longest + MAX_SIDE - 1) / MAX_SIDE)
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, longest / MAX_SIDE) }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

fun Bitmap.toJpeg(quality: Int = 85): ByteArray =
    ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
