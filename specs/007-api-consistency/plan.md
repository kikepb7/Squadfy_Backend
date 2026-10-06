# 007 — Plan técnico

## Enfoque
- **Una sola implementación por endpoint**: cada método de controlador declara su ruta v1 y su ruta antigua (`@GetMapping(value = ["/api/v1/clubs", "/api/club"])`), sin `@RequestMapping` de clase. Evita duplicar controladores y garantiza CA-1 (mismo comportamiento).
- **Deprecación transversal**: `DeprecatedApiFilter` en `app` con una tabla `ruta antigua → sucesora` (patrones `AntPathMatcher`) que añade `Deprecation`, `Sunset` (propiedad `squadfy.api.legacy-sunset`) y `Link`. Si R-3 se descarta, el filtro y los alias no se crean.
- **Rutas que cambian de forma** (el `clubId` pasa del cuerpo a la ruta, `DELETE cancel` → `POST cancel`, `/scheduled` → `?status=`): métodos v1 nuevos que delegan en el mismo servicio; los antiguos se mantienen tal cual.
- **OpenAPI**: `springdoc-openapi-starter-webmvc-ui` (versión compatible con Boot 4.1). `GroupedOpenApi` solo con `/api/v1/**`, `SecurityScheme` Bearer, anotaciones `@Operation`/`@ApiResponse` en controladores, `@Schema` en DTOs. `springdoc.api-docs.enabled` y `swagger-ui.enabled` por propiedad (`API_DOCS_ENABLED`). Permitir `/v3/api-docs/**` y `/swagger-ui/**` en `SecurityConfig` solo cuando esté habilitado.
- **`/me`**: `MeController` en `user` (perfil del usuario autenticado, con email).
- **Privacidad (R-6)**: quitar `email` de `ClubMemberDto` y `ChatParticipantDto` públicos.
- **Convocatoria vigente (R-7)**: `MatchAnnouncementService.getCurrentForClub(clubId, userId)` → siguiente partido `SCHEDULED` futuro y su convocatoria; `myStatus` y posición en espera calculados desde las entradas ordenadas.

## Riesgos
- Compatibilidad de springdoc con Spring Boot 4.1 → verificar la versión al empezar (T1); si no hay compatible, generar OpenAPI estático y documentarlo.
- Divergencia entre ruta antigua y v1 → tests HTTP que llaman a ambas (CA-1, CA-2).

## Estrategia de test
- `@SpringBootTest(RANDOM_PORT)` con `InfrastructureTestContainersConfiguration`: smoke test por grupo de rutas v1 y su alias, cabeceras de deprecación, OpenAPI (CA-3), privacidad (CA-4).
- Integración de servicio para R-7 en `MatchFlowIntegrationTest` (CA-5).
