# Despliegue en Render (staging y producción)

Spec: [`specs/013-deploy-render`](../specs/013-deploy-render/spec.md). Configuración versionada: [`render.yaml`](../render.yaml) y [`.github/workflows/ci.yml`](../.github/workflows/ci.yml).

## 1. Entornos y ramas

```
feature/*  ──PR──▶  develop  ──PR──▶  release  ──PR──▶  master
 (local)          (integración)       (staging)        (producción)
```

| Entorno | Rama | Dónde corre | Perfil | Swagger | Datos |
|---|---|---|---|---|---|
| dev | `feature/*`, `develop` | Tu máquina (`docker compose up` + `bootRun`) | `dev` | sí | Postgres/RabbitMQ/Redis del compose |
| staging | `release` | Render `squadfy-staging` | `prod` | sí (`SPRINGDOC_ENABLED=true`) | Proyecto Supabase **staging** + CloudAMQP y Redis propios |
| producción | `master` | Render `squadfy-prod` | `prod` | no | Proyecto Supabase **prod** + CloudAMQP y Redis propios |

Nada se comparte entre staging y producción: ni BD, ni `JWT_SECRET_BASE64`, ni claves, ni colas.

## 2. GitHub (una vez)

1. Ramas `develop` y `release` (creadas desde `master`). Rama por defecto: `develop` (así los PR de feature apuntan ahí).
2. Settings → Branches → regla para `develop`, `release` y `master`:
   - Require a pull request before merging.
   - Require status checks: **Build & test** (y **Docker image**). En `master`, además: **Version not released yet** y **Legal texts completed**.
   - Do not allow bypassing / force pushes.
3. Los PR van siempre hacia delante: `feature/* → develop`, `develop → release`, `release → master`.

## 3. Supabase (por entorno: staging y prod)

1. New project, región cercana a Render (Frankfurt). Guarda la contraseña de la BD.
2. **Base de datos**: Project Settings → Database → Connection string → **Session pooler** (puerto 5432). Render no tiene salida IPv6 y la conexión directa de Supabase es IPv6. No uses el Transaction pooler (6543): rompe los prepared statements y Flyway.
   - `DB_URL=jdbc:postgresql://aws-0-<región>.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USERNAME=postgres.<project-ref>` · `DB_PASSWORD=<contraseña>`
3. **Migraciones**: Flyway corre solo al arrancar y crea los esquemas (`user_service`, `club_service`, `match_service`, `chat_service`, `notification_service`). Para cambiar el esquema, una migración `V<n>__*.sql` nueva; nunca editar una aplicada.
4. **Storage**: crea los buckets públicos que usa el backend (nombres en `ClubService` y `ProfilePictureService`).
5. Settings → API → "Exposed schemas": solo `public`; los esquemas del backend no deben exponerse por la API REST de Supabase.
6. **Claves**:
   | Clave | Dónde puede estar | Notas |
   |---|---|---|
   | `SUPABASE_PROJECT_URL` | Servidor (no es secreta) | |
   | `anon` / publishable | Cliente (público) | Este backend **no la usa**: la app habla solo con tu API. Si la app llegara a usar Supabase directamente, solo con RLS bien configurada. |
   | `service_role` → `SUPABASE_SERVICE_KEY` | **Solo servidor**: variable secreta en Render y `.env` local | **NUNCA** en la app, en el repo, en logs ni en capturas. Si se filtra, rotarla en Supabase. |

## 4. Servicios gestionados

- **RabbitMQ (CloudAMQP)**: una instancia por entorno (o un vhost distinto). `RABBITMQ_SSL_ENABLED=true`, puerto `5671`.
- **Redis**: Render Key Value o Redis Cloud, uno por entorno (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`).
- **Email (Mailgun SMTP)**: `MAIL_HOST=smtp.mailgun.org`, `MAIL_PORT=587`, usuario/clave, `MAIL_FROM` de un dominio verificado.
- **Firebase**: service account del proyecto. En Render va como **Secret File**, no como variable.

## 5. Secretos

- **Local**: `cp .env.example .env` (ignorado por git y por la imagen Docker). Valores locales sin importancia; `JWT_SECRET_BASE64` propio (`openssl rand -base64 32`).
- **Render**: Dashboard → servicio → Environment. Las variables con `sync: false` de `render.yaml` se piden al crear el Blueprint y se editan aquí; nunca van en git.
- Genera un `JWT_SECRET_BASE64` distinto para cada entorno. Si un secreto estuvo alguna vez en git, rótalo.

Variables que fija `render.yaml` (no secretas): `SPRING_PROFILES_ACTIVE=prod`, `PORT`, `SPRINGDOC_ENABLED`, `NGINX_CLIENT_IP_HEADER=X-Forwarded-For`, `NGINX_TRUSTED_IPS`, `CORS_ALLOWED_ORIGINS`, `WEBSOCKET_ALLOWED_ORIGIN`, `FIREBASE_CREDENTIALS_PATH=file:/etc/secrets/firebase.json`, RabbitMQ SSL y SMTP auth/starttls.

Las que pones tú: `DB_*`, `REDIS_*`, `RABBITMQ_HOST/USERNAME/PASSWORD/VHOST`, `MAIL_*`, `JWT_SECRET_BASE64`, `SUPABASE_PROJECT_URL`, `SUPABASE_SERVICE_KEY`, `APP_PUBLIC_URL` (URL pública del servicio o dominio propio), `RESET_PASSWORD_URL`, `FIREBASE_ANDROID_PACKAGE`.

## 6. Render

1. Cuenta en render.com y conecta GitHub (Account Settings → GitHub) dando acceso a `kikepb7/squadfy_backend`.
2. New → **Blueprint** → selecciona el repo (rama `master`, donde está `render.yaml`) → rellena las variables `sync: false` de **ambos** servicios.
3. Servicio `squadfy-staging` → Environment → **Secret Files** → `firebase.json` con el JSON de la service account (staging). Idem en `squadfy-prod` con la de producción.
4. Cada servicio ya queda con: runtime Docker, 1 instancia, plan Starter (512 MB), health check `/actuator/health/readiness` y **Auto-Deploy = After CI checks pass**. Comprueba en Settings que la rama es `release` (staging) y `master` (prod).
5. Dominio propio (producción): Settings → Custom Domains → `api.squadfy.app`, y el CNAME que indica Render (TLS automático). Actualiza `APP_PUBLIC_URL`.
6. Notas de plan: el plan Free duerme el servicio a los 15 min y rompe el sorteo programado, el chat y el WebSocket; usa Starter como mínimo. Si ves `OutOfMemory`/reinicios, sube a Standard (2 GB). Mantén **una sola instancia**.

## 7. Verificación del primer despliegue (staging)

1. `curl https://<staging>.onrender.com/actuator/health/readiness` → `{"status":"UP"}`.
2. `https://<staging>.onrender.com/swagger-ui.html` carga (solo staging).
3. Registrar un usuario, login, crear un club, subir una foto (Supabase Storage), conectar el chat por WebSocket, recibir una push.
4. **IP del cliente**: en los logs no debe aparecer `Direct connection attempt from ...`. Si aparece, las IPs de origen de Render no están en `NGINX_TRUSTED_IPS`: añade ese rango (el log muestra la IP) y redespliega. Con `NGINX_REQUIRE_PROXY=false` el límite por IP compartiría una única IP para todos los usuarios, no lo uses como atajo.
5. Una migración nueva: aparece en `flyway_schema_history` de la BD de Supabase tras el despliegue.

## 8. Flujo diario

1. `git switch -c feature/<x> develop` → cambios → PR a `develop` (CI: build + tests + imagen).
2. Cuando `develop` está listo: PR `develop → release`. Al fusionar, el CI corre y Render despliega staging.
3. Se prueba en staging. PR `release → master`. Al fusionar y pasar el CI, Render despliega producción y se crea la versión (ver §9).
4. Un hotfix urgente: rama `hotfix/<x>` desde `master`, subir la versión de parche (`1.0.0` → `1.0.1`), PR a `master` y después PR de vuelta a `release` y `develop` para no perderlo.

| Rama | Sale de | Entra en | Despliegue |
|---|---|---|---|
| `feature/<x>` | `develop` | `develop` (PR) | — |
| `develop` | — | `release` (PR) | — (integración) |
| `release` | — | `master` (PR) | staging |
| `master` | — | — | producción + etiqueta `vX.Y.Z` |
| `hotfix/<x>` | `master` | `master`, luego `release` y `develop` | producción |

## 9. Versiones

- La versión vive en `gradle.properties` (`version=X.Y.Z`) y es la de la **próxima** release. Se ve en `GET /actuator/info` (`build.version`) para saber qué hay desplegado en cada entorno.
- Cada fusión en `master` es una release: el workflow `release-tag.yml` crea la etiqueta `vX.Y.Z` y una GitHub Release con las PR incluidas como notas.
- El CI bloquea una PR a `master` si su versión ya está publicada (job **Version not released yet**), así no se pisa una etiqueta.
- También la bloquea si la política de privacidad (`user/src/main/resources/static/legal/privacy.html`) aún tiene huecos `{{…}}` (job **Legal texts completed**, spec 014). Staging sí se despliega con el borrador.
- Tras cada release, sube la versión en `develop` al empezar el siguiente ciclo: `MAJOR` si rompe la API de la app, `MINOR` para funciones nuevas, `PATCH` para correcciones.
5. Rollback: Render → servicio → Events → "Rollback" al deploy anterior (las migraciones aplicadas no se revierten: toda migración debe ser compatible con la versión anterior del código).
