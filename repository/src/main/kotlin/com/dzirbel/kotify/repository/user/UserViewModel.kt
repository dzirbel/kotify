package com.dzirbel.kotify.repository.user

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.util.CurrentTime
import java.time.Instant

@Stable
data class UserViewModel(
    override val id: String,
    override val name: String,
    override val uri: String? = null,
    override val updatedTime: Instant = CurrentTime.instant,
    override val fullUpdatedTime: Instant? = null,
    val images: ImageViewModel = EntityImageViewModel(id, UserTable.UserImageTable.user),
) : EntityViewModel, ImageViewModel by images {

    constructor(user: User) : this(
        id = user.id.value,
        uri = user.uri,
        name = user.name,
        updatedTime = user.updatedTime,
        fullUpdatedTime = user.fullUpdatedTime,
    )

    override fun equals(other: Any?) = other is UserViewModel && id == other.id
    override fun hashCode() = id.hashCode()
}
