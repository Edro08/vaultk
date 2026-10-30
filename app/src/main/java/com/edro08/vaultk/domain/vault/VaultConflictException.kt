package com.edro08.vaultk.domain.vault

import java.io.IOException

class VaultConflictException : IOException("The vault was changed outside Vaultk")
