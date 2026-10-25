package com.edro08.vaultk.domain

enum class Appearance { Dark, Light, System }
enum class FileAccessMode { Standard, Full }

object SecurityOptions {
    val lockTimeoutSeconds = listOf(0, 30, 60, 180, 300)
    val clipboardTimeoutSeconds = listOf(15, 30, 60, 120)
}
