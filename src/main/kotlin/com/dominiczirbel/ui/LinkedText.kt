package com.dominiczirbel.ui

import androidx.compose.material.AmbientTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.platform.AmbientUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration

private const val TAG_URL = "url"

/**
 * Appends a linked URL to this [AnnotatedString.Builder], with the given user-visible [text] which hyperlinks to the
 * given [url].
 *
 * For use with [LinkedText].
 */
@Composable
fun AnnotatedString.Builder.appendLinkedUrl(text: String, url: String) {
    pushStyle(SpanStyle(MaterialTheme.colors.primary, textDecoration = TextDecoration.Underline))
    pushStringAnnotation(TAG_URL, url)
    append(text)
    pop()
}

/**
 * A wrapper around [Text] which supports hyperlinked spans.
 *
 * [text] builds the annotated string, using [appendLinkedUrl] to add linked spans.
 *
 * TODO the [androidx.compose.ui.platform.DesktopUriHandler] does not yet support actually opening the links
 * TODO doesn't appear to be any way to change the cursor on hover yet
 */
@Composable
fun LinkedText(
    style: TextStyle = AmbientTextStyle.current,
    text: @Composable AnnotatedString.Builder.() -> Unit
) {
    val uriHandler = AmbientUriHandler.current
    val annotatedString = AnnotatedString.Builder()
        .also { it.text() }
        .toAnnotatedString()
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        modifier = Modifier.tapGestureFilter { position ->
            layoutResult.value
                ?.getOffsetForPosition(position)
                ?.let { offset ->
                    annotatedString.getStringAnnotations(offset, offset)
                        .firstOrNull { it.tag == TAG_URL }
                        ?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
        },
        onTextLayout = { layoutResult.value = it }
    )
}
