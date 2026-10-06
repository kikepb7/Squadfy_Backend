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
- **RN-8**: Solo los gestores sortean manualmente y solo en partidos `SCHEDULED`. Volver a sortear sustituye los equipos anteriores.
- **RN-9**: **Al cerrarse la convocatoria (22:00 del día anterior) los equipos se publican automáticamente** con un sorteo `AUTO` entre los confirmados. Si un gestor ya había sorteado exactamente con esos jugadores, se respetan sus equipos. Después, los gestores pueden rectificar (nuevo sorteo `AUTO` o equipos `MANUAL`).
- **RN-10**: Los gestores pueden consultar el **equilibrio de los equipos** de un partido para decidir si rectifican: por equipo, número de jugadores, rating medio y total; la diferencia de rating medio; y el **resultado esperado** del equipo A (0–1, donde 0,5 = igualado) según el modelo Elo. Se calcula con los ratings actuales. Los jugadores no tienen acceso (solo ven su propio rating).

## Criterios de aceptación
- **CA-1**: Con 2 porteros y 8 jugadores de campo → 1 portero por equipo.
- **CA-2**: Con 10 centrocampistas de niveles 9.5…2 → diferencia de nivel ≤ 0.5.
- **CA-3**: Con 4 jugadores de nivel 10 y 4 de nivel 2 → 2 "estrellas" en cada equipo.
- **CA-4**: Sortear dos veces seguidas no falla (regresión del índice único) y deja solo los equipos nuevos.
- **CA-5**: Un `PLAYER` que intenta sortear → 403.
- **CA-6**: Modo manual con un jugador no inscrito → 400.
- **CA-7** (RN-9): Con 11 inscritos en un 5v5, al cerrar la convocatoria quedan publicados dos equipos de 5 con los 10 confirmados (el de la lista de espera no juega) y el gestor puede cambiarlos en modo manual.
- **CA-8** (RN-9): Si antes del cierre el gestor sorteó con los mismos confirmados, al cerrar se mantienen esos equipos.
- **CA-9** (RN-10): Con dos equipos de 5 jugadores sin partidos (rating 1000), el equilibrio muestra media 1000 en ambos, diferencia 0 y resultado esperado 0,5.
- **CA-10** (RN-10): Si un equipo es más fuerte, su resultado esperado es > 0,5 y la diferencia de medias es la resta de ambas.
- **CA-11** (RN-10): Un `PLAYER` que consulta el equilibrio → 403; si el partido aún no tiene equipos → 409.

## API (contrato)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/matches/{matchId}/generate-teams` `{mode: AUTO|MANUAL, manualTeamA?, manualTeamB?}` | gestor |
| GET | `/api/player-ratings/club/{clubId}/me` → `{clubId, clubMemberId, rating, matchesRated, isProvisional}` | miembro |
| GET | `/api/matches/{matchId}/team-balance` → `{matchId, teamA: {players, averageRating, totalRating}, teamB: {...}, averageRatingDifference, teamAExpectedScore}` | gestor |

La respuesta es `MatchDto` con `teamA`/`teamB` (ids de miembro).

## Fuera de alcance
- Más de dos equipos, rotaciones o equipos fijos.

## Preguntas abiertas
- [x] Valoración manual: **no**; el nivel se calcula de las estadísticas de partidos (decidido 2026-10-05).
- [x] Cada jugador puede ver **su propio** rating (decidido 2026-10-05): `GET /api/player-ratings/club/{clubId}/me`.
- [x] Los equipos se publican automáticamente al cerrar la convocatoria y el gestor puede rectificarlos (decidido 2026-10-05).
- [ ] ¿Deben los gestores ver también el rating **individual** de cada jugador para rectificar a mano con criterio? (hoy solo ven los agregados por equipo).
