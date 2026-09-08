package eu.ggnext.contentsystem.value.types

import eu.ggnext.contentsystem.engine.TranslationEngine
import net.kyori.adventure.text.Component

/**
 * A [ConfigValue] holding a [Translation], identified by its config [key].
 */
internal data class TranslationValue(
    override val key: String,
    override val value: Translation,
) : ConfigValue<Translation>

/**
 * A translation entry available in German ([de]) and English ([en]).
 *
 * May contain placeholders that get resolved via [TranslationEngine]:
 * - `{key:some.key}` - references another, nested translation
 * - `{0}`, `{1}`, ... - index-based arguments passed in via `args`
 */
data class Translation(
    val de: String,
    val en: String,
) {
    /**
     * Resolves this translation for the given [locale] and deserializes it
     * into a formatted MiniMessage [Component], ready to be sent to a player.
     *
     * @param locale the target locale (e.g. "de", "en")
     * @param args arguments used to replace index placeholders (`{0}`, `{1}`, ...)
     * @return the fully resolved, formatted [Component]
     */
    fun get(
        locale: String,
        args: List<String>? = null,
    ): Component = TranslationEngine.handle(raw(locale), locale, args ?: emptyList())

    /**
     * Resolves this translation for the given [locale] into a plain string,
     * without deserializing it into a [Component].
     *
     * Used internally when this translation is nested inside another one
     * via a `{key:...}` reference, since the surrounding text needs a plain
     * string to splice in rather than a [Component].
     *
     * @param locale the target locale (e.g. "de", "en")
     * @param args arguments used to replace index placeholders (`{0}`, `{1}`, ...)
     * @return the fully resolved, but not yet deserialized, string
     */
    internal fun getAsText(
        locale: String,
        args: List<String> = emptyList(),
    ): String = TranslationEngine.resolve(raw(locale), locale, args)

    /** Returns the raw, unresolved translation string for the given [locale]. */
    private fun raw(locale: String) = if (locale == "de") de else en
}
