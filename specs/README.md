# Specs — Spec-Driven Development

Cada feature vive en `specs/NNN-<nombre>/` con tres documentos:

| Documento | Responde a | Lo aprueba |
|---|---|---|
| `spec.md` | **Qué** y **por qué**: historias, reglas de negocio (RN), criterios de aceptación (CA), contrato API, preguntas abiertas | Producto (tú) |
| `plan.md` | **Cómo**: diseño por capas, datos, riesgos, tests | Técnico |
| `tasks.md` | **Pasos** ejecutables y verificables, marcados al completarse | — |

Principios obligatorios: [`constitution.md`](constitution.md). Plantillas: [`_templates/`](_templates). Flujo detallado para Claude: skill `sdd` (`.claude/skills/sdd/SKILL.md`).

## Flujo
1. **Especificar** → `spec.md` con RN/CA numeradas y preguntas abiertas. No se implementa con preguntas abiertas que afecten al alcance.
2. **Planificar** → `plan.md` respetando la constitución.
3. **Desglosar** → `tasks.md`, tareas pequeñas (≤ ½ día) con ficheros afectados.
4. **Implementar** → tarea a tarea; cada CA cubierto por un test; `./gradlew build` en verde; marcar `[x]`.
5. **Cerrar** → actualizar el estado de la spec y este índice.

## Índice

| # | Feature | Estado | Bloquea MVP |
|---|---|---|---|
| [001](001-clubs-membership/spec.md) | Clubes y membresía | Hecha | — |
| [002](002-match-cycle/spec.md) | Ciclo semanal, convocatoria y lista de espera | Hecha | Sí |
| [003](003-team-draw/spec.md) | Sorteo de equipos equilibrados + rating Elo | Hecha | Sí |
| [004](004-match-results-stats/spec.md) | Resultado del partido y estadísticas | Hecha | — |
| [005](005-match-notifications/spec.md) | Notificaciones del ciclo de partido | Hecha | — |
| [006](006-production-readiness/spec.md) | Preparación para producción | Hecha (OpenAPI y rutas → `api-consistency`) | Solo falta elegir hosting |
| [007](007-api-consistency/spec.md) | API coherente y documentada (v1) | Hecha — falta migrar la app ([guía](../docs/api/migracion-v1.md)) | Sí (app móvil) |

Análisis inicial del repositorio: [`docs/analysis/2026-10-05-analisis-repositorio.md`](../docs/analysis/2026-10-05-analisis-repositorio.md).
