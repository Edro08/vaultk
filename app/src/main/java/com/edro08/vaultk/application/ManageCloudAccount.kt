package com.edro08.vaultk.application

import com.edro08.vaultk.domain.CloudAccount
import com.edro08.vaultk.domain.CloudAccountSession
import com.edro08.vaultk.domain.CloudDriveItem
import com.edro08.vaultk.domain.CloudDownload

class ManageCloudAccount<H>(private val session: CloudAccountSession<H>) {
    fun current(callback: (Result<CloudAccount?>) -> Unit) = session.current(callback)
    fun connect(host: H, callback: (Result<CloudAccount?>) -> Unit) = session.connect(host, callback)
    fun disconnect(callback: (Result<Unit>) -> Unit) = session.disconnect(callback)
    fun checkDriveAccess(callback: (Result<Unit>) -> Unit) = session.checkDriveAccess(callback)
    fun listDrive(folder: CloudDriveItem?, callback: (Result<List<CloudDriveItem>>) -> Unit) =
        session.listDrive(folder, callback)
    fun download(item: CloudDriveItem, progress: (Long, Long?) -> Unit,
        callback: (Result<CloudDownload>) -> Unit) = session.download(item, progress, callback)
}
