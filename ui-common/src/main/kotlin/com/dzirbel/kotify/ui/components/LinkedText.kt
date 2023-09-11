package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.dzirbel.contextmenu.ContextMenuDivider
import com.dzirbel.contextmenu.ContextMenuIcon
import com.dzirbel.contextmenu.ContextMenuParams
import com.dzirbel.contextmenu.GenericContextMenuItem
import com.dzirbel.contextmenu.MaterialContextMenuItem
import com.dzirbel.kotify.ui.util.openInBrowser
import com.dzirbel.kotify.ui.util.setClipboard

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
@ReadOnlyComposable
fun HyperlinkSpanStyle() = SpanStyle(color = MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline)

@Composable
fun UrlLinkedText(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    elements: @DisallowComposableCalls LinkElementScope.() -> Unit,
) {
    LinkedText(
        modifier = modifier,
        key = null,
        style = style,
        unhoveredSpanStyle = HyperlinkSpanStyle(),
        hoveredSpanStyle = HyperlinkSpanStyle(),
        linkContextMenu = { link ->
            listOf(
                MaterialContextMenuItem(
                    label = "Copy",
                    onClick = { setClipboard(link) },
                    leadingIcon = ContextMenuIcon.OfPainterResource("content-copy.svg"),
                ),
                MaterialContextMenuItem(
                    label = "Open in browser",
                    onClick = { openInBrowser(link) },
                    leadingIcon = ContextMenuIcon.OfPainterResource("open-in-new.svg"),
                ),
                ContextMenuDivider,
                UrlContextMenuItem(link),
            )
        },
        onClickLink = { openInBrowser(it) },
        elements = elements,
    )
}

private class UrlContextMenuItem(private val url: String) : GenericContextMenuItem() {
    @Composable
    override fun Content(onDismissRequest: () -> Unit, params: ContextMenuParams, modifier: Modifier) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(url, modifier = modifier.padding(params.measurements.itemPadding))
        }
    }
}

/**
 * Displays text build by [elements] with embedded links, allowing styling the links based on the hover state of the
 * link and handling link clicks.
 *
 * TODO consider optimizing when the entire text is a link
 */
@Composable
fun LinkedText(
    onClickLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null,
    style: TextStyle = LocalTextStyle.current,
    unhoveredSpanStyle: SpanStyle = SpanStyle(),
    hoveredSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    linkContextMenu: ((String) -> List<ContextMenuItem>)? = null,
    elements: @DisallowComposableCalls LinkElementScope.() -> Unit,
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val hoveredOffset = remember { mutableStateOf<Int?>(null) }
    val hoveredLink = remember { mutableStateOf<String?>(null) }

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
        detectTapGestures(matcher = PointerMatcher.mouse(PointerButton.Primary)) { offset ->
            text.characterOffset(offset, layoutResult.value)
                ?.let { text.linkAnnotationAtOffset(it) }
                ?.let(onClickLink)
        }
    }

    val hoverModifier = Modifier
        .onPointerEvent(PointerEventType.Move) { event ->
            val characterOffset = text.characterOffset(event.changes.first().position, layoutResult.value)
            val link = characterOffset?.let { text.linkAnnotationAtOffset(it) }
            hoveredOffset.value = characterOffset
            hoveredLink.value = link
        }
        .onPointerEvent(PointerEventType.Exit) {
            hoveredOffset.value = null
            hoveredLink.value = null
        }

    val textColor = style.color.takeOrElse {
        LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
    }

    // hack: apply pointer icon outside ContextMenuArea so it is cleared in the context menu
    Box(Modifier.pointerHoverIcon(if (hoveredLink.value != null) PointerIcon.Hand else PointerIcon.Default)) {
        ContextMenuArea(
            enabled = linkContextMenu != null && hoveredLink.value != null,
            items = {
                hoveredLink.value?.let { linkContextMenu?.invoke(it) }.orEmpty()
            },
        ) {
            BasicText(
                text = text,
                modifier = modifier.then(clickModifier).then(hoverModifier),
                style = style.copy(color = textColor),
                onTextLayout = { layoutResult.value = it },
            )
        }
    }
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
        val isHovered = hoveredOffset in currentOffset..<endOffset
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
