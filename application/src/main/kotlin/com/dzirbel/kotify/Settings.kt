package com.dzirbel.kotify

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.Settings.SettingsData
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.log.warn
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.assertNotOnUIThread
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.util.concurrent.Executors
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Saves global settings as [SettingsData] objects in JSON.
 */
object Settings {
    @Serializable
    data class SettingsData(
        val colors: KotifyColors = KotifyColors.DARK,
        val debugPanelOpen: Boolean = false,
        val debugPanelDetached: Boolean = false,
        val instrumentationHighlightCompositions: Boolean = false,
        val instrumentationMetricsPanels: Boolean = false,
    )

    private val ioCoroutineContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private var nullableSettings: SettingsData? = null
    private var settings: SettingsData
        get() = nullableSettings ?: (load() ?: SettingsData()).also { nullableSettings = it }
        set(value) {
            val changed = nullableSettings != value
            nullableSettings = value
            if (changed) {
                GlobalScope.launch(ioCoroutineContext) {
                    save(value)
                }
            }
        }

    var colors: KotifyColors by setting(init = { colors }, mutateSettings = { copy(colors = it) })
    var debugPanelOpen: Boolean by setting(init = { debugPanelOpen }, mutateSettings = { copy(debugPanelOpen = it) })
    var debugPanelDetached by setting(init = { debugPanelDetached }, mutateSettings = { copy(debugPanelDetached = it) })
    var instrumentationHighlightCompositions: Boolean by setting(
        init = { instrumentationHighlightCompositions },
        mutateSettings = { copy(instrumentationHighlightCompositions = it) },
    )
    var instrumentationMetricsPanels: Boolean by setting(
        init = { instrumentationMetricsPanels },
        mutateSettings = { copy(instrumentationMetricsPanels = it) },
    )

    private val settingsFile by lazy { Application.settingsDir.resolve("settings.json") }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        decodeEnumsCaseInsensitive = true
    }

    /**
     * Wraps an individual setting in its own backing [MutableState] with initial value provided by [init] and which
     * updates [settings] when it is set.
     */
    private fun <T, V> setting(
        init: SettingsData.() -> V,
        mutateSettings: SettingsData.(V) -> SettingsData,
    ): ReadWriteProperty<T, V> {
        return object : ReadWriteProperty<T, V> {
            @Suppress("DoubleMutabilityForCollection")
            private var nullableState: MutableState<V>? = null

            private val state: MutableState<V>
                get() = nullableState ?: mutableStateOf(settings.init()).also { nullableState = it }

            override fun getValue(thisRef: T, property: KProperty<*>): V = state.value

            override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
                state.value = value
                settings = settings.mutateSettings(value)
            }
        }
    }

    /**
     * Ensures that the settings are loaded from the [settingsFile], i.e. loads them if they have not been loaded or
     * does nothing if they have already been loaded.
     */
    fun ensureLoaded() {
        settings
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun load(file: File = settingsFile): SettingsData? {
        assertNotOnUIThread()
        val start = CurrentTime.mark
        return try {
            file
                .takeIf { it.isFile }
                ?.inputStream()
                ?.use { json.decodeFromStream<SettingsData>(it) }
                ?.also {
                    EventLog.success("Loaded settings from ${settingsFile.absolutePath}", duration = start.elapsedNow())
                }
        } catch (ex: Throwable) {
            EventLog.warn(
                title = "Error loading settings from ${settingsFile.absolutePath}; reverting to defaults",
                content = ex.stackTraceToString(),
                duration = start.elapsedNow(),
            )
            null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun save(data: SettingsData, file: File = settingsFile) {
        val start = CurrentTime.mark
        assertNotOnUIThread()

        try {
            file.outputStream().use { outputStream ->
                json.encodeToStream(data, outputStream)
            }
            EventLog.info("Saved settings to ${settingsFile.absolutePath}", duration = start.elapsedNow())
        } catch (ex: Throwable) {
            EventLog.warn(
                title = "Error saving settings to ${settingsFile.absolutePath}",
                content = ex.stackTraceToString(),
                duration = start.elapsedNow(),
            )
        }
    }
}
