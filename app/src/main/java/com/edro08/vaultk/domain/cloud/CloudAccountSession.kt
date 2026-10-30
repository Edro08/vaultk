package com.edro08.vaultk.domain.cloud

/** Account identity only. Access tokens never leave the authentication adapter. */
data class CloudAccount(val id: String, val username: String)

/** Stable Graph identifiers, never a filename or a download URL. */
data class CloudDriveItem(val driveId: String, val itemId: String, val name: String,
    val folder: Boolean, val size: Long? = null, val eTag: String? = null)

data class CloudDownload(val driveId: String, val itemId: String, val eTag: String, val bytes: Long)

/** Safe diagnostic only: never contains a token, response body or preauthenticated URL. */
enum class CloudDownloadIssue { Metadata, Redirect, Host, Transfer, Size, Signature, Version, Storage, Protected }
class CloudDownloadFailure(val issue: CloudDownloadIssue) : Exception(issue.name)

/** H is an opaque platform host for interactive authentication. */
interface CloudAccountSession<H> {
    fun current(callback: (Result<CloudAccount?>) -> Unit)
    fun connect(host: H, callback: (Result<CloudAccount?>) -> Unit)
    fun disconnect(callback: (Result<Unit>) -> Unit)
    /** Checks access to the signed-in user's drive without listing or downloading files. */
    fun checkDriveAccess(callback: (Result<Unit>) -> Unit)
    fun listDrive(folder: CloudDriveItem?, callback: (Result<List<CloudDriveItem>>) -> Unit)
    /** Stores an encrypted KDBX privately; does not make it an editable vault. */
    fun download(item: CloudDriveItem, progress: (Long, Long?) -> Unit,
        callback: (Result<CloudDownload>) -> Unit)
}
