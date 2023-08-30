package com.dzirbel.kotify.log

/**
 * Returns a read-only view of this [Log], suitable to be exposed from a component that keeps a private [MutableLog].
 */
fun <T> Log<T>.asLog(): Log<T> = ReadonlyLog(this)

private class ReadonlyLog<T>(log: Log<T>) : Log<T> by log
