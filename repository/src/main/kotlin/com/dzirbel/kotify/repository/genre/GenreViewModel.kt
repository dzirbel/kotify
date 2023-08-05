package com.dzirbel.kotify.repository.genre

import com.dzirbel.kotify.db.model.Genre

class GenreViewModel(genre: Genre) {
    val name: String = genre.name
}
