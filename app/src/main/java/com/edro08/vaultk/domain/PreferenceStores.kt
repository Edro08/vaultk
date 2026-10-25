package com.edro08.vaultk.domain

interface AppPreferenceStore {
    var appearance: Appearance
    var searchNotes: Boolean
    var showDetails: Boolean
    var fileAccessMode: FileAccessMode
}

interface SecurityPreferenceStore {
    var timeoutSeconds: Int
    var clipboardSeconds: Int
}
