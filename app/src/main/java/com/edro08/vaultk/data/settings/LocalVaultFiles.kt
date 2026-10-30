package com.edro08.vaultk.data.settings

import android.net.Uri
import android.os.Environment
import com.edro08.vaultk.domain.vault.LocalVaultBrowser
import com.edro08.vaultk.domain.vault.LocalVaultItem
import com.edro08.vaultk.domain.vault.LocalVaultListing
import com.edro08.vaultk.domain.vault.VaultId
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/** Direct file access is confined to shared storage and requires Android's all-files grant. */
class LocalVaultFiles(
    root: File = Environment.getExternalStorageDirectory(),
    private val granted: () -> Boolean = { Environment.isExternalStorageManager() },
) : LocalVaultBrowser {
    private val base = root.canonicalFile

    override val root: VaultId get() = VaultId(base.path)
    override fun parentOf(directory: VaultId): VaultId {
        val file = File(directory.value).canonicalFile
        require(withinRoot(file))
        val parent = file.parentFile?.takeIf(::withinRoot) ?: base
        return VaultId(parent.path)
    }
    override fun list(directory: VaultId): LocalVaultListing {
        val (folders, vaults) = directory(File(directory.value))
        return LocalVaultListing(folders.map(::asItem), vaults.map(::asItem))
    }
    override fun find(query: String): List<LocalVaultItem> = search(query).map(::asItem)

    private fun asItem(file: File): LocalVaultItem =
        LocalVaultItem(VaultId(file.path), file.name, file.parentFile?.let { VaultId(it.path) })

    fun rootDirectory(): File = base

    override fun permitted(): Boolean = granted()
    override fun canRead(id: VaultId): Boolean = canRead(Uri.fromFile(File(id.value)))

    fun withinRoot(file: File): Boolean = file.canonicalFile.path.let { path ->
        path == base.path || path.startsWith(base.path + File.separator)
    }

    fun directory(path: File): Pair<List<File>, List<File>> {
        require(permitted() && withinRoot(path) && path.isDirectory)
        val entries = path.listFiles()?.filter { withinRoot(it) && !it.isHidden } ?: emptyList()
        val order = compareBy<File, String>(String.CASE_INSENSITIVE_ORDER) { it.name }
        return entries.filter { it.isDirectory }.sortedWith(order) to
            entries.filter { isVault(it) }.sortedWith(order)
    }

    fun search(query: String): List<File> {
        require(permitted())
        if (query.isBlank()) return emptyList()
        val results = mutableListOf<File>()
        val seen = mutableSetOf<String>()
        val pending = ArrayDeque<File>().apply { add(base) }
        while (pending.isNotEmpty() && results.size < 100) {
            val folder = pending.removeFirst()
            if (!withinRoot(folder) || !seen.add(folder.canonicalPath)) continue
            folder.listFiles()?.forEach { file ->
                if (!withinRoot(file) || file.isHidden) return@forEach
                if (file.isDirectory) pending.add(file)
                else if (isVault(file) && file.name.contains(query, ignoreCase = true) && results.size < 100)
                    results.add(file)
            }
        }
        return results.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    fun isVault(file: File): Boolean = withinRoot(file) && file.isFile &&
        file.name.endsWith(".kdbx", ignoreCase = true)

    fun canRead(uri: Uri): Boolean = permitted() && uri.scheme == "file" &&
        uri.path?.let { isVault(File(it)) && File(it).canRead() } == true

    fun open(uri: Uri): InputStream {
        if (!canRead(uri)) throw SecurityException("Local vault is unavailable")
        return FileInputStream(File(requireNotNull(uri.path)))
    }
}
