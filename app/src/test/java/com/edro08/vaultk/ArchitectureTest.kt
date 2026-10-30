package com.edro08.vaultk

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureTest {
    @Test fun layerDependenciesStayDirectedTowardTheCore() {
        val source = File("src/main/java/com/edro08/vaultk")
        assertTrue("Production sources must be available", source.isDirectory)
        val violations = source.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            val path = file.relativeTo(source).invariantSeparatorsPath
            val forbidden = when {
                path.startsWith("ui/") || path == "MainActivity.kt" ->
                    listOf("com.edro08.vaultk.data.")
                path.startsWith("domain/") -> listOf("android.", "androidx.",
                    "org.linguafranca.", "com.edro08.vaultk.data.",
                    "com.edro08.vaultk.application.", "com.edro08.vaultk.ui.", "com.edro08.vaultk.app.")
                path.startsWith("application/") -> listOf("android.", "androidx.",
                    "org.linguafranca.", "com.edro08.vaultk.data.",
                    "com.edro08.vaultk.ui.", "com.edro08.vaultk.app.")
                path.startsWith("data/") -> listOf("com.edro08.vaultk.application.",
                    "com.edro08.vaultk.ui.", "com.edro08.vaultk.app.")
                else -> emptyList()
            }
            file.readLines().mapIndexedNotNull { index, line ->
                if (forbidden.any { it in line }) "$path:${index + 1}: ${line.trim()}" else null
            }.asSequence()
        }.toList()
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }
}
