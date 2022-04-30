package com.dzirbel.kotify.network.model

/**
 * Represents a year-month-date combination parse-able from spotify network models, which may not include a month and/or
 * day.
 */
data class ReleaseDate(
    val year: Int,
    val month: Int?,
    val day: Int?,
) : Comparable<ReleaseDate> {
    override fun compareTo(other: ReleaseDate): Int {
        return year.compareTo(other.year).takeIf { it != 0 }
            ?: other.month?.let { month?.compareTo(other.month) }?.takeIf { it != 0 }
            ?: other.day?.let { day?.compareTo(other.day) }?.takeIf { it != 0 }
            ?: 0
    }

    override fun toString(): String {
        return buildString {
            append(year)

            if (month != null) {
                append("-")
                append("%02d".format(month))

                if (day != null) {
                    append("-")
                    append("%02d".format(day))
                }
            }
        }
    }

    companion object {
        private val YEAR_REGEX = """(\d{4})""".toRegex()
        private val MONTH_REGEX = """(\d{4})-(\d{2})""".toRegex()
        private val DAY_REGEX = """(\d{4})-(\d{2})-(\d{2})""".toRegex()

        /**
         * Attempts to parse the given [releaseDate] string, of the format YYYY-MM-DD, YYYY-MM, or YYYY.
         */
        @Suppress("MagicNumber")
        fun parse(releaseDate: String): ReleaseDate? {
            val result = DAY_REGEX.matchEntire(releaseDate)
                ?: MONTH_REGEX.matchEntire(releaseDate)
                ?: YEAR_REGEX.matchEntire(releaseDate)

            val year = result?.groups?.getOrNull(1)?.value?.toIntOrNull()

            return year?.let {
                val month = result.groups.getOrNull(2)?.value?.toIntOrNull()
                val day = month?.let { result.groups.getOrNull(3)?.value?.toIntOrNull() }

                ReleaseDate(year = year, month = month, day = day)
            }
        }

        private fun MatchGroupCollection.getOrNull(index: Int): MatchGroup? {
            return if (index in indices) get(index) else null
        }
    }
}
