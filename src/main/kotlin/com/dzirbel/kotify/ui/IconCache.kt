package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A wrapper around [Icon] which loads an icon with the given [name] from the [IconCache].
 *
 * Mainly exists to ensure [size] is passed appropriately; see [IconCache.load].
 */
@Composable
fun CachedIcon(
    name: String,
    size: Dp = Dimens.iconMedium,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    Icon(
        painter = IconCache
            .load(name, density = LocalDensity.current, size = size)
            .collectAsState(EmptyPainter, context = Dispatchers.IO)
            .value,
        modifier = modifier.size(size),
        contentDescription = contentDescription,
        tint = tint,
    )
}

/**
 * A [Painter] with no content, as a placeholder for loading icons.
 */
private object EmptyPainter : Painter() {
    override val intrinsicSize = Size.Unspecified
    override fun DrawScope.onDraw() {}
}

/**
 * A global in-memory cache of loaded icon resources.
 */
object IconCache {
    private data class IconHash(val name: String, val density: Density, val size: Dp)

    private val classLoader = Thread.currentThread().contextClassLoader
    private val jobs: ConcurrentMap<IconHash, Deferred<Painter>> = ConcurrentHashMap()

    /**
     * Toggles the [IconCache]'s blocking mode, in which calls to [load] are always done synchronously. This is used in
     * screenshot tests to ensure icons are available for the test image.
     */
    var loadBlocking = false

    /**
     * Loads the icon with the given [name] from the application resources, using an in-memory cache to avoid reloading
     * the same icon multiple times.
     *
     * Hack: the [size] must be provided, since [loadSvgPainter] returns a [Painter] rather than the SVG data itself.
     * This [Painter] behaves incorrectly when drawing the same icon at different sizes, so we simply use a different
     * key in the cache for each icon-size combination.
     */
    fun load(name: String, density: Density, size: Dp = Dimens.iconMedium): Flow<Painter> {
        val key = IconHash(name = name, density = density, size = size)

        // happy path: icon is already loaded
        jobs[key]
            ?.takeIf { it.isCompleted }
            ?.getCompleted()
            ?.let { return flowOf(it) }

        return if (loadBlocking) {
            readIcon(name, density)
                .also { jobs[key] = CompletableDeferred(it) }
                .let { flowOf(it) }
        } else {
            flow {
                val deferred = jobs.computeIfAbsent(key) {
                    // cannot use scope local to the composition in case it is removed from the composition before
                    // finishing, but then the same icon is requested again
                    GlobalScope.async { readIcon(name, density) }
                }

                emit(deferred.await())
            }
        }
    }

    private fun readIcon(name: String, density: Density): Painter {
        val iconPath = "$name.svg"
        return requireNotNull(classLoader.getResourceAsStream(iconPath)) { "Icon $iconPath not found" }
            .use { loadSvgPainter(it, density) }
    }
}
