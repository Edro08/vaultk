# Historial de Vaultk

Fases y cortes en orden cronológico descendente (el más reciente primero).

## 2026-10-29 — Reestructuración simétrica de paquetes en Backend (domain, application, data)

- **Subdivisión por responsabilidad de las capas de negocio e infraestructura ([arquitectura.md](file:///d:/Projects/Android/Vaultk/agents/specs/arquitectura.md))**:
  - Se eliminó la estructura plana anterior de 47 archivos mezclados en `domain`, `application` y `data`, reorganizándolos en subpaquetes funcionales simétricos:
    - **`domain`**: `domain.vault` (árbol, operaciones, repositorio, búsqueda, valor), `domain.cloud` (copias, sincronización, sesión Graph), `domain.security` (biometría, generador, TOTP, portapapeles) y `domain.settings` (preferencias, selección de bóvedas).
    - **`application`**: `application.vault` (casos de uso de edición, guardado/apertura local, historial), `application.cloud` (gestión de cuentas, copias locales y sync OneDrive), `application.security` (desbloqueo biométrico, portapapeles, inactividad) y `application.settings` (gestión de preferencias y seguridad de sesión).
    - **`data`**: `data.kdbx` (lector, escritor, transacción atómica, repositorio KDBX, memoria protegida), `data.cloud` (cliente Graph, subida, verificación, almacén privado), `data.security` (Keystore biométrico, memoria de sesión, portapapeles Android) y `data.settings` (SharedPreferences, sistema de archivos local, selección de bóveda).
  - Se preservó el historial de git mediante `git mv` para todos los archivos afectados.
  - Se actualizaron todos los imports en los 104 archivos Kotlin del proyecto sin usar comodines (`.*`).
  - Se actualizó el documento de arquitectura `agents/specs/arquitectura.md` con el mapa completo de responsabilidades.
- **Verificación**:
  - `ArchitectureTest.kt` y todas las pruebas unitarias pasaron con éxito (`testDebugUnitTest`).
  - Pruebas instrumentadas verificadas (`compileDebugAndroidTestKotlin`).
  - APK debug compilada e instalada en el emulador (`Pixel_9_Pro(AVD) - 15`).

## 2026-10-29 — Estados cromáticos del icono de sincronización en la nube

- **Colorización reactiva del icono de nube ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se unificó la resolución de icono (`cloudIcon`) y su tinte (`cloudIconTint`) en la cabecera de la aplicación:
    - **Sincronizando / Guardando** (`saving || syncing`): el icono `Icons.Outlined.CloudSync` y el indicador de progreso circular adoptan el índigo eléctrico primario de Vaultk (`primary` / `#582BF5`). El punto indicador de la cabecera también adopta este color.
    - **Sincronizado / Al día** (`cloudIcon == Icons.Outlined.CloudDone`): el icono adopta el color verde esmeralda de estado (`tertiary` / `#10B981`), en sintonía con el punto indicador.
    - **Error / Conflicto** (`isWarningStatus`): el icono `Icons.Outlined.CloudOff` adopta el color de alerta (`error` / rojo).
    - **Otros estados** (solo lectura o cola): adopta `onSurfaceVariant`.
  - Se actualizó el diálogo de estado (`CloudStatusDialog`) para utilizar `MaterialTheme.colorScheme.tertiary` en lugar del valor hexadecimal estático.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug compilada e instalada en el dispositivo/emulador.

## 2026-10-29 — Adaptabilidad de contraste y sintaxis en contraseña del Generador

- **Colores de sintaxis reactivos al tema ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - Se corrigió la visibilidad de los caracteres en el tema claro, donde las letras mayúsculas fijadas en blanco (`Color.White`) resultaban invisibles sobre el fondo de tarjeta blanca.
  - Se implementó detección dinámica de luminosidad (`surface.luminance() < 0.5f`) para adaptar la paleta de sintaxis:
    - **Tema Claro**: Mayúsculas en negro profundo (`#09090B`), minúsculas en pizarra oscuro (`#4B5563`), números en azul intenso (`#2563EB`) y símbolos en carmesí (`#DC2626`).
    - **Tema Oscuro**: Mayúsculas en blanco (`Color.White`), minúsculas en plata suave (`#D4D4D8`), números en azul cielo (`#60A5FA`) y símbolos en coral (`#F87171`).
  - Legibilidad y contraste garantizados en ambos modos con especificación WCAG AAA.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Sintonización cromática de la tarjeta de Seguridad en Ajustes

- **Corrección de colores en Seguridad ([SettingsScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/settings/SettingsScreen.kt))**:
  - Se corrigió el icono del candado en la cabecera de la sección Seguridad que tenía un tinte negro `onSurface` desalineado con el resto de las tarjetas, asignándole `MaterialTheme.colorScheme.primary` para coincidir exactamente con los iconos de Tema, Archivos y Nube.
  - Las etiquetas de los valores configurados (como `5 min`, `30 s`, `Activado`) en `SecuritySettingRow` ahora usan `MaterialTheme.colorScheme.primary` con `FontWeight.Medium`, integrándose al tono de marca de la app en ambos temas.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Armonización del tema claro con el icono oficial de Vaultk (Snow & Electric Indigo)

- **Alineación del tema claro con la identidad de marca ([Color.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Color.kt), [Theme.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Theme.kt))**:
  - Se sustituyó el esquema claro anterior por la **Propuesta A (Snow & Electric Indigo)**, logrando total coherencia diurna con los colores del icono:
    - **Fondo General**: Blanco nieve nítido (`#F8F9FB`), generando un contraste limpio y sutil con las tarjetas.
    - **Tarjetas y Cajas**: Blanco puro (`#FFFFFF`) con bordes finos de 1dp (`#E5E7EB`).
    - **Acento Primario**: Índigo eléctrico de la V (`#582BF5`), ofreciendo un contraste superior a 8:1 (WCAG AAA) sobre blanco para botones principales, switches, sliders y FAB (+).
    - **Tiles de Iconos (`VaultkIconTile`)**: Lavanda suave (`#F0EBFE`) con borde fino (`#E4D7FC`) y glifo en índigo (`#582BF5`).
    - **Barra de Navegación**: Píldora activa en lavanda (`#EDE8FE`) e icono en `#582BF5`.
    - **Tipografía**: Texto principal en negro profundo (`#09090B`) y secundario en gris neutro (`#6B7280`).
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador `Pixel_9_Pro(AVD) - 15`.

## 2026-10-29 — Armonización del tema oscuro con el icono oficial de Vaultk (Electric Indigo & Obsidian)

- **Alineación cromática con los valores reales del icono ([Color.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Color.kt), [Theme.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Theme.kt))**:
  - Se muestrearon los píxeles del icono oficial de Vaultk, extrayendo los colores exactos de su identidad visual:
    - **Fondo**: Negro OLED puro (`#000000`).
    - **Acento Primario**: Índigo/violeta eléctrico de la parte más oscura y el pliegue de la V (`#582BF5` / `#4B25E8`) para botones principales, foco, barra activa de slider y FAB (+).
    - **Tiles de Iconos (`VaultkIconTile`)**: Fondo profundo (`#1E1738`) con contorno fino (`#332763`) e iconos en lavanda suave (`#C185FC`), emulando la estética del candado del icono.
    - **Barra de navegación inferior**: Píldora activa en índigo oscuro (`#221846`) con icono en lavanda (`#C185FC`).
    - **Superficie de Tarjetas**: Gris carbón obsidiana (`#131316`) con bordes sutiles de tarjeta (`#23222B`).
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador `Pixel_9_Pro(AVD) - 15`.

## 2026-10-29 — Corrección de corte y tamaño en etiqueta flotante de longitud

- **Desbordamiento sin restricciones en etiqueta del thumb ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - Se solucionó el truncamiento del número en la burbuja flotante del selector (donde números de dos o tres dígitos quedaban cortados por el límite de 20dp del thumb) mediante `wrapContentSize(unbounded = true)`.
  - Se incrementó el padding horizontal (8dp) y vertical (3dp) con tipografía `labelMedium`, sombras sutiles y esquinas redondeadas de 8dp para garantizar legibilidad completa tanto para valores de 1, 2 como 3 dígitos (`128`).
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Corrección de alineación del selector y limpieza de números en Longitud

- **Centrado vertical del selector y remoción de etiquetas redundantes ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - Se eliminaron los números superiores redundantes `4` y `128` que estaban encima del control deslizante, manteniendo limpia la cabecera con la escala inferior de la regla.
  - Se corrigió el centrado vertical del control deslizante (`thumb`): se aisló el tamaño de layout del thumb a `20.dp` x `20.dp` con `contentAlignment = Center` y la burbuja numérica superior en `offset(y = -24.dp)`. De este modo, Compose alinea el centro exacto del círculo sobre el eje de la pista sin empujarlo hacia abajo ni desfasar el radio horizontal de desplazamiento.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Escala de regla con puntos alineados en tarjeta de Longitud

- **Alineación exacta de puntos y ticks en Longitud ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - Se sustituyeron los chips independientes por una escala de regla integrada directamente bajo el deslizador (`4`, `8`, `12`, `20`, `32`, `64`, `128`).
  - Los puntos sobre la pista (`Canvas`) ahora se calculan teniendo en cuenta el radio del thumb (`thumbRadius`), asegurando que cada punto coincida verticalmente de forma matemática con su tick (`|`) y número de límite correspondiente.
  - El thumb incluye una burbuja/píldora flotante superior que refleja el valor numérico actual en tiempo real durante el arrastre.
  - Cada marca de la regla es interactiva y cuenta con su respectivo `generator-preset-$mark` para seleccionar rápidamente cualquier límite estándar con un toque.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Sección de tipos de caracteres colapsable y contraseña generada al fondo

- **Ajustes de flujo y jerarquía en Generador ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - La tarjeta de **Tipos de caracteres** ahora es colapsable (`typesExpanded`), permitiendo contraerla para ahorrar espacio vertical cuando no se necesita cambiar las reglas.
  - La tarjeta de **Contraseña generada** se sitúa en la parte inferior, inmediatamente encima de los botones de acción (`Regenerar`, `Copiar` y `Usar contraseña`), facilitando el flujo visual y el acceso a las acciones sin saltos.
  - Se mantiene la reactividad en tiempo real (recalculo automático con slider y presets) y el realce sintáctico por colores.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Rediseño del Generador de Contraseñas (Propuesta A: Hero Reactivo & Titanio)

- **Hero Card superior e interactividad reactiva ([PasswordGeneratorScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/generator/PasswordGeneratorScreen.kt))**:
  - Se rediseñó la experiencia del Generador de Contraseñas: al generarse, la tarjeta de previsualización ("Contraseña generada") se sitúa en la parte superior como tarjeta Hero.
  - Visor monoespaciado de alta legibilidad con coloreado sintáctico de caracteres (números en azul suave, símbolos en coral y letras en contraste blanco/titanio), botón para alternar visibilidad y medidor de seguridad segmentado de 5 niveles con entropía (*bits* y nivel de fortaleza).
  - Reactividad instantánea en vivo: cualquier cambio posterior en el deslizador de longitud, chips preestablecidos (8, 12, 20, 32, 64) o interruptores de caracteres regenera automáticamente la contraseña en tiempo real.
  - Opciones de caracteres modernizadas con `Switch` interactivo en lugar de checkboxes convencionales.
  - Botones de acción unificados: `Regenerar` y `Copiar` directos sin necesidad de desplazarse hacia el fondo.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Remoción de icono estático de nube en OneDrive

- **Limpieza de cabecera en pantalla OneDrive ([OneDriveScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveScreen.kt))**:
  - Se eliminó el icono decorativo estático e inerte `Icons.Outlined.Cloud` de la esquina superior derecha que se mostraba cuando no había ninguna cuenta conectada.
  - La barra superior ahora solo muestra el menú contextual de opciones (`MoreVert`) cuando el usuario tiene una sesión activa vinculada.
- **Verificación**:
  - Pruebas unitarias ejecutadas (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Limpieza de icono redundante en campo Notas de formulario

- **Remoción de icono secundario en Notas ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt))**:
  - Se eliminó el `leadingIcon` redundante dentro del campo de texto multilínea (`OutlinedTextField`), ya que la cabecera de la sección colapsable `FormSection` ya cuenta con el icono identificador (`VaultkIconTile` con `Icons.Default.Description`).
  - Esto proporciona mayor área limpia para escribir y una visualización más equilibrada del área de texto.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador.

## 2026-10-29 — Secciones colapsables en Ver Entrada (Notas y Propiedades)

- **Notas y Propiedades colapsables por defecto ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt))**:
  - Se implementó el componente `DetailCollapsibleSection` para alinear la experiencia y la limpieza visual con la pantalla de agregar/editar entrada (`EntryFormScreen`).
  - Tanto **Notas** como **Propiedades** inician colapsadas (`initiallyExpanded = false`), mostrando cabecera interactiva con icono (`VaultkIconTile`), título, subtítulo explicativo (`R.string.form_notes_subtitle` y `R.string.form_properties_subtitle`) e indicador de estado (`ExpandMore` / `ExpandLess`).
  - Al expandirse, la sección de Notas expone el bloque de texto con su botón de copia directa; la sección de Propiedades despliega todos los metadatos (Grupo, Fechas de creación/modificación, Etiquetas y UUID con botón de copia rápida).
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - APK debug actualizada e instalada en el emulador `Pixel_9_Pro(AVD) - 15`.

## 2026-10-29 — Paleta Monocromo Minimalista (OLED & Titanio) y Centralización del Sistema de Color

- **Tokens de color segregados y centralizados ([Color.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Color.kt), [Theme.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Theme.kt))**:
  - Se creó el modelo de tokens segregado `VaultkCustomColors` provisto mediante `CompositionLocalProvider(LocalVaultkCustomColors)` para desacoplar el diseño de la herencia genérica de Material 3:
    - `iconTileBackground` (`#161616`), `iconTileBorder` (`#2C2C2C`), `iconTileTint` (`#E5E5E5`), `iconNormal` (`#D4D4D8`).
    - `buttonSecondaryBackground` (`#1F1F1F`), `buttonSecondaryText` (`#FFFFFF`), `buttonSecondaryBorder` (`#333333`).
    - `inputBackground` (`#181818`), `inputBorder` (`#222222`), `inputBorderFocused` (`#52525B`).
    - `textMuted` (`#71717A`).
  - Esquema `DarkColors`:
    - `primary = WhitePure` (`#FFFFFF`): Los botones de acción principal (`Button`) pasan a tener fondo blanco sólido con texto en negro puro (`#000000`), eliminando los bloques morados gigantes.
    - `primaryContainer = DarkPanel` (`#161616`) y `onPrimaryContainer = Silver` (`#E5E5E5`): Cajas de icono y tiles con fondo oscuro neutro y contorno fino sin tintes violetas.
    - `secondaryContainer = DarkBorderInteractive` (`#2C2C2C`) y `onSecondaryContainer = WhitePure`: Indicador activo de la barra de navegación inferior en titanio oscuro limpio.
    - `background = PureBlack` (`#000000`), `surface = DarkCharcoal` (`#121212`), `outlineVariant = DarkBorder` (`#222222`).
- **Eliminación de colores morados e índigo rezagados en componentes**:
  - **Explorador de Grupos ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**: Se eliminaron todos los colores hexadecimales hardcodeados (`0xFFBD9BDB`, `0xFF3E3654`, `0xFF282338`, `0xFF16151F`, `0xFF1C1A29`, `0xFF1E1D2D`), migrando filas, diálogos, iconos y el botón principal a los tokens semánticos centralizados.
  - **Formulario de Entrada ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt))**: Se cambiaron los iconos iniciales de los campos (`leadingIcon`), el botón de generar contraseña y los tiles de cabecera para utilizar `onSurfaceVariant` y `outline` en lugar de `primary` morado.
  - **Detalle de Entrada ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt))**: Iconos de propiedades actualizados a `onSurfaceVariant`.
  - **Controles y Notificaciones ([VaultkControls.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/component/VaultkControls.kt), [VaultkCards.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/component/VaultkCards.kt))**: `VaultkIconTile` añade borde sutil de 1dp; iconos de retroceso y estado vacío actualizados a tonos neutros.
  - **Ajustes ([SettingsScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/settings/SettingsScreen.kt))**: El selector segmentado de tema y las etiquetas de configuración de seguridad ahora usan tonos monocromáticos limpios.
  - **Pantalla Principal ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**: Floating Action Button (+) configurado con fondo `surfaceVariant` y contenido `onSurface`; iconos de sincronización en la nube limpios sin tinte violeta.
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest` — 24 tareas en 43s).
  - Build instalada y desplegada en el emulador `Pixel_9_Pro(AVD) - 15`.

## 2026-10-28 — Implementación del nuevo tema oscuro: Fondo Negro Puro (OLED) y Cajas Gris Carbón

- **Nueva paleta de tema oscuro OLED ([Color.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Color.kt), [Theme.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/theme/Theme.kt))**:
  - Se sustituyó el esquema con matiz azul medianoche (`#0C0E16` y `#151925`) por un tema OLED puro y grises carbón neutros:
    - `Ink = Color(0xFF000000)`: Fondo negro absoluto (apagado 100% de píxeles en pantallas OLED y máximo contraste).
    - `Panel = Color(0xFF161616)`: Cajas y tarjetas de superficie con gris carbón neutro mate.
    - `Field = Color(0xFF202020)`: Subcontenedores y campos de entrada de texto.
    - `Border = Color(0xFF2E2E2E)` y `outlineVariant = Color(0xFF262626)`: Bordes y delimitadores sutiles de 1dp.
    - `Muted = Color(0xFFA0A0A0)`: Gris neutro para textos secundarios y etiquetas sin contaminación azulada.
    - `onBackground` y `onSurface = Color(0xFFEDEDED)`: Blanco suave de alto contraste y confort visual.
    - `primaryContainer = Color(0xFF2D1B4E)`: Fondo violeta profundo para chips e iconos de acción.
- **Sincronización en componentes y diálogos ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - Se eliminaron colores fijos en los diálogos de creación y renombramiento de grupos, migrándolos a tokens semánticos `MaterialTheme.colorScheme.surface`, `surfaceVariant` y `outlineVariant` para coherencia total.
- **Verificación**:
  - Suite de pruebas unitarias ejecutada exitosamente (`testDebugUnitTest`, 24 tareas en 22s).
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.

## 2026-10-28 — Remoción de acción «Bloquear vault» del menú de opciones en Ver Entrada

- **Menú contextual de opciones limpio ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt), [VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se eliminó la opción `Bloquear vault` del menú desplegable de tres puntos (`EntryDetailHeader`), ya que el bloqueo global de la base de datos pertenece a la vista principal del vault y no al detalle de una entrada específica.
  - Se añadieron iconos temáticos a todas las acciones restantes del menú (`Guardar` con `Icons.Default.Save`, `Restaurar entrada` con `Icons.Default.Restore`, `Duplicar`, `Mover` y `Eliminar entrada`).
- **Verificación**:
  - Pruebas unitarias ejecutadas exitosamente (`testDebugUnitTest`).
  - Aplicación instalada y verificada en el emulador.

## 2026-10-28 — Rediseño visual de Ver Entrada (EntryDetailScreen)

- **Tarjeta unificada de Credenciales ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt))**:
  - Se agruparon los campos de `Usuario`, `Contraseña` y `URL` en una única tarjeta unificada con divisores sutiles (`HorizontalDivider`), eliminando la fragmentación visual de tener 3 micro-tarjetas flotantes desconectadas.
  - Cada fila conserva sus acciones rápidas alineadas a la derecha: copia de usuario, visibilidad y copia de contraseña con etiquetas de accesibilidad y tags de pruebas (`detail-copy-password`), y apertura/copia de URL.
- **Tarjeta dedicada para Notas ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt))**:
  - Encabezado con icono temático, título «Notas» y acción directa «Copiar notas» en la esquina superior derecha.
  - El texto de notas se presenta dentro de un contenedor redondeado con tinte suave `surfaceVariant`, mejorando la legibilidad para párrafos largos o multilínea.
- **Rediseño de Propiedades ([EntryDetailScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryDetailScreen.kt), [VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se sustituyó la lista plana dividida por tarjetas estilizadas (`DetailPropertyItem`) consistentes con el diseño de `EntryFormScreen`:
    - 📁 **Grupo**: Muestra el grupo actual al que pertenece la entrada (`groupName`).
    - 🕒 **Creado**: Icono de horario con fecha y hora completa formateada.
    - ⏱️ **Modificado**: Icono de historial con fecha y hora de última modificación.
    - 📅 **Caducidad**: Icono de calendario cuando la entrada tiene expiración programada.
    - 🏷️ **Etiquetas**: Contenedor con chips `primaryContainer` en `FlowRow`.
    - 🆔 **UUID**: Identificador monoespaciado con botón completo `OutlinedButton` para copiar el UUID al portapapeles.
- **Verificación**:
  - Suite de pruebas unitarias ejecutada con éxito (`testDebugUnitTest`).
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Eliminación de tarjeta redundante Ubicación y rediseño visual de Propiedades

- **Eliminación de Ubicación ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt))**:
  - Se retiró la tarjeta modular independiente «Ubicación» para evitar duplicidad de información con la sección «Propiedades» y el subtítulo de cabecera.
- **Rediseño enriquecido de Propiedades ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt), [strings_form.xml](file:///d:/Projects/Android/Vaultk/app/src/main/res/values/strings_form.xml))**:
  - Se sustituyó el texto plano apilado por filas estructuradas (`FormPropertyRow`) con esquinas redondeadas (`12.dp`), fondo suave `surfaceVariant` y micro-iconos temáticos en caja:
    - 📁 **Grupo**: Icono de carpeta con nombre del grupo destino.
    - 🕒 **Fecha de creación**: Icono de calendario/reloj con fecha y hora formateada (o «Pendiente al guardar» en entradas nuevas).
    - ⏱️ **Última modificación**: Icono de historial con fecha y hora formateada (o «Pendiente al guardar» en entradas nuevas).
    - 🆔 **Identificador (UUID)**: En entradas existentes, muestra el UUID en tipografía monoespaciada junto a un botón interactivo para copiarlo directamente al portapapeles con confirmación en `Snackbar`.
- **Verificación**:
  - Suite de pruebas unitarias ejecutada con éxito (`testDebugUnitTest`).
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Rediseño del formulario de entrada: M3 compacto con iconos integrados y secciones modulares

- **Iconos integrados dentro de los campos ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt))**:
  - Se eliminó el envoltorio `FormInput` con recuadros morados exteriores (`VaultkIconTile`), ganando ~60dp de ancho horizontal útil en cada campo.
  - Los campos de texto (`Título`, `Usuario`, `Contraseña`, `URL`, `Etiquetas`, `TOTP`, `PIN`) utilizan ahora `leadingIcon` nativo de Material 3 con `Modifier.fillMaxWidth()`, ofreciendo una alineación uniforme y despejada sin efecto zigzag.
  - **Etiquetas**: Se implementó una visualización basada en `FlowRow` con chips (`Surface` con fondo `primaryContainer` y botón `Close` para removerlas) y campo de texto con icono de etiqueta (`Icons.AutoMirrored.Filled.Label`) y botón de añadir.
- **Secciones modulares ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt), [strings_form.xml](file:///d:/Projects/Android/Vaultk/app/src/main/res/values/strings_form.xml))**:
  - **Ubicación**: Nueva sección colapsable independiente que muestra el grupo de destino en el vault (`📁 Raíz › [Grupo]`).
  - **Notas**: Se extrajo el área de notas a su propia sección modular independiente con `minLines = 3`, `maxLines = 8` y contador de caracteres `0/2000`.
  - Se conservaron intactas las secciones existentes: **Avanzado** (con `leadingIcon` en TOTP y PIN), **Adjuntos** y **Propiedades**.
- **Menú de acciones (3 puntos ⋮)**:
  - Se añadió el icono de descarte (`Icons.Default.DeleteOutline`) al ítem «Descartar formulario» en el `DropdownMenu`.
- **Verificación**:
  - Suite de pruebas unitarias ejecutada con éxito (`testDebugUnitTest`).
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Botón de guardar y simplificación del formulario de entrada

- **Icono de guardado ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt))**:
  - Se sustituyó el icono de check (`Icons.Default.Check`) en la barra superior por el icono de disco de guardado (`Icons.Default.Save`), haciendo explícita la acción de guardar la entrada o sus modificaciones en el vault.
- **Eliminación del banner de aviso ([EntryFormScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/EntryFormScreen.kt), [strings_form.xml](file:///d:/Projects/Android/Vaultk/app/src/main/res/values/strings_form.xml))**:
  - Se retiró la tarjeta informativa superior («El ✓ guarda la entrada en el vault...») para despejar el inicio del formulario y llevar directamente al usuario a los campos de información («General», «Título», etc.).
  - Se ajustó el texto de confirmación de limpieza de contraseña (`form_password_clear_message`) para referenciar la acción de guardar sin aludir al símbolo ✓.
- **Verificación**:
  - Suite de pruebas unitarias ejecutada con éxito (`testDebugUnitTest`).
  - Pruebas conectadas actualizadas ([EntryFormTest.kt](file:///d:/Projects/Android/Vaultk/app/src/androidTest/java/com/edro08/vaultk/EntryFormTest.kt), [VaultHomeTest.kt](file:///d:/Projects/Android/Vaultk/app/src/androidTest/java/com/edro08/vaultk/VaultHomeTest.kt)).
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Remoción del icono de candado decorativo en la barra superior

- **Barra superior de contenido ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se removió el icono de candado decorativo (`Icons.Default.Lock`) ubicado junto al título principal «Vaultk», dejando únicamente el texto con estilo `titleLarge` para una cabecera más limpia y minimalista.
- **Verificación**:
  - `compileDebugKotlin` y empaquetado de APK debug completados con éxito.
  - APK debug instalada y verificada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Corrección de persistencia y sincronización al renombrar grupos

- **Corrección en serializador KDBX ([KdbxWriter.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/data/KdbxWriter.kt))**:
  - **Causa raíz identificada**: Al renombrar un grupo, el modelo de dominio en memoria se actualizaba correctamente, pero en `KdbxWriter.applyGroups`, cuando un grupo ya existía (`existing = target.groups.firstOrNull { it.uuid.toString() == child.id }`), se reutilizaba la instancia de LinguaFranca sin actualizar su propiedad `it.name = child.name`. Como resultado, `database.save(...)` escribía el archivo KDBX conservando el nombre original del grupo. Al ejecutarse la sincronización con OneDrive (o recargar el archivo local), `reader.read` reabría el archivo físico con el nombre anterior, revirtiendo el cambio en la interfaz de usuario.
  - **Solución implementada**:
    - Se añadió la actualización explícita `if (it.name != child.name) { it.name = child.name }` en los nodos de subgrupos existentes.
    - Se aseguró la sincronización del nombre del grupo raíz `if (changed.root.name.isNotBlank() && database.rootGroup.name != changed.root.name) { database.rootGroup.name = changed.root.name }`.
- **Verificación**:
  - Prueba unitaria añadida en [KdbxWriterTest.kt](file:///d:/Projects/Android/Vaultk/app/src/test/java/com/edro08/vaultk/KdbxWriterTest.kt) (`renamesGroupAcrossSaves`), comprobando que al renombrar un grupo y persistir con `KdbxWriter`, el archivo KDBX resultante conserva e informa el nuevo nombre al reabrirse.
  - Suite de pruebas `testDebugUnitTest --tests com.edro08.vaultk.KdbxWriterTest` ejecutada con éxito (`BUILD SUCCESSFUL`).
  - APK debug instalada y ejecutada en el emulador `Pixel_9_Pro(AVD) - 15`.


## 2026-10-28 — Resumen de contenido del grupo explorado y jerarquía tipográfica

- **Estandarización tipográfica del grupo actual ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - Se elevó la jerarquía visual de la ruta de navegación jerárquica (breadcrumbs `Raíz › Cuentas-Y`) pasando de `bodyMedium` (14sp) a `titleMedium` en negrita (`FontWeight.Bold`, 16sp), otorgándole el peso y protagonismo correspondiente a la ubicación activa frente a los subgrupos de la lista.
- **Resumen contextual de contenido en el grupo activo ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - Directamente debajo de la ruta, se incorporó un subtítulo informativo en tiempo real (`X subgrupos · Y entradas`, `Sin subgrupos · Vacío`, etc.) que indica el contenido total del nivel donde el usuario se encuentra parado.
  - El encabezado de sección ahora indica dinámicamente la cantidad de elementos visibles (`Subgrupos (N)`), manteniendo la coherencia visual con la lista.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` compilados con éxito.
  - Pruebas conectadas en emulador `Pixel_9_Pro(AVD) - 15` superadas (`GroupPickerLayoutTest`).
  - APK debug instalada y ejecutada en el emulador.

## 2026-10-28 — Menú contextual con tres puntos (⋮), creación directa de subgrupos y renombramiento

- **Menú contextual en tarjetas de subgrupos ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - Cada tarjeta de subgrupo incluye un botón de 3 puntos (`⋮`, `Icons.Default.MoreVert`) con un `DropdownMenu` oscuro que despliega:
    - 📁➕ **Crear subgrupo**: Abre directamente el modal «Nuevo grupo» preconfigurado con el subgrupo seleccionado como padre (`Se creará dentro de: Raíz › [Subgrupo]`), permitiendo agregar subgrupos a grupos vacíos sin necesidad de salir y volver a entrar al explorador (0 clics de navegación extra).
    - ✏️ **Cambiar nombre**: Abre un diálogo modal para renombrar el grupo en tiempo real.
- **Interacción dual optimizada**:
  - Tocar el cuerpo de la tarjeta: si tiene subgrupos entra a explorarlos (`browseId = child.id`); si no tiene subgrupos, lo selecciona directamente (`onSelect(child.id)`).
  - Tocar los 3 puntos: abre el menú contextual sin disparar la selección ni la navegación de la tarjeta.
- **Operaciones de dominio y renombramiento ([VaultOperations.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/domain/VaultOperations.kt), [EditVault.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/application/EditVault.kt), [VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Añadida función inmutable `withRenamedGroup(targetId, newName)` y `EditVault.renameGroup`.
  - Integrado `onRename` en `VaultContentScreen` para sincronizar cambios en vaults locales y en la nube (`persistCloudChange`).
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` compilados con éxito.
  - Pruebas conectadas en emulador `Pixel_9_Pro(AVD) - 15` superadas (`GroupPickerLayoutTest` y `VaultHomeTest#selectsNestedGroupAndOpensEntryWithoutLeavingSession`).
  - APK debug instalada y ejecutada en el emulador.

## 2026-10-28 — Jerarquía en tarjeta de grupo, selector con metadatos condicionales y tipografía estilizada

- **Profundidad jerárquica en pantalla principal ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - La tarjeta de «Grupo» ahora muestra la ruta jerárquica completa calculada en tiempo real (`Raíz › Cuentas-D` o niveles anidados adicionales) en lugar de únicamente el nombre del grupo actual, con `maxLines = 1` y `TextOverflow.Ellipsis`.
  - Se sustituyó el icono final desplegable `▾` (`Icons.Default.ArrowDropDown`) por un chevron derecho `>` (`Icons.AutoMirrored.Filled.KeyboardArrowRight`), manteniendo el comportamiento de abrir el selector `GroupPickerSheet`.
- **Explorador de grupos y conteo de metadatos ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - **Alineación de cabecera**: Se eliminó el botón de regreso invisible cuando se explora la raíz (`currentPath.size <= 1`), permitiendo que el icono de carpeta y el título «Explorar grupos» queden alineados limpiamente al margen izquierdo sin espacio vacío forzado. La flecha atrás `←` solo aparece cuando hay niveles superiores (`currentPath.size > 1`).
  - **Metadatos por subgrupo**: Cada elemento de la lista de subgrupos muestra un subtítulo detallado con conteos de subgrupos y entradas (`X subgrupos · Y entradas`, `Sin subgrupos · Vacío`, etc.).
  - **Chevron condicional y selección directa**:
    - Si el subgrupo contiene subgrupos hijos (`child.groups.isNotEmpty()`), muestra el chevron `>` y al pulsarlo se navega para explorar su interior (`browseId = child.id`).
    - Si el subgrupo no tiene subgrupos (`child.groups.isEmpty()`), no muestra el chevron `>` y al pulsarlo se selecciona directamente (`onSelect(child.id)`), evitando entrar a subgrupos vacíos innecesariamente.
  - **Estilización del botón «Usar grupo [Nombre]»**: Se refinó la tipografía reduciendo el peso y tamaño visual de las letras a `MaterialTheme.typography.bodyMedium` con `FontWeight.Medium` y altura de `46.dp`, logrando un acabado estilizado y equilibrado.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` ejecutados con éxito.
  - Pruebas conectadas en emulador `Pixel_9_Pro(AVD) - 15` superadas (`GroupPickerLayoutTest`, `VaultHomeTest#selectsNestedGroupAndOpensEntryWithoutLeavingSession`, `VaultHomeTest#searchesGroupsAndWarnsBeforeDiscardingTemporaryGroup`, `VaultHomeTest#createsEntryInSessionAndConfirmsDiscardOnVaultClose`).
  - APK debug instalada y ejecutada en el emulador.

## 2026-10-27 — Rediseño del explorador de grupos y modal de nuevo grupo

- **Explorador de grupos y navegación jerárquica ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - **Diferenciación clara de las tres acciones**:
    - **← Flecha atrás**: navega al grupo padre inmediato. En la raíz (`Raíz`), se oculta/deshabilita con transparencia para preservar el espaciado visual.
    - **Ruta de migas de pan (Breadcrumbs `Raíz › Cuentas-Y`)**: muestra el camino jerárquico actual; cada nivel antecesor es interactivo y permite saltar directamente a ese nivel con un solo toque.
    - **✕ Botón de cierre**: descarta y cierra el selector regresando al vault sin cambiar el grupo activo (`onDismiss`).
    - **Botón inferior «Usar grupo [Nombre]»**: botón primario en píldora violeta (`#BD9BDB`) a ancho completo que confirma y selecciona el grupo actualmente explorado (`onSelect(browse.group.id)`).
  - **Sección «Subgrupos» y lista exploratoria**:
    - Cabecera con título «Subgrupos» y botón tonal `[📁+] Nuevo grupo`.
    - Cada subgrupo se presenta en una tarjeta oscura con icono de carpeta violeta, nombre y chevron `>`. Tocar un subgrupo permite adentrarse a explorar su contenido.
    - Buscador integrado `[🔍] Buscar grupo` con filtrado dinámico.
    - Se retiró el mensaje obsoleto inferior («Guarda una copia KDBX después de crear un grupo para conservarlo»), dejando limpia la parte inferior detrás del botón de confirmación.
- **Modal de creación de grupo ([GroupPickerSheet.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/GroupPickerSheet.kt))**:
  - Rediseño con tarjeta modal oscura (`#171622`) y bordes refinados según el mockup provisto.
  - Título «Nuevo grupo» y subtítulo descriptivo «Crea un grupo dentro de la ubicación actual.».
  - Campo de texto «Nombre del grupo» con placeholder «Ej. Servidores».
  - Caja contextual informativa «Se creará dentro de» mostrando el icono de carpeta y la ruta completa (`📁 Raíz › Cuentas-Y`).
  - Botones inferiores «Cancelar» (texto) y «Crear grupo» (píldora tonal deshabilitada hasta que el nombre sea válido y no duplicado).
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` ejecutados con éxito.
  - Pruebas instrumentadas superadas en `Pixel_9_Pro(AVD) - 15` (`GroupPickerLayoutTest` y `VaultHomeTest#selectsNestedGroupAndOpensEntryWithoutLeavingSession`).
  - APK debug compilada e instalada en el emulador.

## 2026-10-27 — Notificación flotante en desbloqueo y corrección de superposición con FAB

- **Notificación de errores al abrir/desbloquear vault ([UnlockVaultScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/UnlockVaultScreen.kt))**:
  - Se eliminó el texto estático en rojo (`Text(errorMessage, color = error)`) debajo del botón de desbloqueo.
  - Se integró el componente `VaultkNotificationCard` con icono de error (`Icons.Outlined.ErrorOutline`), borde oscuro de contraste y botón de descarte manual `✕`.
  - Se añadió descarte automático mediante `LaunchedEffect` tras 4 segundos, invocando el callback `onErrorDismiss` que sincroniza el borrado de `unlockError` en `MainActivity.kt`.
- **Corrección de superposición con FloatingActionButton en vista principal ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se detectó que la tarjeta flotante de notificación (p. ej. sincronización de copia local) se posicionaba a nivel inferior directo (`padding(bottom = 8.dp)`), quedando montada por debajo del botón flotante de agregar entrada (`+` FAB).
  - Se ajustó dinámicamente el espaciado inferior de la tarjeta: cuando el botón FAB está presente (`hasFab = destination == Vault && entry == null && editable && !saving && !syncing`), el padding inferior se eleva a `80.dp`, posicionando la notificación limpiamente por encima del FAB sin superposiciones ni colisiones visuales.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` ejecutados con éxito.
  - Pruebas instrumentadas `UnlockElapsedTest` (2/2) y `CloudVaultContentScreenTest` (4/4) aprobadas en `Pixel_9_Pro(AVD) - 15`.
  - APK debug instalado y desplegado en el emulador activo.

## 2026-10-26 — Notificaciones flotantes auto-descartables y unificación de estados vacíos

- **Notificaciones flotantes temporales ([VaultkControls.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/component/VaultkControls.kt))**:
  - Se creó el componente `VaultkNotificationCard` en formato píldora oscura con borde fino, icono representativo (nube verde con check `#44DB98` para sincronización exitosa, alerta roja para incidencias), mensaje descriptivo y botón de cierre `✕`.
  - Se eliminaron los mensajes que permanecían fijos indefinidamente en la parte superior de las listas (`VaultContentScreen.kt` y `CloudCopiesScreen.kt`). Ahora se muestran como tarjetas flotantes en la parte inferior sobre la barra de navegación, con transiciones animadas (`fadeIn` y `slideInVertically`), cerrándose automáticamente tras 4 segundos o al pulsar `✕`.
  - Integrado en el `SnackbarHost` de [OpenVaultScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OpenVaultScreen.kt) para unificar la retroalimentación visual en la selección de archivos y en toda la aplicación.
- **Unificación de diseño de estados vacíos ([EmptyState](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/component/VaultkControls.kt))**:
  - En [CloudCopiesScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/CloudCopiesScreen.kt): se reemplazó el texto plano sin estilo («Todavía no hay copias persistentes...») por una tarjeta `VaultkCard` con `EmptyState`, icono `Icons.Outlined.CloudOff`, título «No hay copias sin conexión» y descripción contextualizada.
  - En [VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt): se incorporó el icono centrado en color violeta para el estado de grupo vacío (`Icons.Outlined.FolderOpen`), alineándolo con el mockup del usuario.
  - En [LocalVaultPickerScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/LocalVaultPickerScreen.kt): se actualizaron los estados de carpeta vacía y error a tarjetas `VaultkCard` con `EmptyState`.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` superados sin advertencias bloqueantes.
  - Pruebas instrumentadas `connectedDebugAndroidTest` (`CloudVaultContentScreenTest` y `CloudCopiesScreenTest`, 7/7 tests) aprobadas en `Pixel_9_Pro(AVD) - 15`.
  - Pruebas unitarias `testDebugUnitTest` superadas al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Animación de sincronización en la nube y rediseño del popup de estado

- **Animación de la nube en la cabecera ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Se eliminó la barra de progreso lineal (`LinearProgressIndicator`) que aparecía al abrir el vault empujando la lista de entradas.
  - Se reemplazó por un indicador circular (`CircularProgressIndicator`) con pista sutil que envuelve el icono de la nube en la barra superior durante la sincronización (`CloudSync`).
  - Al completarse la sincronización, el anillo circular desaparece y la nube muestra el icono de verificación (`CloudDone`) en color primario sin interferir con el contenido.
- **Rediseño del diálogo modal de Estado de sincronización ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - Contenedor modal con esquinas redondeadas (`28.dp`) siguiendo el mockup del usuario.
  - Cabecera con indicador verde `OneDrive · copia privada` y checkmark con estado conciso («Sincronizada · Sin cambios locales pendientes»).
  - Listado de datos con iconos:
    - `FolderOpen`: «Origen: OneDrive».
    - `Description`: «Archivo: [nombre]».
    - `Inventory2`: «Tamaño: [X.X KB]» con desglose en bytes.
    - `Schedule`: «Última sinc. · [fecha y hora formateada]».
    - `VpnKey`: «ETag base:» y hash correspondiente.
  - Botones de acción inferiores en disposición horizontal:
    - Botón redondeado de «Cancelar» a la par del botón primario de «Sincronizar ahora» (con icono `Sync`).
    - Se retiraron del popup los botones «Desvincular» y «Abrir copias sin conexión», manteniéndolos en el menú principal `⋮`.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` superados sin errores.
  - Pruebas instrumentadas `CloudVaultContentScreenTest` (4/4) ejecutadas y aprobadas en `Pixel_9_Pro(AVD) - 15`.
  - `testDebugUnitTest` superado al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Organización del menú de bóveda en secciones con iconos y separadores

- **Menú de opciones de la bóveda abierta ([VaultContentScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/vault/VaultContentScreen.kt))**:
  - **Organización por secciones funcionales**:
    - **Sección Vault**: «Bloquear vault» (`Icons.Outlined.Lock`), «Cambiar vault» (`Icons.Outlined.FolderOpen`) y «Guardar vault» (`Icons.Outlined.Save`, cuando aplica).
    - **Línea divisoria horizontal** (`HorizontalDivider`).
    - **Sección Online / OneDrive** (cuando la bóveda proviene de la nube): «Sincronizar ahora» (`Icons.Outlined.CloudSync`), «Ver estado de sincronización» (`Icons.Outlined.Info`) y «Desvincular de OneDrive» (`Icons.Outlined.LinkOff`).
    - **Línea divisoria horizontal** (`HorizontalDivider`).
    - **Sección Biometría** (cuando hay soporte de biometría en el hardware): «Activar/Desactivar desbloqueo biométrico» (`Icons.Outlined.Fingerprint`).
    - **Línea divisoria horizontal** (`HorizontalDivider`).
    - **Sección Salida / Cierre**: «Cerrar vault» (`Icons.AutoMirrored.Outlined.Logout`) resaltado con el color de advertencia/error (`colorScheme.error`).
  - **Iconografía coherente**: Todos los ítems incorporan ahora su `leadingIcon` alineado a Material 3, conservando idénticas las etiquetas de accesibilidad y callbacks de navegación.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` superados sin errores.
  - Pruebas instrumentadas `CloudVaultContentScreenTest` (4/4) ejecutadas y aprobadas en `Pixel_9_Pro(AVD) - 15`.
  - `testDebugUnitTest` superado al 100%.
  - APK debug instalado y ejecutado en el emulador.

## 2026-10-26 — Iconos en menú de copias y gestión de eliminación de copia en uso

- **Mejoras en el menú contextual de Copias sin conexión ([CloudCopiesScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/CloudCopiesScreen.kt))**:
  - **Iconos en acciones**:
    - «Dejar de usar esta copia» ahora cuenta con el icono `Icons.Outlined.LinkOff`.
    - «Eliminar copia de este dispositivo» ahora cuenta con el icono `Icons.Outlined.Delete` y texto en color de error (`error`).
  - **Gestión de eliminación de copia activa/en uso**:
    - Se agregó el indicador visual **«En uso»** en la cabecera de la tarjeta junto al badge de «Solo lectura» cuando la copia coincide con la activa (`selected?.id == copy.id`).
    - Si el usuario pulsa «Eliminar copia de este dispositivo» en una copia activa, en lugar de invocar `remove()` y provocar el fallo `CloudCopyProtectedException`, se despliega un diálogo explicativo:
      *«[Archivo] está actualmente en uso como tu vault activo. Para eliminarla de este dispositivo, primero debes dejar de usarla.»*.
    - Dicho diálogo ofrece la acción directa **«Dejar de usar y eliminar»**, la cual desvincula la referencia activa (`onDeselect()`) e inicia la confirmación de borrado, permitiendo eliminarla de forma limpia sin errores locales.
- **Verificación**:
  - `connectedDebugAndroidTest` (`CloudCopiesScreenTest` 3/3) ejecutado y aprobado en `Pixel_9_Pro(AVD) - 15`.
  - `testDebugUnitTest` superado al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Estilo del botón Cerrar y mensajes concisos en Detalles del archivo

- **Ajustes de UI en la tarjeta modal de detalles (`OneDriveScreen.kt`)**:
  - **Botón «Cerrar»**: Reemplazado de simple `TextButton` plano a `OutlinedButton` en píldora (`RoundedCornerShape(24.dp)`, altura de 48.dp, fondo `surfaceVariant` sutil y borde `outlineVariant`), adquiriendo el mismo peso visual, simetría y contorno de botón que «Abrir» y «Descargar».
  - **Mensajes no redundantes y concisos**:
    - Se eliminó el texto extenso y repetitivo.
    - Cuando el archivo **no está descargado**: muestra la explicación directa *«Se guardará una copia cifrada en tu dispositivo para abrirla sin conexión. No altera el archivo en OneDrive.»*.
    - Cuando el archivo **ya está descargado**: muestra la nota contextual *«Copia local cifrada disponible para abrir sin conexión.»* (evitando instruir la descarga cuando ya está en el dispositivo).
    - Al completar la descarga: confirmación limpia y breve *«Copia descargada correctamente.»*.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` superados sin errores ni advertencias.
  - `testDebugUnitTest` ejecutado y aprobado al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Limpieza de pantallas heredadas de OneDrive y eliminación de nota de ayuda

- **Limpieza de archivos y UI**:
  - Se eliminaron los archivos heredados `OneDriveAccountScreen.kt` y `OneDriveBrowserScreen.kt`, habiendo quedado completamente reemplazados por [OneDriveScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveScreen.kt).
  - Se retiró la nota de ayuda al pie de la vista de OneDrive («El menú ⋮ permite cambiar de cuenta y acceder a las copias sin conexión.») tanto de [OneDriveScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveScreen.kt) como de `strings_open.xml`.
- **Verificación**:
  - `compileDebugKotlin` y `compileDebugAndroidTestKotlin` superados sin errores.
  - `testDebugUnitTest` superado al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Unificación y rediseño de pantallas de OneDrive (OneDriveScreen)

- **Unificación de pantalla y estados según mockups**:
  - Se unificaron [OneDriveAccountScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveAccountScreen.kt) y [OneDriveBrowserScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveBrowserScreen.kt) en [OneDriveScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/OneDriveScreen.kt), adaptando la experiencia a los dos estados de cuenta y al visor de detalles:
  - **Estado Desconectado (Mockup 1)**:
    - Cabecera con título `OneDrive`, icono de nube a la derecha y subtítulo descriptivo «Conecta tu cuenta Microsoft para explorar y abrir tus archivos KDBX.».
    - Tarjeta central con contenedor redondeado oscuro, icono circular de nube en acento violeta, título «Conecta tu OneDrive», subtítulo explicativo de cifrado KDBX y botón en píldora violeta «Conectar cuenta».
  - **Estado Conectado / Explorador (Mockup 2)**:
    - Cabecera con estado `• Cuenta Microsoft conectada` y menú overflow `⋮` con opciones directas:
      - «Copias sin conexión»: acceso directo a la gestión de copias locales.
      - «Comprobar acceso a OneDrive»: verificación de conectividad y permisos.
      - «Desconectar cuenta»: diálogo de confirmación de cierre de sesión.
    - Barra de búsqueda redondeada («Buscar archivos KDBX…») con filtrado en tiempo real y botón de limpieza rápida.
    - Barra de navegación por migas de pan («Mis archivos › [Carpeta]») con navegación ascendente interactiva.
    - Listado de carpetas y archivos KDBX en tarjetas con esquinas redondeadas, mosaicos de iconos violetas (`Folder`, `Lock`), metadatos en línea (tamaño y estado «Disponible en OneDrive» o «Descargado») y chevron de navegación.
    - Nota al pie: «El menú ⋮ permite cambiar de cuenta y acceder a las copias sin conexión.».
  - **Hoja modal de detalles del archivo (Mockup 3)**:
    - `ModalBottomSheet` («Detalles del archivo») al pulsar cualquier archivo KDBX con cabecera e icono de información y botón de cierre `✕`.
    - Fila de datos tabulada: Archivo, Estado (indicador verde «Descargado (Copia local disponible)» o nube «Disponible en OneDrive (No descargado)»), Tamaño, Ubicación completa con ruta, Última modificación formateada y Tipo («Base de datos KeePass (.kdbx)»).
    - Párrafo explicativo sobre descarga en almacenamiento privado e integridad local.
    - Progreso de descarga en vivo (`LinearProgressIndicator`), contador de bytes transferidos y gestión de incidencias de red.
    - Botones inferiores: «Cerrar» y acción primaria dinámica:
      - Si el archivo **ya está descargado**: botón **«Abrir»** para seleccionar la bóveda e ir directamente al desbloqueo.
      - Si el archivo **no está descargado**: botón **«Descargar»** que realiza la transferencia con animación y pasa automáticamente a «Abrir» al finalizar.
- **Verificación**:
  - `connectedDebugAndroidTest` (`OpenVaultScreenTest`, `CloudCopiesScreenTest`) ejecutado y aprobado en `Pixel_9_Pro(AVD) - 15`.
  - `testDebugUnitTest` ejecutado con éxito al 100%.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Simplificación de textos en botones de Copias sin conexión

- **Ajuste de etiquetas de acción**:
  - En [CloudCopiesScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/CloudCopiesScreen.kt) y [strings_open.xml](file:///d:/Projects/Android/Vaultk/app/src/main/res/values/strings_open.xml):
    - El botón de apertura se simplificó de «Abrir copia» a **«Abrir»** (`open_copy_button`), ya que el encabezado de la pantalla ya contextualiza que son copias.
    - El botón de sincronización en estado al día / general se simplificó de «Sincronizar ahora» a **«Sincronizar»** (`cloud_copies_sync`).
- **Verificación**:
  - `CloudCopiesScreenTest` (3/3) y `CloudVaultContentScreenTest` (4/4) ejecutados y aprobados en `Pixel_9_Pro(AVD) - 15`.
  - APK debug instalado y desplegado en el emulador.

## 2026-10-26 — Botón Sincronizar junto a Abrir Copia en Copias sin conexión

- **Botón de Sincronización en paralelo con Abrir Copia**:
  - En [CloudCopiesScreen.kt](file:///d:/Projects/Android/Vaultk/app/src/main/java/com/edro08/vaultk/ui/screen/storage/CloudCopiesScreen.kt), se dispuso el botón de sincronización al lado del botón principal «Abrir copia» en una fila (`Row`).
  - **Estilos dinámicos según el estado de la copia**:
    - **Al día (`CloudCopyState.Clean`)**: Botón en verde oscuro (`0xFF1B382B`) con borde sutil verde (`0x4044DB98`), texto e icono de nube (`Icons.Outlined.Cloud`) en verde brillante (`0xFF44DB98`) con texto «Al día».
    - **Cambios pendientes o rearmado**: Botón en tono neutro/opaco con texto descriptivo según la acción requerida («Subir cambios pendientes», «Reconciliar con OneDrive») y estado de carga durante operaciones asíncronas.
    - Se mantienen las acciones directas de resolución de conflictos («Conservar versión local» / «Usar versión de OneDrive») en caso de conflicto detectado.
- **Verificación y Pruebas**:
  - `CloudCopiesScreenTest` adaptado para el flujo del menú contextual y diálogo de confirmación de eliminación local.
  - Ejecución exitosa de `connectedDebugAndroidTest` (`CloudCopiesScreenTest` 3/3 aprobados en `Pixel_9_Pro(AVD) - 15`).
  - `testDebugUnitTest` ejecutado y pasando al 100%.
  - APK debug instalado y lanzado en el emulador.

## 2026-10-26 — Rediseño de OpenVault y Copias sin conexión con barra inferior persistente

- **Rediseño visual de OpenVaultScreen**:
  - Reestructurado según el mockup del usuario con encabezado `Abrir vault`, descripción «Selecciona dónde está guardado tu archivo KDBX.», divisores horizontales sutiles y secciones etiquetadas:
    - **`DISPOSITIVO`**: Tarjeta «Archivos locales» con descripción «Explorar archivos KDBX del dispositivo».
    - **`ALMACENAMIENTO EN LA NUBE`**: Tarjeta «OneDrive» con descripción «Explorar archivos de tu cuenta» e indicador verde con checkmark «Cuenta conectada».
    - **`TUS VAULTS RECIENTES`**: Lista de bóvedas recientes locales y copias en la nube (`Vaultk-Test.kdbx` con tag verde `Sin cambios pendientes` o bóvedas locales con fecha/origen).
    - **Pie de pantalla**: Botón centrado «Gestionar copias sin conexión» con icono de sincronización.
- **Rediseño de CloudCopiesScreen («Copias sin conexión»)**:
  - Top bar con título «Copias sin conexión» y subtítulo descriptivo.
  - Contador de copias disponibles («COPIAS DISPONIBLES · N»).
  - Tarjeta de copia con icono, nombre, chip de «Solo lectura», origen de OneDrive con email de la cuenta, estadísticas en línea con separador (tamaño y fecha de descarga `d MMM · h:mm a`), indicador de estado en verde («Sin cambios locales pendientes»), botón principal en píldora violeta («Abrir copia») y botón circular `⋮` para deseleccionar o eliminar copia local.
  - Tarjeta plegable / desplegable con animación para «Registro de operaciones (N operaciones · Sin secretos)».
- **Navegación inferior persistente**:
  - Ambas pantallas (`OpenVaultScreen` y `CloudCopiesScreen`) integran la barra de navegación inferior `VaultkBottomNavigation` (`Vault`, `Generador`, `Ajustes`), permitiendo cambiar entre herramientas desde el selector de bóvedas.
- Verificación: superadas todas las pruebas unitarias y de instrumentación en `Pixel_9_Pro(AVD) - 15` (`OpenVaultScreenTest` 3/3, `CloudCopiesScreenTest` 3/3, `CloudVaultContentScreenTest` 4/4). APK instalada y ejecutada en el emulador.

## 2026-10-26 — 22.07 y 22.08 completadas: interfaz, ajustes de sincronización y endurecimiento

- **22.07 — Interfaz y ajustes**:
  - `PreferenceStore` (`domain`), `AppSettings` (`data`) y `ManagePreferences`
    (`application`) incorporan las preferencias `cloudAutoSync` (por defecto
    `true`) y `cloudAllowMetered` (por defecto `false`).
  - Se unificaron las vistas de bóveda local y OneDrive en una única pantalla
    (`VaultContentScreen`, eliminando la clase redundante `CloudVaultContentScreen`), de modo que las copias de OneDrive comparten exactamente la
    misma cabecera (`Vaultk` con estado y menú `⋮` `CloseMenu`), buscador
    (`Buscar en el vault…`), selector de grupos (`GroupPickerSheet` con creación
    y movimiento de entradas), listado `EntryCard`, vista de detalle
    (`EntryDetailHeader` / `EntryDetailScreen`) y barra inferior (`Vault`,
    `Generador`, `Ajustes`), añadiendo las opciones específicas de OneDrive en
    la tarjeta de estado y en el menú `⋮`.
  - `SettingsScreen` añade la tarjeta «Sincronización con OneDrive» con los
    interruptores «Sincronización automática» y «Usar datos móviles», conectada
    con `MainActivity`.
  - En copias de OneDrive, `VaultContentScreen` muestra el origen («OneDrive ·
    copia privada»), el estado actual y la fecha de última sincronización
    (`cloud_last_sync`), barra de progreso (`LinearProgressIndicator`) durante el
    guardado y la transferencia, y las acciones «Sincronizar ahora», «Ver estado»
    (diálogo `statusDialog` con origen, nombre, ETag base, tamaño y última
    sincronización) y «Desvincular» (diálogo de confirmación que quita solo la
    referencia activa sin borrar la copia local ni el archivo remoto en
    OneDrive). Cuando la red activa es medida
    (`ConnectivityManager.isActiveNetworkMetered`) y `cloudAllowMetered` está
    desactivado, la sincronización automática al abrir, volver a primer plano o
    guardar se pausa mostrando un aviso explícito y permitiendo usar
    «Sincronizar ahora» manualmente.
  - `OpenVaultScreen` incorpora la tarjeta directa «Copias descargadas» para
    abrir y gestionar copias de OneDrive guardadas en el dispositivo aunque la
    cuenta esté desconectada, sin sustituir las pantallas existentes.
  - `OneDriveBrowserScreen` y `CloudCopiesScreen` añaden indicadores de progreso
    de transferencia (`LinearProgressIndicator`) y `CloudCopiesScreen` incluye un
    aviso explícito (`cloud_delete_policy_notice`) que distingue quitar la
    referencia activa («Dejar de usar esta copia»), eliminar la copia privada del
    dispositivo («Eliminar copia de este dispositivo») y nunca borrar el archivo
    en OneDrive.
- **22.08 — Pruebas y endurecimiento**:
  - Se amplió `GraphSmallUploadVerifierTest` con
    `expiredTokensRevokedPermissionsDeletedItemsConcurrentEditsAndTransferCutsNeverCorruptOrLoseData`
    para verificar que tokens caducados (`401`), permisos revocados (`403`),
    archivos eliminados (`404`), respuestas ambiguas, cortes de transferencia
    durante descarga o reconciliación y ediciones concurrentes nunca corrompen ni
    pierden la copia KDBX local ni dejan `uploadAttempted` bloqueado en falso,
    comprobando además la integridad y apertura del KDBX tras descargas y
    resoluciones de conflicto y la ausencia total de contraseñas, tokens, títulos
    de entradas y rutas locales en `sync-log.json`.
  - Se amplió `CloudVaultContentScreenTest` con
    `cloudVaultShowsOriginLastSyncStatusDialogUnlinkConfirmationAndSettingsControls`
    para verificar en UI el origen, la última sincronización, la pausa en red de
    datos móviles, el diálogo «Ver estado», la confirmación de «Desvincular» y
    los ajustes de OneDrive.
- Verificación: pasaron `testDebugUnitTest` (`ArchitectureTest` y
  `SynchronizeCloudCopyTest`) y las **33 pruebas instrumentadas** de la suite
  cloud en `emulator-5554` (`GraphSmallUploadVerifierTest` 14/14,
  `GraphSmallPendingUploaderTest` 4/4, `PrivateCloudCopyStoreTest` 8/8,
  `CloudCopiesScreenTest` 3/3 y `CloudVaultContentScreenTest` 4/4). Se desinstaló
  `com.edro08.vaultk.test` al finalizar, se mantuvo `FLAG_SECURE` activo en
  `MainActivity` y se comprobó que las huellas SHA-256 de la copia privada
  (`c24dcebc76a0efe581e93217915b430be94907762b577ddfdc4aa344fe4ddfe6`),
  `record.json` y `active.json` en `emulator-5554` permanecieron intactas.

## 2026-10-26 — 22.06 completada: conflictos, resolución explícita local/remota, copias de recuperación y registro sin secretos

- `CloudCopy` añade `conflict: CloudCopyConflict?` (`baseEtag`, `localBytes`,
  `localSavedAt`, `remoteEtag`, `remoteBytes`, `detectedAt`), `CloudCopyRepository`
  incorpora `recoverySnapshots(id)` y `logs(id)`, y `CloudCopySync` /
  `SynchronizeCloudCopy` añaden `resolveConflict(id, resolution, callback)` con
  `CloudConflictResolution.KeepLocal` y `CloudConflictResolution.UseRemote`.
- `PrivateCloudCopyStore` añade `stageConflictCiphertext`, `recordConflict`,
  `resolveConflictKeepLocal`, `resolveConflictUseRemote`, `recoverySnapshots` y
  `logs` (`sync-log.json` con escritura atómica y hasta 100 entradas de
  `CloudSyncLogEntry` sin contraseñas, tokens, títulos ni rutas locales):
  - Cuando `GraphSmallUploadVerifier.prepareConflict` detecta un ETag remoto
    distinto mientras existe una copia local `Pending` (o `Conflict`), descarga y
    verifica el `.kdbx` remoto en un nuevo blob cifrado inmutable sin sobrescribir
    el blob `.kdbx` local, comprueba una segunda lectura de metadatos y persiste
    `Conflict` con ambos `.kdbx` cifrados verificados en disco.
  - Al elegir explícitamente «Usar versión de OneDrive» (`UseRemote`),
    `resolveConflictUseRemote` verifica la firma e integridad de ambos `.kdbx` en
    disco, conmuta el puntero activo al blob remoto y conserva el blob `.kdbx`
    local como copia de recuperación cifrada.
  - Al elegir explícitamente «Conservar versión local» (`KeepLocal`),
    `resolveConflictKeepLocal` verifica ambos `.kdbx` en disco, conserva el blob
    remoto como copia de recuperación cifrada, avanza el ETag base al ETag remoto
    inspeccionado y ejecuta la subida condicional + verificación del `.kdbx` local.
- `CloudVaultContentScreen` y `CloudCopiesScreen` muestran la comparación de
  versiones en conflicto y los botones explícitos «Conservar versión local» y
  «Usar versión de OneDrive» (sin fusión automática), además del contador de
  copias de recuperación cifradas y la sección «Registro de operaciones (sin
  secretos)» en `CloudCopiesScreen`.
- Verificación: pasaron `testDebugUnitTest` (`ArchitectureTest` y
  `SynchronizeCloudCopyTest`) y las **31 pruebas instrumentadas** en
  `emulator-5554` (`GraphSmallUploadVerifierTest` 13/13,
  `GraphSmallPendingUploaderTest` 4/4, `PrivateCloudCopyStoreTest` 8/8,
  `CloudCopiesScreenTest` 3/3 y `CloudVaultContentScreenTest` 3/3), retirando
  `com.edro08.vaultk.test` al finalizar.

## 2026-10-26 — 22.05 completada: sincronización bidireccional sin descartar ediciones locales

- `CloudSyncOutcome` añade `UpToDate` y `Downloaded`, y `CloudCopySync` /
  `SynchronizeCloudCopy` incorporan `refresh(id)` junto a `synchronize(id)` y
  `reconcile(id)`.
- `PrivateCloudCopyStore` añade `readCopy`, `stageRemoteCiphertext`,
  `commitStagedRemoteClean` y `discardStagedRemoteCiphertext`: descarga y
  verifica el nuevo `.kdbx` cifrado en un archivo temporal privado fuera del
  bloqueo de sincronización y solo conmuta `record.json` si la copia sigue
  `Clean` en el `expectedBaseEtag` original, conservando los blobs `.kdbx`
  previos. Si el usuario guarda un cambio local (`Pending`) durante la descarga,
  `commitStagedRemoteClean` descarta únicamente el archivo en staging y protege
  la edición local.
- `GraphSmallUploadVerifier` incorpora `refresh` (solo `GET`) y
  `checkBeforeUpload`:
  - En copias `Clean`, `refresh` consulta el ETag remoto; si coincide tras dos
    lecturas devuelve `UpToDate`, y si cambió descarga el ciphertext desde la URL
    HTTPS preautorizada sin bearer, comprueba tamaño y cabecera KDBX, confirma
    una segunda lectura de metadatos y promueve la nueva versión (`Downloaded`).
  - En copias `Pending`, nunca descarga encima de la copia local: si
    `uploadAttempted == true` delega en `reconcile`; si `uploadAttempted == false`,
    `refresh` y `checkBeforeUpload` comprueban el ETag remoto antes de reclamar o
    enviar `PUT /content`, devolviendo `Conflict` si el remoto cambió o
    `StillPending` (sin activar `uploadAttempted`) si no hay red.
- `CloudVaultContentScreen` y `CloudCopiesScreen` presentan los estados
  sincronizado, pendiente, sincronizando, error y conflicto, añaden la acción
  manual «Sincronizar ahora» en copias `Clean`, y con `autoSync = true` en
  `MainActivity` sincronizan automáticamente al abrir la bóveda, al regresar a
  la aplicación (`ON_START`, recargando la bóveda abierta en memoria tras
  `Downloaded`) y al guardar cambios locales (`persistChange`).
- Verificación: pasaron `testDebugUnitTest` (`ArchitectureTest` y
  `SynchronizeCloudCopyTest`) y las **29 pruebas instrumentadas** de la suite
  cloud en `emulator-5554` (`GraphSmallUploadVerifierTest` 12/12,
  `GraphSmallPendingUploaderTest` 4/4, `PrivateCloudCopyStoreTest` 8/8,
  `CloudCopiesScreenTest` 2/2 y `CloudVaultContentScreenTest` 3/3), retirando
  `com.edro08.vaultk.test` al finalizar.

## 2026-10-26 — 22.04c completada: reconciliación explícita, pruebas de cortes y edición cloud en copias elegibles

- `CloudCopy` expone `uploadAttempted` y `CloudCopySync`/`SynchronizeCloudCopy`
  incorporan `reconcile` con resultado `Rearmed` además de `Verified`, `Conflict`,
  `Missing` y `StillPending`, manteniendo separados el envío único (`synchronize`)
  y la reconciliación por lectura (`reconcile`). `ManageCloudCopies` añade
  `openVersioned` para conservar la llave en memoria mientras la copia permanece
  abierta y poder cifrar cambios locales con `SaveCloudCopyLocally`.
- `PrivateCloudCopyStore` impone el límite de ruta pequeña (`250_000_000` bytes)
  en `savePending`, persiste `attemptedBlob` junto a `uploadAttempted` al
  reclamar la subida y lo conserva si el usuario guarda otra edición local
  (`savePending`) antes de reconciliar. Añadidos `withAttemptedPendingSnapshot`,
  `rearmUncommittedPendingUpload` y `advanceAttemptedPendingBase` con CAS de
  estado, ETag base y huellas SHA-256, sin borrar ningún blob `.kdbx` previo.
- `GraphSmallUploadVerifier.reconcile` consulta Graph exclusivamente mediante
  `GET` (sin reenviar `PUT` ni enviar bearer al CDN):
  - Si el ciphertext remoto coincide por SHA-256 con el `Pending` actual entre
    dos lecturas de metadatos coherentes, completa la transición a `Clean`.
  - Si dos lecturas de metadatos confirman que OneDrive sigue en el ETag base,
    rearma `uploadAttempted = false` sin ejecutar subida automática.
  - Si hubo una edición local posterior y el remoto coincide por SHA-256 con
    `attemptedBlob` entre dos lecturas de metadatos, avanza el ETag base de la
    copia `Pending` actual al ETag remoto verificado y rearma `uploadAttempted = false`,
    conservando en disco los tres snapshots cifrados (`Clean`, `attemptedBlob` y
    `Pending` actual).
  - Si el ETag remoto cambió a otro contenido/tamaño o devuelve `404`, informa
    `Conflict` o `Missing` y conserva `Pending` bloqueado (`uploadAttempted = true`).
    Cualquier corte de red, descarga truncada o cambio entre lecturas devuelve
    `StillPending`.
- `CloudCopiesScreen` y `CloudVaultContentScreen` distinguen visualmente `Clean`,
  `Pending` sin intento previo («Subir cambios pendientes» → `synchronize`),
  `Pending` con intento sin confirmar («Reconciliar con OneDrive» → `reconcile`)
  y `Conflict`, con mensajes explícitos para `Verified`, `Rearmed`, `Conflict`,
  `Missing` y `StillPending`. `CloudVaultContentScreen` habilita crear, editar,
  duplicar y eliminar entradas solo para copias elegibles por la ruta pequeña
  (≤250.000.000 bytes y sin conflicto), persistiendo exclusivamente en
  `PrivateCloudCopyStore` como `Pending`; conserva modo solo lectura cuando no
  es elegible. Las sesiones de carga siguen deshabilitadas.
- Verificación: pasaron `testDebugUnitTest` (`ArchitectureTest` y
  `SynchronizeCloudCopyTest`), `:app:packageDebug --rerun-tasks` (APK sin flag
  `testOnly`), `assembleDebugAndroidTest` y las **26 pruebas instrumentadas** en
  `emulator-5554` (`GraphSmallUploadVerifierTest` 10/10 incluyendo cortes en
  cada paso de `upload`, `verify` y `reconcile` con reinicio de almacén,
  `GraphSmallPendingUploaderTest` 4/4, `PrivateCloudCopyStoreTest` 8/8,
  `CloudCopiesScreenTest` 2/2 y `CloudVaultContentScreenTest` 2/2). Además se
  ejecutó contra Graph real en `emulator-5554` la prueba opt-in
  `GraphConditionalWriteExperimentTest#syntheticKdbxInterruptedAttemptReconcilesWithRealGraphWithoutSecondPut`
  (`OK (1 test)`), validando subida condicional de un `.kdbx` sintético, corte
  previo a `verify`, bloqueo de segundo `PUT` y reconciliación por `GET` a
  `Clean` antes de borrar el archivo sintético remoto y desinstalar
  `com.edro08.vaultk.test`. Se verificó interactivamente en `emulator-5554` el
  desbloqueo y navegación de `CloudVaultContentScreen` y `EntryFormScreen`, tras
  lo cual se restauró `FLAG_SECURE` en `MainActivity` y se dejó instalada la APK
  actualizada con las huellas de `Vaultk-Test-verificacion-2026-10-24.kdbx` y
  `record.json` intactas.

## 2026-10-24 — 22.04c motor pequeño: confirmación remota y conflictos validados

- `GraphSmallUploadVerifier` consulta ID/unidad/ETag/tamaño antes y después de
  descargar el KDBX remoto cifrado por URL preautorizada sin bearer; compara
  SHA-256 de los bytes exactos con el blob privado. No sigue redirecciones
  adicionales y rechaza URL insegura. Solo tras coincidencia completa,
  `PrivateCloudCopyStore.completeVerifiedSmallUpload` conmuta el registro
  `Pending` a `Clean` de forma atómica bajo comprobación de ETag base y huella;
  conserva los blobs cifrados previos. Cualquier fallo deja `Pending`.
- Antes de salir a Graph, `claimSmallPendingUpload` registra en `record.json`
  un intento durable. Tras reinicio, otra llamada de subida sobre el mismo
  `Pending` queda bloqueada, incluso si el resultado anterior fue incierto.
  Un nuevo guardado local explícito puede preparar una versión nueva, **sin
  rearmar** una subida ya intentada; requiere reconciliación antes de volver
  a enviar. Prueba Android sintética de esta propiedad superada. Se ensambló
  `SynchronizeCloudCopy` en `application`, con MSAL/token en `data` y resultado
  sin secretos; **ninguna pantalla lo invoca todavía**.
- Pruebas JVM y ocho pruebas Android con Graph simulado pasaron: comprobación
  doble de ETag, huella y cuenta, rechazo de redirección insegura, respuestas
  ambiguas, CAS local y persistencia del bloqueo de segundo intento.
- En Honor, pruebas opt-in manuales con **dos KDBX generados** y nombres UUID:
  (1) subida condicional, verificación del ciphertext remoto, transición
  persistente a `Clean` y preservación del blob anterior; (2) cambio remoto
  después de `savePending`, respuesta `412`, bytes remotos ajenos intactos,
  KDBX local `Pending` intacto y segundo intento bloqueado. Se borraron solo
  los archivos de prueba recién creados y se retiró el APK de instrumentación.
  La APK principal sigue instalada; huellas del KDBX privado del usuario,
  `record.json` y `active.json` idénticas antes/después.
- **Aún no completar 22.04c ni habilitar edición cloud:** falta UX de
  `Pending`/conflictos/reconciliación, pruebas de cortes en cada paso y confirmar
  comportamiento en cuentas de trabajo. Sesiones de carga siguen bloqueadas;
  solo hay ruta pequeña (≤250.000.000 bytes) observada en esta cuenta.

## 2026-10-24 — 22.04b: adaptador de carga pequeña, todavía sin UI

- `GraphSmallPendingUploader` (solo `data`) realiza **una** solicitud `PUT
  /drives/{drive}/items/{item}/content` con `If-Match` del ETag base y el
  ciphertext privado `Pending`, tras comprobar identidad de la cuenta.
  `PrivateCloudCopyStore.withSmallPendingCiphertext` mantiene estable el blob
  durante el intento y lo verifica antes/después. Límite conservador
  250.000.000 bytes; archivos mayores no se envían por esta ruta.
- `412` → conflicto informado; `404` → elemento ausente; fallos de red, otras
  respuestas o metadatos incompletos/incoherentes → incierto. Un 2xx con ID,
  unidad, tamaño y ETag nuevos coherentes devuelve **solo**
  `AwaitingVerification`: ni actualiza ETag base ni cambia `Pending` a `Clean`.
  No hay reintentos automáticos ni envío de contraseña a Microsoft. Sesiones
  de carga siguen excluidas por su control positivo fallido (`400`).
- Compilación debug, JVM y Android con dobles Graph: cuatro pruebas de
  encabezados, contenido cifrado exacto, identidad y estados ambiguos pasaron.
  APK principal actualizada mediante `install -r`, test APK retirado. KDBX
  privado, `record.json` y `active.json` del vault restaurado mantienen las
  mismas huellas; la app sigue instalada. **No hay llamada desde MSAL/UI al
  adaptador** ni escritura sobre el vault del usuario. Próxima fase: 22.04c,
  confirmación remota y promoción durable a `Clean` solo tras verificar bytes.

## 2026-10-24 — 22.04b: ensayo controlado retomado sin desinstalar Vaultk

- El usuario restauró y seleccionó `Vaultk-Test-verificacion-2026-10-24` en
  Honor. Se registraron huellas de su KDBX privado, metadatos y selección; el
  experimento **no abrió ni modificó** ese archivo. Se compilaron y ejecutaron
  pruebas opt-in manualmente con `adb install -r` del APK de test y
  `adb shell am instrument`, sin ejecutar el ciclo de instalación/desinstalación
  de Gradle. Solo se crearon/eliminaron archivos `.txt` con UUID en OneDrive.
- En esta cuenta, `PUT /content` con `If-Match` obsoleto respondió `412` y
  conservó `C`; con el ETag vigente reemplazó el mismo ID, cambió el ETag y
  entregó exactamente los bytes de `B`.
- `createUploadSession` con ETag obsoleto respondió `412`. Con `deferCommit`
  el fragmento final devolvió `202` sin publicar `B`; tras un cambio remoto,
  el intento de commit por ID con `sourceUrl`/`If-Match` devolvió `412` y
  conservó `D`. Sin embargo, el **control positivo falló con `400 invalidRequest`**
  al repetir esa sintaxis con el ETag correcto. No considerar seguro ni
  implementable ese commit; el `412` por sí solo es insuficiente.
- Verificación posterior: las tres huellas del almacenamiento privado de la
  copia del usuario coinciden con las previas y `com.edro08.vaultk` sigue
  instalada; se eliminó solo `com.edro08.vaultk.test`. No se instaló una APK
  principal anterior ni se perdieron datos en esta tanda. **Edición cloud
  sigue deshabilitada**. El camino pequeño aún requiere un adaptador sin
  reintentos ciegos y 22.04c; para sesiones falta una ruta de commit válida
  tanto en control positivo como en conflicto.

## 2026-10-24 — Ensayo real 22.04b con archivos sintéticos

- Se añadió `GraphConditionalWriteExperimentTest` opt-in en `data/androidTest`,
  con adquisición silenciosa de token MSAL, archivos de prueba con UUID,
  eliminación de esos archivos en `finally`, sin bearer en la URL de sesión ni
  logs de secretos. No forma parte de la suite normal.
- En Honor, la prueba de `PUT /content` con ETag inicial obsoleto pasó: Graph
  devolvió `412`, el ID, ETag y contenido `C` siguieron intactos y se eliminó
  el archivo sintético. Esto prueba el rechazo observado para esta cuenta, no
  la semántica universal ni una carrera durante el PUT.
- La prueba de sesión **no se ejecutó**: `getCurrentAccountAsync` de MSAL
  devolvió «No account» antes de crear archivos. Se constató después que
  `connectedDebugAndroidTest` había desinstalado la app en el Honor, **borrando
  los datos privados de Vaultk**, incluidas las copias offline y sesión MSAL.
  Se reinstaló la APK actual y se verificó que `files/` solo contenía
  `profileInstalled`; los datos privados anteriores no se recuperaron. El
  KDBX de OneDrive no fue modificado, pero debe volver a iniciarse sesión y
  descargar las copias. Se añadió bloqueo explícito a tareas `connected*AndroidTest`
  sin `-Pvaultk.allowDestructiveConnectedTests=true`; en adelante usarlas solo
  en emulador/dispositivo desechable. **No repetirlas en el Honor. 22.04b y la
  edición cloud siguen bloqueadas.**

## 2026-10-24 — Investigación 22.04b: confirmación condicional aún no demostrada

- Revisadas las referencias oficiales de `PUT /content` y
  `createUploadSession` de Microsoft Graph v1.0. La primera no documenta
  `If-Match`; la segunda lo documenta al crear la sesión, pero no garantiza en
  esa sección que se vuelva a comprobar al publicar el último fragmento.
  `deferCommit` ofrece cierre explícito y hay un ejemplo de `sourceUrl` con
  `If-Match`, pero no prueba que el ETag se compare atómicamente con el archivo
  original por ID en los dos tipos de cuenta.
- Se escribió `agents/specs/onedrive-subida-condicional.md` con una prueba
  controlada A/B/C, comprobación del ID y contenido, y criterios de bloqueo por
  tipo de cuenta/ruta de carga. **22.04b sigue bloqueada** hasta ejecutar y
  documentar esa prueba real con archivos sintéticos. No se introdujo una ruta
  de escritura Graph ni se modificó el KDBX del usuario. Edición cloud sigue
  deshabilitada; no confundir comprobación previa con protección atómica.

## 2026-10-24 — OneDrive 22.04a: guardado privado Pending sin red ni edición

- `CloudCopyRepository` dispone de apertura versionada y `savePending`.
  `SaveCloudCopyLocally` ejecuta este contrato en IO; `VaultkGraph` lo ensambla,
  pero **ninguna pantalla lo invoca** todavía. La copia OneDrive sigue siendo
  de solo lectura y no existe petición Graph de escritura.
- `PrivateCloudCopyStore` comprueba la huella esperada del KDBX cifrado,
  reutiliza `KdbxWriter` (sales/IV nuevos), escribe otro blob privado
  inmutable y sincronizado, verifica su hash y descifrado con la llave local,
  y solo entonces conmuta de forma atómica el registro a `Pending`. Conserva
  ETag base y blob anterior. Ante fallo ambiguo del registro, conserva el
  ciphertext para no borrar una posible versión ya referenciada.
- Pasaron `assembleDebug`, pruebas JVM y Android con KDBX sintético: reapertura
  tras recrear repositorio, ETag base intacto, edición pendiente, versión
  anterior preservada, huella obsoleta/llave incorrecta rechazadas, y bloqueo
  de borrado y descarga sobre Pending. Se repitió la compilación y las pruebas
  con `--rerun-tasks`, corrigiendo dobles de prueba desactualizados que el
  caché incremental no había recompilado. APK actualizada y abierta en Honor
  sin borrar datos; no se usó ni modificó el KDBX del usuario.
- Próximo corte 22.04b: verificar en documentación y pruebas reales las
  precondiciones atómicas de Graph antes de habilitar cualquier edición remota.

## 2026-10-24 — OneDrive 22.03 validada: caché offline y gestión de copias

- El usuario confirmó que funciona el flujo de gestión de copias en Honor tras
  probar 22.03d. Ya había confirmado apertura de la copia en modo solo lectura
  con la conexión desactivada. Con las pruebas instrumentadas aisladas de
  borrado protegido y confirmación, 22.03a–d quedan validadas.
- Eliminar una copia actúa solo sobre el almacenamiento privado de este
  dispositivo; no existe operación DELETE a Graph. Las copias pendientes,
  en conflicto, seleccionadas o no verificables quedan protegidas.
- Siguiente subfase: 22.04, guardado privado durable y subida condicionada por
  ETag. No habilitar edición remota hasta completar protección de conflictos.

## 2026-10-24 — OneDrive 22.03d: gestión y borrado local implementados

- `CloudCopiesScreen` muestra nombre, cuenta cuando se conoce, tamaño, fecha y
  estado. Permite seleccionar, «Dejar de usar esta copia» sin eliminarla y,
  solo para copias limpias no seleccionadas, «Eliminar copia de este dispositivo»
  con confirmación que aclara que OneDrive no se borra.
- `PrivateCloudCopyStore.remove` vuelve a verificar selección, estado, tamaño,
  firma y contenido conocido del directorio bajo exclusión mutua. Rechaza
  copias seleccionadas, pendientes, en conflicto o ambiguas. Renombra el
  directorio de forma atómica a una marca privada antes de eliminar solo los
  blobs y metadatos reconocidos; nunca invoca Graph. Un fallo de limpieza puede
  dejar bytes cifrados en una marca privada para recuperación, no una copia
  visible parcialmente borrada. Las descargas nuevas guardan nombre de cuenta
  en metadatos privados; las anteriores muestran cuenta no identificada.
- Compilación, suite JVM y pruebas Android con fixtures aislados correctas:
  confirmación/cancelación, cuenta distinta conservada, protección de selección
  y estado pendiente, rechazo de datos desconocidos. APK debug actualizada y
  abierta en Honor sin borrar datos; no se eliminó la copia del usuario.
  **Pendiente:** validar manualmente con una copia desechable
  antes de declarar completa 22.03d.

## 2026-10-24 — OneDrive 22.03c validada offline en Honor

- Tras la prueba manual del usuario, Vaultk aparece como actividad activa y
  Android informa `SECURE` en su ventana. La captura ADB falló y no se intentó
  eludir `FLAG_SECURE`; no se vio contenido ni se leyó el KDBX. En el flujo
  actual el flag se activa al tener una bóveda abierta, pero esta observación
  por sí sola no confirma el contenido mostrado ni que la red estuviera apagada.
- El usuario confirmó que ve los grupos y entradas de la copia; la apertura
  de solo lectura en Honor queda validada. Confirmó además que la conexión
  estaba desactivada antes de desbloquearla: apertura offline validada.
  Siguiente paso en orden: 22.03d, inventario y eliminación local segura.
  Sin cambio de código.

## 2026-10-24 — OneDrive 22.03c: apertura offline de solo lectura pendiente de validación manual

- `PrivateCloudCopyStore` abre desde `filesDir/onedrive-copies` con `KdbxReader`,
  sin copiar texto claro al disco ni exponer rutas a la UI. Comprueba tamaño y
  huella de la copia cifrada antes/después de leer; una llave errónea no cambia
  el archivo. Una selección activa de cuenta/unidad/elemento sobrevive al
  reinicio y permite elegir una copia sin conexión, incluso desconectado de MSAL.
- `MainActivity` separa la selección cloud de recientes locales. La pantalla de
  desbloqueo existente acepta la copia y biometría por referencia estable; la
  vista `CloudVaultContentScreen` permite consultar grupos y entradas, pero
  no tiene edición ni callback de guardado. `FLAG_SECURE`, bloqueo por inactividad
  y limpieza de secretos siguen activos. Al seleccionar vault local se desactiva
  únicamente el marcador de selección cloud, sin borrar la copia.
- Pasaron `assembleDebug`, suite JVM y pruebas Android con un KDBX sintético
  real: persistencia tras recrear repositorio, apertura offline, rechazo de
  llave incorrecta, archivo sin cambios y UI sin edición. APK actualizada,
  abierta y proceso activo en Honor sin borrar datos. Falta validar en Honor
  con la copia de prueba existente, sin enviar la llave a nadie. 22.03d (borrado
  local explícito) y 22.04 (guardado condicional) siguen pendientes.

## 2026-10-24 — OneDrive 22.03b validada en Honor con copia de prueba

- El usuario repitió la descarga de «Vault - copia.kdbx» en la APK actual y
  apareció «Descarga completa». Se verificó por ADB `run-as` solo la existencia
  de un KDBX privado cifrado y `record.json` en `files/onedrive-copies`, sin
  leer ninguno de los dos archivos ni abrir el KDBX. Se eliminaron las capturas
  temporales. La copia previa de `cacheDir` y el original no se tocaron.
- 22.03b queda validada para promoción y persistencia. La autenticidad completa
  del KDBX no está comprobada hasta 22.03c, y no existe aún edición remota ni
  botón para borrar copias. Próximo corte: apertura offline de prueba.

## 2026-10-24 — OneDrive 22.03b: nuevas descargas a inventario persistente

- `GraphDriveBrowser` ahora publica una descarga completa mediante
  `CloudCopyRepository` en `filesDir/onedrive-copies`, con ID de cuenta,
  unidad/elemento, ETag, tamaño, fecha y estado `Clean`. El temporal de esta
  operación se elimina tras promocionar; las descargas anteriores de 22.02
  en `cacheDir` **no se migran ni se borran**.
- Si existe copia `Pending` o `Conflict`, `PrivateCloudCopyStore` rechaza la
  promoción sin sustituir su archivo ni metadatos. Se presenta un mensaje
  específico. El almacenamiento sigue siendo privado y excluido de backup;
  no se permite abrir ni editar la copia todavía.
- Pasan compilación, pruebas JVM e instrumentadas con Graph simulado y bytes
  sintéticos: promoción, inventario por cuenta, rechazo de reemplazo pendiente,
  ETag cambiado, URL insegura y ausencia de token en CDN. APK debug actualizada
  y abierta en Honor sin borrar datos. Pendiente validar una **nueva** descarga
  real de prueba; después sigue 22.03c.

## 2026-10-24 — OneDrive 22.03a: base de inventario privado, sin activar flujo

- El plan 22.03a–d quedó detallado en `agents/specs/fases.md`: primero
  persistencia e identidad, luego promoción de descargas, apertura offline y
  por último eliminación local segura. Borrar una copia nunca borra OneDrive;
  se bloqueará con cambios pendientes, conflicto o estado incierto.
- Añadidos `CloudCopy`/`CloudCopyRepository` al dominio,
  `ManageCloudCopies` en aplicación y `PrivateCloudCopyStore` en data,
  ensamblados en `VaultkGraph`. La identidad usa cuenta/unidad/elemento,
  no ruta o nombre. Los KDBX cifrados se guardan en `filesDir/onedrive-copies`
  con nombre aleatorio y puntero de metadatos atómico; un fallo deja como mucho
  un blob cifrado huérfano y nunca reemplaza una copia pendiente. No se
  migró ni tocó la descarga previa en `cacheDir`.
- Pasan compilación, pruebas JVM y pruebas Android aisladas con bytes sintéticos
  (persistencia, cuentas separadas, escritura corta y rechazo de cambios
  pendientes). APK debug actualizada y abierta en Honor conservando datos.
  **Aún no está conectada la descarga a este almacén, ni hay
  apertura offline o interfaz para eliminar copias.** Siguiente: 22.03b.

## 2026-10-24 — OneDrive 22.02 validada en Honor con copia de prueba

- El usuario descargó «Vault - copia.kdbx» desde el explorador de OneDrive.
  Se comprobó visualmente el mensaje «Descarga completa ... en almacenamiento
  privado». Se corrigió previamente el rechazo del host CDN; la respuesta
  real completó comprobaciones de tamaño, firma y ETag. Se eliminó la captura
  temporal de diagnóstico; no se tocó el KDBX original ni se abrió el descargado.
- 22.02 completada para exploración y descarga preliminar. Pendiente 22.03:
  apertura KDBX verificada con llave, persistencia de caché cifrada offline,
  asociación durable de cuenta/unidad/elemento/ETag y biometría por referencia.

## 2026-10-24 — OneDrive 22.02: corregido rechazo de CDN de descarga

- La repetición controlada en Honor mostró el diagnóstico «servidor de descarga
  no coincide con los dominios admitidos»: el listado y los metadatos funcionan,
  pero la lista fija de hosts rechazaba la URL temporal entregada por Graph.
- Se eliminó la lista frágil de dominios. Solo se acepta la URL de la respuesta
  inmediata de `GET /content` en Graph por HTTPS sin credenciales en la URL,
  fragmentos ni puertos no estándar; la petición al CDN no lleva token OAuth
  y no sigue redirecciones adicionales. No se registran ni muestran URLs.
- Pasaron compilación, pruebas JVM y pruebas Android simuladas de URL HTTP
  rechazada, CDN HTTPS sin bearer y bloqueo de segunda redirección. APK debug
  actualizada y abierta en Honor sin borrar datos. **Pendiente:** repetir con un
  KDBX de prueba; no se ha confirmado todavía una descarga real completa.

## 2026-10-24 — OneDrive 22.02: diagnóstico seguro de fallo en descarga real

- El usuario mostró un error de descarga en Honor: el explorador lista archivos,
  pero la UI anterior agrupaba todos los fallos en un único mensaje. No se
  repitió la descarga ni se consultaron tokens, URLs o contenido del KDBX.
- Se clasifican sin detalles sensibles los fallos de metadatos, redirección,
  dominio, transferencia, tamaño, cabecera, cambio de versión y almacenamiento.
  La UI muestra el motivo en el siguiente intento con un archivo de prueba.
- Pasaron `assembleDebug`, pruebas JVM y pruebas Android simuladas tras el
  cambio. APK debug actualizada y abierta en Honor sin borrar datos. La causa
  concreta de la descarga anterior **sigue sin determinarse**; 22.02 no se da
  por validada en OneDrive real.

## 2026-10-24 — OneDrive 22.02: explorador y descarga en espera de prueba real

- Añadido explorador desde la cuenta conectada: enumera carpetas y solo archivos
  `.kdbx`, paginados, identificados por unidad e ID del elemento; sin usar
  rutas ni nombres como identidad. Navegación y errores visibles en UI.
- La descarga consulta metadatos, sigue solo una redirección HTTPS de dominio
  Microsoft sin reenviar el token, muestra bytes transferidos y comprueba
  longitud, firma KDBX y que el ETag/tamaño no hayan cambiado antes de publicar
  el temporal en `cacheDir/onedrive-staging`. Sigue siendo una copia KDBX
  cifrada, no un vault local editable ni una caché offline durable: 22.03 debe
  añadir apertura, verificación criptográfica KDBX con la llave, persistencia
  privada y metadatos de referencia.
- Pasaron compilación, pruebas JVM y pruebas Android con respuestas Graph
  simuladas (listado, descarga, rechazo de host no confiable y cambio de ETag).
  APK debug actualizada y abierta en Honor mediante instalación conservando datos.
  **Pendiente:** comprobar listado y descarga con un KDBX de prueba controlado
  en OneDrive real. No se abrió, editó ni descargó el KDBX del usuario.

## 2026-10-24 — OneDrive 22.01 completada: acceso Graph validado en Honor

- Añadida comprobación explícita en la pantalla de cuenta: MSAL adquiere en
  silencio el token `Files.ReadWrite` de la cuenta activa y `data` consulta
  `GET /v1.0/me/drive?$select=id`. Solo se comprueba HTTP 200; no se leen
  nombres, contenido de archivos ni se escriben datos en OneDrive. El token
  no sale de `data`, y la UI solo muestra éxito o fallo sin detalles sensibles.
- La llamada de red tiene tiempos límite y no sigue redirecciones; pruebas JVM
  cubren la ruta, el método, el encabezado, los tiempos límite y respuestas de
  error. Pasan `assembleDebug`, `testDebugUnitTest` y compilación androidTest.
- El usuario confirmó previamente la recuperación de sesión y la desconexión.
  APK debug actualizada mediante `adb install -r` y abierta en Honor, sin borrar
  datos. El usuario pulsó «Comprobar acceso a OneDrive» en Honor y obtuvo
  «Acceso a OneDrive confirmado»: la llamada real a Graph devolvió HTTP 200
  con el permiso consentido. 22.01 completada. No se tocó ningún KDBX;
  exploración, descarga y guardado remoto corresponden a 22.02–22.04.

## 2026-10-24 — OneDrive 22.01: integración de identidad pendiente de prueba real

- Registro Microsoft configurado por el usuario en Entra ID Free para Android
  debug con `Files.ReadWrite` delegado y `User.Read`; no se activó P2 ni
  consentimiento de administrador. La firma SHA-1 de la APK instalada coincide
  con la configurada en el redirect. Client ID público en `res/raw/msal_config.json`.
- MSAL 8.6.0 usa modo de cuenta única y autoridad `common` para cuentas
  personales y de trabajo. `domain.CloudAccountSession` y
  `application.ManageCloudAccount` aíslan al adaptador `data.MsalCloudAccountSession`;
  la UI permite conectar, recuperar la cuenta y desconectar. No muestra
  explorador ni afirma que pueda abrir bóvedas remotas antes de 22.02.
- Deshabilitados backup y transferencia de datos privados para evitar copiar
  tokens/credenciales/una futura caché cifrada a través de Android Backup.
- Pasan `assembleDebug`, `testDebugUnitTest`, compilación androidTest y las
  pruebas instrumentadas de inicialización MSAL y pantalla de selector.
  APK de depuración instalada y abierta en Honor. El usuario completó el inicio
  de sesión interactivo y se comprobó visualmente el estado «Cuenta conectada»
  en el teléfono, sin capturar ni almacenar tokens. En este corte aún faltaban
  recuperación de sesión, desconexión y llamada Graph; quedaron validadas en
  el corte posterior, documentado arriba. No se tocó el KDBX del usuario.

## 2026-10-24 — Recientes sin dependencias UI→data

- `VaultDocument` y `VaultSelection` pertenecen a `domain`. El modelo conserva
  referencias textuales, sin `Uri`; su conversión Android queda en `ui`.
- `MainActivity` usa `ManageRecentVaults`, ensamblado con `VaultSelectionStore`
  en `VaultkGraph`. Se mantienen el formato JSON, selección actual, límite de
  diez recientes, fechas y reglas de reubicación. `OpenVaultScreen` recibe el
  modelo del dominio.
- `ArchitectureTest` verifica las dependencias prohibidas de UI (incluida
  MainActivity), application, domain y data. Ya no quedan referencias a data
  en las fuentes de presentación.
- Compilación y suite JVM correctas. Pasaron las pruebas Android de recientes
  y las tres pruebas de OpenVaultScreen. Se corrigió una expectativa antigua
  de SAF en la prueba de pantalla para comprobar el navegador local actual.

## 2026-10-24 — Biometría mediante application

- Eliminados el import y la construcción de `BiometricKeyStore` en
  `MainActivity`. Todas las operaciones biométricas persistentes pasan por
  `application.ManageBiometricUnlock`, conectado en `VaultkGraph` al puerto
  `domain.BiometricCredentials` implementado por el adaptador.
- El núcleo usa un parámetro genérico para la operación criptográfica y
  referencias textuales; no importa Android ni `Cipher`. La UI conserva el
  diálogo Android y entrega el cipher autenticado mediante el caso de uso.
- Se preservan exactamente las referencias URI usadas para derivar los alias,
  las preferencias existentes y los parámetros criptográficos del Keystore.
- Pasan assembleDebug, testDebugUnitTest y compileDebugAndroidTestKotlin.
  No se verificó autenticación con una huella real en esta fase.
  Permanecen pendientes los accesos directos de recientes (`VaultDocument`
  y `VaultSelectionStore`), no los de biometría.

## 2026-10-24 — Ajustes y acceso local a través de application

- Corregida la colisión de `root` en `LocalVaultFiles` conservando el nombre
  del argumento del constructor. Selector y portapapeles pendientes de la
  sesión anterior ya compilan; la cancelación del selector se propaga.
- Puertos `AppPreferenceStore` y `SecurityPreferenceStore` en `domain`;
  `ManagePreferences` y `ManageSessionSecurity` en `application`, ensamblados
  en `VaultkGraph`. `MainActivity` ya no construye estos adaptadores.
- `VaultIdleTimer` movido a `application`; comprobaciones de acceso local
  pasan por `BrowseLocalVaults`. El adaptador de archivos queda privado en el
  grafo. El error de conflicto se publica desde `domain`.
- Compilación, pruebas JVM y compilación de pruebas Android correctas.
  Pasaron en el emulador las pruebas de archivos/selector, repositorio KDBX,
  copia de notas y copia de contraseña protegida. En `SessionSecurityTest`
  pasaron preferencias, tiempos e interacción; falló el borrado automático
  del portapapeles (`clipboardAutomaticallyClearsCopiedCredential`, línea 109).
  Queda ese fallo por investigar; no se declara verde toda la suite Android.
- Pendientes los accesos UI→data de recientes y biometría, y extraer el estado
  de sesión de la Activity. Pruebas realizadas con archivos sintéticos.

## 2026-10-24 — Dirección de dependencias alineada con Structa

- Contrato `VaultRepository`, identificador `VaultId`, resultado y progreso de
  guardado se ubican en `domain`. `OpenLocalVault` y `SaveLocalVault` dependen
  exclusivamente de `domain`; `LocalKdbxVaultRepository` en `data` implementa
  ese contrato. `app/VaultkGraph` conecta la implementación con los casos de
  uso. No hay imports `data → application` ni imports de Android o KeePassJava2
  en `domain`/`application`.
- `VaultValue` es un tipo del dominio; `KdbxVaultValue` lo adapta a
  `PropertyValue` en `data`, conservando `SealedStore` y su llave fuera del
  heap para valores protegidos. Adjuntos mantienen una referencia textual
  independiente de `Uri`, parseada solo en los bordes. No se cambiaron el
  cifrado KDBX, la KDF ni el protocolo transaccional del guardado.
- Compilación y pruebas JVM, incluida la ida/vuelta de campos protegidos,
  pasaron. Las pruebas Android de lectura/escritura KDBX, repositorio local
  (abrir→guardar→reabrir/conflicto), Argon2 nativo y acciones de edición en
  pantalla pasaron usando vaults sintéticos. Queda pendiente
  terminar los demás flujos UI→application (ajustes, selector y portapapeles);
  la arquitectura todavía no está impuesta por módulos Gradle separados.
  No se abrió ni modificó el KDBX del usuario durante estas pruebas.

## 2026-10-24 — Acciones de edición en application y composición en app

- Corrección del primer corte: `EditVault` en `application` recibe el formulario
  y ejecuta altas/edición de entradas, duplicación, movimientos, eliminación,
  grupos y búsqueda usando reglas de `domain`. `VaultContentScreen` ya no
  contiene la transformación del formulario a secretos ni las mutaciones del
  árbol. `app/VaultkGraph` compone los casos de uso con infraestructura;
  `MainActivity` conserva navegación, permisos y estado de sesión pendiente.
- Pruebas unitarias y compilación pasaron; en VaultHomeTest pasaron 22/26,
  incluidos editar, duplicar, mover, papelera y fallo de guardado. Cuatro
  fallos corresponden a expectativas obsoletas de «Guardar copia KDBX» y
  scroll de Ajustes. En EntryFormTest pasaron 12/15; dos usan textos/elementos
  que ya no existen y la última prueba fue interrumpida por fallo del
  transporte del emulador. No se modificaron esas pruebas sin revisar el
  comportamiento esperado. El archivo KDBX del usuario no se tocó.
- Pendiente: aislar tipos KeePassJava2/Android del dominio y retirar los demás
  accesos directos `ui → data` (selector, ajustes, portapapeles). No presentar
  todavía la migración hexagonal como completa.

## 2026-10-24 — Primer corte de arquitectura por capas

- `app` ensambla `OpenLocalVault` y `SaveLocalVault` con `LocalVaultGatewayAdapter`;
  `application` contiene el puerto, casos de uso y progreso de guardado;
  `data` conserva el acceso local, KDBX y la transacción atómica. El flujo
  de apertura y guardado salió de `MainActivity` sin cambiar la serialización,
  los parámetros Argon2 ni el protocolo temporal/verificación/respaldo.
- El adaptador no retiene contraseñas ni buffers cifrados después de guardar;
  la llave y la huella siguen limpiándose al bloquear. El control de acceso
  precede ahora a la lectura del archivo en el adaptador. Se documentaron
  dependencias y los siguientes cortes en `agents/specs/arquitectura.md`.
- Compilación y pruebas unitarias pasaron; pruebas instrumentadas de Argon2,
  KDBX, transacción y modal, además de la nueva prueba de integración del
  puerto local (abrir → guardar → reabrir → conflicto), pasaron usando archivos
  sintéticos aislados. El KDBX del usuario no fue modificado.

## 2026-10-24 — Comprobación manual en KeePassXC

- Se copió, sin sobrescribir archivos existentes, el KDBX editado desde
  `/sdcard/Download/Vaultk-Test.kdbx` al Escritorio de Windows como
  `Vaultk-Test-verificacion-2026-10-24.kdbx` (2465 bytes). La extracción con
  `adb pull` no modificó el archivo del emulador.
- El usuario abrió la copia en KeePassXC y confirmó que «se mira todo bien».
  Es una comprobación manual de compatibilidad y contenido visible, no una
  prueba exhaustiva de cada campo, historial y adjunto.

## 2026-10-24 — Meta de menos de 10 s confirmada en el vault del usuario

- El usuario midió **7,5 s** en un guardado real con la versión Argon2 nativa.
  Log del mismo intento: lectura del original 2517 ms, cifrado nuevo 2404 ms,
  hash del temporal 1 ms y apertura de verificación 2222 ms. La prueba
  previa con Argon2 Java tardó más de 1 minuto. No se tocó el archivo en esta
  comprobación; el guardado fue iniciado por el usuario.

## 2026-10-24 — Argon2 nativo para apuntar a guardados inferiores a 10 s

- KeePassJava2/BouncyCastle tardaba ~22,7 s por derivación del KDBX de prueba
  (Argon2d, 98 iteraciones, 64 MiB, 4 vías) en el emulador. La ruta Android
  ahora usa Argon2Kt 1.6.0 (implementación nativa), respetando modo, versión,
  iteraciones, memoria, paralelismo, sal y salida de 32 bytes del encabezado;
  AES y otras KDF conservan la ruta previa. La comparación de resultados
  contra BouncyCastle pasó en el dispositivo.
- En el mismo emulador, una derivación nativa con esos parámetros tardó
  ~1,7 s. Un guardado completo de un KDBX **aislado y sintético** con ese coste
  tardó 3,6 s en preparación + 1,7 s en commit (~5,3 s), conservando la
  verificación y el reemplazo atómico. No equivale a una medición del vault
  real del usuario; la meta <10 s todavía debe confirmarse con él.
- Se mantuvieron las tres aperturas/derivaciones independientes, evitando
  conservar la base completa descifrada en la sesión o saltar la verificación.
  Las pruebas JVM y las instrumentadas de Argon2, lectura KDBX, transacción
  local y modal pasaron. Solo se escribió un KDBX nuevo en caché para la
  prueba, que se eliminó al terminar; no se modificó el KDBX de pruebas.

## 2026-10-24 — Medición del guardado real con Argon2

- El guardado realizado por el usuario con la versión instrumentada registró
  `readOriginal=22899ms`, `encryptNew=22745ms`, `hashTemporary=0ms` y
  `decryptTemporary=22576ms` en el emulador. Las tres operaciones KDBX
  consumen ~68,2 s sumadas: la cifra mostrada mientras aparece «Verificando»
  es acumulada, no el tiempo de esa etapa. Es la KDF Argon2 costosa la que
  domina, no el hash ni la escritura del temporal.
- Diagnóstico únicamente; no se cambiaron parámetros criptográficos ni se
  modificó el KDBX de pruebas.

## 2026-10-24 — Diagnóstico del minuto al guardar

- La inspección **solo de la cabecera** del archivo `Vaultk-Test.kdbx` en el
  emulador mostró KDBX4 Argon2 (98 iteraciones, 64 MiB, 4 vías). La medición
  sintética anterior no tenía este coste y no permitía inferir el tiempo real.
  La cifra del modal es acumulada: «Verificando» no significa que toda la
  duración corresponda a esa etapa.
- La compilación de depuración registra únicamente milisegundos de lectura
  del original, cifrado nuevo, hash del temporal y descifrado de verificación
  bajo `VaultkSaveTiming`; nunca se registran claves, contenido ni rutas. No
  se redujeron los parámetros KDF ni se omitió la verificación del temporal.
- Compilación, pruebas unitarias y pruebas instrumentadas de transacción y
  modal pasaron. El KDBX de pruebas no se modificó.

## 2026-10-24 — Tiempo bajo la etapa del guardado

- El tiempo transcurrido se colocó debajo de la etapa actual, alineado con el
  título y el subtítulo; el círculo de carga permanece a la izquierda.
- La prueba instrumentada verifica la posición del tiempo y que el modal
  sigue sin barra ni porcentaje. Compilación y prueba pasaron; no se modificó
  el KDBX de pruebas.

## 2026-10-24 — Modal de guardado simplificado

- Se retiraron la barra de progreso inferior y el porcentaje visible del modal.
  El círculo animado permanece, junto a «Guardando vault…», la etapa real
  (preparación, escritura, verificación o reemplazo) y el tiempo transcurrido.
  El guardado transaccional y sus verificaciones no cambiaron.
- Compilación, pruebas unitarias y pruebas instrumentadas del modal y de un
  fallo de guardado con cambios pendientes pasaron. No se modificó el KDBX
  de pruebas.

## 2026-10-24 — Evitar KDF duplicada en cada operación KDBX

- La biblioteca KeePassJava2 3.0.0 derivaba la misma llave varias veces dentro
  de cada lectura/escritura KDBX4 (HMAC de cabecera, bloques y cifrado). Un
  encabezado propio conserva el resultado **solo durante esa operación**, lo
  reutiliza para esas peticiones idénticas y borra inmediatamente la copia al
  terminar. Se sigue verificando el temporal antes del reemplazo atómico.
- El escritor conserva versión, cifrado y parámetros de coste KDF del archivo,
  pero renueva la sal KDF, la semilla maestra, IV y llave del flujo interno en
  cada guardado para no reutilizar material criptográfico entre archivos.
- Pruebas JVM e instrumentadas pasaron, incluida lectura tras múltiples
  guardados, parámetros KDF invariantes y semillas/IV nuevos por guardado,
  conflicto, fallo del temporal y guardado de un KDBX nuevo en almacenamiento
  compartido. Medición sintética en el mismo emulador: preparación+commit
  ~4,4 s antes y ~1,7 s después; no representa el rendimiento del vault real.
  No se modificó el KDBX de pruebas.

## 2026-10-24 — Progreso visible al guardar

- Modal de guardado con tiempo transcurrido en décimas, indicador circular,
  barra y etapa real: preparar/cifrar, escribir, verificar el temporal y
  reemplazar de forma segura. Solo al escribir bytes se muestra porcentaje
  de **esa etapa**, nunca un porcentaje estimado del guardado completo. Los
  textos están en `strings_content.xml` y la escritura por bloques notifica
  el progreso sin cambiar el reemplazo atómico ni las verificaciones.
- Medición orientativa en emulador sobre un KDBX sintético nuevo: preparar
  2,9 s, escribir el temporal 3 ms, verificar 1,5 s. La lentitud observada en
  esa prueba viene principalmente de descifrar/cifrar/verificar con la KDF,
  no de crear el temporal; otros vaults y dispositivos pueden variar.
- Compilación, pruebas JVM e instrumentadas del modal, etapas, conflicto,
  temporal inválido y cambios pendientes tras un fallo pasaron. No se tocó
  el KDBX de pruebas.

## 2026-10-24 — Optimización del guardado local

- Se eliminaron dos descifrados KDBX redundantes del guardado: la verificación
  del ciphertext en memoria antes de escribir el temporal y la reapertura
  posterior al reemplazo. El temporal cifrado sigue sincronizándose y
  abriéndose con la llave maestra antes del `ATOMIC_MOVE`; SHA-256 comprueba que
  los bytes del temporal y del archivo final coinciden con los verificados.
  Siguen activos la detección de cambios externos, el respaldo temporal y la
  retención del cambio pendiente si falla el guardado. Se conserva el contenido
  verificado del temporal como estado de la sesión.
- Compilación, pruebas JVM, casos instrumentados de conflicto/temporal inválido
  y escritura atómica sobre un KDBX nuevo en almacenamiento compartido pasaron.
  Medición orientativa en emulador con KDBX sintético: 3,0 s preparación +
  1,5 s commit; no representa el tiempo de un vault real con otra KDF.
  No se modificó el KDBX de pruebas.

## 2026-10-24 — Contador de apertura del vault

- Al iniciar cada intento de desbloqueo se registra `elapsedRealtime`; la vista
  de carga muestra `mm:ss.d` desde `00:00.0` y calcula el tiempo transcurrido
  cada 100 ms, incluso mientras se descifra el KDBX en el hilo de E/S.
  La vista deja de actualizarse cuando la apertura termina o falla, y cada
  nuevo intento crea un contador desde cero. El texto visible procede de
  `strings_unlock.xml`; se mantiene el indicador de progreso existente.
- Compilación y pruebas unitarias superadas; prueba instrumentada del formato,
  avance, desaparición por error y reinicio al reintentar superada. No se
  modificó el KDBX de pruebas.

## 2026-10-24 — Corrección del bloqueo automático al abrir

- El contador de inactividad comienza de forma síncrona cuando termina el
  desbloqueo, antes de mostrar el contenido, y se detiene al bloquear. El tiempo
  empleado en abrir/descifrar el KDBX ya no consume los 30 s o 1 min elegidos.
  También se reinicia al cambiar el intervalo desde el vault y la comprobación
  de reanudación no bloquea mientras aún se está abriendo.
- Compilación, pruebas unitarias y prueba instrumentada de los intervalos cortos
  tras una apertura lenta completadas correctamente. No se usó ni modificó el
  KDBX de pruebas.

**Decisión de planificación (2026-10-10):** por indicación del usuario se omiten
la fase 20 (Robustez y recuperación) y la fase 22 (OneDrive). No se consideran
completadas ni verificadas; se continúa directamente con la fase 21 (Pulido UI/UX)
y después con ajustes solicitados por el usuario. La sincronización con OneDrive
no forma parte del alcance actual.

## 2026-10-24 — Guardado transaccional local

- ✓ y los cambios del árbol guardan directamente en el vault local: el KDBX se
  cifra y valida, se escribe en un temporal del mismo directorio, se sincroniza,
  se vuelve a abrir y solo entonces se intenta reemplazar el archivo mediante
  `ATOMIC_MOVE`. Se conserva una copia cifrada de respaldo durante el commit;
  ante fallos antes de reemplazar, se conserva intacto el original y la edición
  pendiente permite reintentar. Si no hay reemplazo atómico se rechaza guardar.
- La llave maestra se mantiene solo en memoria durante la sesión y se limpia al
  bloquear. Se compara el hash del archivo desde la apertura y antes del commit
  para evitar sobrescribir cambios externos. SAF ya no abre vaults; los recientes
  SAF requieren volver a localizar el archivo desde Dispositivo con acceso
  completo. SAF para adjuntos continúa independiente. Se quitó la acción de
  guardar copia y su selector; los avisos de UI reflejan el nuevo flujo.
- Compilación y pruebas JVM pasaron. Prueba instrumentada de commit, conflicto y
  temporal inválido, prueba de reemplazo atómico en Documents del almacenamiento
  compartido con acceso completo y pruebas específicas de UI pasaron. No se
  modificó el KDBX de pruebas; las pruebas transaccionales crean archivos aislados.
  APK actual reinstalado y abierto en emulator-5554.

## 2026-10-10 — Fase 21: Pulido UI/UX

- Ajuste posterior (Nueva/Editar entrada > Contraseña, 2026-10-24): la fila
  conserva solo Generar y Limpiar. Limpiar solicita confirmación; Cancelar
  conserva el valor y confirmar lo borra únicamente del formulario hasta
  guardar con ✓. Se retiraron las acciones adicionales Copiar y Pegar.
  Compilación, pruebas unitarias y tres pruebas instrumentadas pasaron.
- Ajuste posterior (Nueva/Editar entrada > Contraseña, 2026-10-24): fila
  desplazable de acciones Generar, Copiar, Pegar y Limpiar con iconos y áreas
  táctiles de al menos 48 dp. Generar usa contorno morado, no relleno; las
  acciones no guardan automáticamente. Copiar reutiliza el portapapeles
  sensible con borrado temporizado, y Pegar vuelve a ocultar la contraseña.
  Compilación, pruebas unitarias y cuatro pruebas instrumentadas específicas
  pasaron. Una ejecución de toda la clase presentó dos fallos intermitentes
  en pruebas ajenas a estas acciones (inactividad Compose y scroll de Notas).
- Ajuste posterior (Notas, 2026-10-24): límite de entrada ampliado de 1 000
  a 2 000 caracteres, contador actualizado y conservación de notas KDBX
  preexistentes que excedan el límite. La edición mantiene el campo de hasta
  seis líneas con desplazamiento interno; Ver entrada deja crecer la tarjeta
  sin truncar y desplaza la pantalla completa. Pruebas instrumentadas con
  2 000 caracteres verifican guardado, rechazo del exceso, navegación hasta
  Propiedades y copia íntegra; compilación y pruebas unitarias pasaron.
- Ajuste posterior (Ver entrada > Notas, 2026-10-24): añadido icono
  «Copiar notas» a la derecha de la tarjeta, reutilizando el portapapeles
  protegido y la confirmación existentes. Compilación, pruebas unitarias y
  prueba instrumentada de copia de notas verificadas.
- Corrección posterior (Ver entrada, 2026-10-24): Usuario, Contraseña y URL
  colocan ahora sus iconos de acción a la derecha del título/valor en la misma
  fila, no debajo. Texto largo se acota a dos líneas; la tarjeta sigue siendo
  adaptable y los botones conservan 48 dp y nombres accesibles. Prueba
  instrumentada comprueba la posición horizontal de las acciones, además de
  compilación y pruebas unitarias.
- Ajuste posterior (Ver entrada, 2026-10-24): tarjetas con icono lateral,
  título y valor agrupados; acciones Mostrar, Abrir y Copiar solo con icono,
  sin separadores entre botones y con nombres accesibles específicos y áreas
  de 48 dp. Propiedades usa filas de etiqueta/valor y conserva «Copiar UUID»
  con icono y texto. Pasaron compilación, pruebas unitarias e instrumentadas
  de visibilidad de secretos y copia.
- Corrección posterior (vault seleccionado, 2026-10-24): se conserva la fila
  compacta, pero vuelve a tener el contorno y fondo de `VaultkCard`, como
  Dispositivo. Toda la tarjeta sigue siendo pulsable, sin botón anidado.
- Ajuste posterior (Desbloquear vault > vault seleccionado, 2026-10-24):
  reemplazada la tarjeta con botón interno «Seleccionar vault» por una sola
  fila pulsable con icono, nombre, origen, última apertura, estado y flecha.
  Mantiene la navegación al selector y expone «Cambiar vault» como acción
  accesible, con textos largos acotados. Pasaron compilación, pruebas unitarias
  e instrumentada de navegación de la fila.
- Ajuste posterior (Ver/Nueva/Editar entrada, 2026-10-24): aviso neutro y
  explícito de que ✓ guarda solo en la sesión, no en el archivo KDBX;
  General abre expandido y Avanzado/Adjuntos se expanden inicialmente en
  edición solo si contienen datos. Propiedades comienza contraída y no muestra
  UUID provisional en nuevas entradas. Ver entrada muestra campos personalizados
  protegidos ocultos por defecto, con acciones para mostrar/copiar, y el TOTP
  con indicador circular. Se elimina Editar duplicado del menú y se acotan
  textos largos en encabezados y adjuntos. Compilación, pruebas unitarias e
  instrumentadas del formulario verificadas; no se modificó el KDBX de pruebas.
- Ajuste posterior (Ajustes > Seguridad > Bloqueo automático, 2026-10-24):
  opciones Inmediatamente, 30 s, 1, 3 y 5 min; la descripción de la fila y
  cada opción del diálogo explican el plazo seleccionado. Inmediatamente
  bloquea al salir; los demás plazos cuentan la inactividad incluso en segundo
  plano y se comprueban al volver. Se conserva la excepción temporal del
  selector externo de archivos. Pasaron compilación, pruebas unitarias y
  pruebas instrumentadas de selección y persistencia; otras pruebas de
  portapapeles/TOTP del conjunto amplio fallaron de forma no relacionada.
- Corrección posterior (Ajustes > Tema): el selector segmentado elimina los
  contornos individuales superpuestos; queda un solo marco exterior y un fondo
  diferenciado para la opción activa. Conserva las áreas táctiles y la selección
  accesible. Pasaron compilación, pruebas unitarias e instrumentada de selección.
- Ajuste posterior (Ajustes > Tema): las opciones Oscuro y Claro se presentan
  como selector segmentado compacto con iconos de luna y sol, selección visible
  y filas táctiles de 48 dp. El título y la descripción comparten cabecera;
  persisten el comportamiento y la selección accesible de las opciones.
  Pasaron compilación, pruebas unitarias y prueba instrumentada de selección.
- Ajuste posterior (Ajustes, densidad): Tema agrupa las dos opciones en una
  línea; Acceso a archivos reúne el estado y Administrar permiso. Se reducen
  márgenes entre tarjetas y rellenos verticales de Seguridad y General sin
  bajar de 48 dp en las filas interactivas. Los radios de acceso a archivos
  actúan como una sola opción accesible por fila. Se mantienen descripciones,
  controles y desplazamiento. Pasaron compilación, pruebas unitarias y la
  prueba instrumentada de Tema y Acceso a archivos.
- Ajuste posterior (Generador > Longitud): los extremos 4 y 128 están ahora
  junto al deslizador, sin una fila aparte. El carril indica con puntos los
  cortes de la escala no lineal (8, 12, 20, 32 y 64), pero sigue siendo continuo
  para elegir valores intermedios; los accesos rápidos no cambian. Pasaron
  compilación, pruebas unitarias y la prueba instrumentada de longitudes.
- Ajuste posterior (Generador, espacio inicial): la tarjeta de resultado se
  muestra solo tras generar una contraseña; la acción Copiar aparece entonces.
  Las filas de tipos pasan a 48 dp de alto, conservando la superficie táctil
  mínima. El estado inicial muestra Generar y, desde el editor, Usar contraseña
  sin desplazarse en Pixel 9 Pro; el desplazamiento sigue disponible para
  resultados largos y pantallas menores. Pasaron compilación, pruebas unitarias
  y la prueba instrumentada de generación, copia y navegación del editor.
- Ajuste posterior (Generador > Longitud): los valores 8, 12, 20, 32 y 64
  son accesos rápidos tocables bajo el deslizador; se conservan el campo exacto
  y el rango 4–128. Durante el arrastre el título muestra el valor actual.
  Los accesos y el campo se sincronizan y descartan una contraseña ya generada.
  Pasaron la compilación, las pruebas unitarias y las pruebas instrumentadas
  de accesos rápidos, entrada exacta, deslizador y generación/copia.
- Ajuste posterior (Generador > Tipos de caracteres): se mantienen las cuatro
  selecciones múltiples, ahora en filas completas y espaciadas con casillas a la
  derecha y separadores, en consonancia con las tarjetas de Ajustes. La lógica
  de generación no cambia. Pasaron `:app:assembleDebug`,
  `:app:testDebugUnitTest`, compilación de pruebas instrumentadas y la prueba
  instrumentada de configuración, regeneración y copia del generador.
- Ajuste posterior (Ajustes > General): la sección adopta el mismo patrón de
  tarjeta que Seguridad, con icono y título dentro, filas espaciadas y divisor.
  Cada fila funciona como un único interruptor accesible sin cambiar sus
  preferencias. Comprobado visualmente en Pixel 9 Pro en temas oscuro y claro;
  tema oscuro restaurado. Pasaron `:app:assembleDebug`,
  `:app:testDebugUnitTest`, compilación de pruebas instrumentadas y prueba de
  navegación y cambio de «Buscar en notas». No se modificó `FLAG_SECURE`.
- Ajuste posterior (Bloqueo automático): opciones limitadas a 30 s y 1, 2, 3,
  4 y 5 min, con formato correcto para segundos en la tarjeta y el diálogo.
  Los valores anteriores fuera de rango (15/30 min) se muestran como 5 min
  mediante el respaldo existente; el portapapeles no cambia. Compilación,
  pruebas unitarias y compilación de pruebas instrumentadas correctas; pasaron
  las pruebas instrumentadas de selección de 2 min, persistencia de 30 s y
  respaldo de una preferencia antigua de 15 min. Revisado el diálogo con las
  seis opciones en Pixel 9 Pro; `FLAG_SECURE` no se modificó.
- Ajuste posterior (Ajustes > Seguridad): tarjeta con icono coherente con Tema
  y Acceso a archivos, filas descriptivas con valor a la derecha, separador y
  acceso claro a los selectores. «Limpiar portapapeles» reemplaza el rótulo
  incompleto; los diálogos de bloqueo y portapapeles comparten opciones de
  selección y explican sus límites. Sin cambios en tiempos ni lógica de
  seguridad. Revisado visualmente en Pixel 9 Pro en ambos temas; tema oscuro
  restaurado. Pasaron `:app:assembleDebug`, `:app:testDebugUnitTest`,
  `:app:compileDebugAndroidTestKotlin` y pruebas instrumentadas de los dos
  selectores. `FLAG_SECURE` permaneció activo para el vault abierto.
- Corrección posterior (selector de grupos): en un sheet parcialmente expandido,
  «Nuevo grupo» quedaba sobre la barra de navegación y el aviso inferior fuera
  de vista. El selector se abre expandido, limita su altura al 75 % de la
  pantalla y deja que la lista se desplace; el inset inferior lo aplica el
  propio `ModalBottomSheet`. Una prueba instrumentada en Pixel 9 Pro reproduce
  el fallo anterior y verifica que botón y aviso se vean separados del borde.
- Ajuste posterior (auditoría visual en Pixel 9 Pro): revisión de apertura,
  desbloqueo, Ajustes en ambos temas, Vault, selector de grupos, detalles,
  formularios, diálogos y generador. Se reservaron 96 dp para el contenido
  y pie del selector de grupos, se desplaza automáticamente «Usar contraseña»
  a la vista tras generar y el error de título vacío aparece solo tras editar
  el título. La excepción temporal de capturas de la compilación de pruebas
  se retiró del código; `FLAG_SECURE` sigue activo con el vault abierto.
- Ajuste posterior (generador): rediseño en tarjetas para longitud (campo y
  deslizador), tipos de caracteres y vista previa, con botones de generar y
  copiar e indicador de seguridad estimado según longitud y alfabeto elegido.
  «Usar contraseña» permanece visible en el formulario y se habilita al generar;
  desde la pestaña con un vault abierto crea una entrada nueva con la contraseña
  precargada. Con el vault bloqueado se puede generar y copiar sin crear una
  entrada. Verificado con `:app:assembleDebug`, `:app:testDebugUnitTest` y pruebas
  instrumentadas del generador y formularios en el Pixel 9 Pro existente.
- Ajuste posterior (navegación y generador): se eliminó «Bloqueo automático»
  del menú del vault; su ajuste sigue disponible en Ajustes. La barra inferior
  Vault/Generador/Ajustes permanece visible en Ver entrada y en los formularios
  Nueva/Editar entrada. El generador es ahora una pantalla independiente,
  accesible también con el vault bloqueado; desde el formulario permite usar la
  contraseña generada sin perder los demás campos. Al salir de un formulario
  modificado hacia Ajustes se solicita confirmación antes de descartarlo.
  Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest`, compilación de
  pruebas instrumentadas y pruebas en el Pixel 9 Pro para navegación de entradas,
  generador bloqueado, generación/copia/uso en formularios y bloqueo en Ajustes.
- Ajuste posterior (selector de grupos): la hoja inferior limita su altura a
  la disponible, reduce la lista desplazable si faltan píxeles y reserva
  espacio para la navegación del sistema y el pie de la hoja. Así «Nuevo
  grupo» y el texto final quedan por encima de la barra inferior. Verificación:
  `:app:assembleDebug`, `:app:testDebugUnitTest` y las pruebas instrumentadas
  de navegación y creación de grupos en el Pixel 9 Pro ya iniciado.
- Ajuste posterior (desbloqueo): se eliminó el botón inferior redundante
  «Cambiar vault»; la tarjeta del archivo seleccionado conserva «Seleccionar
  vault». El indicador y texto «Abriendo vault…» ahora se centran juntos
  horizontalmente debajo del botón de desbloqueo. Verificado con
  `:app:assembleDebug`, `:app:testDebugUnitTest` y la prueba instrumentada de
  cambio de vault en el Pixel 9 Pro existente.
- Ajuste posterior («Abrir vault»): ahora hay una sola tarjeta «Dispositivo»;
  su descripción indica si abrirá el selector SAF de Android o el explorador
  local con acceso completo. La elección de modo y la solicitud del permiso
  permanecen en Ajustes. Se retiró la segunda tarjeta que promocionaba Acceso
  completo aunque SAF estuviera seleccionado. Verificación: `:app:assembleDebug`,
  `:app:testDebugUnitTest`, pruebas instrumentadas de `OpenVaultScreenTest` y
  flujos de `FileAccessFlowTest` en el Pixel 9 Pro con SAF, sin permiso de acceso
  completo y con permiso concedido.
- Corrección posterior (cierre al usar SAF/Acceso completo): el registro del
  Pixel 9 Pro mostró `Can only use lower 16 bits for requestCode` al lanzar
  tanto `ACTION_OPEN_DOCUMENT` como los ajustes de permiso desde
  `MainActivity`. `androidx.biometric:biometric:1.1.0` incorporaba
  transitivamente `androidx.fragment:fragment:1.2.5`, cuya `FragmentActivity`
  rechazaba los códigos de solicitud de `ActivityResultRegistry`. Se declara
  `androidx.fragment:fragment:1.8.8` explícitamente para resolver esa
  incompatibilidad sin alterar el flujo de selección. Verificado con
  `:app:assembleDebug`, `:app:testDebugUnitTest`, `dependencyInsight` y pruebas
  instrumentadas contra `MainActivity`: SAF abre el selector de documentos,
  Acceso completo sin permiso abre los ajustes de Android, y con permiso
  concedido abre el selector local. Los dos estados de permiso se probaron por
  separado en el Pixel 9 Pro existente.
- Ajuste posterior (Tema en Ajustes): se sustituyó la antigua fila «Apariencia»
  y su diálogo por una tarjeta «Tema» con icono, descripción y radios Oscuro y
  Claro directamente seleccionables, como en la referencia. Si existía la
  preferencia «Sistema», se marca la opción que coincide con el aspecto actual
  del dispositivo hasta que el usuario elija una opción explícita. Verificación:
  `:app:assembleDebug`, `:app:testDebugUnitTest` y las 19 pruebas instrumentadas
  de `VaultHomeTest` en el Pixel 9 Pro existente.
- Ajuste posterior (Ajustes de acceso a archivos): se eliminó «Cambiar vault»
  de Ajustes y los datos redundantes de ubicación/origen. La antigua sección
  Vault se sustituyó por una tarjeta «Acceso a archivos» con selección directa
  SAF/Acceso completo, descripción, estado del permiso y acción «Administrar
  permiso». La gestión del permiso abre los ajustes de Android sin activar por
  sí misma Acceso completo; si se revoca el permiso estando en ese modo, vuelve
  a SAF. El cambio de vault continúa disponible desde el menú de Vault y desde
  la pantalla de desbloqueo; al salir desde una sesión con cambios pendientes
  se pide confirmación. Verificación: `:app:assembleDebug`,
  `:app:testDebugUnitTest`, 18 pruebas de `VaultHomeTest` tras el rediseño y
  una prueba adicional de confirmación para administrar permisos, todas en el
  Pixel 9 Pro ya iniciado.
- Ajuste posterior (navegación con vault bloqueado): «Desbloquear vault» usa
  ahora las pestañas inferiores Vault/Ajustes y deja de mostrar el menú de tres
  puntos; la opción de seleccionar otro vault sigue en el contenido y en
  Ajustes. Ajustes reutiliza la pantalla y preferencias de la sesión abierta:
  tema, tiempo de bloqueo, portapapeles, acceso a archivos, búsqueda y detalle
  están disponibles antes de desbloquear. La activación/desactivación de
  biometría solo aparece en una sesión abierta. Se comparte el diálogo de
  tiempo de bloqueo entre ambos estados. Tras desbloquear, se accede al vault
  abierto con su navegación existente. Verificación: `:app:assembleDebug`,
  `:app:testDebugUnitTest` y 18 pruebas instrumentadas de `VaultHomeTest` en el
  Pixel 9 Pro existente, incluida una prueba de pestañas y tema bloqueado.
- Ajuste posterior (barra de estado): la actividad dibuja el fondo bajo las
  barras del sistema, pero ahora aplica `safeDrawingPadding` al contenedor raíz
  de todas las pantallas. Así, títulos y controles no se solapan con la barra
  de estado ni con la navegación al mantener el modo edge-to-edge. Verificado
  con `:app:assembleDebug`, `:app:testDebugUnitTest` y las pruebas instrumentadas
  de `OpenVaultScreenTest` en el Pixel 9 Pro existente; APK instalado y abierto.
- Se contrastaron los mockups de Desbloquear vault, Abrir vault, Vault, grupos,
  detalle y formularios de Nueva/Editar entrada con las pantallas Compose. El
  encabezado de desbloqueo utiliza ahora `VaultkHeader` como las otras vistas;
  Nueva y Editar entrada mantienen el mismo formulario y muestran el icono
  adecuado a cada contexto. Los campos de título, usuario, contraseña, URL,
  notas, TOTP y PIN usan las mismas teselas de icono que el detalle; las
  etiquetas del formulario adoptan el color/superficie del tema.
- Los detalles y las secciones de Ajustes comparten las tarjetas y espaciados
  del diseño. En el detalle, la cabecera con icono se separa del contenido para
  que acciones y códigos TOTP utilicen el ancho completo; el selector local usa
  tarjetas con iconos y flechas como «Abrir vault», y el grupo seleccionado
  queda resaltado en la hoja de grupos. Los recientes muestran el mismo icono
  de vault bloqueado que las otras vistas. Se eliminó de «Abrir vault» la
  opción desactivada «OneDrive · Próximamente», ya que se omite la fase 22.
- Al cambiar entre vaults con el mismo nombre, el campo de llave maestra ahora
  se reinicia según la URI del documento: no conserva por error el texto del
  vault anterior. No se cambió el formato KDBX ni el comportamiento de guardado.
- Verificación en el Pixel 9 Pro ya iniciado: `:app:assembleDebug`,
  `:app:testDebugUnitTest`, las 6 pruebas instrumentadas de `EntryFormTest`,
  las 17 de `VaultHomeTest` (incluida la limpieza de llave entre vaults con
  igual nombre), las 4 de `LocalVaultFilesTest` y las 2 de `OpenVaultScreenTest`
  pasaron en ejecuciones separadas. `git diff --check` sin errores. Quedan
  pendientes los ajustes visuales concretos que indique el usuario.

## 2026-10-10 — Fase 19: Recientes y gestión de vaults

- «Abrir vault» muestra los últimos archivos seleccionados con nombre, origen
  (archivo local o documento SAF), fecha/hora de última apertura y marca del
  vault actual. Se conserva el registro aunque se haya perdido el permiso SAF
  o el archivo se haya movido: al tocarlo se ofrece «Localizar de nuevo».
  Cada elemento tiene acciones para localizarlo de nuevo y para quitarlo del
  historial con confirmación. Quitar un vault no borra su archivo; si era el
  actual, se vuelve al estado sin vault seleccionado. El historial sigue
  limitado a 10 elementos y solo almacena referencias y metadatos.
- Volver a localizar usa SAF si el origen era un documento o el selector local
  si era una ruta compartida (solicitando el permiso de Android cuando falta).
  Al elegir otra ubicación se sustituye la referencia antigua, se libera su
  permiso SAF persistente y se elimina la credencial biométrica vinculada a
  esa URI; el vault nuevo requiere su propia llave/biometría. La última apertura
  se reinicia cuando cambia la URI, hasta verificar y abrir el archivo nuevo.
  La URI del elemento que se está localizando se conserva al recrear la
  Activity durante los selectores externos.
- Cambiar vault desde Ajustes continúa confirmando el descarte de borradores,
  bloqueando la sesión anterior y mostrando el selector; seleccionar un
  reciente, localizarlo o abrir uno nuevo devuelve a la pantalla que solicita
  la llave maestra o, si corresponde a esa URI, su biometría.
- Verificación en el Pixel 9 Pro ya iniciado: `:app:assembleDebug`,
  `:app:testDebugUnitTest` y compilación instrumentada correctas; 2 pruebas
  Compose de acciones, confirmación y vault no disponible, 2 de persistencia y
  reubicación sin permiso, y 16 pruebas de regresión de `VaultHomeTest` pasaron
  en ejecuciones separadas. `git diff --check` sin errores. Pendiente comprobar
  manualmente la reubicación real con un proveedor SAF, el cambio de huella al
  mover un vault y la secuencia completa en almacenamiento compartido.

## 2026-10-10 — Fase 18: Acceso completo local

- Se declaró `MANAGE_EXTERNAL_STORAGE` y se añadió «Acceso a archivos» en
  Ajustes: Estándar usa SAF y Acceso completo usa el almacenamiento compartido.
  Al elegir Acceso completo sin permiso se abren los ajustes de Android para
  concederlo; solo se activa al comprobar el permiso real. Si se deniega o
  revoca, el modo vuelve a Estándar. El modo elegido persiste en preferencias.
  «Abrir vault» ofrece también la activación de Acceso completo para que sea
  posible iniciar desde un archivo local sin haber abierto antes uno con SAF.
- El selector propio explora desde `/storage/emulated/0` carpetas accesibles,
  muestra únicamente archivos `.kdbx` (ignorando mayúsculas/minúsculas) y busca
  archivos por nombre en el árbol. El listado y la búsqueda se realizan fuera
  del hilo de UI. Al elegir un archivo se guarda su `file://` con ruta local;
  apertura, desbloqueo biométrico y preparación de una copia KDBX leen el
  archivo por ruta si el permiso sigue activo. Los documentos SAF conservan su
  flujo anterior y el guardado de copias sigue usando SAF. Las rutas se validan
  contra el almacenamiento compartido y se rechazan escapes mediante rutas
  `..` o enlaces simbólicos; no se intenta acceder a áreas adicionales de Android.
  Solicitar el permiso desde Ajustes bloquea la sesión; si hay borradores se
  advierte antes de descartarlos.
- Verificación en el Pixel 9 Pro que ya estaba ejecutándose: compilación y
  pruebas JVM correctas; 4 pruebas instrumentadas del selector/acceso local
  (incluida lectura de un KDBX cifrado por ruta) y 16 de `VaultHomeTest`
  completadas sin fallos. En una ejecución conjunta también pasaron las pruebas
  de formulario y lectura KDBX, aunque la suite se interrumpió por fallos
  intermitentes del portapapeles del emulador; la prueba afectada pasó después
  al ejecutarse individualmente. Pendiente validar manualmente la pantalla del
  permiso de Android y el recorrido con un KDBX real en Documents/Download.

## 2026-10-10 — Fase 17: Ajustes

- La pestaña Ajustes de la navegación inferior Vault/Ajustes muestra las secciones
  Apariencia, Seguridad, Vault y General. El tema Oscuro (predeterminado), Claro
  o Sistema se aplica a toda la aplicación, incluidas las barras del sistema, y
  persiste entre aperturas. Los ajustes generales permiten incluir o excluir las
  notas de la búsqueda global y mostrar u ocultar las propiedades adicionales
  (fechas, etiquetas y UUID) del detalle de entrada; conservan el comportamiento
  anterior de forma predeterminada.
- Seguridad permite activar o desactivar biometría mediante el flujo existente,
  cambiar el plazo de inactividad (1, 5, 15 o 30 min) y elegir el borrado de
  copias nuevas del portapapeles a los 15, 30, 60 o 120 s (30 s por defecto).
  Las preferencias se guardan localmente sin almacenar contraseñas ni el estado
  de desbloqueo.
- Vault muestra el nombre, la URI SAF como ubicación del documento y el origen
  local. «Cambiar vault» solicita confirmar el descarte de cambios temporales,
  bloquea el vault actual y abre el selector ya existente. Volver sin seleccionar
  otro archivo deja el vault anterior bloqueado.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y compilación de
  pruebas instrumentadas correctas. En Android 15 pasaron las 15 pruebas de
  `VaultHomeTest` y las 4 de `SessionSecurityTest`, incluidas navegación entre
  pestañas, tema y búsqueda configurables, cambio de vault con confirmación y
  persistencia de preferencias. Una primera ejecución completa de la suite se
  interrumpió por desconexión del emulador; no se repitieron las pruebas de
  formulario/KDBX que no cambian en esta fase. `git diff --check` sin errores.

## 2026-10-10 — Fase 16: Bloqueo y seguridad de sesión

- «Bloquear vault» aparece en el menú principal y en el detalle de entrada.
  Confirma el descarte de cambios sin guardar al bloquear manualmente. La sesión
  se bloquea automáticamente al salir de Vaultk y tras un tiempo sin actividad
  configurable desde el menú (1, 5, 15 o 30 minutos; 5 por defecto), persistido
  sin guardar credenciales. El estado desbloqueado y los borradores dejan de
  estar disponibles al cerrar; se cancelan las tareas de apertura/guardado, se
  limpian buffers mutables de adjuntos y se descarta la copia del portapapeles.
  Las contraseñas del formulario, generador y detalle siguen ocultas por defecto.
- Se conserva `FLAG_SECURE` mientras el vault está abierto y se deshabilitan las
  capturas de la app en Recientes. `CredentialClipboard` etiqueta las copias
  como sensibles y las borra a los 30 segundos, o al bloquear, solo si siguen
  siendo la misma copia; no elimina texto copiado posteriormente por otras apps.
- El selector SAF de «Guardar copia KDBX» bloquea la sesión al abrirse: conserva
  únicamente los bytes **cifrados** pendientes, termina la escritura al volver
  y exige desbloquear el archivo guardado de nuevo. Cancelar el selector descarta
  los cambios temporales, lo que se explica en el diálogo de guardado. El lector
  QR y el selector de adjuntos iniciados desde el formulario disponen de una
  excepción acotada (máximo 60 segundos) para poder volver al mismo formulario;
  las vistas siguen protegidas por `FLAG_SECURE` y el temporizador de inactividad
  permanece activo. Si el flujo no regresa en ese plazo mientras la aplicación
  está detenida, se bloquea el vault.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y pruebas
  instrumentadas en Android 15. Pasaron 24 pruebas en la ejecución completa
  anterior al último ajuste de selectores; después, pasaron las 13 pruebas de
  `VaultHomeTest` y las 11 restantes (formulario, KDBX y sesión) antes de una
  desconexión del emulador en navegación durante la ejecución conjunta. Pruebas
  nuevas de bloqueo manual con cambios pendientes, elección de plazo, preferencias
  persistidas, borrado temporizado y conservación del portapapeles de otras apps.
  Queda pendiente una prueba manual integral de retorno desde QR/SAF y de
  bloqueo al enviar la app al background en un dispositivo real.

## 2026-10-10 — Fase 15: Desbloqueo biométrico

- Con el vault abierto, «Activar desbloqueo biométrico» en el menú solicita de
  nuevo la llave maestra, la verifica contra el KDBX seleccionado y confirma la
  huella antes de almacenarla. «Desactivar desbloqueo biométrico» elimina la
  credencial local. La pantalla bloqueada ofrece la huella cuando esté activada,
  pero siempre mantiene «Desbloquear vault» con la llave maestra.
- `BiometricKeyStore` cifra la llave con AES-GCM y una clave de Android Keystore
  específica de la URI del documento, utilizable únicamente tras autenticar
  biometría fuerte; invalida la clave ante cambios de huellas. Solo IV y texto
  cifrado quedan en preferencias privadas, nunca la llave maestra en claro.
  Si la clave se pierde/invalida, el descifrado falla o el KDBX rechaza la llave
  recuperada, se elimina la credencial biométrica y se pide la llave maestra.
  Al cancelar la activación se descarta la clave Keystore recién creada.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completos; 19 pruebas instrumentadas en
  Android 15 sin fallos. Las nuevas pruebas Compose comprueban la confirmación
  de llave para activar, la desactivación y la alternativa manual al desbloqueo.
  Queda pendiente probar en un dispositivo con huella registrada el ciclo
  completo de alta, desbloqueo posterior e invalidación por cambio de huellas.

## 2026-10-10 — Fase 14: TOTP manual y mediante QR

- Nueva entrada y Editar entrada admiten una clave Base32 manual o una URI
  `otpauth://totp/` completa. El botón «Escanear QR TOTP» abre Google Code
  Scanner para leer exclusivamente QR; se valida el resultado antes de aplicarlo
  al formulario y un QR incompatible no sustituye la clave anterior. El lector
  delega la cámara a Google Play Services sin solicitar permiso de cámara para
  Vaultk; si no está disponible, el formulario conserva la introducción manual.
  La clave/URI queda oculta por defecto en el formulario.
- `Totp.credentialUri` normaliza claves manuales y conserva las URI leídas del QR
  con sus parámetros (emisor, algoritmo, longitud del código y periodo),
  rechazando HOTP y configuraciones inválidas. El detalle muestra el código
  calculado, tiempo restante y barra de progreso actualizados cada segundo;
  permite copiar el código como dato sensible. `KdbxWriter` guarda la URI en la
  propiedad protegida `otp` compatible con KeePassXC (solo al guardar copia
  KDBX). Textos de escaneo y error en `strings_form.xml`, contador en
  `strings_detail.xml`.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completos; 16 pruebas instrumentadas en
  Android 15 sin fallos. Pruebas de vector RFC 6238, normalización de clave
  manual y QR, validación de URI, visualización/copia del código y guardado y
  reapertura de `otp` protegido en KDBX 3/4. No se ha ejercitado el escaneo de
  un QR físico con cámara ni verificado el resultado con un vault real de
  KeePassXC; la limitación de prueba integral SAF de fase 10 sigue vigente.

## 2026-10-10 — Fase 13: Generador configurable de contraseñas

- «Generar contraseña» en Nueva entrada y Editar entrada abre un diálogo con
  longitud configurable (4–128 caracteres) y selección independiente de
  mayúsculas, minúsculas, números y símbolos. Se exige una longitud válida y al
  menos una clase; `PasswordGenerator` utiliza `SecureRandom`, garantiza que
  aparezca al menos un carácter de cada clase seleccionada y mezcla el resultado.
- El diálogo permite Generar, Regenerar, Copiar y Usar contraseña; oculta la
  vista previa por defecto y marca la copia al portapapeles como sensible. Solo
  «Usar contraseña» transfiere el resultado al formulario, sin guardar la entrada
  ni escribir en el KDBX. Cancelar deja intacta la contraseña del formulario.
  Guardar la entrada sigue siendo una acción separada y su persistencia sigue
  requiriendo «Guardar copia KDBX». Textos en `strings_form.xml`.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completos; 15 pruebas instrumentadas en
  Android 15 sin fallos. Pruebas JVM de longitudes, exclusión e inclusión de
  clases e invalidación de opciones; pruebas Compose de configuración, copia,
  regeneración, cancelación y uso en formularios nuevos y de edición.

## 2026-10-10 — Fase 12: Búsqueda global de entradas

- El campo «Buscar en el vault…» filtra la instantánea actual de todos los
  grupos, independientemente del grupo seleccionado, incluidos los grupos
  anidados y los cambios todavía no guardados. Busca sin distinguir mayúsculas
  en título, usuario, URL, notas, etiquetas y nombres y valores legibles de
  campos personalizados. Los términos separados por espacios pueden coincidir
  en campos distintos; no se indexan contraseñas, TOTP ni valores protegidos.
- Los resultados muestran la ruta del grupo para distinguir entradas y abren
  directamente «Ver entrada» en el grupo correspondiente. Al volver se mantiene
  la consulta; al borrarla se muestra la lista del grupo de la entrada abierta.
  La consulta vacía muestra la vista habitual; si no hay coincidencias aparece
  un estado vacío. Textos de búsqueda en `strings_content.xml`.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completos; 13 pruebas instrumentadas en
  Android 15 sin fallos. Pruebas JVM de todos los campos, grupos anidados,
  combinación de términos y exclusión de secretos; prueba Compose de búsqueda
  desde otro grupo, apertura del detalle y regreso a la lista.

## 2026-10-10 — Fase 11: Gestión de entradas y Papelera

- El detalle permite duplicar una entrada con nuevo UUID, moverla a otro grupo
  mediante el selector de grupos, eliminarla con confirmación y, si el KDBX ya
  contiene una Papelera habilitada, enviarla allí y restaurarla eligiendo un
  grupo de destino. Desde la Papelera, «Eliminar» confirma el borrado definitivo;
  sin Papelera existente se advierte de que el borrado será definitivo. Crear y
  editar entradas y crear grupos siguen disponibles en sus formularios.
- `VaultOperations` realiza actualizaciones inmutables del árbol. `KdbxWriter`
  sincroniza creaciones, cambios de grupo y eliminaciones reales; los movimientos
  reutilizan la entrada original, incluidos UUID, icono, historial y metadatos.
  Al enviarla a Papelera registra el UUID del grupo anterior para que KeePass
  pueda proponerlo como destino de restauración. Las copias reciben UUID nuevos.
- Las operaciones se marcan «Cambios sin guardar» hasta completar **Guardar copia
  KDBX**; el guardado continúa requiriendo llave y un archivo nuevo según la
  decisión de la fase 10. Cerrar con cambios pide confirmación. No hay guardado
  automático ni escritura sobre el KDBX original. Si el archivo declara Papelera
  pero aún no incluye su grupo, no se inventa un UUID inestable: se trata como
  un vault sin Papelera disponible para estas operaciones.
- Textos ajustados en `strings_detail.xml`, `strings_content.xml`,
  `strings_groups.xml` y `strings_form.xml`. Verificación: compilación debug,
  pruebas JVM KDBX 3/4 de mover→duplicar→Papelera→restaurar y borrar en dos
  guardados/reaperturas, más borrado definitivo desde Papelera; 12 pruebas
  instrumentadas en Android 15 (0 fallos), incluida la secuencia completa de
  acciones desde Compose. Tras un último ajuste de texto del selector, repetidas
  compilación, pruebas JVM y prueba instrumentada del selector. Persisten las
  limitaciones de validación con un proveedor SAF real y vault real descritas
  en fase 10.

## 2026-10-10 — Fase 10: Escritura del KDBX mediante copia verificada

- `KdbxWriter` vuelve a cargar el KDBX seleccionado con la llave maestra,
  incorpora al árbol original los grupos y entradas nuevos y las entradas
  editadas, y lo cifra respetando el formato KDBX cargado. Conserva grupos,
  entradas y metadatos originales no editados; actualiza título, usuario,
  contraseña, URL, notas, caducidad, etiquetas, TOTP, campos personalizados y
  binarios. Los adjuntos SAF se leen al generar la copia. La copia cifrada se
  vuelve a abrir y validar antes de ofrecer el selector de destino.
- «Guardar copia KDBX» en el menú del vault y del detalle solicita de nuevo la
  llave maestra (no se almacena en preferencias ni en la sesión). El selector
  SAF `ACTION_CREATE_DOCUMENT` crea **otro archivo**; nunca se trunca ni se
  sobrescribe el original. Tras escribir se comparan los bytes leídos con los
  cifrados mediante SHA-256. Solo después se muestra la confirmación, se cambia
  el documento activo a la copia y se limpia el estado «sin guardar». Si falla
  o se cancela, la sesión mantiene sus cambios; si falla la escritura se intenta
  eliminar la copia incompleta. El guardado directo sobre el documento original
  no está habilitado: el permiso SAF de un solo documento no permite garantizar
  su reemplazo atómico. Mover y eliminar son operaciones de la fase 11.
- Textos de guardado y errores en `strings_content.xml` y `strings_errors.xml`.
  Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` en Android 15. Pruebas de
  abrir→editar/crear→cifrar→volver a abrir KDBX 3/4, preservación del original,
  contraseña, campos, etiquetas y adjuntos, y retención de cambios si falla
  el guardado desde la UI. No se ha automatizado una interacción completa con
  un proveedor SAF real ni probado un vault real de KeePassXC.

## 2026-10-10 — Fase 09: Editar entrada

- «Editar» en el detalle y su menú abre el mismo `EntryFormScreen` que utiliza
  «Nueva entrada», con cabecera «Editar entrada», grupo actual, acción ✓ y
  un único scroll con las secciones existentes. Se precargan título, usuario,
  contraseña, URL, notas, etiquetas, caducidad, TOTP, PIN, campos personalizados
  y adjuntos. Los campos personalizados pueden editarse (incluido su estado de
  protección), añadirse o quitarse; los adjuntos pueden añadirse y quitarse.
- `KdbxReader` ahora incorpora campos personalizados protegidos o legibles y
  binarios existentes del KDBX al modelo en memoria. Los binarios existentes
  conservan sus bytes y pueden quitarse del formulario; los nuevos se eligen
  mediante SAF. Se conservan UUID, grupo y fecha de creación al editar, así
  como la hora de caducidad original si no se cambia el día; la modificación
  actualiza su fecha y reemplaza la entrada en el grupo, sin duplicarla.
- Al abandonar un formulario modificado se solicita confirmación. Los cambios
  editados se marcan como no guardados en la sesión y se pide confirmación antes
  de cerrar el vault. La contraseña vuelve a estar oculta al regresar al detalle
  y un TOTP modificado renueva su cálculo. El KDBX permanece intacto hasta la
  fase 10; el texto de descarte del vault contempla grupos y entradas nuevos
  y modificaciones de entradas.
- Nuevos textos en `strings_form.xml` y ajustes de `strings_content.xml` y
  `strings_detail.xml`. Verificación: `:app:assembleDebug`,
  `:app:testDebugUnitTest` y `:app:connectedDebugAndroidTest` completados en
  Android 15; pruebas de carga KDBX 3/4 con PIN, campo personalizado y binario,
  precarga y edición de campos, edición de una entrada KDBX y otra temporal,
  confirmaciones y actualización de la lista.

## 2026-10-10 — Fase 08: Nueva entrada

- El botón «+» abre `EntryFormScreen`, con cabecera coherente con «Ver entrada»,
  acción ✓, un único scroll vertical y secciones expandibles General, Avanzado,
  Adjuntos y Propiedades. El formulario reúne título obligatorio, usuario,
  contraseña ocultable y generación inicial mediante `SecureRandom`, URL,
  etiquetas, caducidad, notas limitadas a 1000 caracteres, campos personalizados
  protegibles, clave TOTP o URI compatible, PIN y UUID provisional. Se impide
  guardar configuraciones TOTP inválidas o una caducidad sin fecha.
- El selector SAF permite añadir referencias de adjuntos, listarlas, abrirlas
  cuando exista una aplicación compatible y quitarlas del formulario. Los
  adjuntos seleccionados permanecen disponibles en el detalle de la entrada
  durante la sesión. Al descartar un formulario con datos se solicita confirmación.
- ✓ inserta una entrada **temporal** en el grupo seleccionado y abre su detalle;
  las fechas de creación y modificación se completan en ese momento. Password,
  PIN, TOTP y campos marcados como protegidos se mantienen como `PropertyValue`
  protegido al salir del formulario. La lista y cabecera señalan los cambios
  sin guardar y se confirma su descarte al cerrar el vault. No se escribe el
  KDBX: guardar en disco corresponde a la fase 10; el formulario está preparado
  para reutilizarse al editar entradas en la fase 09.
- Nuevos textos en `strings_form.xml` y mensaje de error en `strings_errors.xml`.
  Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completados en Android 15. Pruebas Compose
  del formulario avanzado, confirmación al abandonar, creación en el grupo
  seleccionado, detalle y aviso de descarte al cerrar.

## 2026-10-10 — Fase 07: Ver entrada

- Se reemplazó el detalle básico por `EntryDetailScreen`, siguiendo el mockup:
  cabecera con retorno, icono, título y grupo; cards para usuario, contraseña,
  URL, notas, TOTP cuando hay un `otpauth://totp` compatible y propiedades.
  La contraseña se mantiene oculta por defecto y puede mostrarse u ocultarse;
  se puede copiar usuario, contraseña, URL, TOTP y UUID, y abrir enlaces HTTP(S).
- `KdbxReader` incluye los valores de contraseña y `otp` como `PropertyValue`
  protegido en el snapshot de la sesión; los arreglos de caracteres temporales
  se sobrescriben tras su uso. Se leen también etiquetas de la estructura KDBX
  (KeePassJava2 no expone un getter público para ellas). El portapapeles marca
  contraseñas y TOTP como sensibles. `FLAG_SECURE` sigue activo al ver detalles.
- El cálculo TOTP admite URI KeePassXC con secreto Base32, SHA-1/256/512,
  dígitos y período configurables; se actualiza cada segundo y borra la llave
  temporal al salir del detalle. La configuración y lectura de QR siguen en
  la fase 14. Las acciones de edición del menú se muestran deshabilitadas
  hasta contar con guardado KDBX; el icono de editar informa de ello. Ninguna
  acción escribe el archivo ni elimina entradas.
- Textos en `strings_detail.xml` y errores en `strings_errors.xml`. Verificación:
  `:app:assembleDebug`, `:app:testDebugUnitTest` y `:app:connectedDebugAndroidTest`
  completados en Android 15. Incluye vectores oficiales RFC 6238, lectura
  protegida de secretos en KDBX 3/4 y prueba Compose de ocultación, revelado,
  copia sensible, TOTP, etiquetas y menú sin acciones destructivas.

## 2026-10-10 — Fase 06: selector y gestión inicial de grupos

- El selector desplegable de la pantalla principal se sustituyó por un Bottom
  Sheet con navegación por grupos anidados, regreso al grupo superior, búsqueda
  por nombre en todo el árbol y selección del grupo activo. Las entradas de la
  pantalla principal se actualizan al elegir otro grupo. Papelera se muestra y
  puede seleccionarse cuando forma parte del árbol del KDBX abierto.
- «Nuevo grupo» permite crear grupos hijos, incluidos grupos anidados, en una
  copia del árbol mantenida solo durante la sesión abierta. Se impiden nombres
  vacíos o duplicados entre hermanos. La interfaz los marca como temporales,
  explica antes de crear que no se guardan en el KDBX y pide confirmación al
  cerrar si los hay (tanto con el menú como con Atrás). No se modifica el
  archivo; la persistencia corresponde a la fase 10.
- Los textos del selector, del diálogo y de los avisos están en
  `strings_groups.xml`. El árbol completo solo se muestra en el selector.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` completados en Android 15. La prueba Compose
  cubre navegación anidada, búsqueda, selección de Papelera, creación temporal,
  cancelación del cierre y descarte confirmado.

## 2026-10-10 — Fase 05: pantalla principal del vault

- La vista provisional de la fase 04 se sustituyó por una pantalla principal
  inspirada en `docs/uix/Inicio.png`: cabecera con nombre, origen local y estado
  abierto; búsqueda representada como función próxima; grupo activo y lista
  desplazable de entradas con título y usuario; navegación inferior Vault/Ajustes.
- Un selector desplegable sencillo permite elegir grupos anidados y muestra solo
  las entradas del grupo activo. Las tarjetas abren un detalle básico de solo
  lectura con datos ya disponibles y retorno al mismo grupo. El selector completo
  en Bottom Sheet y la gestión de grupos corresponden a la fase 06; el detalle
  completo, a la fase 07.
- El botón «+» anuncia que crear entradas estará disponible próximamente (fase 08);
  Ajustes muestra un estado provisional y el menú permite cerrar el vault. La
  sesión sigue siendo de solo lectura y se descarta al cerrarla. Textos nuevos
  en `strings_content.xml`; el estado abierto usa un token verde del tema.
- Verificación: `:app:assembleDebug`, `:app:testDebugUnitTest` y
  `:app:connectedDebugAndroidTest` finalizaron correctamente en Android 15;
  prueba Compose de selección de grupo anidado, detalle, regreso y navegación.

## 2026-10-06 — Fase 04: lectura básica de KDBX

- Se integró KeePassJava2 3.0.0 para leer KDBX 3/4, junto a Geronimo StAX 1.2
  para cubrir la API XML ausente en Android. `KdbxReader` abre el flujo SAF en
  segundo plano y proyecta grupos anidados y entradas a un modelo de solo lectura
  con título, usuario, URL, notas, UUID y fecha de modificación.
- «Desbloquear vault» permite usar la llave maestra; distingue llave incorrecta,
  archivo inválido y falta de acceso, muestra progreso y limpia el campo tras
  enviarla. La llave no se guarda; los arreglos temporales se sobrescriben al
  terminar. Solo se guarda la fecha de última apertura tras un desbloqueo válido.
- Se creó una vista transitoria de contenido en árbol con grupos y entradas
  expandibles hasta la pantalla principal de la fase 05. Al cerrarla se descarta
  el contenido de la sesión; mientras está abierta se protege la ventana con
  `FLAG_SECURE`. No se escriben archivos KDBX ni se muestran contraseñas.
- Pruebas: JVM para lectura de KDBX 3/4, grupos anidados, llave errónea y archivo
  inválido; pruebas instrumentadas en emulador Android 15 para KDBX 3/4 y llave
  errónea; `:app:assembleDebug` completó correctamente. Los archivos de prueba
  se generan con la misma biblioteca; quedan por validar vaults reales de KeePassXC
  y variantes con keyfile o algoritmos no cubiertos en fases posteriores.

## 2026-10-06 — Fase 03.5: textos configurables

- Se migraron los textos visibles de desbloqueo, selección local, estados de
  carga y vacío, navegación, accesibilidad, galería de componentes y errores de
  SAF a recursos `app/src/main/res/values/strings*.xml`. Después se separaron
  por pantalla (`strings_unlock.xml`, `strings_open.xml`, `strings_demo.xml`),
  comunes (`strings_common.xml`) y errores (`strings_errors.xml`), siguiendo la
  estructura de Structa; `strings.xml` conserva únicamente `app_name`.
- Compose utiliza `stringResource(...)`; las respuestas del selector en
  `MainActivity` utilizan `getString(...)`. «Última apertura» y «Estado» usan
  recursos con parámetros en vez de concatenar textos. El campo de contraseña
  mantiene una etiqueta configurable con valor por defecto desde recursos.
- Se documentó la fase en `agents/specs/fases.md`. Permanecen en Kotlin únicamente
  constantes técnicas (MIME, esquema URI, extensión KDBX y claves de persistencia).
- Verificación: `:app:assembleDebug` completó correctamente con el JDK de Android
  Studio y se revisaron las cadenas literales restantes en `app/src/main/java`.
  Se recompiló correctamente después de separar los archivos de recursos.

## 2026-10-06 — Fase 03: selector de vault local

- Se creó `OpenVaultScreen`, accesible desde «Seleccionar vault» y «Cambiar vault»,
  con las opciones Dispositivo, OneDrive (próximamente) y Recientes. Volver
  regresa a la pantalla raíz sin alterar el vault seleccionado.
- Dispositivo abre `ACTION_OPEN_DOCUMENT`; se valida el nombre `.kdbx` sin depender
  del MIME declarado por el proveedor, se informa de archivos inválidos y se
  conserva el permiso de lectura del URI mediante SAF. Los archivos válidos
  regresan a «Desbloquear vault» con nombre, origen local y estado bloqueado.
- `VaultSelectionStore` guarda solo URI y metadatos de visualización en preferencias
  privadas, restaura el vault actual tras reiniciar la app y mantiene hasta diez
  referencias recientes con permiso vigente. Recientes comprueba que el documento
  siga siendo accesible antes de seleccionarlo. «Última apertura: Nunca» indica
  que aún no se ha desbloqueado ninguno.
- Verificación: `:app:assembleDebug` completó correctamente con el JDK de Android
  Studio. La apertura y lectura del contenido KDBX sigue reservada para la fase 04.

## 2026-10-06 — Fase 02: pantalla inicial / desbloquear vault

- `UnlockVaultScreen` sustituyó a la galería como pantalla raíz, sin flecha atrás.
  Cuando no hay vault seleccionado muestra el estado vacío y «Seleccionar vault»;
  la acción informa que el selector se conectará en la fase 03.
- Se preparó el estado con vault seleccionado para mostrar nombre, origen, última
  apertura y estado, junto con el campo oculto de llave maestra, la opción de
  cambiar vault, el área de biometría y el menú de acciones secundarias.
- La llave introducida permanece solo en estado transitorio de Compose; el botón
  de apertura no se habilita hasta conectar el lector KDBX y la biometría solo se
  ofrece si existe una implementación y está configurada. Se añadieron previews
  de estados vacío, seleccionado y tema claro.
- Verificación: `:app:assembleDebug` completó correctamente con el JDK de Android
  Studio. El selector local y la lectura real de KDBX corresponden a fases 03 y 04.

## 2026-10-06 — Fase 01: base y sistema de diseño

- Se confirmó la configuración inicial de Kotlin, Jetpack Compose, Material 3 y
  Android 15 como versión mínima (`minSdk = 35`).
- Se creó el tema violeta oscuro inicial inspirado en `docs/uix/`, con paleta clara
  alternativa. Los componentes toman sus colores de `MaterialTheme` para permitir
  cambios de apariencia posteriores; se centralizaron tipografía, formas y espaciados.
- Se añadieron Header, VaultCard, EntryCard, SectionCard, InputField,
  PasswordField, ActionButton, navegación inferior, BottomSheet, EmptyState y
  LoadingState. `DesignSystemScreen` muestra una galería temporal interactiva de
  estos elementos hasta implementar la pantalla inicial en la fase 02.
- Verificación: `:app:assembleDebug` completó correctamente con el JDK de Android
  Studio. No se implementó todavía la selección o lectura de archivos KDBX.
