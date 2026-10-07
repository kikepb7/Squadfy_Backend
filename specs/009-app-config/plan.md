# 009 — Plan

- **Configuración**: `AppConfig` (`@ConfigurationProperties("squadfy.app-config")`) en `user/infrastructure/config`, con `emailVerification.enabled = true` por defecto. En `application.yml`: `squadfy.app-config.email-verification.enabled: ${EMAIL_VERIFICATION_ENABLED:true}`. En `application-prod.yml` el valor está fijado a `true` (RN-4). La variable se documenta en `.env.example`.
- **Servicio**: `AuthService.register` crea el usuario con `hasVerifiedEmail = !enabled`. Si la verificación está desactivada, publica `UserEvent.Verified` y termina; si no, sigue el flujo actual (token + `Created`). `AuthService.login` solo lanza `EmailNotVerifiedException` cuando `enabled` es `true`.
- **Eventos**: `club` trata `Created` y `Verified` igual (alta del participante). `notification` solo envía correo con `Created`/`RequestResendVerification`, así que con `Verified` no sale ningún correo.
- **Tests**: `EmailVerificationEnabledIntegrationTest` (CA-1) y `EmailVerificationDisabledIntegrationTest` (CA-2, CA-3), con Testcontainers.
- **Riesgo**: desactivarla en producción por error. Lo mitiga el valor fijado en `application-prod.yml`.
