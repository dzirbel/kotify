package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
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
import com.dzirbel.kotify.ui.util.instrumentation.Ref
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.onPointerEvent
import com.dzirbel.kotify.ui.util.openInBrowser
import com.dzirbel.kotify.ui.util.setClipboard
import kotlinx.collections.immutable.persistentSetOf
import java.awt.SystemColor.text

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
 * A common [SpanStyle] which corresponds to the common URL hyperlink style, underlined and colored with the primary
 * color.
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
                    label = "Copy URL",
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
    // use a Ref to avoid recomposition when the layout result changes since it is only used in callbacks
    val layoutResult = remember { Ref<TextLayoutResult?>(null) }

    val hoveredRangeStart = remember { mutableStateOf<Int?>(null) }

    val hoveredLink = remember { Ref<String?>(null) }
    val hoveringLink = remember { mutableStateOf(false) }

    val linkResult = if (hoveredSpanStyle != unhoveredSpanStyle) {
        // if the styles are different, we need to rebuild the text on hover offset change
        remember(hoveredRangeStart.value, key) {
            LinkElementBuilder(
                hoveredRangeStart = hoveredRangeStart.value,
                unhoveredSpanStyle = unhoveredSpanStyle,
                hoveredSpanStyle = hoveredSpanStyle,
            )
                .apply(elements)
                .build()
        }
    } else {
        // if the styles are the same, we do not need to rebuild the text on hover offset change
        remember(key) {
            LinkElementBuilder(unhoveredSpanStyle).apply(elements).build()
        }
    }

    val coloredStyle = style.copy(
        color = style.color.takeOrElse { LocalContentColor.current.copy(alpha = LocalContentAlpha.current) },
    )

    val text = linkResult.annotatedString

    if (linkResult.onlyText) {
        // if there are no links, just display the text
        BasicText(text = text, modifier = modifier, style = coloredStyle)
        return
    }

    val clickModifier = if (linkResult.singleLink == null) {
        Modifier.pointerInput(text) {
            detectTapGestures(matcher = PointerMatcher.mouse(PointerButton.Primary)) { offset ->
                text.characterOffset(offset, layoutResult.value)
                    ?.let { text.annotationAtOffset(it)?.item }
                    ?.let(onClickLink)
            }
        }
    } else {
        Modifier.onClick { onClickLink(linkResult.singleLink) }
    }

    val hoverModifier = if (linkResult.singleLink == null) {
        Modifier
            .onPointerEvent(persistentSetOf(PointerEventType.Enter, PointerEventType.Move)) { event ->
                val characterOffset = text.characterOffset(event.changes.first().position, layoutResult.value)
                val range = characterOffset?.let { text.annotationAtOffset(it) }
                val link = range?.item

                hoveredRangeStart.value = range?.start
                hoveredLink.value = link
                hoveringLink.value = link != null
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoveredRangeStart.value = null
                hoveredLink.value = null
                hoveringLink.value = false
            }
    } else {
        Modifier
            .onPointerEvent(PointerEventType.Enter) {
                hoveredRangeStart.value = 0
                hoveredLink.value = linkResult.singleLink
                hoveringLink.value = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoveredRangeStart.value = null
                hoveredLink.value = null
                hoveringLink.value = false
            }
    }

    // hack: apply pointer icon outside ContextMenuArea so it is cleared in the context menu
    Box(
        modifier = Modifier
            .instrument()
            .pointerHoverIcon(if (hoveringLink.value) PointerIcon.Hand else PointerIcon.Default),
    ) {
        ContextMenuArea(
            enabled = linkContextMenu != null && hoveringLink.value,
            items = {
                hoveredLink.value?.let { linkContextMenu?.invoke(it) }.orEmpty()
            },
        ) {
            BasicText(
                text = text,
                modifier = modifier.then(clickModifier).then(hoverModifier),
                style = coloredStyle,
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
    private val hoveredRangeStart: Int?,
    private val unhoveredSpanStyle: SpanStyle,
    private val hoveredSpanStyle: SpanStyle,
) : LinkElementScope {

    data class Result(
        /**
         * The generated [AnnotatedString] including spans for [link]s.
         */
        val annotatedString: AnnotatedString,

        /**
         * Whether the generated [AnnotatedString] contains only plain text (no links).
         */
        val onlyText: Boolean,

        /**
         * The single [link] destination if the [AnnotatedString] contains a single [link] and no other text.
         */
        val singleLink: String?,
    )

    private val builder = AnnotatedString.Builder()
    private var currentOffset = 0

    private var numText = 0
    private var numLinks = 0
    private var singleLink: String? = null

    /**
     * Convenience constructor when the style for hovered and unhovered spans are the same.
     */
    constructor(spanStyle: SpanStyle) : this(
        hoveredRangeStart = null,
        unhoveredSpanStyle = spanStyle,
        hoveredSpanStyle = spanStyle,
    )

    override fun text(text: String) {
        builder.append(text)
        currentOffset += text.length
        numText++
    }

    override fun link(text: String, link: String?) {
        if (link == null) {
            text(text = text)
            return
        }

        numLinks++
        singleLink = if (numLinks == 1) link else null

        val spanStyle = if (hoveredRangeStart == currentOffset) hoveredSpanStyle else unhoveredSpanStyle

        builder.append(
            AnnotatedString(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(item = spanStyle, start = 0, end = text.length)),
            ),
        )

        val endOffset = currentOffset + text.length

        builder.addStringAnnotation(
            tag = ANNOTATION_TAG_LINK,
            annotation = link,
            start = currentOffset,
            end = endOffset,
        )

        currentOffset = endOffset
    }

    fun build(): Result {
        return Result(
            annotatedString = builder.toAnnotatedString(),
            onlyText = numLinks == 0,
            singleLink = singleLink.takeIf { numText == 0 },
        )
    }
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
 * Gets the annotation range at the given [characterOffset], or null if there is none.
 */
private fun AnnotatedString.annotationAtOffset(characterOffset: Int): AnnotatedString.Range<String>? {
    return getStringAnnotations(tag = ANNOTATION_TAG_LINK, start = characterOffset, end = characterOffset)
        .firstOrNull()
}
