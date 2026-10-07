# 010 — Borrado de cuenta y rate limit por cuenta

- **Estado**: Implementada (2026-10-08)
- **Módulos**: user (principal), club, chat, notification, match, app
- **Dependencias**: 001, 005, 007

## Contexto y objetivo
Dos requisitos de la app que bloquean su publicación:

- **BE-GAP-1 — Borrado de cuenta.** Apple (App Store 5.1.1(v)) exige poder borrar la cuenta desde la app, y Google Play exige además una **URL web** donde pedir el borrado sin la app. Hoy no existe.
- **BE-GAP-2 — Rate limit por IP.** El refresh, el login y el registro se limitan por IP (10/h). Varios jugadores en la misma red (el wifi del campo) comparten IP y se bloquean entre sí. El límite debe ser **por cuenta** (usuario, token o email), con un límite por IP mucho más alto solo como red de seguridad.

Decisiones de producto (2026-10-08): propiedad de clubes **transferida automáticamente**, historial **anonimizado**, login y registro **también** limitados por cuenta, y página web con **formulario servido por el backend**.

## Reglas de negocio

### A. Borrado de cuenta
- **RN-A1**: Un usuario borra su cuenta desde la app confirmando su **contraseña**. Desde la web, con su **email y contraseña**. Una contraseña incorrecta no borra nada (401).
- **RN-A2**: El borrado es **inmediato e irreversible**. El email y el nombre de usuario quedan libres para un registro nuevo.
- **RN-A3**: Se eliminan los datos personales: email, nombre de usuario, contraseña, foto de perfil (también del almacenamiento), tokens de sesión, tokens de verificación y de contraseña, dispositivos de push, preferencias de notificación, ausencias y **todos sus mensajes de chat**.
- **RN-A4**: Las sesiones abiertas dejan de valer al momento: los refresh tokens se borran y los access tokens emitidos antes del borrado se rechazan (401) aunque no hayan caducado.
- **RN-A5**: **Historial anonimizado**: sus fichas de club se conservan, sin datos personales, como «Usuario eliminado» (más un sufijo corto, porque los nombres son únicos), para que marcadores, estadísticas y equipos de los partidos pasados de los demás sigan cuadrando. Esas fichas salen de los clubes como si el usuario saliera: dejan de verse y le retiran (a él y a sus invitados) de las convocatorias abiertas, promocionando la lista de espera.
- **RN-A6**: **Clubes de los que es OWNER**: la propiedad pasa automáticamente al **ADMIN activo más antiguo** (por fecha de alta en el club) o, si no hay, al **miembro activo más antiguo**. Si era el único miembro activo, el **club se elimina** con sus partidos, convocatorias, estadísticas y ratings.
- **RN-A7**: Sale de todos sus chats. Un chat que se queda sin participantes desaparece. En los chats que creó, el creador pasa a ser otro participante.
- **RN-A8**: La página web de borrado es pública, explica qué se borra y qué se conserva anonimizado, y pide confirmación explícita antes de borrar.

### B. Rate limit
- **RN-B1**: Los límites se aplican por cuenta:
  | Operación | Límite por cuenta | Red de seguridad por IP |
  |---|---|---|
  | Refresh | 60/h por usuario del refresh token | 300/h; los tokens inválidos o caducados cuentan 30/h por IP |
  | Login | 10/h por email | 300/h |
  | Registro | — (cada registro es una cuenta nueva) | 50/h |
  | Borrado de cuenta | 5/h por usuario (app) o por email (web) | 50/h (web) |
- **RN-B2**: Al superar un límite se responde **429** con `{code: "RATE_LIMIT_EXCEEDED", message}` y la cabecera `Retry-After` en segundos.
- **RN-B3**: Los límites se activan con `RATE_LIMIT_ENABLED` (activos en `prod`, apagados por defecto en local y tests).

## Criterios de aceptación
- **CA-1** (RN-A1): Dado un usuario autenticado, cuando llama a `DELETE /me` con una contraseña incorrecta, entonces recibe 401 y su cuenta sigue funcionando.
- **CA-2** (RN-A2/A3/A4): Dado un usuario con dispositivo, chat con mensajes y ausencia, cuando borra su cuenta, entonces recibe 204, su login da 401, su access token anterior da 401, sus mensajes ya no aparecen en el chat y puede registrarse otra cuenta con el mismo email.
- **CA-3** (RN-A5): Dado un usuario confirmado en una convocatoria abierta llena con lista de espera, cuando borra su cuenta, entonces deja de aparecer entre los miembros del club, el primero en espera pasa a confirmado y sus estadísticas pasadas se conservan para el club.
- **CA-4** (RN-A6): Dado un OWNER de un club con un ADMIN y un PLAYER más antiguo, cuando borra su cuenta, entonces el ADMIN pasa a OWNER. Dado un OWNER único miembro de otro club, ese club se elimina junto con sus datos de partidos.
- **CA-5** (RN-A7): Dado un chat creado por el usuario con otros dos participantes, cuando borra su cuenta, entonces el chat sigue con los otros dos y uno de ellos figura como creador.
- **CA-6** (RN-A1/A8): Dado un usuario sin la app, cuando abre `/account/delete` y envía su email y contraseña, entonces la cuenta queda borrada. Con una contraseña incorrecta, nada cambia.
- **CA-7** (RN-B1): Dados 12 usuarios distintos desde la misma IP, cuando cada uno hace login y refresh, entonces ninguno recibe 429.
- **CA-8** (RN-B1/B2): Dado un mismo usuario que supera 60 refresh en una hora (o 10 logins con el mismo email), entonces recibe 429 `RATE_LIMIT_EXCEEDED` con `Retry-After`, y otro usuario desde la misma IP sigue pudiendo entrar.

## API (contrato)
| Método | Ruta | Permiso | Cuerpo | Respuesta |
|---|---|---|---|---|
| DELETE | `/api/v1/me` | auth | `{password}` | **204**; 401 `INVALID_CREDENTIALS` |
| POST | `/api/v1/auth/delete-account` | público | `{email, password}` | **204**; 401 `INVALID_CREDENTIALS` |
| GET | `/account/delete` | público | — | Página HTML con el formulario (URL para Google Play) |

Cambios de comportamiento: 429 con cuerpo `{code, message}` y `Retry-After` en todos los límites.

## Fuera de alcance
- Periodo de gracia o recuperación de la cuenta tras borrarla.
- Exportación de datos (portabilidad).
- Aviso al nuevo OWNER cuando recibe un club por borrado del anterior.

## Preguntas abiertas
- (ninguna)
