package de.ggnext.contentsystem.engine

import de.ggnext.contentsystem.value.store.TranslationStore
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * Central engine for resolving translation strings.
 *
 * Supports two kinds of placeholders inside a translation string:
 * - `{key:some.key}` - references another, nested translation
 *   (resolved recursively via [TranslationStore])
 * - `{0}`, `{1}`, ... - index-based arguments supplied at runtime
 */
internal object TranslationEngine {
    /** Matches nested key references like `{key:some.key}`. */
    private val keyRegex = Regex("""\{key:([^}]+)}""")

    /** Matches index-based argument placeholders like `{0}`, `{1}`. */
    private val argRegex = Regex("""\{(\d+)}""")

    /**
     * Fully resolves a translation string and deserializes the result
     * into a MiniMessage [Component].
     *
     * @param translation the raw translation string (e.g. from the database)
     * @param locale the target locale (e.g. "de", "en")
     * @param args list of arguments used to replace index placeholders (`{0}`, `{1}`, ...)
     * @return the final, formatted [Component]
     */
    fun handle(
        translation: String,
        locale: String,
        args: List<String>,
    ): Component = MiniMessage.miniMessage().deserialize(resolve(translation, locale, args))

    /**
     * Recursively resolves a translation string into a plain string,
     * without deserializing it to MiniMessage.
     *
     * Nested `{key:...}` references are loaded via [TranslationStore] and
     * resolved through [resolve] as well, so arbitrarily deep nesting gets
     * concatenated into a single combined string. Index placeholders
     * (`{0}`, `{1}`, ...) are replaced afterwards with the corresponding
     * entries from [args]. If no argument exists for a given index, the
     * placeholder is left unchanged.
     *
     * @param translation the raw translation string
     * @param locale the target locale, passed on to nested translations
     * @param args list of arguments for index placeholders
     * @return the fully resolved, but not yet deserialized, string
     */
    fun resolve(
        translation: String,
        locale: String,
        args: List<String>,
    ): String {
        var result = translation
        result =
            keyRegex.replace(result) { match ->
                val key = match.groupValues[1]
                val value by TranslationStore(key)
                value.getAsText(locale, args)
            }
        result =
            argRegex.replace(result) { match ->
                val index = match.groupValues[1].toInt()
                args.getOrNull(index) ?: match.value
            }
        return result
    }
}
