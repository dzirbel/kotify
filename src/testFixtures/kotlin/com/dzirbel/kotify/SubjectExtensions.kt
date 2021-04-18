package com.dzirbel.kotify

import com.google.common.truth.Subject

/**
 * Fails if the subject is null when [shouldBeNull] is false or non-null when [shouldBeNull] is true.
 */
fun Subject.isNullIf(shouldBeNull: Boolean) {
    if (shouldBeNull) isNull() else isNotNull()
}
