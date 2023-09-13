package com.dzirbel.kotify.repository.genre

import com.dzirbel.kotify.db.model.Genre

data class GenreViewModel(val name: String) {
    constructor(genre: Genre) : this(genre.name)
}
