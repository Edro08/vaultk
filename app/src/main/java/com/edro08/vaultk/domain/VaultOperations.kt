package com.edro08.vaultk.domain

/** Immutable tree updates; an entry keeps its ID when moved and gets a fresh ID when copied. */
fun VaultGroup.insertEntry(groupId: String, entry: VaultEntry): VaultGroup =
    if (id == groupId) copy(entries = entries + entry)
    else copy(groups = groups.map { it.insertEntry(groupId, entry) })

fun VaultGroup.replaceEntry(groupId: String, entry: VaultEntry): VaultGroup =
    if (id == groupId) copy(entries = entries.map { if (it.id == entry.id) entry else it })
    else copy(groups = groups.map { it.replaceEntry(groupId, entry) })

fun VaultGroup.removeEntry(groupId: String, entryId: String): VaultGroup =
    if (id == groupId) copy(entries = entries.filterNot { it.id == entryId })
    else copy(groups = groups.map { it.removeEntry(groupId, entryId) })

fun VaultGroup.moveEntry(sourceId: String, destinationId: String, entryId: String): VaultGroup {
    require(sourceId != destinationId) { "The entry is already in this group" }
    val source = groupsAndSelf().single { it.id == sourceId }
    val item = source.entries.single { it.id == entryId }
    require(groupsAndSelf().any { it.id == destinationId }) { "Destination does not exist" }
    return removeEntry(sourceId, entryId).insertEntry(destinationId, item)
}

fun VaultGroup.groupsAndSelf(): List<VaultGroup> = listOf(this) + groups.flatMap { it.groupsAndSelf() }

fun VaultGroup.withChild(parentId: String, child: VaultGroup): VaultGroup =
    if (id == parentId) copy(groups = groups + child)
    else copy(groups = groups.map { it.withChild(parentId, child) })

fun VaultGroup.isInside(groupId: String, ancestorId: String): Boolean =
    groupsAndSelf().firstOrNull { it.id == ancestorId }?.groupsAndSelf()?.any { it.id == groupId } == true
