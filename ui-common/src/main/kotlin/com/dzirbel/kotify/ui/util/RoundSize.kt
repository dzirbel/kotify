package com.dzirbel.kotify.ui.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

fun Size.roundToIntSize() = IntSize(width.roundToInt(), height.roundToInt())
