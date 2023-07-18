package com.dzirbel.kotify.util

/**
 * Returns the result of formatting this [Int] as an amount with the given singular [unit], e.g. "2 units".
 */
fun Int.formattedWithUnit(unit: String): String {
    return if (this == 1) "$this $unit" else "$this ${unit}s"
}

/**
 * Returns the result of formatting this [Long] as an amount with the given singular [unit], e.g. "2 units".
 */
fun Long.formattedWithUnit(unit: String): String {
    return if (this == 1L) "$this $unit" else "$this ${unit}s"
}
