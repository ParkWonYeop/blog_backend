package me.wypark.blogbackend.application.post

object SlugGenerator {

    fun generate(input: String?, title: String, isTaken: (String) -> Boolean): String {
        val base = input?.takeUnless(String::isBlank)
            ?: title.trim().replace(WHITESPACE, "-").lowercase()

        var candidate = sanitize(base)
        var suffix = 1
        while (isTaken(candidate)) {
            candidate = "$base-${suffix++}"
        }
        return candidate
    }

    private fun sanitize(value: String): String = value
        .replace("?", "")
        .replace(";", "")

    private val WHITESPACE = Regex("\\s+")
}
