# 013 — Plan técnico

## Enfoque
Cambios mínimos de código y el resto como configuración versionada (`render.yaml`, workflow, documentación). Alternativa descartada: `server.forward-headers-strategy` de Spring, porque `IpResolver` ya centraliza la IP del cliente y la regla "última entrada no confiable" evita el spoofing.

## Cambios por capa
- **app / config**: `server.port: ${PORT:8080}`; `springdoc.*.enabled: ${SPRINGDOC_ENABLED:false}`; propiedad `squadfy.cors.allowed-origins`; `SecurityConfig` con `.cors` y un `CorsConfigurationSource` (patrones de origen, sin credenciales).
- **user / infrastructure**: `NginxConfig.clientIpHeader` (por defecto `X-Real-IP`); `IpResolver` recorre la cabecera de derecha a izquierda y devuelve la primera IP válida no confiable.
- **infra**: `render.yaml` (staging `release`, prod `master`), `.github/workflows/ci.yml` con las ramas nuevas, `.env.example`, `docs/DEPLOY.md`, `docs/BACKEND.md`.

## Riesgos
- Que las IPs de origen de Render no estén en `10.0.0.0/8` → en staging se comprueba con una petición real y el log "Direct connection attempt" (se documenta en `DEPLOY.md`).
- Con Cloudflare delante de Render la IP resuelta sería la del borde, no la del usuario → documentado; el límite por cuenta (spec 010) sigue siendo la protección principal.

## Estrategia de test
- Unitario: `IpResolverTest` (CA-2, CA-3).
- Integración: `CorsIntegrationTest` (CA-4); `ApiV1IntegrationTest` ya cubre OpenAPI activo (CA-5); `SquadfyApplicationTests` mantiene los 404/401.
