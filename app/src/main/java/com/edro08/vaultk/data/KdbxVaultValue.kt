package com.edro08.vaultk.data

import com.edro08.vaultk.domain.VaultValue
import com.edro08.vaultk.domain.VaultValueFactory
import org.linguafranca.pwdb.PropertyValue

/** KeePassJava2's sealed store keeps protected values encrypted with its key off-heap. */
internal class KdbxVaultValue(val delegate: PropertyValue) : VaultValue {
    override val isProtected: Boolean get() = delegate.isProtected
    override val valueAsChars: CharArray get() = delegate.valueAsChars
}

class KdbxVaultValueFactory : VaultValueFactory {
    override fun create(chars: CharArray, protected: Boolean): VaultValue = KdbxVaultValue(
        if (protected) PropertyValue.SealedStore(chars) else PropertyValue.BytesStore(chars))
}

internal fun VaultValue.toPropertyValue(): PropertyValue {
    if (this is KdbxVaultValue) return delegate
    val chars = valueAsChars
    return try {
        if (isProtected) PropertyValue.SealedStore(chars) else PropertyValue.BytesStore(chars)
    } finally { chars.fill('\u0000') }
}
