package com.dzirbel.kotify.repository.user

import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.util.LazyTransactionStateFlow
import com.dzirbel.kotify.db.util.smallest

class UserViewModel(user: User) {
    val id: String = user.id.value
    val name: String = user.name

    val thumbnailImageUrl = LazyTransactionStateFlow("user $name thumbnail image") { user.images.smallest()?.url }
}
