package com.dzirbel.kotify.db.util

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select

/**
 * Joins the table for the desired entities of type [E] on the given [idColumn], returning entities for which the
 * [idColumn] matches [id].
 */
fun <ID : Comparable<ID>, EID : Comparable<EID>, E : Entity<EID>> EntityClass<EID, E>.entitiesFor(
    id: ID,
    idColumn: Column<EntityID<ID>>,
    joinTable: Table = idColumn.table,
    distinct: Boolean = true,
): Iterable<E> {
    val entityTable = this.table
    return entityTable
        .innerJoin(joinTable)
        .slice(entityTable.columns)
        .select { idColumn eq id }
        .withDistinct(distinct)
        .let { this.wrapRows(it) }
}
