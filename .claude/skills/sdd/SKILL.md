---
name: sdd
description: Flujo Spec-Driven Development de Squadfy. Usar al empezar o continuar cualquier feature o cambio funcional (nueva funcionalidad, regla de negocio, endpoint), al escribir o actualizar specs/plan/tasks en specs/NNN-*, o cuando el usuario pida "especificar", "planificar", "desglosar tareas" o "implementar la spec NNN".
---

# SDD en Squadfy

Fuente de verdad: `specs/`. Lee siempre primero `specs/constitution.md` y `specs/README.md` (índice y estado).

## 1. Identificar la fase
- ¿Existe `specs/NNN-*/` para lo pedido? Si no → **Especificar**.
- ¿`spec.md` tiene preguntas abiertas que cambian el alcance? → pregúntalas al usuario (AskUserQuestion) antes de seguir. No inventes reglas de negocio.
- ¿Hay `plan.md` y `tasks.md`? → **Implementar** la primera tarea `[ ]` no bloqueada.

## 2. Especificar (`spec.md`)
- Copia `specs/_templates/spec.md` a `specs/NNN-<kebab>/spec.md` (NNN = siguiente número libre).
- Reglas de negocio numeradas `RN-n`, verificables y sin detalles de implementación.
- Criterios de aceptación `CA-n` en formato Dado/Cuando/Entonces, cada uno referenciando sus RN.
- Tabla de contrato API con permiso (autenticado / miembro / gestor / owner).
- Añade la fila al índice de `specs/README.md`.

## 3. Planificar (`plan.md`)
- Usa la skill `squadfy-conventions` para respetar módulos, capas y puertos.
- Indica cambios por capa, modelo de datos (migración Flyway), eventos, riesgos y qué test cubre cada CA.
- Comprueba la constitución: permisos en servicio, lógica pura en `domain/model` con `Clock`, sin SQL entre esquemas.

## 4. Desglosar (`tasks.md`)
- Tareas `- [ ] T<n> [P?] descripción — ficheros`, pequeñas y verificables. `[P]` si son paralelizables.
- Orden: dominio puro + tests → persistencia → servicio → API → integración.

## 5. Implementar
- Una tarea cada vez. Al terminar: test que cubre el CA, `verify-backend` en verde, marca `[x]`.
- Si descubres que una regla era incorrecta o incompleta, **actualiza `spec.md` en el mismo cambio** y avísalo al usuario.
- Al completar todas las tareas: cambia el estado en `spec.md` y en `specs/README.md`.

## Reglas
- No implementes funcionalidad que no esté en una spec aprobada; si el usuario lo pide sin spec, crea primero una spec mínima y confírmala.
- Escribe specs y planes en español; código e identificadores en inglés.
