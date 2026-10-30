package com.edro08.vaultk

import androidx.test.platform.app.InstrumentationRegistry
import com.edro08.vaultk.data.cloud.MsalCloudAccountSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test

class MsalCloudAccountSessionTest {
    @Test fun msalInitializesAndCanReadCachedAccountWithoutInteractiveSignIn() {
        val latch = CountDownLatch(1)
        var result: Result<*>? = null
        val session = MsalCloudAccountSession(InstrumentationRegistry.getInstrumentation().targetContext)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            session.current { account -> result = account; latch.countDown() }
        }
        assertTrue("MSAL did not return a session result", latch.await(30, TimeUnit.SECONDS))
        result!!.getOrThrow()
    }
}
