package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
import com.dzirbel.kotify.ui.util.firstAsState
import com.dzirbel.kotify.ui.util.imageSemantics
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.paintLazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
    size: Dp? = Dimens.iconMedium,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
) {
    val density = LocalDensity.current
    val painterState = remember(name, density) {
        IconCache.load(IconCache.IconParams(name = name, density = density))
    }
        .firstAsState()

    Box(
        modifier = modifier
            .instrument()
            .let { if (size == null) it else it.size(size) }
            .paintLazy(tint = tint) { painterState.value }
            .imageSemantics(contentDescription),
    )
}

/**
 * A global in-memory cache of loaded icon resources.
 */
object IconCache {
    data class IconParams(val name: String, val density: Density)

    private val classLoader = Thread.currentThread().contextClassLoader
    private val jobs: ConcurrentMap<IconParams, StateFlow<Painter?>> = ConcurrentHashMap()

    /**
     * Toggles the [IconCache]'s blocking mode, in which calls to [load] are always done synchronously.
     *
     * This is used in screenshot tests to ensure icons are available for the test image. Defaulting to true (and having
     * the main function set this to false) is a bit of hack, but easier than making sure that tests enable blocking
     * mode.
     */
    var loadBlocking = true

    /**
     * Loads the icon with the given [name] from the application resources, using an in-memory cache to avoid reloading
     * the same icon multiple times.
     *
     * Hack: the [size] must be provided, since [loadSvgPainter] returns a [Painter] rather than the SVG data itself.
     * This [Painter] behaves incorrectly when drawing the same icon at different sizes, so we simply use a different
     * key in the cache for each icon-size combination.
     */
    fun load(params: IconParams): StateFlow<Painter?> {
        return jobs.computeIfAbsent(params) {
            val flow = MutableStateFlow<Painter?>(null)
            if (loadBlocking) {
                flow.value = readIcon(params.name, params.density)
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    flow.value = readIcon(params.name, params.density)
                }
            }

            flow
        }
    }

    private fun readIcon(name: String, density: Density): Painter {
        if (!loadBlocking) {
            assertNotOnUIThread()
        }

        val iconPath = "$name.svg"
        return requireNotNull(classLoader.getResourceAsStream(iconPath)) { "Icon $iconPath not found" }
            .use { loadSvgPainter(it, density) }
    }
}
