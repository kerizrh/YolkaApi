# YolkaApi — Backend REST API 🐾

Backend RESTful para la plataforma de gestión y cuidado integral de mascotas **Yolka**, desarrollado con **Java 21**, **Spring Boot 4.1.1** y **MongoDB Atlas**.

---

## Requisitos Previos

- **Java JDK 21** instalado y configurado en `JAVA_HOME`.
- **MongoDB Atlas** (Cluster activo).
- Archivos de configuración en `config/` (ignorados por Git):
  - `config/firebase/<service-account>.json` (Firebase Admin SDK para notificaciones push FCM).
  - `config/googleapis/<client_secret>.json` (Credenciales OAuth 2.0 de Google).

---

## Configuración de Variables de Entorno (`.env`)

Crea un archivo `.env` en la raíz de `YolkaApi/` (este archivo está en `.gitignore` y **nunca** debe subirse al repositorio):

```env
PORT=8080
BASE_URL=http://localhost:8080/api/v1

# MongoDB Atlas (sin comillas alrededor de la URI)
MONGODB_URI=MONGODB_URI
MONGO_DB_NAME=MONGO_DB_NAME

# Seguridad JWT y Cifrado AES-256-GCM (Respaldos .yolka)
JWT_SECRET=<tu_jwt_secret_seguro>
ENCRYPTION_KEY=<tu_encryption_key_32_bytes>

# Rutas de credenciales JSON
FIREBASE_ADMIN_CREDENTIALS_JSON_PATH=FIREBASE_ADMIN_CREDENTIALS_JSON_PATH
GOOGLE_OAUTH_JSON_PATH=GOOGLE_OAUTH_JSON_PATH

# Almacenamiento de respaldos cifrados
BACKUP_STORAGE_PATH=BACKUP_STORAGE_PATH

# Servidor SMTP (Opcional en desarrollo: si se deja vacío, los enlaces/códigos se muestran en consola)
SMTP_HOST=SMTP_HOS
SMTP_PORT=SMTP_PORT
SMTP_USER=SMTP_USER
SMTP_PASS=SMTP_PASS
```

> **Nota importante sobre `MONGODB_URI`:** No coloques comillas dobles (`"..."`) alrededor del valor en el archivo `.env`, ya que Spring Boot interpreta las comillas literalmente como parte del esquema de conexión.

---

## Cómo Compilar y Ejecutar el Proyecto

Abre una terminal PowerShell en la carpeta `YolkaApi`:

```powershell
cd \YolkaApi
```

### 1. Ejecutar el servidor (Modo normal)
```powershell
.\mvnw.cmd spring-boot:run
```

### 2. Ejecutar limpiando caché de compilación (Recomendado tras editar modelos)
Si utilizas VS Code o un IDE cuyo compilador en segundo plano modifique `target/classes` sin procesar las anotaciones de **Lombok** (provocando el error `ClassFormatError: Invalid method Code length 0 in class file ...Builder`), ejecuta siempre con `clean`:

```powershell
.\mvnw.cmd clean spring-boot:run
```

### 3. Solo compilar para verificar errores
```powershell
.\mvnw.cmd clean compile
```

---

## Cómo Probar los Endpoints

El servidor inicia por defecto en `http://localhost:8080`. Al arrancar por primera vez, el `DataSeeder` poblará automáticamente en MongoDB Atlas:
- **6 Clínicas Veterinarias** (región de Colima y Villa de Álvarez, con horarios 24h y urgencias).
- **8 Consejos de Cuidado (`Tips`)** clasificados por categoría y especie.

### Opción A: Colección de Postman (Recomendado)
1. Abre **Postman** y haz clic en **Import**.
2. Selecciona el archivo [`YolkaApi.postman_collection.json`](./YolkaApi.postman_collection.json) ubicado en esta carpeta.
3. Ejecuta primero **1. Auth -> Registrar Usuario** o **Iniciar Sesión**.
4. La colección incluye un script automático que guarda el `token` (`x-token`) y el `petId` en las variables de la colección para que puedas probar todos los demás endpoints con un solo clic.

### Opción B: VS Code REST Client
- Abre el archivo [`test-api.http`](./test-api.http) en VS Code (con la extensión *REST Client*) y haz clic en `Send Request` sobre cada bloque.

---

## Resumen de Endpoints Principales (`/api/v1`)

| Módulo | Método | Endpoint | Requiere `x-token` | Descripción |
| :--- | :---: | :--- | :---: | :--- |
| **Auth** | `POST` | `/api/v1/auth/register` | No | Registro con correo/contraseña y envío de enlace/código de confirmación |
| **Auth** | `GET` | `/api/v1/auth/confirm-account?token=...` | No | Confirmación de cuenta mediante enlace desde el correo (devuelve HTML) |
| **Auth** | `POST` | `/api/v1/auth/confirm-account` | No | Confirmación de cuenta mediante código de 6 dígitos |
| **Auth** | `POST` | `/api/v1/auth/request-confirmation` | No | Reenvío de correo de confirmación |
| **Auth** | `POST` | `/api/v1/auth/login` | No | Inicio de sesión local (`BCrypt` + emisión de JWT) |
| **Auth** | `POST` | `/api/v1/auth/google` | No | Inicio de sesión / registro con Google (`idToken`) |
| **Auth** | `POST` | `/api/v1/auth/recover-password` | No | Envío de código de 6 dígitos para recuperar contraseña |
| **Auth** | `POST` | `/api/v1/auth/reset-password` | No | Restablecimiento de contraseña con código |
| **Usuarios** | `GET` | `/api/v1/users/me` | Sí | Obtener perfil y preferencias del usuario autenticado |
| **Usuarios** | `PUT` | `/api/v1/users/me` | Sí | Actualizar perfil, preferencias o token FCM |
| **Mascotas** | `GET` | `/api/v1/pets` | Sí | Listar mascotas activas del usuario (`deleted: false`) |
| **Mascotas** | `POST` | `/api/v1/pets` | Sí | Crear mascota con esquema inicial de vacunas sugerido |
| **Mascotas** | `GET` | `/api/v1/pets/{petId}/dashboard` | Sí | Dashboard de mascota con conteos y próximos eventos |
| **Salud** | `POST` | `/api/v1/pets/{petId}/vaccines` | Sí | Registrar vacuna en la cartilla |
| **Salud** | `POST` | `/api/v1/pets/{petId}/dewormings` | Sí | Registrar desparasitación interna o externa |
| **Salud** | `POST` | `/api/v1/pets/{petId}/consultations` | Sí | Registrar consulta médica y tratamiento |
| **Higiene** | `POST` | `/api/v1/pets/{petId}/hygiene` | Sí | Registrar tarea de higiene con recurrencia automática |
| **Peso** | `POST` | `/api/v1/pets/{petId}/weights` | Sí | Registrar medición de peso para historial/gráfica |
| ** Veterinarias** | `GET` | `/api/v1/vets` | No | Directorio de clínicas veterinarias (filtros `emergency`, `open24Hours`, `search`) |
| **Consejos** | `GET` | `/api/v1/tips` | No | Consejos de cuidado (filtros `category`, `species`) |
| **Notificaciones** | `GET` | `/api/v1/notifications` | Sí | Listar recordatorios y notificaciones |
| **Notificaciones** | `POST` | `/api/v1/notifications` | Sí | Crear recordatorio y disparar push FCM |
| **Respaldos** | `POST` | `/api/v1/backups/export` | Sí | Generar respaldo cifrado `.yolka` (`AES-256-GCM`) |
| **Respaldos** | `POST` | `/api/v1/backups/restore/{id}` | Sí | Descifrar y restaurar respaldo `.yolka` |

---

## Arquitectura de Datos en MongoDB

- **IDs Únicos (`UUID v4`)**: Generados mediante `UuidGeneratorHelper` tanto para documentos raíz como para subdocumentos embebidos (vacunas, desparasitaciones, consultas, higiene y pesos).
- **Soft Delete (Borrado Lógico)**: Todos los documentos implementan `deleted: false` y `deletedAt: Instant`, preservando la integridad histórica sin eliminar registros físicamente.
- **Indexación Automática**: Configurada con `@Indexed(unique = true)` y `@CompoundIndexes` (`spring.data.mongodb.auto-index-creation=true`).
