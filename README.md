# 🛡️ Vaultk

Gestor de contraseñas para Android, diseñado para abrir y gestionar bóvedas KDBX con enfoque en privacidad, simplicidad y control total de los datos.

---

### 🔒 Acceso y seguridad

* **Desbloqueo rápido**: Acceso mediante biometría (huella dactilar) o llave maestra.
* **Bloqueo automático**: Cierre de sesión tras un periodo de inactividad.
* **Limpieza de portapapeles**: Borrado automático de credenciales copiadas tras unos segundos.
* **Protección de pantalla**: Bloqueo contra capturas y visualización en segundo plano (`FLAG_SECURE`).

### 📁 Gestión de credenciales

* **Organización**: Estructura de grupos con búsqueda rápida e instantánea de entradas.
* **Generador de contraseñas**: Creación de claves robustas con diferenciación de color entre letras, números y símbolos.
* **Autenticación en dos pasos**: Generación integrada de códigos 2FA / TOTP en cada entrada.
* **Campos y notas**: Soporte para campos personalizados y notas seguras.

### ☁️ Sincronización y almacenamiento

* **Sincronización con OneDrive**: Conexión directa para mantener las contraseñas al día con el ordenador.
* **Copia local sin conexión**: Acceso y guardado continuo incluso sin conexión a internet.
* **Detección de conflictos**: Alerta de cambios remotos para evitar sobrescribir datos editados en otro dispositivo.
* **Compatibilidad estándar**: Formato KDBX, compatible con KeePassXC y herramientas del ecosistema KeePass.

---

### ⚙️ Entorno

* **Sistema**: Android 15 o superior.
* **Formato**: KDBX 3.1 / KDBX 4.
* **Interfaz**: Material 3 con soporte para tema claro y tema oscuro.
