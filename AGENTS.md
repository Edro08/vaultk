# Vaultk — guía del proyecto

Vaultk es una aplicación Android para abrir y gestionar archivos KDBX.

- [Arquitectura](agents/specs/arquitectura.md)
- [Historial](agents/specs/history.md)

Al terminar cada fase, documentar lo implementado, las decisiones relevantes y
la verificación en `agents/specs/history.md`. Mantener orden cronológico
descendente: la fase más reciente siempre al principio, debajo de la cabecera.

## Convención de commits
Todos los commits deben iniciar obligatoriamente con el prefijo del nombre de la rama actual en la que se esté trabajando:
`<nombre-de-la-rama>: <descripción del cambio>`

*Ejemplo:* si la rama es `feat/initial-app`, el commit debe iniciar con `feat/initial-app: <descripción del cambio>`.
