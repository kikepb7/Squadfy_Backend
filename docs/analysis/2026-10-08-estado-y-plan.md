# Estado del repositorio y plan hasta producción — 2026-10-08

Continúa el [análisis inicial](2026-10-05-analisis-repositorio.md). Ramas analizadas: `master` (`2119d11`), `develop` (`922f63b`) y `release`.

## 1. Estado actual

### Código
| Área | Estado |
|---|---|
| Specs 001–012 (clubes, ciclo de partido, sorteo y rating, resultados, notificaciones, producción, API v1, paridad con la app, flags, borrado de cuenta, cierre del MVP) | ✅ en `master` |
| Spec 013 (Render staging/prod, flujo de ramas, CORS, IP real tras proxy) | ✅ código en `develop`; falta el alta de cuentas y el primer despliegue |
| Tests | 150 en `develop` (unitarios + integración con Testcontainers), en verde |
| Avisos de compilación y deprecación | Ninguno |
| Migraciones Flyway | V1–V9 |
| Documentación | `docs/BACKEND.md` (contrato para la app), `docs/DEPLOY.md` (despliegue), specs por funcionalidad |

### Ramas
```
feature/<x> ──PR──▶ develop ──PR──▶ release ──PR──▶ master ──▶ etiqueta vX.Y.Z
                  (integración)    (staging)      (producción)
```
| Rama | Commit | Contenido |
|---|---|---|
| `master` | `2119d11` | Specs 001–012 |
| `release` | `2119d11` | Creada desde `master` (2026-10-08); aún no se ha promocionado nada |
| `develop` | `922f63b` | `master` + spec 013 |

Las ramas de funcionalidad antiguas siguen en el repositorio: 13 fusionadas y 2 sin fusionar (`test-feature`, `claude/squadfy-backend-patch-xyagdj`, muy por detrás de `master`).

### Versiones
La versión de la próxima release está en `gradle.properties` (`1.0.0`) y se ve en `GET /actuator/info`. Al fusionar en `master` se crea la etiqueta `v1.0.0` con su GitHub Release (`release-tag.yml`). Una PR a `master` con una versión ya publicada falla en el CI.

### Limitaciones conocidas (aceptadas para el MVP)
- **Una sola instancia.** El WebSocket (chat y avisos `CLUB_DATA_CHANGED`) vive en memoria.
- **Mensajes antiguos en las colas.** Al pasar RabbitMQ a Jackson 3, los mensajes antiguos pendientes en las colas podrían no leerse. En un despliegue nuevo no aplica.
- **Rollback limitado.** Un rollback en Render no revierte migraciones, así que cada migración debe ser compatible con la versión anterior del código.

## 2. Qué queda por hacer

Responsable: **C** = código (Claude), **T** = tú (cuentas, configuración, decisiones), **A** = conversación de la app.

### Fase 0 — Estructura de ramas y versiones (hecha hoy, salvo los ajustes de GitHub)
| # | Tarea | Resp. | Estado |
|---|---|---|---|
| 0.1 | Ramas `develop` y `release` en GitHub | C | ✅ |
| 0.2 | Versión en `gradle.properties`, etiqueta automática y comprobación de versión en el CI | C | ✅ en `feature/release-process` (PR a `develop`) |
| 0.3 | Rama por defecto `develop` y protección de `develop`, `release` y `master`: PR obligatoria, checks **Build & test** y **Docker image** (en `master` también **Version not released yet**), sin force push | T | ⏳ `docs/DEPLOY.md` §2 |

### Fase 1 — Cerrar la versión 1.0.0 en `develop`
| # | Tarea | Resp. | Bloquea |
|---|---|---|---|
| 1.1 | Página pública de **política de privacidad** (como `/account/delete`). Hace falta el texto legal o aprobar un borrador | C + T | Tiendas |
| 1.2 | Rotar la contraseña de CloudAMQP (compartida en el chat); quitar el `RABBITMQ_PASSWORD` antiguo de IntelliJ; JDK del proyecto → temurin-21 | T | Seguridad |
| 1.3 | (Opcional) Términos de uso, si se quieren mostrar en el registro | C + T | — |

### Fase 2 — Infraestructura de staging (guía: `docs/DEPLOY.md` §3–6)
| # | Tarea | Resp. |
|---|---|---|
| 2.1 | Proyecto Supabase **staging** (BD por *session pooler*, buckets de Storage, solo `public` expuesto) | T |
| 2.2 | CloudAMQP y Redis de staging | T |
| 2.3 | Mailgun con **dominio verificado** (sin él, el correo de recuperar contraseña solo llega a destinatarios autorizados) | T |
| 2.4 | Firebase: service account de staging | T |
| 2.5 | PR `develop → release`. Después, Render → Blueprint **desde la rama `release`** (es la primera que tendrá `render.yaml`) → rellenar los secretos del servicio de staging | T |

### Fase 3 — Validación en staging
| # | Tarea | Resp. |
|---|---|---|
| 3.1 | Checklist de `docs/DEPLOY.md` §7: health, Swagger, registro/login, club, foto, WebSocket, push, IP real del cliente | T (+ C si algo falla) |
| 3.2 | La app KMP migrada a `/api/v1` y probada contra staging (cambios de las specs 008–012 en `docs/BACKEND.md` §15) | A |
| 3.3 | Correcciones que salgan: `feature/*` → `develop` → `release` | C |

### Fase 4 — Producción 1.0.0
| # | Tarea | Resp. |
|---|---|---|
| 4.1 | Infraestructura de producción: Supabase, CloudAMQP, Redis, Firebase y secretos **distintos** de staging (también `JWT_SECRET_BASE64`) | T |
| 4.2 | PR `release → master` → CI → Render despliega producción → etiqueta `v1.0.0` | T |
| 4.3 | Dominio `api.squadfy.app` en Render y `APP_PUBLIC_URL` | T |
| 4.4 | URLs para las tiendas: `https://api.squadfy.app/account/delete` y la de la política de privacidad | T |
| 4.5 | Subir la versión de `develop` a `1.1.0` para el siguiente ciclo | C |

### Fase 5 — Operación (recomendado justo después del lanzamiento)
| # | Tarea | Resp. |
|---|---|---|
| 5.1 | Alertas de Render (deploy fallido, servicio caído) y un monitor externo de `/actuator/health/readiness` | T |
| 5.2 | Copias de seguridad de la BD de producción: plan de Supabase con backups/PITR, o `pg_dump` programado | T |
| 5.3 | Seguimiento de errores (p. ej. Sentry) y retención de logs | C + T |

### Backlog posterior al MVP
- Varias instancias: repartir los avisos del WebSocket y el chat por RabbitMQ.
- Temporadas con nombre configuradas por club (hoy son rangos de fechas).
- Exportación de datos del usuario (portabilidad).
- Avisar al nuevo OWNER cuando hereda un club por un borrado de cuenta.
- Feature flags por club o por usuario, cambiables en caliente (requiere panel de administración).

## 3. Orden recomendado
1. Fusionar `feature/release-process` en `develop` y configurar GitHub (0.2–0.3).
2. Política de privacidad (1.1) mientras se dan de alta las cuentas de staging (2.x).
3. Primera promoción `develop → release` y validación en staging junto con la app (3.x).
4. Producción y etiqueta `v1.0.0` (4.x); justo después, alertas y backups (5.x).
