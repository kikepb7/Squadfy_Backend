# 005 — Notificaciones del ciclo de partido

- **Estado**: Borrador
- **Módulos**: match (publica), notification (consume), common (eventos)
- **Dependencias**: 002, 003; infraestructura push existente (Firebase)

## Contexto y objetivo
Que los miembros se enteren sin abrir la app de que la convocatoria está abierta, de que se cierra pronto y de los equipos sorteados.

## Reglas de negocio
- **RN-1**: Al abrirse una convocatoria (`opensAt`), push a todos los miembros del club.
- **RN-2**: X horas antes de `closesAt` (por defecto 24 h), recordatorio a los miembros **no** inscritos si quedan plazas.
- **RN-3**: Al publicarse los equipos (automáticamente al cierre o por rectificación del gestor), push a los confirmados con su equipo.
- **RN-4**: Al cancelar un partido, push a los inscritos.
- **RN-5**: Cada notificación se envía como mucho una vez por convocatoria y tipo (idempotencia).
- **RN-6** (si hay lista de espera): al promocionar a un jugador, push a ese jugador.

## Criterios de aceptación
- **CA-1**: Abrir convocatoria → un evento `MatchAnnouncementOpened` y una push por dispositivo registrado de cada miembro.
- **CA-2**: Reejecutar el job → no se reenvían notificaciones.

## Preguntas abiertas
- [ ] ¿Preferencias de notificación por usuario/club (silenciar)?
- [ ] ¿Email además de push?
