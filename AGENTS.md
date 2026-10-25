# Vaultk — guía del proyecto

Vaultk es una aplicación Android para abrir y gestionar archivos KDBX.

- [Plan por fases](agents/specs/fases.md)
- [Arquitectura](agents/specs/arquitectura.md)
- [Historial](agents/context/history.md)

Al terminar cada fase, documentar lo implementado, las decisiones relevantes y
la verificación en `agents/context/history.md`. Mantener orden cronológico
descendente: la fase más reciente siempre al principio, debajo de la cabecera.

Los nuevos textos visibles, mensajes y descripciones de accesibilidad van en
`app/src/main/res/values/strings*.xml`: `strings.xml` solo para el nombre de la
app, `strings_common.xml` para textos compartidos, `strings_<pantalla>.xml` para
cada pantalla y `strings_errors.xml` para errores. Usar `stringResource` en
Compose y `getString` fuera de Compose; los textos variables usan recursos con
formato.
