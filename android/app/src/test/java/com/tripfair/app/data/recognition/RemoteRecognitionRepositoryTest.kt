package com.tripfair.app.data.recognition

import com.tripfair.app.data.network.ApiClient
import com.tripfair.app.data.network.ApiResult
import com.tripfair.app.ui.screens.camera.recognitionErrorMessage
import com.tripfair.app.R
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteRecognitionRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteRecognitionRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = ApiClient.retrofit(server.url("/api/v1/").toString(), ApiClient.httpClient()).create(RecognitionApi::class.java)
        repository = RemoteRecognitionRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun respond(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build())
    }

    @Test
    fun `the photo goes up as the multipart field the backend reads`() = runTest {
        respond(
            200,
            """{"items":[{"landmark":"Dragon Bridge","confidence":0.96,
               "place":{"id":4,"source":"GOOGLE","name":"Cầu Rồng","category":"SCENIC","isFavorite":false}},
               {"landmark":"Han River","confidence":0.41,"place":null}],"attribution":"Google Maps"}""",
        )

        val result = (repository.recognize(byteArrayOf(-1, -40, -1, 0)) as ApiResult.Success).value

        assertEquals("Cầu Rồng", result.items.first().place?.name)
        assertNull(result.items.last().place)
        val request = server.takeRequest()
        assertEquals("POST /api/v1/places/recognize", "${request.method} ${request.url.encodedPath}")
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"image\"; filename=\"photo.jpg\""))
        assertTrue(body.contains("Content-Type: image/jpeg"))
    }

    @Test
    fun `a backend without Cloud Vision says recognition is not ready`() = runTest {
        respond(503, """{"statusCode":503,"code":"RECOGNITION_UNAVAILABLE","message":"Vision is not configured"}""")

        val result = repository.recognize(byteArrayOf(1))

        assertEquals(R.string.recognition_error_unavailable, recognitionErrorMessage((result as ApiResult.Failure).error))
    }
}
