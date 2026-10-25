package com.edro08.vaultk.domain

/** A match keeps its parent so a result can open even when another group is selected. */
data class VaultSearchResult(val entry: VaultEntry, val groupId: String, val groupPath: List<String>)

/** Search the complete current session snapshot, without indexing passwords or protected values. */
fun VaultGroup.searchEntries(query: String, includeNotes: Boolean = true): List<VaultSearchResult> {
    val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (terms.isEmpty()) return emptyList()
    val results = mutableListOf<VaultSearchResult>()
    fun visit(group: VaultGroup, path: List<String>) {
        val currentPath = path + group.name
        group.entries.forEach { entry ->
            val fields = buildList {
                add(entry.title)
                add(entry.username)
                add(entry.url)
                if (includeNotes) add(entry.notes)
                addAll(entry.tags)
                entry.customFields.forEach { (name, value) ->
                    add(name)
                    if (!value.isProtected) {
                        val chars = value.valueAsChars
                        try { add(String(chars)) } finally { chars.fill('\u0000') }
                    }
                }
            }
            if (terms.all { term -> fields.any { it.contains(term, ignoreCase = true) } }) {
                results += VaultSearchResult(entry, group.id, currentPath)
            }
        }
        group.groups.forEach { visit(it, currentPath) }
    }
    visit(this, emptyList())
    return results
}
