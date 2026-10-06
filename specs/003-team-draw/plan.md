# 003 — Plan técnico

## Algoritmo (implementado en `match/domain/model/TeamBalancer.kt`)
1. Barajar con `Random` inyectable (variedad + tests deterministas con semilla).
2. Agrupar por posición en orden `GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD, sin posición`.
3. Dentro de cada grupo, de mayor a menor nivel, asignar al equipo con: (a) menos jugadores de esa posición, (b) menos jugadores en total, (c) menor nivel acumulado. Esto garantiza RN-2 y RN-3 por construcción.
4. Búsqueda local: aplicar el intercambio entre jugadores **de la misma posición** que más reduzca la diferencia de nivel, hasta que ninguno mejore (máx. 100 iteraciones). Conserva RN-2 y RN-3.

## Rating de jugador: por qué Elo y no ML supervisado
Un club tiene 10–22 jugadores y un partido por semana: un modelo supervisado (regresión, gradient boosting...) no tendría datos suficientes para aprender ni para validarse, y sería opaco. Los sistemas de rating bayesianos/Elo son el estándar para emparejar en deportes y juegos (Elo, Glicko, TrueSkill): aprenden online partido a partido, funcionan con pocos datos y son explicables.

Implementación (`match/domain/model/PlayerRatingCalculator.kt`, función pura con tests):
- `E_A = 1 / (1 + 10^((R̄_B − R̄_A)/400))` con la media de rating de cada equipo; `S_A ∈ {1, 0.5, 0}`.
- Multiplicador por margen: `M = 1 + ln(1 + |gd|)/2`.
- Delta de equipo por jugador: `±K_i · M · (S_A − E_A)`, con `K_i = 20 + 20·max(0, 1 − partidos/10)` (provisional para nuevos).
- Delta individual: `6 · (perf_i − perf̄)` con `perf = goles + 0.75·asist − 0.5·amarillas − 2·rojas`, limitado a ±15 y recentrado para que sume cero.

Persistencia en `match_service`: `player_ratings (club_id, club_member_id, rating, matches_rated)` y `player_rating_changes (match_id, club_member_id, delta)` para revertir al reabrir. Se aplica en `MatchService.completeMatch` y se revierte en `reopenMatch` (solo el último partido cerrado, para no romper la secuencia).

Evolución posible (cuando haya volumen): TrueSkill/Glicko-2 para modelar la incertidumbre (σ) y priorizarla en el sorteo; calibrar pesos con los partidos históricos minimizando el error de predicción del resultado.

## Equilibrio de equipos (RN-10)
- Dominio puro `TeamBalanceModel.of(matchId, teamARatings, teamBRatings)` en `match/domain/model`: medias, totales, diferencia y `teamAExpectedScore = PlayerRatingCalculator.expectedScore(mediaA, mediaB)` (misma fórmula que actualiza el rating, así la cifra es coherente con cómo aprende el modelo).
- Se usan **medias** para comparar y esperar resultado (los equipos pueden diferir en un jugador); los totales se muestran como información.
- `MatchTeamService.getTeamBalance(matchId, userId)`: `requireManager`, equipos desde `MatchService.loadMatch`, ratings desde `PlayerRatingService.ratingsFor`. Sin equipos → `InvalidMatchStateException` (409).
- Endpoint en `MatchController`. Cuando exista la API v1 (spec de `api-consistency`) se expondrá en la nueva ruta.

## Clasificación por rating (RN-11)
- El puerto `ClubMembershipProvider` gana `findAllMembers(clubId)` (también lo necesitará la spec 005 para los destinatarios de notificaciones).
- Dominio puro `RatingLeaderboard.rank(entries)`: orden por rating desc, partidos desc e id (estable); posición de competición sobre el rating redondeado (lo que ve el usuario).
- `PlayerRatingService.getLeaderboard(clubId, userId)` (miembro) y `getMyRating` reutiliza la clasificación para devolver `rank` y `totalPlayers`.
- La respuesta solo lleva `clubMemberId`; el cliente cruza nombre y foto con `GET /api/club/{clubId}/members`.

## Estrategia de test
- Unitarios (hechos): `TeamBalancerTest` (tamaños 2–22, porteros, posiciones, nivel, estrellas, determinismo), `PlayerRatingCalculatorTest` (ganador/perdedor, empate, sorpresa, margen, suma cero individual, novatos).
- Unitarios: `TeamBalanceModelTest` (CA-9, CA-10, CA-12), `RatingLeaderboardTest` (CA-14).
- Integración: CA-4..CA-8, CA-11 y CA-13 en `MatchFlowIntegrationTest`; `findAllMembers` en `ClubMembershipQueryServiceIntegrationTest`.
