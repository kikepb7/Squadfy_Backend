# 007 — Plan técnico

## Enfoque
- **Sustitución directa**: cada controlador cambia su `@RequestMapping`/rutas a v1; sin alias (app no publicada). Los controladores se reorganizan por recurso v1 (p. ej. `ClubMatchController` para `/clubs/{clubId}/matches`), siempre delegando en los servicios existentes.
- **Rutas que cambian de forma** (el `clubId` pasa del cuerpo a la ruta, `DELETE cancel` → `POST cancel`, `/scheduled` → `?status=`): DTOs de petición sin `clubId`; el servicio no cambia.
- **OpenAPI**: `springdoc-openapi-starter-webmvc-ui` (versión compatible con Boot 4.1). `GroupedOpenApi` solo con `/api/v1/**`, `SecurityScheme` Bearer, anotaciones `@Operation`/`@ApiResponse` en controladores, `@Schema` en DTOs. `springdoc.api-docs.enabled` y `swagger-ui.enabled` en `false` por defecto y `true` en `application-dev.yml`. `/v3/api-docs/**` y `/swagger-ui/**` permitidos en `SecurityConfig` (sin springdoc activo responden 404).
- **`/me`**: `MeController` en `user` (perfil del usuario autenticado, con email).
- **Privacidad (R-6)**: quitar `email` de `ClubMemberDto` y `ChatParticipantDto` públicos.
- **Convocatoria vigente (R-7)**: `MatchAnnouncementService.getCurrentForClub(clubId, userId)` → siguiente partido `SCHEDULED` futuro y su convocatoria; `myStatus` y posición en espera calculados desde las entradas ordenadas.

## Riesgos
- Compatibilidad de springdoc con Spring Boot 4.1 → verificar la versión al empezar (T1); si no hay compatible, generar OpenAPI estático y documentarlo.
- Olvidar una ruta → test que comprueba que todas las rutas registradas empiezan por `/api/v1`.

## Estrategia de test
- `@SpringBootTest(RANDOM_PORT)` con `InfrastructureTestContainersConfiguration`: smoke test por grupo de rutas v1 y su alias, cabeceras de deprecación, OpenAPI (CA-3), privacidad (CA-4).
- Integración de servicio para R-7 en `MatchFlowIntegrationTest` (CA-5).
