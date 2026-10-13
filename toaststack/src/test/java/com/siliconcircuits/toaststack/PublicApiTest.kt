package com.siliconcircuits.toaststack

import java.io.File
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlinx.validation.api.dump
import kotlinx.validation.api.filterOutNonPublic
import kotlinx.validation.api.loadApiFromJvmClasses
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
    fun `the public API matches the committed dump`() {
        // The main classes come as a folder or a jar, wherever ToastStack.class is.
        val classes = File(ToastStack::class.java.protectionDomain.codeSource.location.toURI())
        val signatures = if (classes.isFile) {
            JarFile(classes).use { it.loadApiFromJvmClasses() }
        } else {
            classes.walk().filter { it.isFile && it.extension == "class" }.map { it.inputStream() }.loadApiFromJvmClasses()
        }
        assertTrue("No classes found in $classes", signatures.isNotEmpty())
        val api = signatures.filterOutNonPublic()
        val actual = buildString { api.dump(this) }
        val dumpFile = File("api/toaststack.api")

        if (System.getProperty("updateApiDump") == "true") {
            dumpFile.writeText(actual)
            return
        }
        assertEquals(
            "The public API changed. If that is on purpose, add a CHANGELOG entry and run " +
                "./gradlew :toaststack:testDebugUnitTest -PupdateApiDump, then commit api/toaststack.api.",
            dumpFile.readText().replace("\r\n", "\n"),
            actual,
        )
    }

    @Test
    fun `every Kotlin sample in the README is in the compiled samples file`() {
        // Unit tests run in the module folder.
        // Line ends are normalised, so a Windows checkout reads the same text.
        val readme = File("../README.md").readText().replace("\r\n", "\n")
        val samples = File("src/test/java/com/siliconcircuits/toaststack/samples/ReadmeSamples.kt").readText().replace("\r\n", "\n")
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
