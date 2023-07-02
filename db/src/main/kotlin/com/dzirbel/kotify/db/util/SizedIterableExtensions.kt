package com.dzirbel.kotify.db.util

import com.dzirbel.kotify.db.model.Image
import com.dzirbel.kotify.db.model.ImageTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times

/**
 * Returns the [Image] with the largest width x height in this [SizedIterable], or null if it is empty.
 */
internal fun SizedIterable<Image>.largest(): Image? {
    return this
        .copy() // copy to ensure SizedIterable has not already been loaded
        .orderBy(ImageTable.width.times(ImageTable.height) to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
}

/**
 * Returns the [Image] with the smallest width x height in this [SizedIterable], or null if it is empty.
 */
internal fun SizedIterable<Image>.smallest(): Image? {
    return this
        .copy() // copy to ensure SizedIterable has not already been loaded
        .orderBy(ImageTable.width.times(ImageTable.height) to SortOrder.ASC)
        .limit(1)
        .firstOrNull()
}
