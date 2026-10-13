package com.siliconcircuits.toaststack

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps the public entry points and the README samples in line with each other. */
class PublicApiTest {

    private fun KFunction<*>.optionNames() = parameters.mapNotNull { it.name }.toSet()

    private fun KClass<*>.function(name: String) = memberFunctions.single { function ->
        function.name == name && function.parameters.any { it.name == "message" }
    }

    // Set through the handle (withAction, withSecondaryAction, onShow) or the builder only.
    private val handleOptions = setOf("actionLabel", "onAction", "secondaryActionLabel", "onSecondaryAction", "onShow")

    @Test
    fun `every show entry point takes the same options`() {
        val options = ToastStackState::class.function("show").optionNames()

        assertEquals(options, ToastStack::class.function("show").optionNames() - "hostTag")
        val builder = ToastBuilder::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>().map { it.name }
        assertEquals(options, builder.toSet() - handleOptions)
        assertEquals(options - "onDismiss", ToastStackState::class.function("showAndAwait").optionNames())
        assertEquals(options - "onDismiss", ToastStack::class.function("showAndAwait").optionNames() - "hostTag")
    }

    @Test
    fun `every Kotlin sample in the README is in the compiled samples file`() {
        // Unit tests run in the module folder.
        val readme = File("../README.md").readText()
        val samples = File("src/test/java/com/siliconcircuits/toaststack/samples/ReadmeSamples.kt").readText()
        fun normalize(text: String) = text.lines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("import ") }
            .joinToString("\n")

        val blocks = Regex("```kotlin\n(.*?)```", RegexOption.DOT_MATCHES_ALL).findAll(readme)
            .map { it.groupValues[1] }
            .filterNot { "implementation(" in it || "repositories {" in it } // Gradle setup
            .toList()

        assertTrue(blocks.size > 10)
        // Whole lines only, so a block cannot match part of a longer line.
        val compiled = "\n" + normalize(samples) + "\n"
        blocks.forEach { block ->
            assertTrue("README sample is not in the samples file:\n$block", "\n" + normalize(block) + "\n" in compiled)
        }
    }
}
