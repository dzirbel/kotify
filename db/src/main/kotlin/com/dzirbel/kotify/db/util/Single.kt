package com.dzirbel.kotify.db.util

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnSet
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.select

/**
 * Convenience function which selects a single row of a single [column] from this [ColumnSet] (i.e. table or join),
 * matching the given [where] clause.
 */
fun <T> ColumnSet.single(
    column: Column<T>,
    order: Pair<Expression<*>, SortOrder>? = null,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): T? {
    return slice(column)
        .select(where = where)
        .run { if (order == null) this else orderBy(order) }
        .limit(1)
        .firstOrNull()
        ?.get(column)
}

fun <A, B> ColumnSet.single(
    column1: Column<A>,
    column2: Column<B>,
    order: Pair<Expression<*>, SortOrder>? = null,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): Pair<A, B>? {
    return slice(column1, column2)
        .select(where = where)
        .run { if (order == null) this else orderBy(order) }
        .limit(1)
        .firstOrNull()
        ?.let { row -> Pair(row[column1], row[column2]) }
}

fun <A, B, C> ColumnSet.single(
    column1: Column<A>,
    column2: Column<B>,
    column3: Column<C>,
    order: Pair<Expression<*>, SortOrder>? = null,
    where: SqlExpressionBuilder.() -> Op<Boolean>,
): Triple<A, B, C>? {
    return slice(column1, column2, column3)
        .select(where = where)
        .run { if (order == null) this else orderBy(order) }
        .limit(1)
        .firstOrNull()
        ?.let { row -> Triple(row[column1], row[column2], row[column3]) }
}
