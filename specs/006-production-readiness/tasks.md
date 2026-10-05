# 006 — Tareas

- [ ] T1 Resolver preguntas abiertas (hosting, servicios gestionados, cliente existente).
- [ ] T2 Boot 4.0.x GA y eliminar repos snapshot/milestone — PR aislado.
- [ ] T3 Flyway + `V1__baseline.sql` por esquema; `ddl-auto: validate` en todos los perfiles.
- [ ] T4 [P] Config por variables de entorno + `.env.example`; quitar `squadfy.email.to` de pruebas.
- [ ] T5 [P] `git update-index --chmod=+x gradlew`.
- [ ] T6 Testcontainers: ya usado en `match` y `club` (`@DataJpaTest` + `@ServiceConnection`); falta extraerlo a testFixtures en `common` y que `contextLoads` no dependa de infraestructura externa.
- [ ] T7 [P] Dockerfile multi-stage + `docker-compose.yml` local.
- [ ] T8 [P] Actuator health/info + regla en `SecurityConfig`.
- [ ] T9 [P] CI en GitHub Actions.
- [ ] T10 [P] `allOpen` de entidades en `squadfy.spring-boot-service`; `group = "com.kikepb"`.
- [ ] T11 springdoc-openapi con Bearer.
- [ ] T12 Unificar rutas REST (R-9) coordinado con el cliente.
- [x] T13 `.gitignore`: `**/build/`, `.DS_Store`, `.kotlin/`.
