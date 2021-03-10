package com.dominiczirbel.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.skija.Image

/**
 * Loads an image from a remote [url] and exposes the loaded value as a [State] of [ImageBitmap], where the state's
 * current value will reflect the loaded state, initially null.
 */
@Composable
fun LoadImage(url: String, client: OkHttpClient = Spotify.configuration.okHttpClient): State<ImageBitmap?> {
    return remember {
        flow {
            val request = Request.Builder().url(url).build()
            client.newCall(request).await().use { response ->
                // TODO blocking method call
                response.body?.bytes()?.let {
                    emit(Image.makeFromEncoded(it).asImageBitmap())
                }
            }
        }
    }.collectAsState(initial = null, context = Dispatchers.IO)
}
