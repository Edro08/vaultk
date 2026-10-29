package com.edro08.vaultk.domain

interface AppPreferenceStore {
    var appearance: Appearance
    var searchNotes: Boolean
    var showDetails: Boolean
    var fileAccessMode: FileAccessMode
    var cloudAutoSync: Boolean
        get() = true
        set(_) = Unit
    var cloudAllowMetered: Boolean
        get() = true
        set(_) = Unit
}

interface SecurityPreferenceStore {
    var timeoutSeconds: Int
    var clipboardSeconds: Int
}
