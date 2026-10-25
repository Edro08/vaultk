# Historial de Vaultk

Fases completadas en orden cronológico descendente (la más reciente primero).

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
