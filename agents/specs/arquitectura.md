# Arquitectura de Vaultk

Un solo módulo Android por ahora. `app` es el punto de entrada y composición;
no es una cuarta capa de negocio ni contiene los casos de uso.

```text
ui (Compose) → application (casos de uso) → domain (modelos, reglas y puertos)
                         ↑                                  ↑
                  app (composición)                data (implementa puertos)
```

`application` coordina las acciones de usuario y llama reglas y puertos de
`domain`; `data` implementa esos puertos. Ni `domain` ni `application` importan
`data`, Android o KeePassJava2. Como en Structa, los contratos de persistencia
pertenecen a `domain`, no a `application`.
`app/VaultkGraph` es la raíz de composición; `MainActivity` conserva por ahora
los contratos Android y la navegación. La presentación ya no referencia `data`;
`ArchitectureTest` comprueba esa frontera y las dependencias del núcleo.

- `domain`: entidades, valores, reglas de edición/búsqueda y puertos; sin
  Android ni clases de KeePassJava2.
- `application`: casos de uso que dependen de los puertos del dominio; no
  contiene implementaciones de almacenamiento ni detalles KDBX.
- `data`: KDBX, Argon2 nativo, archivos locales, transacción atómica,
  preferencias, biometría y adaptadores de los puertos.
- `app`/`ui`: configuración Android, permisos, navegación y presentación;
   `MainActivity` consume casos de uso ensamblados por `VaultkGraph`.

## Corte implementado: vault local

`EditVault` recibe datos del formulario y ejecuta crear/editar, duplicar,
mover, eliminar, crear grupos y buscar mediante operaciones del dominio. Las
pantallas ya no construyen entradas ni modifican el árbol directamente.
`OpenLocalVault` y `SaveLocalVault` usan `VaultRepository` de `domain`. El adaptador
`LocalKdbxVaultRepository` encapsula la lectura, comparación de huellas,
reescritura KDBX y `LocalVaultTransaction`. El caso de uso y la UI reciben
`VaultSaveProgress` desde `domain`, sin importar clases de `data`. `VaultId` es
un identificador opaco para el caso de uso; el adaptador local interpreta su
valor como ruta. `VaultValue` representa los campos sin exponer `PropertyValue`;
en `data`, `KdbxVaultValue` delega el cifrado de memoria a `SealedStore` para
no inventar una nueva implementación criptográfica. Los adjuntos usan una
referencia textual, interpretada como `Uri` solo en los bordes Android/KDBX.
La contraseña y la huella
se mantienen solo en la sesión; el adaptador no retiene copias. El guardado
sigue usando temporal cifrado, apertura de verificación, respaldo y reemplazo
atómico. No cambiar ese protocolo durante una migración de paquetes.

## Ajustes, selector y portapapeles

`ManagePreferences` y `ManageSessionSecurity` acceden a contratos de preferencias
de `domain`; `AppSettings` y `SessionSecurity` los implementan en `data`.
`VaultIdleTimer` vive en `application` y recibe tiempos monotónicos de Android.
`BrowseLocalVaults` usa `LocalVaultBrowser` para navegar y comprobar acceso;
`VaultkGraph` mantiene privado el adaptador `LocalVaultFiles`.
`CopyCredential` usa el puerto de portapapeles del dominio y se inyecta en Compose.
`VaultConflictException` pertenece al dominio para poder presentar el conflicto.

`ManageBiometricUnlock` utiliza `domain.BiometricCredentials<C>`. El parámetro
genérico mantiene la operación criptográfica opaca para el núcleo; el grafo
conecta `BiometricKeyStore` con `Cipher`. La UI muestra el diálogo Android y
llama al caso de uso con la operación autenticada, sin invocar el adaptador.
Las referencias conservan la URI textual para mantener los alias existentes.

`ManageRecentVaults` accede a `VaultSelection` del dominio, implementado por
`VaultSelectionStore`. `VaultDocument` contiene referencias textuales; la
conversión a `Uri` ocurre en el borde Android. El formato persistido no cambia.

## OneDrive 22.01 (identidad)

`CloudAccountSession<H>` en `domain` recibe un host de autenticación opaco;
`ManageCloudAccount` es el caso de uso y `MsalCloudAccountSession` el adaptador
Android conectado en `VaultkGraph`. El núcleo solo recibe identidad de cuenta,
no tokens ni tipos MSAL. `OneDriveAccountScreen` gestiona conexión/desconexión;
no se ofrece aún exploración de archivos. El registro Android debug y permiso
Graph delegado están configurados; falta probar autorización interactiva real.
Android Backup se deshabilitó para evitar transferencia de caché de sesión y
futuras copias KDBX privadas. Para publicar habrá que registrar el hash de la
firma de producción y generar la configuración MSAL correspondiente.

## OneDrive 22.02–22.03c (descarga y lectura)

`CloudCopyRepository` mantiene IDs de cuenta, unidad y elemento como identidad
estable; `PrivateCloudCopyStore` guarda solo KDBX cifrado y metadatos atómicos en
`filesDir/onedrive-copies`, fuera del almacenamiento compartido. `ManageCloudCopies`
ofrece selección y apertura al nivel de aplicación. El lector KDBX recibe los
bytes desde `data`, sin ruta local expuesta a presentación ni subida de la llave
a Graph. `CloudVaultContentScreen` es una vista de solo lectura separada de
`VaultContentScreen`: no comparte el callback de guardado local ni edición.
La biometría usa cuenta/unidad/elemento, sin atarla al nombre ni al ETag.
La gestión de copias se realiza a través de `ManageCloudCopies`: quitar la
selección no borra el KDBX; eliminar requiere una copia limpia no seleccionada,
confirmación y comprobaciones de integridad estructural en `data`, sin llamar
a Graph. En 22.04a `SaveCloudCopyLocally` prepara y verifica otro KDBX cifrado,
mantiene el ETag base y publica el puntero privado como `Pending` antes de
cualquier posible subida. La UI aún no permite edición cloud; la integración
de subida y su confirmación siguen pendientes en 22.04b–c.

En 22.04b `GraphSmallPendingUploader` vive solo en `data`: lee un snapshot
`Pending` cifrado bajo exclusión del almacén, comprueba la cuenta y el tamaño,
envía una única carga pequeña por ID con `If-Match` y devuelve un candidato
`AwaitingVerification` sin marcar `Clean`. Ninguna pantalla ni el adaptador
MSAL lo invocan todavía. Cargas por sesión permanecen bloqueadas; la
confirmación y transición durable de estado pertenecen a 22.04c.

En 22.04c el intento de subida se reclama duraderamente antes de abrir la
conexión (`uploadAttempted` y `attemptedBlob`): no se repite a ciegas tras un
corte, ni al guardar otra edición local sobre una copia cuyo envío ya se intentó.
`GraphSmallUploadVerifier` confirma dos veces ID/ETag/tamaño/unidad alrededor de
una lectura íntegra del ciphertext remoto; solo al coincidir la huella privada
el almacén pasa atómicamente a `Clean` bajo CAS de ETag base y huella local.
Para copias `Pending` con intento previo e incierto, `reconcile` usa únicamente
`GET` para confirmar si la subida ya quedó publicada en OneDrive, rearmar el
envío cuando dos lecturas confirman que el ETag base no cambió, o avanzar el
ETag base al snapshot intentado verificado si hubo otra edición local posterior,
conservando todos los blobs cifrados y manteniendo el bloqueo ante conflicto,
elemento ausente o corte. MSAL implementa `CloudCopySync` (`synchronize` y
`reconcile` y `refresh`) y `SynchronizeCloudCopy` lo expone a `application`.
En 22.05 la sincronización bidireccional consulta el ETag remoto con `GET`:
- Cuando la copia local está `Clean`, `refresh` comprueba el ETag al abrir, al
  volver a la app y bajo petición manual («Sincronizar ahora»); si cambió en
  OneDrive, descarga el nuevo `.kdbx` cifrado en staging privado, verifica dos
  lecturas coherentes de metadatos y firma KDBX, y lo promueve atómicamente solo
  si la copia sigue `Clean` en el ETag base esperado (`commitStagedRemoteClean`),
  recargando la bóveda abierta en memoria y conservando el blob anterior.
- Cuando la copia está `Pending`, nunca se descarga encima de la edición local:
  `checkBeforeUpload` y `refresh` consultan el ETag remoto antes de reclamar o
  enviar `PUT /content`, evitando bloquear `uploadAttempted` si no hay red y
  detectando conflictos antes de subir. Al guardar un cambio local, la UI dispara
  la sincronización automática mostrando los estados sincronizado, pendiente,
  sincronizando, error y conflicto.
`CloudCopiesScreen` y `CloudVaultContentScreen` muestran el estado explícito de
la copia y ofrecen «Sincronizar ahora» (`refresh` en `Clean`), «Subir cambios
pendientes» (`synchronize` en `Pending` sin intento) o «Reconciliar con OneDrive»
(`reconcile` en `Pending` con intento). `CloudVaultContentScreen` habilita crear,
editar, duplicar y eliminar entradas solo para copias elegibles por la ruta
pequeña (≤250.000.000 bytes, sin conflicto), guardando siempre en
`PrivateCloudCopyStore` mediante `SaveCloudCopyLocally` y sin compartir la ruta
local del dispositivo. Las sesiones de carga siguen deshabilitadas.

En 22.06, cuando se detecta un conflicto entre una edición local `Pending` y un
cambio remoto en OneDrive, `GraphSmallUploadVerifier.prepareConflict` descarga y
verifica el `.kdbx` remoto en un blob cifrado inmutable adicional sin reemplazar
el `.kdbx` local, y `PrivateCloudCopyStore.recordConflict` registra ambos en
`record.json` (`CloudCopyConflict`). El usuario debe elegir explícitamente (sin
fusión automática) entre «Conservar versión local» (`resolveConflictKeepLocal`,
que conserva el blob remoto como copia de recuperación cifrada, avanza el ETag
base al ETag remoto inspeccionado y sube el `.kdbx` local) o «Usar versión de
OneDrive» (`resolveConflictUseRemote`, que conserva el blob local como copia de
recuperación cifrada y activa el blob remoto verificado). Todas las transiciones
registran entradas en `sync-log.json` (`CloudSyncLogEntry`) sin secretos, tokens,
títulos ni rutas locales.

En 22.07–22.08, `PreferenceStore`, `AppSettings` y `ManagePreferences` exponen
`cloudAutoSync` y `cloudAllowMetered`, controlables desde `SettingsScreen` sin
sustituir las pantallas existentes. `VaultContentScreen` es la vista única tanto
para vaults locales como para copias privadas de OneDrive (`CloudVaultContentScreen`
delega en ella), manteniendo el mismo encabezado, menú `⋮`, buscador,
`GroupPickerSheet` y listado de entradas, e incorporando en modo OneDrive el
origen, el estado y la fecha de última sincronización, indicadores de progreso de
transferencia, las acciones «Sincronizar ahora», «Ver estado» y «Desvincular»
(tanto en la tarjeta de estado como en el menú `⋮`), y la pausa automática en
redes medidas (`ConnectivityManager`). `OpenVaultScreen` ofrece acceso directo a
«Copias descargadas» aunque la cuenta esté desconectada, y `CloudCopiesScreen`
distingue explícitamente quitar la referencia activa, eliminar la copia privada
del dispositivo y nunca borrar el archivo remoto en OneDrive. La suite
instrumentada verifica cortes de transferencia, ediciones concurrentes, tokens
caducados, permisos revocados, archivos eliminados, integridad KDBX tras descarga
y ausencia de secretos en registros.

## Siguientes cortes, separados y con pruebas

1. Extraer el estado de sesión, borrado de secretos y bloqueo de `MainActivity`
   a un controlador de aplicación; mantener `FLAG_SECURE` en la capa Android.
2. Mover el estado de edición, el reintento de guardado y los errores de
   `VaultContentScreen`/`MainActivity` a un controlador de presentación que
   llame casos de uso, sin alterar la semántica de cambios pendientes.
3. Evaluar módulos Gradle para imponer las dependencias
   en compilación; el módulo Android único no lo impide todavía.

En cada paso comprobar abrir, editar, guardar, reabrir, conflicto y fallos de
escritura; no medir rendimiento sobre el KDBX del usuario sin su intervención.
