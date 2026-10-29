package com.edro08.vaultk.application

import com.edro08.vaultk.domain.AppPreferenceStore
import com.edro08.vaultk.domain.SecurityPreferenceStore
import com.edro08.vaultk.domain.SecurityOptions
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.FileAccessMode

class ManagePreferences(private val store: AppPreferenceStore) {
    var appearance: Appearance
        get() = store.appearance
        set(value) { store.appearance = value }
    var searchNotes: Boolean
        get() = store.searchNotes
        set(value) { store.searchNotes = value }
    var showDetails: Boolean
        get() = store.showDetails
        set(value) { store.showDetails = value }
    var fileAccessMode: FileAccessMode
        get() = store.fileAccessMode
        set(value) { store.fileAccessMode = value }
    var cloudAutoSync: Boolean
        get() = store.cloudAutoSync
        set(value) { store.cloudAutoSync = value }
    var cloudAllowMetered: Boolean
        get() = store.cloudAllowMetered
        set(value) { store.cloudAllowMetered = value }
}

class ManageSessionSecurity(private val store: SecurityPreferenceStore) {
    var timeoutSeconds: Int
        get() = store.timeoutSeconds
        set(value) {
            require(value in SecurityOptions.lockTimeoutSeconds)
            store.timeoutSeconds = value
        }
    var clipboardSeconds: Int
        get() = store.clipboardSeconds
        set(value) {
            require(value in SecurityOptions.clipboardTimeoutSeconds)
            store.clipboardSeconds = value
        }
}
