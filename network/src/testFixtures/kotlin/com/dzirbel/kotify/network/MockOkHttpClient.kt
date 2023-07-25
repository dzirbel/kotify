package com.dzirbel.kotify.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

class MockOkHttpClient(
    private val responseCode: Int = 200,
    private val responseMessage: String = "OK",
    responseBody: ResponseBody = "".toResponseBody("text/plain".toMediaType()),
    private val delayMs: Long? = null,
    val requests: MutableList<Request> = mutableListOf(),
) : OkHttpClient() {
    private val client = Builder().addInterceptor(::intercept).build()

    // extract the response body data since a ResponseBody can only be consumed once
    private val bodyBytes = responseBody.bytes()
    private val bodyMediaType = responseBody.contentType()

    private fun intercept(chain: Interceptor.Chain): Response {
        requests.add(chain.request())
        delayMs?.let { runBlocking { delay(it) } }
        return Response.Builder()
            .code(responseCode)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .message(responseMessage)
            .body(bodyBytes.toResponseBody(bodyMediaType))
            .build()
    }

    override fun newCall(request: Request): Call = client.newCall(request)
}
