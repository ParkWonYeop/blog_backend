package me.wypark.blogbackend.architecture

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.test.assertTrue

class LayerDependencyTest {

    private val sourceRoot: Path = Path.of("src/main/kotlin/me/wypark/blogbackend")

    @Test
    fun `domain does not depend on outer layers`() {
        assertNoImports(
            layer = "domain",
            forbiddenLayers = setOf("api", "application", "core", "infrastructure")
        )
    }

    @Test
    fun `application does not depend on delivery or infrastructure layers`() {
        assertNoImports(
            layer = "application",
            forbiddenLayers = setOf("api", "infrastructure")
        )
    }

    @Test
    fun `api depends on application contracts instead of domain internals`() {
        assertNoImports(
            layer = "api",
            forbiddenLayers = setOf("domain", "infrastructure")
        )
    }

    private fun assertNoImports(layer: String, forbiddenLayers: Set<String>) {
        val forbiddenPrefixes = forbiddenLayers.map { "import me.wypark.blogbackend.$it." }
        val violations = mutableListOf<String>()
        Files.walk(sourceRoot.resolve(layer)).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.extension == "kt" }
                .forEach { file ->
                    file.readText().lineSequence()
                        .filter { line -> forbiddenPrefixes.any(line::startsWith) }
                        .mapTo(violations) { line -> "${sourceRoot.relativize(file)}: $line" }
                }
        }

        assertTrue(violations.isEmpty(), violations.joinToString(separator = "\n"))
    }
}
