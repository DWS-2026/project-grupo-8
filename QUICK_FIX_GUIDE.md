# 🔧 GUÍA DE CORRECCIÓN RÁPIDA - SOLUCIONES CÓDIGO

> **Audiencia:** Desarrollador de HashPass  
> **Tiempo estimado:** 2-3 horas  
> **Urgencia:** INMEDIATA - ANTES DE CUALQUIER DEPLOY A PRODUCCIÓN


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

<!-- Sección de mejora SSRF eliminada por petición del desarrollador; la lógica relevante fue aplicada directamente en el código fuente. -->


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
  -Dserver.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD} \
  -Dserver.ssl.key-password=${SSL_KEY_PASSWORD} \
  -jar target/hashpass.jar

# O usar Docker:
docker build -t hashpass:latest .
docker run -e DB_PASSWORD=$DB_PASSWORD \
           -e SSL_KEYSTORE_PASSWORD=$SSL_KEYSTORE_PASSWORD \
           hashpass:latest
```

---

**¡Las correcciones completadas!** La aplicación estará lista para producción.
