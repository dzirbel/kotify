package com.dzirbel.kotify

/**
 * A wrapper around the command-line arguments passed to the application.
 */
data class CLIArguments(
    val cacheDir: String?,
    val settingsDir: String?,
    val logDir: String?,
    val debug: Boolean,
) {
    companion object {
        fun parse(args: Array<String>): CLIArguments {
            var cacheDir: String? = null
            var settingsDir: String? = null
            var logDir: String? = null
            var debug = false

            var i = 0
            while (i < args.size) {
                when (val arg = args[i]) {
                    "--cache-dir" -> cacheDir = args.getOrNull(++i)
                    "--settings-dir" -> settingsDir = args.getOrNull(++i)
                    "--log-dir" -> logDir = args.getOrNull(++i)
                    "--debug" -> debug = true
                    else -> error("unknown argument: $arg")
                }

                i++
            }

            return CLIArguments(
                cacheDir = cacheDir,
                settingsDir = settingsDir,
                logDir = logDir,
                debug = debug,
            )
        }
    }
}
