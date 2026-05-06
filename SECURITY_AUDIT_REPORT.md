# 🔐 REPORTE EXHAUSTIVO DE AUDITORÍA DE SEGURIDAD - HashPass

**Fecha:** 2024  
**Auditor:** Análisis Automático Avanzado  
**Nivel de Criticidad:** ALTO - Múltiples vulnerabilidades de seguridad detectadas  
**Estado General:** ⚠️ REQUIERE CORRECCIÓN INMEDIATA

---

## 📋 ÍNDICE EJECUTIVO

| Categoría | Críticas | Altas | Medias | Bajas |
|-----------|----------|-------|--------|-------|
| **JWT/Autenticación** | 0 | 1 | 1 | 0 |
| **Gestión de Errores** | 0 | 2 | 1 | 0 |
| **Logging de Seguridad** | 1 | 1 | 0 | 0 |
| **Validación de Entrada** | 0 | 1 | 2 | 1 |
| **Rate Limiting** | 0 | 1 | 0 | 0 |
| **TOTAL** | **4** | **6** | **4** | **1** |

---

## 🔴 VULNERABILIDADES CRÍTICAS (Nivel 1 - Corrección Inmediata)


## 🟠 VULNERABILIDADES ALTAS (Nivel 2)

### 5. **Clave Secreta JWT Regenerada en Cada Restart**

**Archivo:** [src/main/java/com/hashpass/security/jwt/JwtTokenProvider.java](src/main/java/com/hashpass/security/jwt/JwtTokenProvider.java)  
**Línea:** 20  
**Severidad:** 🟠 **ALTA**  
**CVSS Score:** 7.5

#### Problema
```java
// ❌ INSEGURO - Nueva clave en cada reinicio
private final SecretKey jwtSecret = Jwts.SIG.HS256.key().build();
```

Cada instancia genera una clave nueva aleatoria. Esto invalida todos los tokens existentes cuando la aplicación reinicia.

#### Impacto
- **Tokens válidos invalidan después de restart**
- **En sistemas distribuidos, tokens de un servidor rechazan en otros**
- **Mala experiencia de usuario (sesiones perdidas)**
- **Inconsistencia entre instancias**

#### Solución
```java
@Component
public class JwtTokenProvider {
    
    private final SecretKey jwtSecret;
    private final JwtParser jwtParser;

    public JwtTokenProvider(@Value("${jwt.secret}") String secretKey) {
        // ✅ CORRECTO - Usar secreto configurado
        this.jwtSecret = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(secretKey)
        );
        this.jwtParser = Jwts.parser()
            .verifyWith(jwtSecret)
            .build();
    }
    
    // Resto del código...
}
```

**En application.properties:**
```properties
# ✅ Usar secret fuerte desde variable de entorno
jwt.secret=${JWT_SECRET:base64_encoded_secret}
jwt.expiration=${JWT_EXPIRATION:3600000}
```

---



### 8. **No Hay Rate Limiting en Endpoints de Autenticación**

**Archivo:** [src/main/java/com/hashpass/controller/rest/UserRestController.java](src/main/java/com/hashpass/controller/rest/UserRestController.java)  
**Línea:** 49  
**Severidad:** 🟠 **ALTA**  
**CVSS Score:** 7.1

#### Problema
```java
// ❌ INSEGURO - Sin rate limit en login/register
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // Sin protección contra fuerza bruta
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, request.password()));
    // Attacker puede probar millones de passwords
}

@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
    // Sin rate limit - posible spam de registros
}
```

#### Impacto
- **Ataques de fuerza bruta sin limitar**
- **Bypass del account lockout**
- **Spam de registros**
- **DoS lógico**

#### Solución - Implementar Rate Limiting

**1. Añadir dependencia:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

**2. Crear anotación personalizada:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    String key() default "ip";
    int requests() default 5;
    int minutes() default 15;
}
```

**3. Implementar interceptor:**
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        
        HandlerMethod method = (HandlerMethod) handler;
        RateLimited annotation = method.getMethodAnnotation(RateLimited.class);
        if (annotation == null) return true;
        
        String key = getKey(annotation.key(), request);
        Bucket bucket = cache.computeIfAbsent(key, 
            k -> createBucket(annotation.requests(), annotation.minutes()));
        
        if (bucket.tryConsume(1)) {
            return true;
        }
        
        response.setStatus(429);
        response.getWriter().write("Too many requests");
        return false;
    }
    
    private Bucket createBucket(int requests, int minutes) {
        return Bucket.builder()
            .addLimit(Limit.of(requests, Bandwidth.simple(requests, 
                Duration.ofMinutes(minutes))))
            .build();
    }
    
    private String getKey(String keyType, HttpServletRequest request) {
        if ("ip".equals(keyType)) {
            return request.getRemoteAddr();
        }
        return request.getRemoteAddr();
    }
}
```

**4. Usar en endpoints:**
```java
@PostMapping("/login")
@RateLimited(requests = 5, minutes = 15)
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // ... código ...
}

@PostMapping("/register")
@RateLimited(requests = 3, minutes = 60)
public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
    // ... código ...
}
```

---

## 🟡 VULNERABILIDADES MEDIAS (Nivel 3)

### 9. **Validación de URL Insuficiente - SSRF Parcial**

**Archivo:** [src/main/java/com/hashpass/security/HtmlSanitizer.java](src/main/java/com/hashpass/security/HtmlSanitizer.java)  
**Línea:** 100-118  
**Severidad:** 🟡 **MEDIA**  
**CVSS Score:** 5.3

#### Problema - Falsa Sensación de Seguridad
```java
// Parcialmente seguro pero con gaps
private boolean isBlockedHost(String host) {
    // ✅ Detecta: localhost, 127.0.0.1, 192.168.x.x
    // ✅ Detecta: metadata.google.internal
    
    // ❌ NO detecta: 
    // - 169.254.x.x (link-local - puede ser Azure metadata)
    // - Nombres con puntuación DNS (xn-- IDN)
    // - URLs encoded (http://192%2E168%2E1%2E1)
    // - Redirecciones HTTP (redirect a localhost)
}
```

#### Impacto Limitado
- **SSRF débil posible con técnicas avanzadas**
- **Acceso a servicios internos via redirects**

#### Solución Mejorada
```java
private boolean isBlockedHost(String host) {
    String normalizedHost = IDN.toASCII(host.trim().toLowerCase(Locale.ROOT));
    if (normalizedHost.isBlank()) {
        return true;
    }

    // ✅ MEJORADO - Bloquear patrones peligrosos
    if ("localhost".equals(normalizedHost)
            || normalizedHost.endsWith(".localhost")
            || "127.0.0.1".equals(normalizedHost)
            || "0.0.0.0".equals(normalizedHost)
            || normalizedHost.equals("metadata.google.internal")
            || normalizedHost.equals("169.254.169.254")) {  // ✅ AWS metadata
        return true;
    }

    try {
        InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
        for (InetAddress address : addresses) {
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.getHostAddress().startsWith("169.254")  // ✅ Link-local
                    || address.getHostAddress().startsWith("10.")
                    || address.getHostAddress().startsWith("172.16.")
                    || address.getHostAddress().startsWith("192.168.")) {
                return true;
            }
        }
    } catch (UnknownHostException ex) {
        return true;  // Cuando falla resolución, bloquear por seguridad
    }

    return false;
}
```

---

### 10. **Acceso a GET /api/v1/images sin Restricción Completa**

**Archivo:** [src/main/java/com/hashpass/controller/rest/ImageRestController.java](src/main/java/com/hashpass/controller/rest/ImageRestController.java)  
**Línea:** 47, 60  
**Severidad:** 🟡 **MEDIA**  
**CVSS Score:** 5.7

#### Problema
```java
@GetMapping("/profiles")  // Público
public ResponseEntity<?> getAllProfiles() {
    return ResponseEntity.ok(imageService.findAll("profiles"));
}

@GetMapping("/credentials")  // Público
public ResponseEntity<?> getAllCredentials() {
    return ResponseEntity.ok(imageService.findAll("credentials"));
}
```

En WebSecurityConfig:
```java
// ❌ Estos endpoints son permitAll
.requestMatchers(HttpMethod.GET, "/api/v1/images/profiles").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/v1/images/credentials").hasRole("ADMIN")
```

Requieren ADMIN pero no hay validación real de quién es ADMIN desde JWT.

#### Impacto
- **Posible enumeración de imágenes**
- **Filtración de datos si no hay JWT**

#### Solución
Asegurar que ADMIN_ROLE se valida desde JWT cuando JWT esté activo:
```java
// Cuando JWT filter esté habilitado, esto funcionará correctamente
.requestMatchers(HttpMethod.GET, "/api/v1/images/profiles").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/v1/images/credentials").hasRole("ADMIN")
```

---

### 11. **Validación de Teléfono Muy Restrictiva**

**Archivo:** [src/main/java/com/hashpass/security/HtmlSanitizer.java](src/main/java/com/hashpass/security/HtmlSanitizer.java)  
**Línea:** 49-55  
**Severidad:** 🟡 **MEDIA**  
**CVSS Score:** 4.2

#### Problema
```java
// ❌ Regex muy restrictiva
public String sanitizePhoneNumber(String input) {
    String sanitizedPhone = sanitizeOptionalPlainText(input);
    if (sanitizedPhone == null) {
        return null;
    }
    // Solo permite [0-9+()\\s-]{6,32} - excluye caracteres válidos
    if (!sanitizedPhone.matches("[0-9+()\\s-]{6,32}")) {
        return null;  // Rechaza números válidos
    }
    return sanitizedPhone;
}
```

Números válidos rechazados: `+34-600-111-111`, `+34 (600) 111111`

#### Impacto
- **Experiencia de usuario pobre**
- **Números internacionales rechazados injustamente**

#### Solución
```java
public String sanitizePhoneNumber(String input) {
    if (input == null || input.isBlank()) {
        return null;
    }
    
    String sanitized = input.trim();
    
    // ✅ MEJORADO - Regex más flexible
    // Permite: +34 600 111 111, +34-600-111-111, (34) 600-111, etc.
    if (!sanitized.matches("[+]?[0-9\\s()\\-.]{6,}")) {
        return null;
    }
    
    // Validar longitud sin caracteres especiales
    String digitsOnly = sanitized.replaceAll("[^0-9+]", "");
    if (digitsOnly.length() < 6 || digitsOnly.length() > 15) {
        return null;  // E.164 standard
    }
    
    return sanitized;
}
```

---

## 📌 VULNERABILIDADES BAJAS (Nivel 4)


## 🛡️ ANÁLISIS DE ARCHIVOS DESCONECTADOS O SIN SEGURIDAD

### A. Carpeta `/jwt` - Estado Incompleto

**Ubicación:** [src/main/java/com/hashpass/security/jwt/](src/main/java/com/hashpass/security/jwt/)

| Archivo | Estado | Notas |
|---------|--------|-------|
| `JwtTokenProvider.java` | ⚠️ Activo pero limitado | Clave regenerada cada restart |
| `JwtRequestFilter.java` | ✅ **ACTIVO** | Integrado en la cadena REST |
| `UserLoginService.java` | ⚠️ Activo | Solo para manual login, no usado en general |
| `TokenType.java` | ✅ OK | Define tipos de tokens |

**Conclusión:** JWT está técnicamente implementado y el filtro está **activo** en la cadena de seguridad REST. Queda pendiente la gestión persistente del secreto.

---

### B. Endpoints Sin Protección Adecuada

```
GET  / (público) ✅
GET  /login (público) ✅
GET  /register (público) ✅
GET  /password-login (público) ✅
GET  /plan/** (público) ✅
GET  /reviews/** (público) ✅  [Nota: Debería mostrar solo resúmenes]
GET  /api/v1/images/profiles (ADMIN) ⚠️
GET  /api/v1/images/credentials (ADMIN) ⚠️
GET  /api/v1/reviews (público) ✅  [Pero pueden contenerdatos sensibles]
```

---

## 🔧 CÓDIGO DE CORRECCIÓN POR PRIORIDAD



### PASO 2: JWT Filter

El filtro JWT ya está activo en la cadena REST, así que este paso no requiere cambios adicionales.

---

### PASO 3: Implementar Rate Limiting (30 minutos)

Ver sección anterior (punto 8 - Solución Rate Limiting)

---


---

## 📋 CHECKLIST DE CORRECCIÓN

```
CRÍTICAS (Debe hacerse HOY):
☐ Remover contraseña MySQL de application.properties
☐ Remover contraseña admin de DatabaseInitializer.java  
☐ Remover contraseña keystore de application.properties
☐ Cambiar credenciales en BD
☐ Limpiar Git history
☐ Habilitar JWT filter en WebSecurityConfig
☐ Externalizar JWT secret

ALTAS (Esta semana):
☐ Implementar Rate Limiting en login/register
☐ Cambiar logging a nivel WARN
☐ Genericizar mensajes de error
☐ Crear SecurityAuditLogger
☐ Generar nuevo keystore con contraseña fuerte
☐ Extraer JWT secret a aplicación

MEDIAS (Próxima semana):
☐ Mejorar validación SSRF
☐ Revisar endpoints de imágenes
☐ Mejorar validación de teléfonos
☐ Implementar CORS seguro

BAJAS (Próximas semanas):
☐ Externalizar keystore en producción
☐ Implementar CSP headers
☐ Añadir HSTS headers
☐ Implementar API versioning
☐ Documentar API security
```

---

## 📞 RECOMENDACIONES FINALES

### Herramientas de Testing
```bash
# Validar headers de seguridad
curl -I https://localhost:8443

# Verificar certificado SSL
openssl s_client -connect localhost:8443

# Test de fuerza bruta (si implementas rate limiting)
for i in {1..10}; do curl -X POST http://localhost:8443/api/v1/users/login; done

# OWASP ZAP scanning
zaproxy --cmd -quickurl http://localhost:8443
```

### Best Practices
1. ✅ Usar HTTPS en todo (actualmente lo haces)
2. ✅ Implementar CORS restrictivo
3. ✅ Añadir security headers:
   ```
   X-Content-Type-Options: nosniff
   X-Frame-Options: DENY
   X-XSS-Protection: 1; mode=block
   Strict-Transport-Security: max-age=31536000
   ```
4. ✅ Implementar WAF (Web Application Firewall)
5. ✅ Realizar scans de seguridad regulares

---

**Resumen:** HashPass tiene arquitectura segura pero **credenciales expuestas** y la **gestión persistente del secreto JWT** requieren corrección inmediata antes de deploy a producción.
