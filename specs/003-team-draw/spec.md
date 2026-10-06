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
- **RN-10**: Los gestores pueden consultar el **equilibrio de los equipos** de un partido para decidir si rectifican: por equipo, número de jugadores, rating medio y total; la diferencia de rating medio; y el **resultado esperado** del equipo A (0–1, donde 0,5 = igualado) según el modelo Elo. Se calcula con los ratings actuales e incluye el **rating individual de cada jugador** de ambos equipos, para rectificar a mano con criterio.

## Criterios de aceptación
- **CA-1**: Con 2 porteros y 8 jugadores de campo → 1 portero por equipo.
- **CA-2**: Con 10 centrocampistas de niveles 9.5…2 → diferencia de nivel ≤ 0.5.
- **CA-3**: Con 4 jugadores de nivel 10 y 4 de nivel 2 → 2 "estrellas" en cada equipo.
- **CA-4**: Sortear dos veces seguidas no falla (regresión del índice único) y deja solo los equipos nuevos.
- **CA-5**: Un `PLAYER` que intenta sortear → 403.
- **CA-6**: Modo manual con un jugador no inscrito → 400.
- **CA-7** (RN-9): Con 11 inscritos en un 5v5, al cerrar la convocatoria quedan publicados dos equipos de 5 con los 10 confirmados (el de la lista de espera no juega) y el gestor puede cambiarlos en modo manual.
- **CA-8** (RN-9): Si antes del cierre el gestor sorteó con los mismos confirmados, al cerrar se mantienen esos equipos.
- **RN-11**: Los ratings son **públicos dentro del club**: cualquier miembro ve la **clasificación por rating** de todos los miembros (como una tabla de clasificación), ordenada de mayor a menor. Los empates comparten posición (1, 2, 2, 4) comparando el rating redondeado. Los jugadores sin partidos aparecen con el rating inicial y marcados como provisionales. Cada jugador ve además su posición en `/me`.
- **CA-9** (RN-10): Con dos equipos de 5 jugadores sin partidos (rating 1000), el equilibrio muestra media 1000 en ambos, diferencia 0 y resultado esperado 0,5.
- **CA-10** (RN-10): Si un equipo es más fuerte, su resultado esperado es > 0,5 y la diferencia de medias es la resta de ambas.
- **CA-11** (RN-10): Un `PLAYER` que consulta el equilibrio → 403; si el partido aún no tiene equipos → 409.
- **CA-12** (RN-10): El equilibrio lista cada jugador de cada equipo con su rating, de mayor a menor.
- **CA-13** (RN-11): Un miembro cualquiera obtiene la clasificación con todos los miembros del club; tras cerrar un partido 2-0, los ganadores quedan por encima de los perdedores. Un no miembro → 403.
- **CA-14** (RN-11): Ratings 1100, 1000, 1000, 950 → posiciones 1, 2, 2, 4; `/me` del tercero devuelve posición 2 de 4.

## API (contrato)
| Método | Ruta | Permiso |
|---|---|---|
| POST | `/api/matches/{matchId}/generate-teams` `{mode: AUTO|MANUAL, manualTeamA?, manualTeamB?}` | gestor |
| GET | `/api/player-ratings/club/{clubId}` → `[{rank, clubMemberId, rating, matchesRated, isProvisional}]` (clasificación) | miembro |
| GET | `/api/player-ratings/club/{clubId}/me` → `{clubId, clubMemberId, rating, matchesRated, isProvisional, rank, totalPlayers}` | miembro |
| GET | `/api/matches/{matchId}/team-balance` → `{matchId, teamA: {players, averageRating, totalRating, playerRatings: [{clubMemberId, rating}]}, teamB: {...}, averageRatingDifference, teamAExpectedScore}` | gestor |

La respuesta es `MatchDto` con `teamA`/`teamB` (ids de miembro).

## Fuera de alcance
- Más de dos equipos, rotaciones o equipos fijos.

## Preguntas abiertas
- [x] Valoración manual: **no**; el nivel se calcula de las estadísticas de partidos (decidido 2026-10-05).
- [x] ~~Cada jugador ve solo su propio rating~~ → sustituido: los ratings son públicos en el club como clasificación (decidido 2026-10-06, RN-11).
- [x] Los equipos se publican automáticamente al cerrar la convocatoria y el gestor puede rectificarlos (decidido 2026-10-05).
- [x] Los gestores ven el rating individual de cada jugador en el equilibrio de equipos (decidido 2026-10-06, RN-10).
