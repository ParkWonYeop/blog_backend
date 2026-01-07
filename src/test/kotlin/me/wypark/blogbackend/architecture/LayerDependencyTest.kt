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
