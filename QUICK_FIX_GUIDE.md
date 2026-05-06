# 🔧 GUÍA DE CORRECCIÓN RÁPIDA - SOLUCIONES CÓDIGO

> **Audiencia:** Desarrollador de HashPass  
> **Tiempo estimado:** 2-3 horas  
> **Urgencia:** INMEDIATA - ANTES DE CUALQUIER DEPLOY A PRODUCCIÓN



## 2️⃣ JWT TOKEN PROVIDER

### Paso 2.1: Actualizar JwtTokenProvider para usar variable de entorno

**Archivo:** `src/main/java/com/hashpass/security/jwt/JwtTokenProvider.java`

```java
package com.hashpass.security.jwt;

import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private final SecretKey jwtSecret;
    private final JwtParser jwtParser;
    
    @Value("${jwt.expiration:3600000}")
    private long tokenExpiration;

    // ✅ NUEVO: Inyectar secret desde propiedades
    public JwtTokenProvider(@Value("${jwt.secret}") String secretKeyString) {
        // Decodificar desde Base64
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyString);
        this.jwtSecret = Keys.hmacShaKeyFor(decodedKey);
        this.jwtParser = Jwts.parser()
            .verifyWith(jwtSecret)
            .build();
    }

    public String tokenStringFromHeaders(HttpServletRequest req) {
        String bearerToken = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken == null) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        if (!bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header does not start with Bearer: " + bearerToken);
        }
        return bearerToken.substring(7);
    }

    private String tokenStringFromCookies(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalArgumentException("No cookies found in request");
        }

        for (Cookie cookie : cookies) {
            if (TokenType.ACCESS.cookieName.equals(cookie.getName())) {
                String accessToken = cookie.getValue();
                if (accessToken == null) {
                    throw new IllegalArgumentException("Cookie %s has null value".formatted(TokenType.ACCESS.cookieName));
                }
                return accessToken;
            }
        }
        throw new IllegalArgumentException("No access token cookie found in request");
    }

    public Claims validateToken(HttpServletRequest req, boolean fromCookie) {
        var token = fromCookie ?
                tokenStringFromCookies(req) :
                tokenStringFromHeaders(req);
        return validateToken(token);
    }

    public Claims validateToken(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(TokenType.ACCESS, userDetails).compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        var token = buildToken(TokenType.REFRESH, userDetails);
        return token.compact();
    }

    private JwtBuilder buildToken(TokenType tokenType, UserDetails userDetails) {
        var currentDate = new Date();
        var expiryDate = Date.from(new Date().toInstant().plus(tokenType.duration));
        return Jwts.builder()
                .claim("roles", userDetails.getAuthorities())
                .claim("type", tokenType.name())
                .subject(userDetails.getUsername())
                .issuedAt(currentDate)
                .expiration(expiryDate)
                .signWith(jwtSecret, SignatureAlgorithm.HS256);
    }
}
```

**Generar JWT_SECRET seguro:**
```bash
# Linux/Mac
openssl rand -base64 64 | tr -d '\n'

# O desde Java
java -jar -cp ".:spring-security-core*.jar" -c "
    import javax.crypto.KeyGenerator;
    KeyGenerator kg = KeyGenerator.getInstance(\"HmacSHA256\");
    kg.init(256);
    System.out.println(java.util.Base64.getEncoder().encodeToString(kg.generateKey().getEncoded()));
"

# Copiar output a .env.local
# JWT_SECRET=<output aquí>
```

---

## 3️⃣ IMPLEMENTAR RATE LIMITING

### Paso 3.1: Añadir dependencia en pom.xml

```xml
<dependency>
    <groupId>io.github.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>7.6.0</version>
</dependency>
```

### Paso 3.2: Crear anotación @RateLimited

**Crear archivo:** `src/main/java/com/hashpass/security/RateLimited.java`

```java
package com.hashpass.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    String key() default "ip";
    int requests() default 5;
    int minutes() default 15;
}
```

### Paso 3.3: Crear interceptor

**Crear archivo:** `src/main/java/com/hashpass/security/RateLimitInterceptor.java`

```java
package com.hashpass.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        RateLimited annotation = method.getMethodAnnotation(RateLimited.class);
        
        if (annotation == null) {
            return true;
        }

        String key = getKey(annotation.key(), request);
        Bucket bucket = cache.computeIfAbsent(key, 
            k -> createBucket(annotation.requests(), annotation.minutes()));

        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("SECURITY_EVENT=RATE_LIMIT_EXCEEDED key={} ip={}", 
            key, request.getRemoteAddr());
        
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"Too many requests. Try again later.\"}");
        return false;
    }

    private Bucket createBucket(int requests, int minutes) {
        Bandwidth limit = Bandwidth.classic(requests, 
            Refill.intervally(requests, Duration.ofMinutes(minutes)));
        return Bucket4j.builder()
            .addLimit(limit)
            .build();
    }

    private String getKey(String keyType, HttpServletRequest request) {
        if ("user".equals(keyType)) {
            var principal = request.getUserPrincipal();
            if (principal != null) {
                return "user:" + principal.getName();
            }
        }
        // Default: por IP
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

Corregir imports:
```java
import java.time.Duration;
```

### Paso 3.4: Registrar interceptor

**Editar:** `src/main/java/com/hashpass/config/WebConfig.java` (crear si no existe)

```java
package com.hashpass.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hashpass.security.RateLimitInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }
}
```

### Paso 3.5: Aplicar en endpoints críticos

**Editar:** `src/main/java/com/hashpass/controller/rest/UserRestController.java`

```java
@PostMapping("/login")
@RateLimited(requests = 5, minutes = 15)  // ✅ AÑADIR
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // ... código existente ...
}

@PostMapping("/register")
@RateLimited(requests = 3, minutes = 60)  // ✅ AÑADIR
public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
    // ... código existente ...
}
```

**Editar:** `src/main/java/com/hashpass/controller/web/UserController.java`

```java
@PostMapping("/register")
@RateLimited(requests = 3, minutes = 60)  // ✅ AÑADIR
public String registerPost(/* parámetros existentes */) {
    // ... código existente ...
}
```

---



## 5️⃣ MEJORAR MANEJO DE ERRORES

### Paso 5.1: Crear GlobalExceptionHandler

**Crear archivo:** `src/main/java/com/hashpass/controller/GlobalExceptionHandler.java`

```java
package com.hashpass.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        // ✅ Loguear el error completo internamente
        log.error("Illegal argument error", ex);
        
        // ✅ Devolver mensaje genérico al cliente
        return ResponseEntity.badRequest()
            .body(Map.of("message", "Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(
            Exception ex, 
            HttpServletRequest request) {
        
        // ✅ Loguear completo internamente
        log.error("Unexpected error in {}", request.getRequestURI(), ex);
        
        // ✅ Devolver genérico al cliente
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("message", "An error occurred processing your request"));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        
        log.warn("Access denied", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("message", "Access denied"));
    }
}
```

---

## 6️⃣ MEJORAR VALIDACIÓN SSRF

### Editar HtmlSanitizer.java

**Archivo:** `src/main/java/com/hashpass/security/HtmlSanitizer.java`

En el método `isBlockedHost()`:

```java
private boolean isBlockedHost(String host) {
    String normalizedHost = IDN.toASCII(host.trim().toLowerCase(Locale.ROOT));
    if (normalizedHost.isBlank()) {
        return true;
    }

    // ✅ MEJORADO - Bloquear todos los patrones peligrosos
    if ("localhost".equals(normalizedHost)
            || normalizedHost.endsWith(".localhost")
            || "127.0.0.1".equals(normalizedHost)
            || "0.0.0.0".equals(normalizedHost)
            || "::1".equals(normalizedHost)  // IPv6 loopback
            || normalizedHost.equals("metadata.google.internal")
            || normalizedHost.equals("169.254.169.254")) {  // AWS metadata
        return true;
    }

    try {
        InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
        for (InetAddress address : addresses) {
            byte[] addr = address.getAddress();
            
            // ✅ Detectar redes privadas por octetos
            if (addr.length == 4) {  // IPv4
                if (addr[0] == 10 ||  // 10.0.0.0/8
                    (addr[0] == 172 && addr[1] >= 16 && addr[1] <= 31) ||  // 172.16.0.0/12
                    (addr[0] == 192 && addr[1] == 168) ||  // 192.168.0.0/16
                    (addr[0] == 169 && addr[1] == 254) ||  // 169.254.0.0/16 (link-local)
                    addr[0] == 127) {  // 127.0.0.0/8 (loopback)
                    return true;
                }
            }
            
            // ✅ Detectar usando métodos de InetAddress
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return true;
            }
        }
    } catch (UnknownHostException ex) {
        // ✅ Si no se puede resolver, bloquear por seguridad
        log.warn("Unable to resolve host: {} - blocking as SSRF protection", host);
        return true;
    }

    return false;
}
```

---

## ✅ CHECKLIST DE APLICACIÓN

Después de hacer todos los cambios:

```bash
# 1. Compilar
mvn clean compile
# ✓ Verificar que NO hay errores

# 2. Testear localmente
mvn spring-boot:run
# ✓ Verificar que inicia sin errores
# ✓ Verificar que logs NO muestran credenciales

# 3. Test de login
curl -X POST http://localhost:8443/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@hashpass.local","password":"contraseña"}'
# ✓ Debería retornar 200 o 401, no exponer errores internos

# 4. Test de rate limiting
for i in {1..10}; do 
  curl -X POST http://localhost:8443/api/v1/users/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"test"}'
done
# ✓ Después de 5 intentos, debería retornar 429

# 5. Verificar Git
git log --all --grep="password" --oneline
# ✓ NO debería haber commits con passwords

# 6. Revisar archivos secretos
git ls-files | grep -E "\.env|\.jks|application-prod"
# ✓ NO debería retornar nada
```

---

## 🚀 DEPLOY A PRODUCCIÓN

```bash
# 1. Compilar JAR
mvn clean package

# 2. Ejecutar con variables de entorno
java \
  -Dspring.config.location=application.properties \
  -Dspring.datasource.url=${DB_URL} \
  -Dspring.datasource.username=${DB_USERNAME} \
  -Dspring.datasource.password=${DB_PASSWORD} \
  -Djwt.secret=${JWT_SECRET} \
  -Dserver.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD} \
  -Dserver.ssl.key-password=${SSL_KEY_PASSWORD} \
  -jar target/hashpass.jar

# O usar Docker:
docker build -t hashpass:latest .
docker run -e DB_PASSWORD=$DB_PASSWORD \
           -e JWT_SECRET=$JWT_SECRET \
           -e SSL_KEYSTORE_PASSWORD=$SSL_KEYSTORE_PASSWORD \
           hashpass:latest
```

---

**¡Las correcciones completadas!** La aplicación estará lista para producción.
