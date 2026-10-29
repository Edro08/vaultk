# Vaultk — Plan de desarrollo por fases

Vaultk será una aplicación Android para abrir, consultar y modificar archivos `.kdbx`.

La aplicación estará enfocada inicialmente en Android 15 o superior y tendrá como pilares:

- Compatibilidad con archivos KDBX.
- Acceso local.
- Acceso completo al almacenamiento compartido.
- Desbloqueo mediante llave maestra.
- Desbloqueo biométrico.
- Gestión de grupos y entradas.
- Edición y reescritura segura del vault.
- Integración con OneDrive.

---

# FASE 01 — Base del proyecto y Design System

Crear la aplicación base de Vaultk.

Configurar:

- Kotlin.
- Jetpack Compose.
- Material 3.
- Android 15 como versión mínima.
- Tema oscuro inicial.
- Colores violeta utilizados en los mockups.
- Tipografías.
- Radios de componentes.
- Espaciados.
- Iconos.
- Cards.
- Inputs.
- Bottom Navigation.

Crear componentes reutilizables para evitar que cada pantalla tenga un estilo diferente.

Ejemplos:

- Header.
- VaultCard.
- EntryCard.
- SectionCard.
- InputField.
- PasswordField.
- ActionButton.
- BottomSheet.
- EmptyState.
- LoadingState.

El objetivo de esta fase es que todas las pantallas posteriores mantengan el mismo lenguaje visual.

---

# FASE 02 — Pantalla inicial / Desbloquear Vault

Crear la primera pantalla que aparece al abrir Vaultk.

Debe seguir el mockup de `Desbloquear vault`.

No debe tener flecha atrás porque es la pantalla raíz de la aplicación.

Debe mostrar, cuando existe un vault seleccionado:

- Nombre del archivo `.kdbx`.
- Origen del archivo.
- Última apertura.
- Estado del vault.
- Opción `Seleccionar vault`.
- Opción para introducir la llave maestra.
- Área reservada para desbloqueo biométrico.
- Menú `⋮` para acciones secundarias.

Si todavía no existe ningún vault configurado, debe mostrar algo como:

    No hay ningún vault seleccionado.

    [ Seleccionar vault ]

Esta pantalla será responsable de decidir qué vault se abrirá.

---

# FASE 03 — Selector de Vault local

Crear la pantalla:

    Abrir vault

Debe mostrar inicialmente:

    Dispositivo
    OneDrive

OneDrive todavía puede aparecer visualmente como opción futura, pero no debe implementarse en esta fase.

Agregar también:

    Recientes

para mostrar vaults utilizados anteriormente.

Al seleccionar `Dispositivo`, inicialmente utilizar el selector de archivos de Android.

Solo deben aceptarse archivos:

    *.kdbx

Al seleccionar un archivo, guardar su referencia para poder mostrarlo posteriormente en la pantalla `Desbloquear vault`.

---

# FASE 03.5 — Textos configurables mediante recursos

Migrar los textos visibles de las pantallas, componentes y mensajes de error a
`app/src/main/res/values/strings*.xml`, separados por pantalla, textos comunes
y errores. Reservar `strings.xml` para el nombre de la aplicación.

Utilizar `stringResource(R.string...)` en Compose y `getString(R.string...)` en
Android fuera de Compose. Los textos con valores variables deben usar argumentos
de formato (`%1$s`) y las descripciones de accesibilidad también deben estar en
recursos. Conservar como constantes de código los identificadores internos,
extensiones y tipos MIME que no son textos de interfaz.

Para las próximas fases, añadir nuevos textos visibles a recursos desde el inicio.

---

# FASE 04 — Lectura básica de archivos KDBX

Integrar el motor encargado de leer archivos `.kdbx`.

En esta fase Vaultk debe poder:

- recibir un archivo KDBX;
- solicitar la llave maestra;
- intentar desbloquearlo;
- detectar una llave incorrecta;
- leer grupos;
- leer entradas;
- leer propiedades básicas.

Todavía no es necesario editar.

El objetivo es lograr el flujo:

    Seleccionar vault
        ↓
    Introducir llave maestra
        ↓
    Abrir KDBX
        ↓
    Mostrar contenido

Nunca guardar la llave maestra en texto plano.

---

# FASE 05 — Pantalla principal del Vault

Crear la pantalla principal mostrada en los mockups.

Header:

    Vaultk 🔒
    ● NombreVault · Local · Abierto

Después:

    Buscar en el vault...

Selector del grupo:

    📁 Servicios ▼

Lista de entradas:

    🔑 Certiport
       usuario...

    🔑 Mega
       usuario...

    🔑 Norton
       usuario...

Bottom Navigation:

    Vault
    Ajustes

Cada entrada debe abrir su pantalla de detalle.

Debe existir un botón `+` para crear una entrada.

---

# FASE 06 — Selector y gestión de grupos

Implementar el selector de grupos mediante Bottom Sheet.

Al tocar:

    📁 Servicios ▼

mostrar:

    Seleccionar grupo

    Buscar grupo

    📁 Raíz
    📁 Code
    📁 Crítico
    📁 Email
    📁 Servicios
    🗑 Papelera

    + Nuevo grupo

Debe permitir:

- cambiar de grupo;
- entrar en grupos;
- soportar grupos anidados;
- crear grupos;
- mostrar Papelera;
- buscar grupos.

La pantalla principal debe actualizar las entradas según el grupo seleccionado.

No mostrar permanentemente todo el árbol de grupos en la pantalla principal.

---

# FASE 07 — Ver Entrada

Crear la pantalla de detalle siguiendo el mockup aprobado.

Header:

    ← [icono] Mega
              Servicios

                  ✏   ⋮

Mostrar mediante cards:

## Usuario

- Valor.
- Copiar.

## Contraseña

- Oculta por defecto.
- Mostrar / Ocultar.
- Copiar.

## URL

- Abrir.
- Copiar.

## Notas

Mostrar las notas de la entrada.

## TOTP

Si existe:

- mostrar código;
- mostrar tiempo restante;
- copiar código.

## Propiedades

Mostrar:

- fecha de modificación;
- etiquetas;
- UUID;
- otros metadatos relevantes.

El menú `⋮` debe contener acciones secundarias:

    Editar
    Duplicar
    Mover
    Eliminar entrada

`Eliminar entrada` no debe mostrarse como una card permanente.

Debe solicitar confirmación antes de eliminar.

---

# FASE 08 — Nueva Entrada

Crear la pantalla `Nueva entrada`.

Debe mantener exactamente el mismo patrón visual del header utilizado en `Ver entrada`.

Header:

    ← [icono] Nueva entrada
              Grupo

                  ✓   ⋮

El botón `✓` guarda la entrada.

Toda la pantalla debe ser un único scroll vertical.

No utilizar tabs.

Dividir visualmente el contenido en secciones.

## General

Debe contener:

- Título.
- Usuario.
- Contraseña.
- Mostrar contraseña.
- Generar contraseña.
- URL.
- Etiquetas.
- Caducidad.
- Notas.

## Avanzado

Debe permitir:

- campos personalizados;
- TOTP;
- PIN;
- otros valores KDBX.

## Adjuntos

Debe permitir:

- añadir archivo;
- mostrar adjuntos;
- eliminar adjunto;
- abrir adjunto cuando sea posible.

## Propiedades

En una entrada nueva mostrar:

- UUID.
- Grupo.
- valores que ya estén disponibles.

Las fechas creadas/modificadas se completarán al guardar.

Las secciones pueden expandirse y contraerse.

---

# FASE 09 — Editar Entrada

Reutilizar la misma pantalla y componentes de `Nueva entrada`.

No crear dos formularios diferentes.

La diferencia principal será:

    Nueva entrada
    → campos inicialmente vacíos

    Editar entrada
    → campos cargados desde KDBX

Header:

    ← [icono] Editar entrada
              Servicios

                  ✓   ⋮

El botón `✓` guarda los cambios.

Debe permitir modificar:

- título;
- usuario;
- contraseña;
- URL;
- etiquetas;
- caducidad;
- notas;
- campos personalizados;
- TOTP;
- adjuntos.

La pantalla debe continuar siendo un único scroll.

---

# FASE 10 — Escritura y guardado del KDBX

Implementar la modificación real del vault.

Después de crear, editar, mover o eliminar una entrada:

- modificar el modelo en memoria;
- generar nuevamente el KDBX;
- guardar el archivo;
- confirmar que la operación terminó correctamente.

Nunca sobrescribir el archivo parcialmente.

Si ocurre un error al guardar:

- conservar los cambios en memoria cuando sea posible;
- informar al usuario;
- no mostrar falsamente el vault como guardado.

Probar especialmente:

    Abrir
    → editar
    → guardar
    → cerrar
    → volver a abrir
    → comprobar cambios

Esta fase es crítica antes de utilizar Vaultk con un vault real.

---

# FASE 11 — Crear, mover, duplicar y eliminar entradas

Completar las operaciones de gestión.

Implementar:

- crear entrada;
- editar;
- duplicar;
- mover a otro grupo;
- eliminar;
- restaurar desde Papelera si KDBX lo permite;
- crear grupos;
- mover entradas entre grupos.

Después de cada operación debe guardarse correctamente el vault.

---

# FASE 12 — Búsqueda

Implementar:

    Buscar en el vault...

Debe buscar principalmente por:

- título;
- usuario;
- URL;
- etiquetas;
- notas;
- campos personalizados cuando corresponda.

Los resultados deben poder abrir directamente `Ver entrada`.

La búsqueda debe funcionar independientemente del grupo actualmente seleccionado.

---

# FASE 13 — Generador de contraseñas

Implementar el botón de generación utilizado en:

    Nueva entrada
    Editar entrada

Permitir configurar como mínimo:

- longitud;
- mayúsculas;
- minúsculas;
- números;
- símbolos.

Debe permitir:

    Generar
    Regenerar
    Copiar
    Usar contraseña

No guardar automáticamente una contraseña generada hasta que se guarde la entrada.

---

# FASE 14 — TOTP

Completar soporte TOTP.

Permitir:

- introducir secret manualmente;
- configurar TOTP mediante QR;
- visualizar código;
- visualizar tiempo restante;
- copiar código.

La información debe guardarse de forma compatible con KDBX para poder seguir utilizando el mismo vault desde KeePassXC.

---

# FASE 15 — Desbloqueo biométrico

Implementar el desbloqueo mediante huella.

La llave maestra seguirá siendo la credencial real del KDBX.

La biometría solo permitirá recuperar de forma segura esa llave en el dispositivo.

Primera configuración:

    Introducir llave maestra
        ↓
    Abrir correctamente el vault
        ↓
    Activar "Desbloqueo biométrico"
        ↓
    Proteger llave maestra mediante Android Keystore

En futuras aperturas:

    Abrir Vaultk
        ↓
    Huella
        ↓
    Recuperar llave maestra protegida
        ↓
    Abrir KDBX

Siempre debe existir:

    Usar llave maestra

como alternativa.

Si la biometría deja de funcionar o la clave de Android Keystore queda invalidada:

    solicitar nuevamente la llave maestra.

La llave maestra nunca debe almacenarse en texto plano.

---

# FASE 16 — Bloqueo y seguridad de sesión

Agregar comportamiento de seguridad de la aplicación.

Implementar:

- bloquear vault manualmente;
- bloquear al enviar Vaultk al background;
- tiempo configurable antes de bloqueo;
- ocultar información sensible al cambiar de aplicación;
- limpiar información sensible cuando se cierre el vault;
- ocultar contraseñas por defecto;
- borrar el portapapeles después de copiar credenciales.

El menú principal debe incluir:

    Bloquear vault

---

# FASE 17 — Ajustes

Crear la pantalla:

    Ajustes

Manteniendo la Bottom Navigation:

    Vault
    Ajustes

Opciones iniciales:

## Apariencia

- Oscuro.
- Claro.
- Sistema, si se desea.

## Seguridad

- Desbloqueo biométrico.
- Tiempo de bloqueo automático.
- Borrar portapapeles después de X segundos.

## Vault

- Vault actual.
- Ubicación.
- Origen.
- Cambiar vault.

## General

- comportamiento de búsqueda;
- mostrar/ocultar información adicional;
- otras preferencias.

---

# FASE 18 — Acceso Completo Local

Agregar un segundo modo para abrir vaults locales sin depender únicamente de SAF.

Solicitar:

    android.permission.MANAGE_EXTERNAL_STORAGE

Agregar en Ajustes:

    Acceso a archivos

    ○ Estándar
      Selector de archivos de Android.

    ○ Acceso completo
      Acceso directo al almacenamiento compartido.

Cuando Acceso completo esté activo, Vaultk debe disponer de su propio selector local.

Ejemplo:

    Dispositivo

    Buscar archivos...

    📁 Documents
    📁 Download
    📁 Passwords
    📁 Vaults

    🔐 Edro.kdbx
    🔐 Personal.kdbx

Solo mostrar archivos `.kdbx` cuando el usuario esté seleccionando un vault.

Guardar la ruta del vault seleccionado.

Este modo debe funcionar con rutas como:

    /storage/emulated/0/Documents
    /storage/emulated/0/Download
    /storage/emulated/0/Vaults

No intentar convertir `MANAGE_EXTERNAL_STORAGE` en acceso root ni utilizarlo para saltarse restricciones adicionales de Android.

---

# FASE 19 — Recientes y gestión de Vaults

Completar la pantalla:

    Abrir vault

Agregar historial de vaults utilizados.

Cada elemento debe mostrar:

- nombre;
- origen;
- última apertura.

Ejemplo:

    Edro.kdbx
    Dispositivo · Abierto recientemente

Permitir:

- abrir;
- quitar de recientes;
- localizar nuevamente si el archivo fue movido;
- cambiar vault actual.

Al cambiar de vault:

- bloquear el vault actual;
- limpiar información sensible;
- cargar el nuevo vault;
- solicitar biometría o llave maestra correspondiente.

---

# FASE 20 — Robustez y recuperación

Antes de integrar OneDrive, fortalecer el funcionamiento local.

Probar:

- archivos KDBX pequeños y grandes;
- contraseña incorrecta;
- archivos dañados;
- adjuntos;
- campos personalizados;
- grupos anidados;
- TOTP;
- múltiples modificaciones;
- cierres inesperados;
- errores de escritura;
- archivo eliminado o movido;
- almacenamiento sin espacio.

No avanzar a sincronización remota hasta que abrir, modificar y reescribir localmente sea confiable.

---

# FASE 21 — Pulido UI/UX

Revisar todos los mockups implementados.

Comprobar consistencia entre:

    Desbloquear vault
    Abrir vault
    Vault
    Selector de grupo
    Ver entrada
    Nueva entrada
    Editar entrada
    Ajustes

Mantener:

- mismos headers;
- mismos iconos;
- mismo tamaño de acciones;
- mismos radios;
- mismas cards;
- mismos colores;
- mismo comportamiento de scroll.

Especialmente:

    Ver entrada
    Nueva entrada
    Editar entrada

deben sentirse como partes de la misma aplicación.

---

# FASE 22 — OneDrive (subfases 22.01–22.08)

Objetivo: seleccionar cualquier `.kdbx` del OneDrive propio, conservar una
copia KDBX cifrada en almacenamiento privado, abrirla offline y sincronizarla
sin sobrescribir cambios de otro dispositivo. Conservar el acceso local directo
y SAF únicamente para adjuntos. Primera entrega funcional: 22.01–22.04; las
subfases siguientes completan la sincronización y recuperación.

## 22.01 — Identidad Microsoft

- Seguir [el registro de Entra ID](onedrive-registro.md) para cuentas personales
  y de trabajo antes de probar autenticación real.
- Registrar Vaultk como aplicación pública Android en Entra ID; configurar
  identificador de aplicación, tipos de cuenta admitidos y redirección basada
  en paquete y firma real (depuración y publicación por separado).
- Integrar MSAL para OAuth 2.0 con PKCE, sin client secret dentro del APK.
- Para navegar por la unidad propia completa solicitar el permiso delegado
  `Files.ReadWrite` mediante consentimiento; `Files.ReadWrite.AppFolder` solo
  sirve si se restringe el selector a la carpeta de aplicación. No solicitar
  permisos de aplicación ni `Files.ReadWrite.All` por defecto.
- Conectar, recuperar cuenta/token silenciosamente y desconectar; gestionar
  caducidad y revocación sin registrar tokens.

**Terminado cuando:** se inicia sesión y solo se accede al ámbito consentido.

## 22.02 — Explorador y descarga

- Incorporar OneDrive en `Abrir vault`, listar carpetas y filtrar `.kdbx`.
- Seleccionar por identificadores estables de unidad y elemento, no por ruta
  ni nombre; descargar con progreso a temporal privado y verificar respuesta
  e integridad antes de publicar la copia en caché.
- No exponer contenido, URL de descarga o token en mensajes y registros.

**Terminado cuando:** puede seleccionarse y descargarse un KDBX.

## 22.03 — Apertura y caché offline

- Abrir la copia cifrada con el motor KDBX existente sin persistir texto claro;
  conservar ID de cuenta, unidad/elemento, ETag base y referencia privada.
- Mantener biometría por referencia estable y apertura offline tras la primera
  descarga. Separar caché de OneDrive de los vaults locales compartidos.
- Definir exclusiones de backup/transferencia para caché, credenciales y
  metadatos sensibles antes de escribirlos; no borrar cambios pendientes al
  desconectar ni al cambiar de cuenta.

**Terminado cuando:** el vault abre online y offline tras la primera descarga.

### Orden de entrega 22.03 (copias y apertura)

1. **22.03a — Inventario privado:** definir referencia estable
   `(cuenta, unidad, elemento)`, nombre solo para mostrar, ETag base, tamaño,
   fecha y estado `limpia/pendiente/conflicto`. Escritura atómica de metadatos
   y KDBX cifrado bajo `filesDir`, fuera de la caché temporal y excluida de
   backup/transferencia. No migrar ni borrar automáticamente descargas previas
   de `cacheDir`; nunca usar un nombre proporcionado por Graph como ruta.
2. **22.03b — Promoción segura:** descargar a un temporal, comprobar tamaño,
   firma y ETag, publicar la copia cifrada y el inventario de manera recuperable.
   No sustituir una copia con cambios pendientes o estado desconocido. Probar
   corte de proceso, falta de espacio y archivo parcial con datos sintéticos.
3. **22.03c — Apertura:** abrir solo la copia privada con el lector KDBX y la
   contraseña introducida localmente; verificar integridad completa al abrir,
   admitir offline y biometría por referencia estable, sin escribir al remoto.
   Bloquear guardado/edición remotos hasta 22.04; no reutilizar el guardado
   local sobre esta copia como si fuera una bóveda compartida del dispositivo.
4. **22.03d — Gestionar copias:** mostrar tamaño, fecha, cuenta y estado, con
   acción explícita «Eliminar copia de este dispositivo» y confirmación. Nunca
   enviar DELETE a Graph ni borrar la cuenta. Prohibir eliminación de copias
   pendientes, en conflicto, en uso o cuyo estado no pueda verificarse;
   conservar sus metadatos de recuperación. Pruebas de aislamiento entre
   cuentas y de fallo parcial. No ofrecer «Borrar datos» como alternativa.

**Puerta de validación:** completar cada paso con tests de adaptador/caso de
uso, sin usar el KDBX real del usuario. Validar apertura y borrado voluntario
solo con vaults sintéticos antes de considerar 22.03 completada.

## 22.04 — Guardado y subida condicionada

- Reutilizar escritura temporal, verificación de KDBX y reemplazo seguro en
  almacenamiento privado; marcar `pendiente` de manera durable antes de subir.
- Leer versión remota y usar escritura condicional con `If-Match` del ETag
  conocido (incluida la creación/confirmación de sesiones de carga según la
  operación Graph). Un HEAD previo **no** reemplaza la precondición atómica.
- Tratar `412`, eliminación remota, error de red y resultado ambiguo como
  pendiente/conflicto, nunca como éxito. Confirmar ID/ETag remoto y solo
  entonces actualizar la base de sincronización. No reintentar a ciegas una
  subida cuyo resultado sea desconocido.

**Terminado cuando:** el guardado local sobrevive a un fallo de subida y no
sobrescribe una versión remota inesperada.

### Orden de entrega 22.04 (sin edición remota prematura)

1. **22.04a — Guardado privado:** preparar KDBX cifrado con el escritor existente,
   verificarlo con la llave local y huella esperada, publicar blob inmutable y
   `Pending` de forma atómica. Conservar el ETag base y la versión anterior como
   recuperación. Sin UI editable ni subida aún. Probar cortes y llave errónea
   con un KDBX sintético; no usar la copia del usuario.
2. **22.04b — Subida condicional:** implementar `If-Match` real del ETag base en
   Graph (carga pequeña y sesión de carga según tamaño), sin HEAD como garantía.
   Comprobar la semántica de cada endpoint; si no admite precondición atómica,
   no activar escritura en ese modo. Nunca reenviar la contraseña ni reintentar
   automáticamente una operación de resultado incierto. Protocolo de pruebas y
   límites documentales en [onedrive-subida-condicional.md](onedrive-subida-condicional.md).
3. **22.04c — Confirmación y UX:** verificar ID y ETag tras la respuesta,
   actualizar `Clean` solo cuando sea inequívoco. Conservar `Pending/Conflict`
   ante 412, 404, error de red o resultado ambiguo. Entonces habilitar la
   edición remota con estados explícitos y sin ruta al guardado local compartido.

**Puerta:** no habilitar edición de OneDrive hasta demostrar con pruebas de
concurrencia que ningún cambio ajeno puede sobrescribirse silenciosamente.

## 22.05 — Sincronización bidireccional

- Consultar ETag al abrir y antes de guardar; descargar cambios ajenos solo
  cuando no hay cambios locales pendientes.
- Sincronizar al guardar, al regresar a la aplicación y bajo petición manual;
  estados sincronizado, pendiente, sincronizando, error y conflicto.

**Terminado cuando:** ambos sentidos funcionan sin descartar ediciones locales.

## 22.06 — Conflictos y recuperación

- Conservar ambos KDBX cifrados antes de resolver; mostrar versiones y permitir
  elegir explícitamente local o remota, sin fusión automática inicial.
- Añadir copias de recuperación cifradas y registro de operaciones sin secretos.

**Terminado cuando:** ningún conflicto se resuelve con sobrescritura silenciosa.

## 22.07 — Interfaz y ajustes

- Mostrar origen Local/OneDrive, estado y última sincronización; acciones
  Sincronizar ahora, Ver estado y Desvincular, con progreso de transferencias.
- Ajustar sincronización automática y datos móviles sin sustituir las pantallas
  existentes de vault, grupos y entradas.
- Mantener «Copias descargadas» accesible aunque la cuenta esté desconectada;
  distinguir borrar copia local, quitar referencia y borrar en OneDrive.

**Terminado cuando:** el flujo nube se integra en la interfaz existente.

## 22.08 — Pruebas y endurecimiento

- Probar cortes durante transferencia, ediciones concurrentes, tokens caducados,
  permisos revocados, archivos eliminados y respuestas ambiguas.
- Verificar integridad y apertura KDBX tras descarga, y ausencia de secretos
  en logs y notificaciones. No usar ni modificar el KDBX del usuario en pruebas.

**Terminado cuando:** interrupciones y conflictos no pierden datos.

Mantener los contratos remotos en `domain`, orquestación en `application`,
MSAL/Graph/archivos privados en `data` y ensamblaje en `VaultkGraph`; ninguna
pantalla invoca adaptadores directamente. La contraseña maestra no se envía a
Microsoft. Cada corte debe verificarse antes de continuar con el siguiente.

---

# Resultado final

Al completar todas las fases, Vaultk deberá soportar:

    KDBX
    ├── Local mediante SAF
    ├── Local mediante Acceso completo
    └── OneDrive

y proporcionar:

    Desbloqueo
    ├── Llave maestra
    └── Biometría

    Vault
    ├── Grupos
    ├── Entradas
    ├── Búsqueda
    ├── TOTP
    ├── Adjuntos
    └── Campos personalizados

    Entradas
    ├── Ver
    ├── Crear
    ├── Editar
    ├── Duplicar
    ├── Mover
    └── Eliminar

La prioridad debe ser siempre:

    1. No corromper el KDBX.
    2. No perder cambios.
    3. No exponer la llave maestra.
    4. Mantener compatibilidad con KeePassXC.
    5. Después agregar sincronización con OneDrive.
