package com.dzirbel.kotify.log

/**
 * Returns a read-only view of this [Log], suitable to be exposed from a component that keeps a private [MutableLog].
 */
fun <E : Log.Event> Log<E>.asLog(): Log<E> = ReadonlyLog(this)

private class ReadonlyLog<E : Log.Event>(log: Log<E>) : Log<E> by log
