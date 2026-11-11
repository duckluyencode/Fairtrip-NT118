package com.tripfair.app.ui.screens.camera

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripfair.app.R
import com.tripfair.app.data.network.ApiError
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.data.recognition.RecognitionDto
import com.tripfair.app.data.recognition.RecognitionRepository
import com.tripfair.app.data.recognition.loadPhoto
import com.tripfair.app.data.recognition.toJpeg
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** A photo and what was recognized in it; kept in memory only, photos are not stored. */
class RecognizedPhoto(val image: ImageBitmap, val result: RecognitionDto)

@StringRes
fun recognitionErrorMessage(error: ApiError): Int = when (error.code) {
    ApiError.NETWORK -> R.string.error_network
    "RECOGNITION_UNAVAILABLE" -> R.string.recognition_error_unavailable
    "INVALID_IMAGE", "PAYLOAD_TOO_LARGE" -> R.string.recognition_error_image
    "TOO_MANY_REQUESTS" -> R.string.auth_error_too_many
    else -> R.string.error_unexpected
}

/** "Nhận diện địa điểm": reads the photo, uploads it and holds the answer for the result screen. */
class RecognitionViewModel(private val recognition: RecognitionRepository) : ViewModel() {
    var analyzing by mutableStateOf(false)
        private set

    @get:StringRes
    var error by mutableStateOf<Int?>(null)
        private set

    var result by mutableStateOf<RecognizedPhoto?>(null)
        private set

    private var job: Job? = null

    fun analyze(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        job?.cancel()
        job = viewModelScope.launch {
            analyzing = true
            error = null
            val bitmap = loadPhoto(appContext, uri)
            if (bitmap == null) {
                error = R.string.recognition_error_image
            } else {
                when (val answer = recognition.recognize(bitmap.toJpeg())) {
                    is ApiResult.Success -> result = RecognizedPhoto(bitmap.asImageBitmap(), answer.value)
                    is ApiResult.Failure -> error = recognitionErrorMessage(answer.error)
                }
            }
            analyzing = false
        }
    }

    /** The phone has no camera app to take the photo with. */
    fun onNoCamera() {
        error = R.string.recognition_error_no_camera
    }

    fun cancel() {
        job?.cancel()
        analyzing = false
    }

    fun onNavigated() {
        result = null
    }
}
