package me.wypark.blogbackend.application.post

object MarkdownImageExtractor {

    fun extractFileNames(content: String): List<String> {
        return IMAGE_PATTERN.findAll(content)
            .map { match -> match.groupValues[1].substringAfterLast("/") }
            .toList()
    }

    private val IMAGE_PATTERN = Regex("!\\[.*?\\]\\((.*?)\\)")
}
