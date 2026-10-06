# 005 — Notificaciones del ciclo de partido

- **Estado**: Hecha (rama `match-notifications-feature`)
- **Módulos**: match (publica), notification (consume), common (eventos)
- **Dependencias**: 002, 003; infraestructura push existente (Firebase)

## Contexto y objetivo
Que los miembros se enteren sin abrir la app de que la convocatoria está abierta, de que se cierra pronto y de los equipos sorteados.

## Reglas de negocio
- **RN-1**: Al abrirse una convocatoria (llega `opensAt`), push a todos los miembros del club.
- **RN-2**: 24 h antes de `closesAt` (configurable), recordatorio a los miembros **no inscritos** (ni confirmados ni en espera) si quedan plazas confirmadas libres. Si la convocatoria se abrió dentro de esas 24 h, no hay recordatorio (acabarían de recibir el aviso de apertura).
- **RN-3**: Al publicarse los equipos (automáticamente al cierre o cuando un gestor los sortea o rectifica), push a cada jugador de los equipos indicando en cuál juega.
- **RN-4**: Al cancelar un partido, push a todos los inscritos (confirmados y en espera).
- **RN-5**: Apertura y recordatorio se envían como mucho **una vez** por convocatoria, aunque el proceso se repita o haya varias instancias.
- **RN-6**: Al promocionar a un jugador desde la lista de espera (alguien se desapunta o sale del club), push a ese jugador.
- **RN-7**: Un usuario puede **silenciar un club**: deja de recibir sus push, salvo la de promoción desde la lista de espera (le afecta directamente).
- **RN-8**: Solo canal **push** (Firebase). Textos en español, con fecha y hora en la zona horaria del club.
- **RN-9**: Las notificaciones solo se emiten si la operación que las origina se confirma (nunca por cambios revertidos).

## Criterios de aceptación
- **CA-1** (RN-1): Al llegar `opensAt`, el proceso emite una notificación de apertura con todos los miembros del club como destinatarios.
- **CA-2** (RN-5): Reejecutar el proceso no vuelve a emitir apertura ni recordatorio.
- **CA-3** (RN-2): A menos de 24 h del cierre con plazas libres, el recordatorio va solo a los no inscritos; con el cupo lleno no se envía.
- **CA-4** (RN-3): Al cierre, la notificación de equipos lleva los usuarios del equipo A y del B por separado.
- **CA-5** (RN-4): Cancelar un partido con 11 inscritos en un 5v5 notifica a los 11.
- **CA-6** (RN-6): Si un confirmado se desapunta, el promocionado recibe su aviso.
- **CA-7** (RN-7): Con dos usuarios con dispositivo y uno de ellos con el club silenciado, una notificación de apertura solo llega al dispositivo del otro; la de promoción llega aunque esté silenciado.
- **CA-8** (RN-7): Un no miembro no puede cambiar la configuración de notificaciones del club → 403.

## API (contrato)
| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/v1/clubs/{clubId}/notification-settings` → `{clubId, muted}` | miembro |
| PUT | `/api/v1/clubs/{clubId}/notification-settings` `{muted}` → `{clubId, muted}` | miembro |

## Preguntas abiertas
- [x] Cada usuario puede **silenciar las notificaciones de un club** (decidido 2026-10-06).
- [x] Solo **push** por ahora; otros canales (email…) se especificarán si se piden (decidido 2026-10-06).
