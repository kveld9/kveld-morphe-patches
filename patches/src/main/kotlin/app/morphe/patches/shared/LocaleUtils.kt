package app.morphe.patches.shared

object LocaleUtils {
    // Non-language qualifier tokens that appear in Android resource directory names
    private val KNOWN_NON_LANGUAGE_QUALIFIERS = setOf(
        "ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi", "anydpi", "tvdpi",
        "port", "land", "square", "long", "notlong", "small", "normal", "large", "xlarge",
        "car", "desk", "television", "appliance", "watch", "vrheadset",
        "night", "notnight", "round", "notround", "widecg", "nowidecg", "highdr", "lowdr",
        "notouch", "stylus", "finger", "keysexposed", "keyshidden", "keyssoft",
        "nokeys", "qwerty", "12key", "nonav", "dpad", "trackball", "wheel",
        "ldrtl", "ldltr",
    )

    /**
     * Extracts normalized language codes from an Android resource directory name.
     * Examples:
     *   "values-es-rUS" -> ["es"]
     *   "values-b+es+419" -> ["es", "es-419"]
     *   "values-night-v31" -> [] (base fallback)
     *   "drawable-xxhdpi" -> [] (base fallback)
     */
    fun extractResourceLanguages(dirName: String): List<String> {
        val segments = dirName.split("-")
        if (segments.size < 2) return emptyList()

        val languages = mutableListOf<String>()

        for (seg in segments.drop(1)) {
            // BCP-47 tag: e.g. "b+es+419" or "b+sr+Latn"
            if (seg.startsWith("b+")) {
                val parts = seg.split("+")
                if (parts.size >= 2) {
                    languages.add(parts[1].lowercase())
                    if (parts.size >= 3) {
                        languages.add("${parts[1]}-${parts[2]}".lowercase())
                    }
                }
                continue
            }

            // Region code (e.g. "rUS", "r419", "rCN") -> skip
            if (seg.startsWith("r") && seg.length in 3..4 && (seg.drop(1).all { it.isUpperCase() } || seg.drop(1).all { it.isDigit() })) {
                continue
            }

            // Version qualifier (e.g. "v21", "v31") -> skip
            if (seg.startsWith("v") && seg.length >= 2 && seg.drop(1).all { it.isDigit() }) {
                continue
            }

            // Smallest width / width / height (e.g. "sw600dp", "w720dp", "h1024dp") -> skip
            if ((seg.startsWith("sw") || seg.startsWith("w") || seg.startsWith("h")) && seg.endsWith("dp")) {
                continue
            }

            // ISO 639-1 / 639-2 language code (2-3 lowercase letters)
            if (seg.length in 2..3 && seg.all { it.isLowerCase() } && seg !in KNOWN_NON_LANGUAGE_QUALIFIERS) {
                languages.add(seg)
            }
        }

        return languages
    }

    /**
     * Normalizes a user input locale string (e.g. "es, es-419, en-US, pt-BR") into a set of lowercase match tokens.
     */
    fun parseTargetLocales(rawInput: String?, defaultLocales: Set<String> = setOf("en", "en-us")): Set<String> {
        val parsed = (rawInput ?: "")
            .split(",")
            .map { it.trim().removeSuffix(".pak").lowercase() }
            .filter { it.isNotEmpty() }
            .flatMap { tag ->
                val base = tag.substringBefore("-").substringBefore("_")
                listOf(tag, base)
            }
            .toMutableSet()

        parsed.addAll(defaultLocales.map { it.lowercase() })
        return parsed
    }

    /**
     * Formats byte amounts into clean human-readable units (MB if >= 1MB, otherwise KB).
     */
    fun formatBytes(bytes: Long): String {
        return if (bytes >= 1024 * 1024) {
            String.format(java.util.Locale.US, "%.2f MB", bytes.toDouble() / (1024 * 1024))
        } else {
            "${bytes / 1024} KB"
        }
    }

    /**
     * Cleans Dalvik type descriptor strings to plain class names (e.g. 'Lcom/example/Foo;' -> 'Foo', 'Lfnr;' -> 'fnr').
     */
    fun cleanClassName(type: String): String {
        return type.substringAfterLast('/').removePrefix("L").removeSuffix(";")
    }
}
