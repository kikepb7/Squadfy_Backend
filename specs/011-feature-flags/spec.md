# 011 — Feature flags, página de verificación de email y dispositivos propios

- **Estado**: Hecha (2026-10-08)
- **Módulos**: common, app, user, notification
- **Dependencias**: 009 (la sustituye), 010
- **Sustituye**: el nodo `squadfy.app-config` de la spec 009

## Contexto y objetivo
- Los interruptores funcionales estaban repartidos (`squadfy.app-config.email-verification.enabled`, `squadfy.rate-limit.enabled`) y resultaban confusos. Se sustituyen por **feature flags** con un único formato, para activar o desactivar funciones según lo que haga falta en cada entorno.
- Decisión de producto (2026-10-08): **la verificación de email deja de ser obligatoria, también en producción**.
- El enlace de verificación del correo abría una página en blanco.
- `DELETE /devices/{token}` permitía borrar el dispositivo de otro usuario conociendo su token.

## Reglas de negocio

### A. Feature flags
- **RN-A1**: Cada función desactivable es un flag con nombre (`kebab-case`) y valor por defecto. Se configura en `squadfy.features.<nombre>` y con la variable de entorno `FEATURE_<NOMBRE>` (p. ej. `FEATURE_EMAIL_VERIFICATION=false`).
- **RN-A2**: Flags iniciales:
  | Flag | Defecto | Efecto |
  |---|---|---|
  | `email-verification` | **desactivado** (también en `prod`) | Exige verificar el email antes de entrar (comportamiento de la spec 009) |
  | `rate-limit` | desactivado; **activado en `prod`** | Límites por cuenta y por IP de la spec 010 |
- **RN-A3**: Un flag desconocido en la configuración impide arrancar (evita erratas silenciosas).
- **RN-A4**: La app puede leer los flags con `GET /api/v1/features` sin sesión, para adaptar la interfaz (por ejemplo, no mostrar «revisa tu email» si la verificación está desactivada).
- **RN-A5**: Las variables antiguas `EMAIL_VERIFICATION_ENABLED` y `RATE_LIMIT_ENABLED` se siguen aceptando si no se define la nueva.

### B. Página de verificación
- **RN-B1**: El enlace del correo abre la página pública `/account/verify-email?token=…`, que verifica el email y muestra el resultado: verificado, enlace caducado, enlace ya usado o no válido, con la indicación de volver a la app.
- **RN-B2**: `GET /api/v1/auth/verify?token=` se mantiene, así que los enlaces de correos ya enviados siguen funcionando.

### C. Dispositivos
- **RN-C1**: Un usuario solo puede dar de baja **sus** dispositivos. Si el token no existe o es de otro usuario, responde 404 y no cambia nada.

## Criterios de aceptación
- **CA-1** (RN-A2): Dada la configuración por defecto, cuando un usuario se registra, entonces queda verificado y puede entrar sin verificar el email.
- **CA-2** (RN-A1/A2): Dado `email-verification` activado, cuando un usuario se registra e intenta entrar sin verificar, entonces recibe 403 `EMAIL_NOT_VERIFIED`.
- **CA-3** (RN-A4): `GET /api/v1/features` sin token devuelve el estado de cada flag.
- **CA-4** (RN-A3): Una configuración con un flag inexistente no arranca, con un mensaje que lo nombra.
- **CA-5** (RN-B1): La página `/account/verify-email` es pública y llama a la verificación; el correo de verificación enlaza a ella.
- **CA-6** (RN-C1): Dado el dispositivo de otro usuario, cuando intento darlo de baja, entonces recibo 404 y el dispositivo sigue registrado; con el mío recibo 204.

## API (contrato)
| Método | Ruta | Permiso | Respuesta |
|---|---|---|---|
| GET | `/api/v1/features` | público | `{"email-verification": false, "rate-limit": true}` |
| GET | `/account/verify-email?token=` | público | Página HTML |
| DELETE | `/api/v1/devices/{token}` | auth (propio) | **204**; 404 si no es tuyo |

## Fuera de alcance
- Cambiar flags en caliente o por club/usuario (requiere panel de administración).

## Preguntas abiertas
- (ninguna)
