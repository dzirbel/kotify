package com.dzirbel.kotify.ui.page

import com.dzirbel.kotify.db.model.ImageSize
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.ui.SpotifyImageCache
import java.io.File

class FakeImageViewModel(val imageUrl: String? = null) : ImageViewModel {
    override fun imageUrlFor(size: ImageSize) = LazyTransactionStateFlow(imageUrl)

    companion object {
        fun fromFile(filename: String): FakeImageViewModel {
            SpotifyImageCache.set("kotify://$filename", File("src/test/resources/$filename"))
            return FakeImageViewModel("kotify://$filename")
        }
    }
}
