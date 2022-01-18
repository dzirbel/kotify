package com.dzirbel.kotify

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

class MockRequestInterceptor(
    var responseCode: Int = 200,
    var responseMessage: String = "OK",
    responseBody: ResponseBody = "".toResponseBody("text/plain".toMediaType()),
    var delayMs: Long? = null,
    val requests: MutableList<Request> = mutableListOf(),
) {
    val client: OkHttpClient
        get() = OkHttpClient.Builder().addInterceptor(::intercept).build()

    var responseBody: ResponseBody = responseBody
        set(value) {
            field = value
            bodyBytes = responseBody.bytes()
            bodyMediaType = responseBody.contentType()
        }

    // extract the response body data since a ResponseBody can only be consumed once
    private var bodyBytes = responseBody.bytes()
    private var bodyMediaType = responseBody.contentType()

    private fun intercept(chain: Interceptor.Chain): Response {
        requests.add(chain.request())
        delayMs?.let { Thread.sleep(it) }
        return Response.Builder()
            .code(responseCode)
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .message(responseMessage)
            .body(bodyBytes.toResponseBody(bodyMediaType))
            .build()
    }
}
