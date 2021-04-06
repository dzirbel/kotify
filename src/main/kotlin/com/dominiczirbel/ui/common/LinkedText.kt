package com.dominiczirbel.ui.common

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.dominiczirbel.ui.util.openInBrowser
import java.awt.Cursor

private const val ANNOTATION_TAG_URL = "url"

/**
 * Builds an [AnnotatedString] with embedded links, which have both a [SpanStyle] with the standard link styling (using
 * the given [color] and an underline) and an annotation which can be processed by [LinkedText] for clicks.
 */
class LinkedTextBuilder(color: Color) {
    private val annotatedStringBuilder = AnnotatedString.Builder()

    private val linkStyle = SpanStyle(color = color, textDecoration = TextDecoration.Underline)

    /**
     * Appends the given non-annotated [text].
     */
    fun append(text: String): LinkedTextBuilder {
        annotatedStringBuilder.append(text)
        return this
    }

    /**
     * Appends the given [text], annotated with a link to the given [url].
     */
    fun appendLink(text: String, url: String): LinkedTextBuilder {
        annotatedStringBuilder.append(
            AnnotatedString.Builder().apply {
                append(
                    AnnotatedString(
                        text = text,
                        spanStyles = listOf(AnnotatedString.Range(item = linkStyle, start = 0, end = text.length))
                    )
                )

                addStringAnnotation(tag = ANNOTATION_TAG_URL, annotation = url, start = 0, end = text.length)
            }.toAnnotatedString()
        )
        return this
    }

    /**
     * Returns an [AnnotatedString] built from the previous calls to [append] and [appendLink].
     */
    fun build(): AnnotatedString = annotatedStringBuilder.toAnnotatedString()
}

/**
 * A convenience constructor for [LinkedTextBuilder] which supplies the current material theme primary color, since
 * constructors cannot be [Composable].
 */
@Composable
fun LinkedTextBuilder() = LinkedTextBuilder(color = MaterialTheme.colors.primary)

/**
 * Wraps a [ClickableText] with processing handle clicks for [text] built by [LinkedTextBuilder].
 *
 * TODO right click to open menu with coping the url as an option
 */
@Composable
fun LinkedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClick: (String?) -> Unit = {
        it?.let { openInBrowser(it) }
    }
) {
    val textColor = style.color.takeOrElse {
        LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
    }

    ClickableText(
        text = text,
        modifier = modifier.hoverCursor(hoverCursor = Cursor(Cursor.HAND_CURSOR)),
        style = style.merge(TextStyle(color = textColor)),
        onClick = { offset ->
            onClick(
                text.getStringAnnotations(tag = ANNOTATION_TAG_URL, start = offset, end = offset)
                    .firstOrNull()
                    ?.item
            )
        }
    )
}
