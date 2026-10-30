package com.edro08.vaultk

import com.edro08.vaultk.domain.security.PasswordGenerator
import com.edro08.vaultk.domain.security.PasswordOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {
    @Test fun respectsLengthAndEnabledClassesAndIncludesEverySelectedClass() {
        val classes = listOf(
            PasswordOptions(uppercase = true, lowercase = false, digits = false, symbols = false) to
                Regex("[A-Z]{4}"),
            PasswordOptions(uppercase = false, lowercase = true, digits = false, symbols = false) to
                Regex("[a-z]{4}"),
            PasswordOptions(uppercase = false, lowercase = false, digits = true, symbols = false) to
                Regex("[0-9]{4}"),
            PasswordOptions(uppercase = false, lowercase = false, digits = false, symbols = true) to
                Regex("[!@#\\$%&*?_+=.\\-]{4}"),
        )
        classes.forEach { (options, allowed) ->
            repeat(10) { assertTrue(allowed.matches(PasswordGenerator.generate(options.copy(length = 4)))) }
        }
        repeat(20) {
            val password = PasswordGenerator.generate(PasswordOptions(length = 64))
            assertEquals(64, password.length)
            assertTrue(password.any(Char::isUpperCase))
            assertTrue(password.any(Char::isLowerCase))
            assertTrue(password.any(Char::isDigit))
            assertTrue(password.any { it in "!@#\$%&*?_-+=." })
        }
        assertEquals(128, PasswordGenerator.generate(PasswordOptions(length = 128)).length)
    }

    @Test fun rejectsInvalidConfigurationsAndRegenerationProducesNewPassword() {
        for (options in listOf(PasswordOptions(length = 3), PasswordOptions(length = 129),
            PasswordOptions(uppercase = false, lowercase = false, digits = false, symbols = false))) {
            var rejected = false
            try { PasswordGenerator.generate(options) } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
        assertFalse(PasswordGenerator.generate(PasswordOptions()) == PasswordGenerator.generate(PasswordOptions()))
    }
}
