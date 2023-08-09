package com.dzirbel.kotify.repository.user

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserTable
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel

@Stable
class UserViewModel(user: User) :
    EntityViewModel(user),
    ImageViewModel by EntityImageViewModel(user, UserTable, UserTable.UserImageTable.user)
