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

# FASE 22 — OneDrive

Implementar OneDrive como última fase.

Agregar inicio de sesión con cuenta Microsoft.

Desde:

    Abrir vault

permitir:

    OneDrive

Crear selector propio:

    OneDrive
    cuenta@outlook.com

    Buscar en OneDrive...

    Mi unidad > Passwords

    📁 Backups
    📁 Personal

    🔐 Edro.kdbx
    🔐 Personal.kdbx

Permitir:

- navegar carpetas;
- buscar `.kdbx`;
- seleccionar vault;
- descargarlo;
- abrirlo;
- modificarlo;
- volver a subirlo.

El vault debe guardar información suficiente para recordar su archivo remoto.

La pantalla principal podrá mostrar:

    Vaultk 🔒
    ● Edro · OneDrive · Sincronizado

Estados posibles:

    Sincronizado
    Sincronizando
    Cambios pendientes
    Sin conexión
    Conflicto
    Error

Antes de sobrescribir el archivo remoto, comprobar que no haya sido modificado por otro dispositivo.

Si existe un conflicto:

- no sobrescribir silenciosamente;
- informar al usuario;
- permitir recuperar o resolver el conflicto.

OneDrive debe reutilizar exactamente las mismas pantallas:

    Vault
    Grupos
    Ver entrada
    Nueva entrada
    Editar entrada

La única diferencia debe ser dónde se lee y guarda el archivo.

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
