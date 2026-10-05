# 003 — Sorteo de equipos equilibrados

- **Estado**: Hecha (rama `match-feature`)
- **Módulos**: match, club (datos de jugador), common
- **Dependencias**: 001, 002, 004 (el rating se actualiza al cerrar partidos)

## Contexto y objetivo
Con los jugadores inscritos en la convocatoria, formar dos equipos lo más parejos posible teniendo en cuenta la posición de cada jugador y su nivel (derivado de estadísticas), con variedad entre sorteos.

## Reglas de negocio
- **RN-1**: Solo se sortea entre los jugadores `CONFIRMED` de la convocatoria (no la lista de espera); mínimo 2.
- **RN-2**: Los tamaños de los equipos difieren como mucho en 1.
- **RN-3**: Cada posición se reparte lo más equitativamente posible; los porteros primero (uno por equipo si hay dos).
- **RN-4**: Se minimiza la diferencia de nivel total entre equipos sin romper RN-2 ni RN-3.
- **RN-5**: Ante igualdad de nivel, el sorteo es aleatorio: repetir el sorteo puede dar alineaciones distintas.
- **RN-6**: El nivel es un **rating calculado automáticamente** (nadie lo introduce a mano), por club y jugador, que empieza en 1000 y se actualiza cada vez que se cierra un partido:
  - **Componente de equipo**: sube si tu equipo gana y baja si pierde; más si el resultado era inesperado o la diferencia de goles es grande.
  - **Componente individual**: goles y asistencias suman, tarjetas restan, en relación con la media del partido (suma cero: no infla el rating global).
  - Los jugadores nuevos se mueven más rápido hasta que su rating se estabiliza (~10 partidos).
  - Reabrir un partido revierte exactamente los cambios que produjo.
- **RN-7**: Modo `MANUAL`: el gestor indica ambos equipos; deben ser disjuntos, no vacíos, con diferencia ≤ 1 y solo con inscritos.
- **RN-8**: Solo los gestores sortean y solo en partidos `SCHEDULED`. Volver a sortear sustituye los equipos anteriores.

## Criterios de aceptación
- **CA-1**: Con 2 porteros y 8 jugadores de campo → 1 portero por equipo.
- **CA-2**: Con 10 centrocampistas de niveles 9.5…2 → diferencia de nivel ≤ 0.5.
- **CA-3**: Con 4 jugadores de nivel 10 y 4 de nivel 2 → 2 "estrellas" en cada equipo.
- **CA-4**: Sortear dos veces seguidas no falla (regresión del índice único) y deja solo los equipos nuevos.
- **CA-5**: Un `PLAYER` que intenta sortear → 403.
- **CA-6**: Modo manual con un jugador no inscrito → 400.

## API (contrato)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/matches/{matchId}/generate-teams` `{mode: AUTO|MANUAL, manualTeamA?, manualTeamB?}` | gestor |
| GET | `/api/player-ratings/club/{clubId}/me` → `{clubId, clubMemberId, rating, matchesRated, isProvisional}` | miembro |

La respuesta es `MatchDto` con `teamA`/`teamB` (ids de miembro).

## Fuera de alcance
- Más de dos equipos, rotaciones o equipos fijos.

## Preguntas abiertas
- [x] Valoración manual: **no**; el nivel se calcula de las estadísticas de partidos (decidido 2026-10-05).
- [x] Cada jugador puede ver **su propio** rating (decidido 2026-10-05): `GET /api/player-ratings/club/{clubId}/me`.
- [ ] ¿Se publican los equipos automáticamente al cerrar la convocatoria o el gestor los confirma?
