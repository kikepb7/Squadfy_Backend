# 014 — Política de privacidad pública

- **Estado**: Hecha (2026-10-08), con el texto en **borrador** a falta de los datos del responsable y de revisión legal
- **Módulos**: user (página estática), app (seguridad), CI
- **Dependencias**: 010 (borrado de cuenta), 013 (despliegue)

## Contexto y objetivo
Apple y Google exigen una URL pública con la política de privacidad para publicar la app. Se sirve desde el backend, igual que la página de borrado de cuenta, y describe lo que el backend trata realmente.

## Reglas de negocio
- **RN-1**: `/legal/privacy` es una página pública (sin sesión) en español, legible en móvil, sin cookies ni analítica.
- **RN-2**: Describe responsable, datos tratados, finalidades y bases legales, destinatarios y proveedores, transferencias internacionales, conservación, visibilidad dentro de los clubes, invitados, derechos (con enlace a `/account/delete`), menores, seguridad y cambios.
- **RN-3**: Los datos del responsable son huecos `{{…}}` que se rellenan antes de producción. Una PR a `master` con huecos sin rellenar no pasa el CI; staging sí puede desplegarse con el borrador.
- **RN-4**: La página de borrado de cuenta enlaza a la política.

## Criterios de aceptación
- **CA-1** (RN-1): `GET /legal/privacy` sin token responde 200 con la política.
- **CA-2** (RN-2/RN-4): La política enlaza a `/account/delete`, y la página de borrado enlaza a la política.
- **CA-3** (RN-3): El job del CI **Legal texts completed** falla en las PR a `master` mientras queden `{{` en `privacy.html`.

## API (contrato)
| Método | Ruta | Permiso | Respuesta |
|---|---|---|---|
| GET | `/legal/privacy` | público | Página HTML (URL para App Store y Google Play) |

## Fuera de alcance
- Términos y condiciones de uso.
- Versiones de la política en otros idiomas.

## Preguntas abiertas (huecos del borrador)
- [ ] `{{RESPONSABLE}}` (nombre o razón social), `{{NIF}}`, `{{DIRECCION}}`, `{{EMAIL_CONTACTO}}` y `{{FECHA}}`.
- [ ] Edad mínima: el borrador fija 14 años (art. 7 LOPDGDD). Revisar si el producto se dirige a menores.
- [ ] Revisión por un profesional del texto final.
