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
