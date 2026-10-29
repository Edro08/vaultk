package com.edro08.vaultk.data

import android.content.Context
import com.edro08.vaultk.domain.Appearance
import com.edro08.vaultk.domain.FileAccessMode

class AppSettings(context: Context) : com.edro08.vaultk.domain.AppPreferenceStore {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override var appearance: Appearance
        get() = runCatching { Appearance.valueOf(preferences.getString("appearance", "Dark") ?: "Dark") }
            .getOrDefault(Appearance.Dark)
        set(value) { preferences.edit().putString("appearance", value.name).apply() }

    override var searchNotes: Boolean
        get() = preferences.getBoolean("search_notes", true)
        set(value) { preferences.edit().putBoolean("search_notes", value).apply() }

    override var showDetails: Boolean
        get() = preferences.getBoolean("show_details", true)
        set(value) { preferences.edit().putBoolean("show_details", value).apply() }

    override var fileAccessMode: FileAccessMode
        get() = runCatching { FileAccessMode.valueOf(preferences.getString("file_access_mode", "Standard") ?: "Standard") }
            .getOrDefault(FileAccessMode.Standard)
        set(value) { preferences.edit().putString("file_access_mode", value.name).apply() }

    override var cloudAutoSync: Boolean
        get() = preferences.getBoolean("cloud_auto_sync", true)
        set(value) { preferences.edit().putBoolean("cloud_auto_sync", value).apply() }

    override var cloudAllowMetered: Boolean
        get() = preferences.getBoolean("cloud_allow_metered", true)
        set(value) { preferences.edit().putBoolean("cloud_allow_metered", value).apply() }
}
