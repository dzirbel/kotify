package com.dzirbel.kotify.log

interface Logging<E : Log.Event> {
    val log: Log<E>
}
