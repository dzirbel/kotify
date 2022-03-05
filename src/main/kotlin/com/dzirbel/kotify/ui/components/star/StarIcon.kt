package com.dzirbel.kotify.ui.components.star

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

@Composable
fun StarIcon(
    filled: Boolean,
    starSize: Dp = Dimens.iconSmall,
    contentDescription: String? = null,
) {
    StarIcon(filledPercent = if (filled) 1.0 else 0.0, starSize = starSize, contentDescription = contentDescription)
}

@Composable
fun StarIcon(
    filledPercent: Double,
    starSize: Dp = Dimens.iconSmall,
    contentDescription: String? = null,
) {
    require(filledPercent in 0.0..1.0)

    when (filledPercent) {
        1.0 -> Icon(
            modifier = Modifier.size(starSize),
            imageVector = Icons.Filled.Star,
            contentDescription = contentDescription,
            tint = LocalColors.current.star,
        )

        0.0 -> Icon(
            modifier = Modifier.size(starSize),
            imageVector = Icons.Filled.Star,
            contentDescription = contentDescription,
            tint = LocalColors.current.text.copy(alpha = ContentAlpha.disabled),
        )

        else -> Box {
            Icon(
                modifier = Modifier.size(starSize),
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = LocalColors.current.text.copy(alpha = ContentAlpha.disabled),
            )

            Icon(
                modifier = Modifier.size(starSize).clip(PercentWidthRectangle(percent = filledPercent)),
                imageVector = Icons.Filled.Star,
                contentDescription = contentDescription,
                tint = LocalColors.current.star,
            )
        }
    }
}

/**
 * A simple shape which occupies [percent] of the width along the left side of a particular bounding box and all of the
 * height.
 */
private class PercentWidthRectangle(private val percent: Double) : Shape {
    init {
        require(percent in 0.0..1.0)
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Rectangle(Rect(Offset.Zero, size.copy(width = (size.width * percent).toFloat())))
    }
}
