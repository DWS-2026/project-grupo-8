# Configuración de JWT en Proyecto Grupo 8

## Descripción General

El proyecto 8 ha sido configurado con autenticación JWT (JSON Web Tokens) basada en la configuración del proyecto 0. Esto permite que los endpoints de la API se protejan con tokens de autenticación y se puedan testear de forma segura.

## Cambios Realizados

### 1. **Servicio UserLoginService**
Se ha actualizado el servicio `UserLoginService` para aceptar email y password directamente (no un objeto LoginRequest), generando tokens JWT automáticamente.

**Ubicación:** `com.hashpass.security.jwt.UserLoginService`

**Métodos principales:**
- `login(response, email, password)`: Autentica el usuario y devuelve tokens JWT
- `refresh(response, refreshToken)`: Refresca el token de acceso
- `logout(response)`: Cierra la sesión eliminando los tokens

### 2. **Controlador UserRestController**
Se han añadido tres nuevos endpoints:

#### 2.1 Login (`POST /api/v1/users/login`)
```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
```

**Respuesta exitosa (200):**
```json
{
  "status": "SUCCESS",
  "message": "Auth successful. Tokens are created in cookie.",
  "error": null
}
```

Las cookies `AuthToken` y `RefreshToken` se establecen automáticamente.

#### 2.2 Refresh Token (`POST /api/v1/users/refresh`)
```bash
curl -X POST http://localhost:8080/api/v1/users/refresh \
  -H "Cookie: RefreshToken=<your-refresh-token>"
```

**Respuesta exitosa (200):**
```json
{
  "status": "SUCCESS",
  "message": "Auth successful. Tokens are created in cookie.",
  "error": null
}
```

Se genera un nuevo token de acceso.

#### 2.3 Logout (`POST /api/v1/users/logout`)
```bash
curl -X POST http://localhost:8080/api/v1/users/logout \
  -H "Cookie: AuthToken=<your-access-token>"
```

**Respuesta exitosa (200):**
```json
{
  "message": "Logout successful"
}
```

### 3. **Seguridad Web (WebSecurityConfig)**
El `apiFilterChain` está configurado con:
- **Rutas públicas:** `/api/v1/users/login` y `/api/v1/users/register`
- **Rutas protegidas:** Requieren rol `USER` o `ADMIN` según el recurso

### 4. **Validación JWT**
El `JwtRequestFilter` valida automáticamente los tokens en cada solicitud a endpoints protegidos.

**Duración de tokens:**
- **AccessToken (AuthToken):** 5 minutos
- **RefreshToken:** 7 días

## Cómo Testear los Endpoints

### 1. **Con Postman**

#### Paso 1: Login
1. Abre Postman
2. Crea una nueva solicitud `POST`
3. URL: `http://localhost:8080/api/v1/users/login`
4. Headers: `Content-Type: application/json`
5. Body (raw JSON):
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
6. Haz clic en "Send"
7. En la respuesta, verifica que recibes `status: SUCCESS`
8. Las cookies se establecen automáticamente en la pestaña "Cookies"

#### Paso 2: Acceder a un endpoint protegido
1. Crea una nueva solicitud `GET`
2. URL: `http://localhost:8080/api/v1/users/me`
3. Las cookies se envían automáticamente (Postman las gestiona)
4. Haz clic en "Send"
5. Deberías recibir los datos del usuario autenticado

#### Paso 3: Refresh Token
1. Crea una nueva solicitud `POST`
2. URL: `http://localhost:8080/api/v1/users/refresh`
3. Las cookies se envían automáticamente
4. Haz clic en "Send"
5. Se genera un nuevo AccessToken

#### Paso 4: Logout
1. Crea una nueva solicitud `POST`
2. URL: `http://localhost:8080/api/v1/users/logout`
3. Haz clic en "Send"
4. Los tokens se eliminan

### 2. **Con cURL**

```bash
# 1. Login
TOKEN_RESPONSE=$(curl -c cookies.txt -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}')

# 2. Acceder a un endpoint protegido
curl -b cookies.txt http://localhost:8080/api/v1/users/me

# 3. Refresh token
curl -b cookies.txt -X POST http://localhost:8080/api/v1/users/refresh

# 4. Logout
curl -b cookies.txt -X POST http://localhost:8080/api/v1/users/logout
```

### 3. **Con el archivo Postman Collection**

El proyecto incluye un archivo `api.postman_collection.json` que puede importarse en Postman para facilitar el testing de los endpoints.

## Estructura de Carpetas JWT

```
src/main/java/com/hashpass/security/jwt/
├── AuthResponse.java          # Respuesta de autenticación
├── JwtRequestFilter.java      # Filtro que valida tokens
├── JwtTokenProvider.java      # Generador y validador de tokens
├── LoginRequest.java          # DTO de solicitud de login
├── TokenType.java             # Enum de tipos de token
├── UnauthorizedHandlerJwt.java # Manejador de errores JWT
└── UserLoginService.java      # Servicio de login con JWT
```

## Seguridad

- **Tokens almacenados en cookies HttpOnly:** Protegidos contra XSS
- **CSRF deshabilitado para API:** Necesario para APIs REST
- **Sesiones stateless:** La API no mantiene estado en el servidor
- **Validación de roles:** Los endpoints verifican permisos basados en roles

## Endpoints Protegidos Ejemplo

Los siguientes endpoints requieren autenticación JWT:

```
GET /api/v1/users/me                    # Ver usuario actual
GET /api/v1/users/{id}                  # Ver datos de usuario
GET /api/v1/credentials/**              # Ver credenciales
POST /api/v1/credentials/**             # Crear credenciales
PUT /api/v1/credentials/**              # Actualizar credenciales
DELETE /api/v1/credentials/**           # Eliminar credenciales
```

## Troubleshooting

### 401 Unauthorized
- El token ha expirado → Usa `/api/v1/users/refresh`
- El token no es válido → Vuelve a hacer login

### 403 Forbidden
- El usuario no tiene el rol requerido
- Verifica que el usuario sea `USER` o `ADMIN`

### Cookie no se envía
- En Postman: Asegúrate de que Postman esté habilitado para gestionar cookies
- En cURL: Usa `-b cookies.txt` para enviar cookies

## Referencias

- **JWT Tokens:** https://tools.ietf.org/html/rfc7519
- **Spring Security:** https://spring.io/projects/spring-security
- **JJWT Library:** https://github.com/jwtk/jjwt
