package com.edro08.vaultk

import com.edro08.vaultk.domain.security.Totp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {
    @Test fun matchesRfc6238Vectors() {
        // RFC 6238, Appendix B: the SHA-1 secret is ASCII "12345678901234567890".
        val otp = Totp.parse("otpauth://totp/Test?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&digits=8")!!
        assertEquals("94287082", otp.codeAt(59))
        assertEquals("07081804", otp.codeAt(1_111_111_109))
        assertEquals(1, otp.secondsLeft(59))
        otp.clear()
    }

    @Test fun rejectsInvalidConfiguration() {
        assertNull(Totp.parse("otpauth://hotp/Test?secret=GEZDGNBV"))
        assertNull(Totp.parse("otpauth://totp/Test?secret=INVALID1"))
        assertNull(Totp.parse("otpauth://totp/Test?secret=GEZDGNBV&digits=500"))
        assertNull(Totp.parse("otpauth://totp/Test?secret=GEZDGNBV&digits=incorrecto"))
        assertNull(Totp.parse("otpauth://totp/Test?secret=GEZDGNBV&period=incorrecto"))
        assertNull(Totp.credentialUri("otpauth://hotp/Test?secret=GEZDGNBV"))
        assertNull(Totp.credentialUri("https://example.org/?secret=GEZDGNBV"))
        assertNull(Totp.credentialUri(" "))
    }

    @Test fun manualAndQrCredentialsKeepCompatibleOtpUriAndParameters() {
        val manual = Totp.credentialUri("gez dgnb vgy3 tqojq gezd gnbv gy3t qojq")!!
        assertEquals("otpauth://totp/?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", manual)
        val qr = "otpauth://totp/Issuer:ana%40example.org?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Issuer&algorithm=SHA1&digits=8&period=30"
        assertEquals(qr, Totp.credentialUri("  $qr  "))
        val otp = Totp.parse(qr)!!
        assertEquals("94287082", otp.codeAt(59))
        assertTrue(otp.secondsLeft(59) in 1..30)
        otp.clear()
    }
}
