package com.edro08.vaultk

import com.edro08.vaultk.application.cloud.ManageCloudAccount
import com.edro08.vaultk.domain.cloud.CloudAccount
import com.edro08.vaultk.domain.cloud.CloudAccountSession
import com.edro08.vaultk.domain.cloud.CloudDriveItem
import com.edro08.vaultk.domain.cloud.CloudDownload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManageCloudAccountTest {
    @Test fun connectsRestoresAndDisconnectsThroughPort() {
        val session = object : CloudAccountSession<String> {
            var account: CloudAccount? = null
            override fun current(callback: (Result<CloudAccount?>) -> Unit) = callback(Result.success(account))
            override fun connect(host: String, callback: (Result<CloudAccount?>) -> Unit) {
                account = CloudAccount("id-$host", "user@example.com")
                callback(Result.success(account))
            }
            override fun disconnect(callback: (Result<Unit>) -> Unit) {
                account = null
                callback(Result.success(Unit))
            }
            override fun checkDriveAccess(callback: (Result<Unit>) -> Unit) =
                callback(if (account == null) Result.failure(IllegalStateException()) else Result.success(Unit))
            override fun listDrive(folder: CloudDriveItem?, callback: (Result<List<CloudDriveItem>>) -> Unit) =
                callback(Result.success(emptyList()))
            override fun download(item: CloudDriveItem, progress: (Long, Long?) -> Unit,
                callback: (Result<CloudDownload>) -> Unit) = callback(Result.failure(UnsupportedOperationException()))
        }
        val useCase = ManageCloudAccount(session)
        useCase.connect("host") { assertEquals("id-host", it.getOrThrow()?.id) }
        useCase.current { assertEquals("user@example.com", it.getOrThrow()?.username) }
        useCase.checkDriveAccess { it.getOrThrow() }
        useCase.disconnect { it.getOrThrow() }
        useCase.current { assertNull(it.getOrThrow()) }
    }
}
