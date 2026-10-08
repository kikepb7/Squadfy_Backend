# 011 — Plan técnico

- **common**: enum `Feature` (`domain/feature`) con clave y valor por defecto; `FeatureFlags` (`infrastructure/config`, `@ConfigurationProperties("squadfy")` con `features: Map<String, Boolean>`) con `isEnabled(Feature)`, `snapshot()` y validación de claves desconocidas al arrancar.
- **app**: `application.yml` declara cada flag con `${FEATURE_X:${LEGACY:defecto}}`; `application-prod.yml` solo activa `rate-limit`. `FeatureController` (`GET /api/v1/features`, público en `SecurityConfig`).
- **user**: `AuthService` y `RateLimiter` consultan `FeatureFlags`; se elimina `AppConfig`. Página estática `static/account/verify-email.html` (view controller `/account/verify-email`).
- **notification**: el correo de verificación enlaza a `/account/verify-email`; `unregisterDevice(userId, token)` solo borra el propio (`DeviceTokenNotFoundException` → 404).
- **Tests**: `FeatureFlags` (unitario, CA-4), verificación por defecto y activada (CA-1, CA-2), `/features` y páginas (CA-3, CA-5), dispositivos (CA-6).
