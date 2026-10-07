# 009 — App config: verificación de email desactivable

- **Estado**: Hecha
- **Módulos**: user, app (configuración)
- **Dependencias**: —

## Contexto y objetivo
Para probar la app de punta a punta (local o QA) hace falta crear muchos usuarios. La verificación por email lo impide: el login devuelve 403 `EMAIL_NOT_VERIFIED` hasta abrir el enlace del correo, y en entornos con Mailgun no se puede leer ese correo. Se añade un nodo de configuración `squadfy.app-config` con un interruptor para desactivar la verificación fuera de producción.

## Historias de usuario
- Como QA, quiero desactivar la verificación de email para registrar usuarios de prueba y entrar en la app sin abrir correos.
- Como owner del producto, quiero que en producción la verificación sea siempre obligatoria.

## Reglas de negocio
- **RN-1**: `squadfy.app-config.email-verification.enabled` (variable `EMAIL_VERIFICATION_ENABLED`, por defecto `true`) decide si se exige verificar el email.
- **RN-2**: Con la verificación desactivada, el registro crea el usuario ya verificado. No genera token ni envía correo, y publica `UserEvent.Verified` en lugar de `UserEvent.Created`, para que los demás módulos den de alta al participante.
- **RN-3**: Con la verificación desactivada, el login no comprueba `hasVerifiedEmail`, así que también entran los usuarios registrados antes de desactivarla.
- **RN-4**: En el perfil `prod` el valor está fijado a `true` en `application-prod.yml` y no se puede cambiar con variables de entorno.

## Criterios de aceptación
- **CA-1** (RN-1): Dada la configuración por defecto, cuando un usuario se registra e intenta entrar sin verificar, entonces recibe 403 `EMAIL_NOT_VERIFIED`.
- **CA-2** (RN-2): Dada la verificación desactivada, cuando un usuario se registra, entonces `hasVerifiedEmail = true` y el login devuelve 200 con tokens.
- **CA-3** (RN-3): Dada la verificación desactivada, cuando entra un usuario sin verificar registrado antes, entonces el login devuelve 200.

## API (contrato)
Sin cambios de rutas ni de DTOs. Solo cambia el comportamiento de `POST /auth/register` y `POST /auth/login` según la configuración.

## Fuera de alcance
- Exponer la configuración a la app por un endpoint. La app sigue mostrando «te hemos enviado un email» tras el registro, pero con el interruptor apagado puede iniciar sesión sin más.

## Preguntas abiertas
- (ninguna)
