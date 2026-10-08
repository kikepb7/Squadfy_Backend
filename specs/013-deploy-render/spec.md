# 013 — Despliegue en Render y flujo de ramas

- **Estado**: En curso
- **Módulos**: app, user (IP del cliente), CI/infra
- **Dependencias**: 006 (imagen Docker, CI), 010 (rate limit por IP)

## Contexto y objetivo
Desplegar el backend en Render con dos entornos reales (staging y producción) y un flujo de ramas que impida desplegar cualquier rama: `feature/* → develop → release → master`. `master` es producción; `release` es staging; `develop` es integración (no se despliega). El CI valida cada PR y Render hace el CD solo cuando el CI del commit está en verde.

## Reglas de negocio
- **RN-1**: La aplicación escucha en el puerto de la variable `PORT` (que Render inyecta); sin ella, en 8080.
- **RN-2**: Detrás de un proxy de confianza, la IP del cliente es la última entrada de la cabecera configurada (`X-Forwarded-For` en Render, `X-Real-IP` con nginx) que no pertenezca a un proxy de confianza. Una entrada falsificada por el cliente (más a la izquierda) nunca prevalece.
- **RN-3**: La cabecera y los proxies de confianza se configuran por variable de entorno (`NGINX_CLIENT_IP_HEADER`, `NGINX_TRUSTED_IPS`); el comportamiento por defecto (nginx, `X-Real-IP`) no cambia.
- **RN-4**: Los orígenes CORS permitidos se configuran con `CORS_ALLOWED_ORIGINS` (patrones separados por comas). Vacío = ningún origen cruzado permitido (la app móvil no lo necesita). En `dev`: `http://localhost:*`. Producción: `https://squadfy.app`. Sin credenciales/cookies.
- **RN-5**: Swagger/OpenAPI se activa con `SPRINGDOC_ENABLED` (desactivado por defecto y en producción; activado en `dev` y en staging).
- **RN-6**: El CI (build + tests + imagen) corre en PRs y pushes a `develop`, `release` y `master`.
- **RN-7**: Render despliega `release` → staging y `master` → producción, solo tras CI en verde (`autoDeployTrigger: checksPass`). `develop` no se despliega. Los secretos nunca están en el repo (`sync: false`).

## Criterios de aceptación
- **CA-1** (RN-1): Dado `PORT=9090`, cuando arranca, entonces el servidor escucha en 9090.
- **CA-2** (RN-2, RN-3): Dados proxies de confianza `10.0.0.0/8` y cabecera `X-Forwarded-For`, cuando llega una petición desde `10.1.2.3` con `X-Forwarded-For: 6.6.6.6, 203.0.113.9`, entonces la IP del cliente es `203.0.113.9`; sin cabecera válida y con `require-proxy`, falla; desde un origen no confiable con `require-proxy`, falla.
- **CA-3** (RN-3): Con la configuración por defecto, `X-Real-IP` sigue funcionando como hasta ahora.
- **CA-4** (RN-4): Dado `CORS_ALLOWED_ORIGINS=https://squadfy.app`, cuando un preflight llega desde ese origen entonces se permite; desde otro origen no.
- **CA-5** (RN-5): Con `SPRINGDOC_ENABLED=false` `/v3/api-docs` responde 404; con `true`, 200.
- **CA-6** (RN-6, RN-7): `render.yaml` define los dos servicios con sus ramas, health check `/actuator/health/readiness` y secretos con `sync: false`.

## API (contrato)
Sin endpoints nuevos.

## Fuera de alcance
- Alta de cuentas (Supabase, CloudAMQP, Redis, Render, Firebase) y protección de ramas en GitHub: pasos manuales en `docs/DEPLOY.md`.
- Escalado a varias instancias (WebSocket en memoria, ver `BACKEND.md` §16).

## Preguntas abiertas
- [x] Rama de producción: `master`; integración `develop`; staging `release` (decidido 2026-10-08).
- [x] CORS desde ya (`https://squadfy.app` y `localhost`) (decidido 2026-10-08).
