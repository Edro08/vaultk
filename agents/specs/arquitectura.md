# Arquitectura de Vaultk

Un solo módulo Android (`app`). Sigue los principios de **Clean Architecture** y arquitectura hexagonal con flujo unidireccional de dependencias hacia el núcleo. `app` actúa como composición e inyección manual de dependencias; no es una capa de negocio.

```text
ui (Compose) ───► application (Casos de Uso) ───► domain (Modelos, Reglas y Puertos)
                            ▲                                  ▲
                            │                                  │
                     app (VaultkGraph)              data (Implementación de Puertos)
```

- **`domain`**: Entidades inmutables, objetos de valor, reglas de negocio del árbol/búsqueda y contratos/puertos de persistencia e integración. Estrictamente independiente: sin dependencias de Android, AndroidX, Compose ni bibliotecas de terceros como KeePassJava2.
- **`application`**: Casos de uso orquestadores que coordinan las acciones del usuario interactuando con los puertos y reglas del dominio. No contiene detalles de almacenamiento, vistas ni dependencias de Android.
- **`data`**: Implementación de adaptadores y puertos del dominio: motor KDBX con KeePassJava2 y Argon2 nativo, transacción atómica de guardado, almacenamiento seguro de copias privadas en la nube, cliente Microsoft Graph, sesión MSAL, Keystore biométrico y preferencias.
- **`app`**: Raíz de composición (`VaultkGraph`) y punto de entrada (`MainActivity`). Conecta las implementaciones de `data` con los casos de uso de `application` y los expone a la UI.
- **`ui`**: Presentación declarativa en Jetpack Compose, navegación y diseño visual. No tiene acceso directo a `data`; consume casos de uso y modelos de dominio expuestos a través de `VaultkGraph`.

`ArchitectureTest.kt` valida automáticamente en compilación que las fronteras arquitectónicas se respeten sin dependencias prohibidas.

---

## Organización de paquetes por responsabilidad

Tanto la interfaz como las capas del backend están organizadas simétricamente en subpaquetes según su área funcional:

```text
com.edro08.vaultk/
├── app/                                    # Raíz de composición Android
│   └── VaultkGraph.kt                      # Grafo manual de dependencias y adaptadores
│
├── domain/                                 # Núcleo de negocio (sin dependencias Android)
│   ├── vault/                              # Entidades, árbol, operaciones, búsqueda y puertos de vault
│   │   ├── VaultContent.kt                 # VaultContent, VaultGroup, VaultEntry, VaultAttachment
│   │   ├── VaultOperations.kt              # Operaciones inmutables sobre el árbol de grupos/entradas
│   │   ├── VaultRepository.kt              # Puerto VaultRepository, VaultId, VaultSaveProgress
│   │   ├── VaultSearch.kt                  # Búsqueda profunda en títulos, usuarios, URL y notas
│   │   ├── VaultValue.kt                   # Puerto VaultValue y VaultValueFactory (memoria segura)
│   │   ├── VaultConflictException.kt       # Excepción de colisión/modificación externa
│   │   └── LocalVaultBrowser.kt            # Puerto para explorar archivos KDBX locales
│   ├── cloud/                              # Modelos y contratos de sincronización en la nube (OneDrive)
│   │   ├── CloudCopy.kt                    # CloudCopy, CloudCopyId, estados y registro de sync
│   │   ├── CloudCopySync.kt                # Puerto CloudCopySync y resultados de sincronización
│   │   └── CloudAccountSession.kt          # Puerto CloudAccountSession y contratos de descarga
│   ├── security/                           # Seguridad, biometría, TOTP y contraseñas
│   │   ├── BiometricCredentials.kt         # Puerto BiometricCredentials<C>
│   │   ├── PasswordGenerator.kt            # Algoritmo de generación de contraseñas por entropía
│   │   ├── Totp.kt                         # Generador de códigos TOTP según RFC 6238
│   │   └── CredentialClipboardPort.kt      # Puerto de portapapeles seguro
│   └── settings/                           # Preferencias del usuario y selección de vaults
│       ├── VaultPreferences.kt             # Modelos Appearance, FileAccessMode, SecurityOptions
│       ├── VaultSelection.kt               # Modelo VaultDocument y puerto VaultSelection
│       └── PreferenceStores.kt             # Puertos AppPreferenceStore y SecurityPreferenceStore
│
├── application/                            # Casos de uso
│   ├── vault/                              # Casos de uso sobre vaults locales
│   │   ├── EditVault.kt                    # Edición, creación, duplicación, movimiento y borrado
│   │   ├── LocalVaultUseCases.kt           # OpenLocalVault y SaveLocalVault
│   │   ├── BrowseLocalVaults.kt            # Navegación del árbol de archivos locales
│   │   └── ManageRecentVaults.kt           # Gestión del historial de vaults abiertos
│   ├── cloud/                              # Casos de uso para OneDrive y copias cloud
│   │   ├── ManageCloudAccount.kt           # Conexión/desconexión de cuenta Microsoft
│   │   ├── ManageCloudCopies.kt            # Gestión de copias locales privadas de la nube
│   │   ├── SaveCloudCopyLocally.kt         # Guardado de cambios locales en copia privada
│   │   └── SynchronizeCloudCopy.kt         # Sincronización, subida y reconciliación con Graph
│   ├── security/                           # Casos de uso de seguridad
│   │   ├── ManageBiometricUnlock.kt        # Desbloqueo rápido por biometría
│   │   ├── CopyCredential.kt               # Copiado al portapapeles con temporizador de limpieza
│   │   └── VaultIdleTimer.kt               # Temporizador de inactividad para bloqueo automático
│   └── settings/                           # Casos de uso de ajustes y configuración
│       └── ManagePreferences.kt            # ManagePreferences y ManageSessionSecurity
│
├── data/                                   # Adaptadores e infraestructura
│   ├── kdbx/                               # Motor de archivos KDBX (KeePassJava2)
│   │   ├── KdbxReader.kt                   # Parser y descifrado de archivos .kdbx
│   │   ├── KdbxWriter.kt                   # Serialización y cifrado KDBX
│   │   ├── LocalKdbxVaultRepository.kt     # Adaptador de VaultRepository
│   │   ├── LocalVaultTransaction.kt        # Transacción atómica (temporal, verify, reemplazo)
│   │   ├── KdbxVaultValue.kt               # Implementación de memoria cifrada (SealedStore)
│   │   └── VaultKdbxHeader.kt              # Inspección rápida de encabezados KDBX
│   ├── cloud/                              # Adaptadores de OneDrive y Microsoft Graph
│   │   ├── GraphDriveBrowser.kt            # Navegación y descarga segura de archivos
│   │   ├── GraphDriveProbe.kt              # Sondeo de endpoints de Microsoft Graph
│   │   ├── GraphSmallPendingUploader.kt    # Subida condicional If-Match (≤250MB)
│   │   ├── GraphSmallUploadVerifier.kt     # Verificación doble de ETag y huella tras subida
│   │   ├── MsalCloudAccountSession.kt      # Adaptador de autenticación MSAL y CloudCopySync
│   │   └── PrivateCloudCopyStore.kt        # Almacén privado en filesDir/onedrive-copies
│   ├── security/                           # Adaptadores de seguridad de plataforma
│   │   ├── BiometricKeyStore.kt            # Keystore Android y cifrado de claves biométricas
│   │   ├── SessionSecurity.kt              # Almacén de temporizadores de sesión y portapapeles
│   │   └── AndroidCredentialClipboard.kt   # Implementación del portapapeles de Android
│   └── settings/                           # Adaptadores de preferencias y selección
│       ├── AppSettings.kt                  # Persistencia de ajustes (SharedPreferences)
│       ├── LocalVaultFiles.kt              # Adaptador de navegación del sistema de archivos local
│       └── VaultSelectionStore.kt          # Persistencia de lista de vaults recientes
│
└── ui/                                     # Capa de presentación (Jetpack Compose)
    ├── component/                          # Tarjetas, controles y elementos reutilizables
    ├── screen/
    │   ├── generator/                      # Generador interactivo de contraseñas
    │   ├── settings/                       # Pantalla de ajustes (tema, seguridad, acceso, nube)
    │   ├── storage/                        # Apertura, desbloqueo, selector y gestión de copias
    │   └── vault/                          # Contenido del vault, vista de entradas y formularios
    └── theme/                              # Paletas oficiales Obsidian/Snow & Electric Indigo
```

---

## Subsistemas y Cortes Implementados

### 1. Bóveda local KDBX (`vault` y `kdbx`)
- **Edición desacoplada (`EditVault`)**: Gestiona la creación, edición, duplicación, movimiento y eliminación de entradas y grupos de forma inmutable sobre el árbol de `VaultGroup`.
- **Transacción atómica de guardado (`LocalVaultTransaction`)**: Protocolo estricto contra corrupción:
  1. Escritura en archivo temporal cifrado.
  2. Apertura y verificación criptográfica íntegra del temporal antes de comprometerlo.
  3. Respaldo previo y reemplazo atómico en disco.
- **Protección de memoria (`KdbxVaultValue`)**: Los campos sensibles (`password`, `otp`, campos personalizados protegidos) delegan en `SealedStore` para mantener los caracteres cifrados en memoria volátil.

### 2. OneDrive y Sincronización en la Nube (`cloud`)
- **Almacén privado aislado (`PrivateCloudCopyStore`)**: Las copias remotas se almacenan cifradas en `filesDir/onedrive-copies`, fuera del almacenamiento compartido, con metadatos atómicos en `record.json`.
- **Identidad estable (`CloudCopyId`)**: Las copias se identifican por triplete inmutable `(accountId, driveId, itemId)`, permitiendo renombrar el archivo en la nube sin desvincularlo.
- **Subida condicional y verificación doble (`GraphSmallPendingUploader` / `GraphSmallUploadVerifier`)**:
  - Reclamo durable del intento de subida (`uploadAttempted`) antes de abrir la conexión de red.
  - Envío condicional mediante `PUT /content` con cabecera `If-Match` para prevenir sobrescritura de cambios remotos concurrentes.
  - Doble verificación de ETag, tamaño y huella digital del blob remoto antes de promover el estado a `Clean`.
- **Resolución manual de conflictos**: Si se detecta un cambio remoto mientras hay cambios locales pendientes (`Conflict`), se descarga la versión remota a un blob de recuperación inmutable y se solicita al usuario elegir explícitamente entre «Conservar versión local» o «Usar versión de OneDrive».
- **Vista unificada (`VaultContentScreen`)**: Pantalla única para bóvedas locales y en la nube con selector de grupos, búsqueda, indicadores reactivos de sincronización (violeta al sincronizar, verde al estar al día, rojo en error) y menú de acciones cloud («Sincronizar ahora», «Ver estado», «Desvincular»).

### 3. Seguridad y Ajustes (`security` y `settings`)
- **Biometría desacoplada (`ManageBiometricUnlock` / `BiometricKeyStore`)**: El núcleo maneja una operación opaca `BiometricCredentials<C>` sin acoplarse al `Cipher` ni a las APIs de Android Biometric.
- **Portapapeles con autodestrucción (`CopyCredential` / `AndroidCredentialClipboard`)**: Limpia automáticamente el contenido sensible copiado tras el tiempo configurado (por defecto 30 segundos) o al bloquear la bóveda.
- **Inactividad y bloqueo automático (`VaultIdleTimer`)**: Temporizador basado en tiempo monotónico para bloquear la sesión tras inactividad.

---

## Verificación Arquitectónica

El cumplimiento estricto de las reglas de aislamiento se comprueba mediante `ArchitectureTest.kt`:
1. `ui` y `MainActivity` no pueden importar clases de `data.*`.
2. `domain` no puede importar `android.*`, `androidx.*`, `org.linguafranca.*`, `data.*`, `application.*`, `ui.*` ni `app.*`.
3. `application` no puede importar `android.*`, `androidx.*`, `org.linguafranca.*`, `data.*`, `ui.*` ni `app.*`.
4. `data` no puede importar `application.*`, `ui.*` ni `app.*`.

---

## Siguientes Pasos de Refinamiento Arquitectónico

1. **Controlador de sesión**: Extraer el estado de sesión, borrado de secretos y bloqueo de `MainActivity` hacia un controlador de aplicación dedicado, manteniendo `FLAG_SECURE` en la frontera de Android.
2. **Controlador de presentación**: Centralizar el estado de edición y reintentos de guardado de `VaultContentScreen` en un presenter/viewmodel liviano de UI.
3. **Modularización Gradle (opcional)**: Evaluar la separación en módulos Gradle independientes (`:domain`, `:application`, `:data`, `:ui`, `:app`) para imponer las fronteras de dependencia a nivel de compilación.
