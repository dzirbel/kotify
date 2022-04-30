package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.openInBrowser

private const val ANNOTATION_TAG_LINK = "link"

/**
 * Defines a simple DSL for constructing an [AnnotatedString] which has plain [text] and [link] components.
 */
interface LinkElementScope {
    /**
     * Appends a plain text element with the given [text].
     */
    fun text(text: String)

    /**
     * Appends a linked text, with the given visible [text] and annotated [link] (typically a URL but can be a generic
     * string).
     *
     * If [link] is null, this is equivalent to [text].
     */
    fun link(text: String, link: String?)

    /**
     * Appends [elements] as a [separator]-joined list (where the separate is plain [text]), invoking [onElement] to
     * generate the link element(s) for each value of [elements].
     */
    fun <T> list(elements: List<T>, separator: String = ", ", onElement: LinkElementScope.(T) -> Unit) {
        elements.forEachIndexed { index, element ->
            onElement(element)

            if (index != elements.lastIndex) {
                text(separator)
            }
        }
    }
}

/**
 * A common [SpanStyle] which corresponds to the common URL hyperlink style, underlined and colored with
 * [Colors.primary].
 */
@Composable
fun HyperlinkSpanStyle() = SpanStyle(color = LocalColors.current.primary, textDecoration = TextDecoration.Underline)

/**
 * Displays text build by [elements] with embedded links, allowing styling the links based on the hover state of the
 * link and handling link clicks.
 *
 * TODO right click to open menu with coping the url as an option
 */
@Composable
fun LinkedText(
    modifier: Modifier = Modifier,
    key: Any? = null,
    style: TextStyle = LocalTextStyle.current,
    unhoveredSpanStyle: SpanStyle = SpanStyle(),
    hoveredSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    onClickLink: (String) -> Unit = { openInBrowser(it) },
    elements: LinkElementScope.() -> Unit,
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val hoveredOffset = remember { mutableStateOf<Int?>(null) }
    val hoveringLink = remember { mutableStateOf(false) }

    // TODO maybe optimize if unhoveredSpanStyle and hoveredSpanStyle are the same
    val text = remember(hoveredOffset.value, key) {
        LinkElementBuilder(
            hoveredOffset = hoveredOffset.value,
            unhoveredSpanStyle = unhoveredSpanStyle,
            hoveredSpanStyle = hoveredSpanStyle,
        )
            .apply(elements)
            .build()
    }

    val clickModifier = Modifier.pointerInput(text) {
        detectTapGestures { offset ->
            text.characterOffset(offset, layoutResult.value)
                ?.let { text.linkAnnotationAtOffset(it) }
                ?.let(onClickLink)
        }
    }

    val hoverModifier = Modifier
        .pointerMoveFilter(
            onMove = { offset ->
                val characterOffset = text.characterOffset(offset, layoutResult.value)
                val link = characterOffset?.let { text.linkAnnotationAtOffset(it) }
                hoveredOffset.value = characterOffset
                hoveringLink.value = link != null
                true
            },
            onExit = {
                hoveredOffset.value = null
                hoveringLink.value = false
                true
            },
        )
        .pointerHoverIcon(if (hoveringLink.value) PointerIconDefaults.Hand else PointerIconDefaults.Default)

    val textColor = style.color.takeOrElse {
        LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
    }

    BasicText(
        text = text,
        modifier = modifier.then(clickModifier).then(hoverModifier),
        style = style.copy(color = textColor),
        onTextLayout = { layoutResult.value = it },
    )
}

/**
 * Implementation of [LinkElementScope] which can [build] an [AnnotatedString] based on the given hover state and
 * styles.
 */
private class LinkElementBuilder(
    private val hoveredOffset: Int?,
    private val unhoveredSpanStyle: SpanStyle,
    private val hoveredSpanStyle: SpanStyle,
) : LinkElementScope {
    private val builder = AnnotatedString.Builder()
    private var currentOffset = 0

    override fun text(text: String) {
        builder.append(text)
        currentOffset += text.length
    }

    override fun link(text: String, link: String?) {
        if (link == null) {
            text(text = text)
            return
        }

        val endOffset = currentOffset + text.length
        val isHovered = hoveredOffset in currentOffset until endOffset
        val spanStyle = if (isHovered) hoveredSpanStyle else unhoveredSpanStyle

        builder.append(
            AnnotatedString(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(item = spanStyle, start = 0, end = text.length)),
            ),
        )

        builder.addStringAnnotation(
            tag = ANNOTATION_TAG_LINK,
            annotation = link,
            start = currentOffset,
            end = endOffset,
        )

        currentOffset = endOffset
    }

    fun build() = builder.toAnnotatedString()
}

/**
 * Gets the offset of the character at the given [offset] of this [AnnotatedString], according to its [layoutResult].
 */
private fun AnnotatedString.characterOffset(offset: Offset, layoutResult: TextLayoutResult?): Int? {
    return layoutResult?.let { _ ->
        // get the closest offset, but this may not actually be the character being hovered since it is only the closest
        val closestOffset = layoutResult.getOffsetForPosition(offset).coerceAtMost(this.length - 1)

        // check that it is the hovered character, adjusting the previous character if not
        val charBoundingBox = layoutResult.getBoundingBox(closestOffset)
        if (charBoundingBox.contains(offset)) return closestOffset

        val adjusted = (closestOffset - 1).coerceAtLeast(0)

        // if the adjusted box still does not contain the offset, nothing is being hovered - this happens when the text
        // breaks and the offset is beyond the line end
        return adjusted.takeIf { layoutResult.getBoundingBox(it).contains(offset) }
    }
}

/**
 * Gets the link annotation at the given [characterOffset], or null if there is none.
 */
private fun AnnotatedString.linkAnnotationAtOffset(characterOffset: Int): String? {
    return getStringAnnotations(tag = ANNOTATION_TAG_LINK, start = characterOffset, end = characterOffset)
        .firstOrNull()
        ?.item
}
