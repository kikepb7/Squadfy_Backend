# 014 — Plan

- Página estática `user/src/main/resources/static/legal/privacy.html`, servida en `/legal/privacy` con un *view controller* (`WebMvcConfig`) y pública en `SecurityConfig`. Mismo estilo que `/account/delete`.
- Contenido basado en lo que guarda cada módulo (user, club, match, chat, notification) y en los proveedores de `docs/DEPLOY.md`.
- CI: job `legal` en `ci.yml`, solo en las PR a `master`, que busca `{{` en la página.
- Test HTTP en `ApiV1IntegrationTest` (CA-1, CA-2).
