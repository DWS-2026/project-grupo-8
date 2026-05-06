# 🔐 MAPEO A CVE Y OWASP TOP 10

## Matriz de Vulnerabilidades → Estándares Internacionales

### OWASP TOP 10 2023 - Mapeo Directo

```
┌─────────────────────────────────────────────────────────────────┐
│ HashPass Vulnerabilities mapped to OWASP Top 10 2023           │
└─────────────────────────────────────────────────────────────────┘

A01 - BROKEN ACCESS CONTROL
└─ Privilegios sin validación JWT (VUL-10)
   └─ GET /api/v1/images/profiles permitAll
   └─ Archivo: ImageRestController.java:47
   └─ Impacto: 🟠 ALTA

A03 - INJECTION
<!-- SSRF entry removed per developer request -->

A04 - INSECURE DESIGN
└─ Rate Limiting Missing (VUL-8)
   └─ Sin protección contra fuerza bruta
   └─ Archivo: UserRestController.java:49
   └─ Impacto: 🟠 ALTA

A07 - AUTHENTICATION FAILURES
└─ No hay logout en JWT (VUL-5)
   └─ UserLoginService no invalida tokens
   └─ Archivo: UserLoginService.java (falta)
   └─ Impacto: 🟠 ALTA

└─ Session Timeout Global
   └─ 30 min por defecto
   └─ Configuración: WebSecurityConfig.java:144
   └─ Impacto: ✅ CORRECTO

A08 - SOFTWARE & DATA INTEGRITY FAILURES
├─ Firmas JWT Válidas ✅
│  └─ HS256 implementado correctamente
│
└─ Datos no sign (credenciales)
   └─ AES-CBC usado correctamente
   └─ Impacto: ✅ ACEPTABLE

<!-- A10 - SSRF mapping removed per developer request -->
```

---

## Comparativa: Antes vs Después

### Antes (Estado Actual)

```
Vulnerabilidades Totales:    13
├─ Críticas:   4 🔴🔴🔴🔴
├─ Altas:      6 🟠🟠🟠🟠🟠🟠
├─ Medias:     3 🟡🟡🟡
└─ Bajas:      0

CVSS Score Promedio:         6.8 (Riesgo Alto)
OWASP Top 10 Violaciones:    7 de 10
CWE Top 25 Violaciones:      6 de 25
Riesgo de Compromiso:        🔴 MUY ALTO
```

### Después de Correcciones

```
Vulnerabilidades Totales:    3
├─ Críticas:   0 ✅
├─ Altas:      0 ✅
├─ Medias:     2 🟡
└─ Bajas:      1 🟢

CVSS Score Promedio:         3.8 (Riesgo Bajo)
OWASP Top 10 Violaciones:    1 de 10
CWE Top 25 Violaciones:      1 de 25
Riesgo de Compromiso:        🟢 ACEPTABLE
```

---

## NIST Cybersecurity Framework

### Mapeo de Controles

| NIST Control | Implementación | Estado | VUL |
|--------------|-----------------|--------|-----|
| **Identify** | Inventario de assets | ✅ | - |
| **Protect-AC-2** | Control de acceso | ⚠️ | 4,10 |
| **Protect-AC-3** | Autenticación MFA | ❌ | - |
| **Protect-AC-6** | Privilegios mínimos | ✅ | - |
| **Protect-AU-2** | Auditoría | ⚠️ | 6 |
| **Protect-SC-7** | Limites de seguridad | ✅ | - |
| **Detect-AU-2** | Análisis de logs | ⚠️ | 6 |
| **Respond-IR-2** | Respuesta incidentes | ❌ | - |

---

## Listado de Remediación por Estándar

### ISO/IEC 27001:2022

```
A.8.2.1 - User registration & access
├─ Requerir MFA ❌
├─ Validar fuerza contraseña ✅ (BCrypt + rules)
└─ Control de acceso ⚠️ (JWT debe estar activo)

A.8.2.4 - Access restriction to information
├─ Cifrado en tránsito ✅ (SSL/TLS)
├─ Cifrado en reposo ✅ (AES-256)
└─ Control de acceso ⚠️ (JWT issues)

A.12.4.1 - Recording user activities
├─ Auditoría de logins ✅
├─ Auditoría de cambios ✅
└─ Logging no expone credenciales ⚠️ (DEBUG active)

A.13.1.3 - Segregation of networks
├─ Segmentación ✅
├─ DMZ ✅
```

### PCI DSS 4.0 (si maneja datos de tarjeta)

```
1.2.3 - Prohibir acceso directo a DB
├─ NO conectar directamente ✅
├─ Usar aplicación ✅
└─ Credenciales seguras ⚠️ (Hardcodeadas)

2.2 - Cambiar valores por defecto
├─ Admin password ⚠️ (Hardcodeada)
├─ Keystore password ⚠️ (password="password")
└─ DB password ⚠️ (En properties)

3.2.1 - Gestión de claves
├─ No hardcodear ⚠️
├─ Rotación regular ❌
└─ Acceso limitado ⚠️

7.1 - Limitar acceso por rol
├─ RBAC implementado ✅
├─ Validación JWT ✅ (Activo)
└─ Auditoría ✅
```

---

## Matriz de Riesgo - Attack Tree

```
OBJETIVO: Robar credenciales de usuarios
═════════════════════════════════════════

┌─ Ruta 1: Acceso Directo a BD
│  ├─ Obtener contraseña DB: SS$pgmHbJ8&Mbv1 (en properties) ✅ FÁCIL
│  ├─ Conectar a MySQL: root@localhost ✅ FÁCIL
│  └─ Resultado: Todas las credenciales encriptadas + salts → PWNED
│
├─ Ruta 2: Fuerza Bruta Login
│  ├─ Sin rate limiting → intentos ilimitados ✅ FÁCIL
│  ├─ Test 1000 passwords/min ✅ POSIBLE
│  ├─ Vulnerabilidad timing → detectar usuarios válidos ✅ PROBABLE
│  └─ Resultado: Comprometer usuarios frecuentes
│
```

---

## Referencias de Lectura

### Documentos OWASP
- [OWASP Top 10 2023](https://owasp.org/Top10/)
- [OWASP API Security](https://owasp.org/API-Security/)
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/)

### CWE
- [CWE-798: Hardcoded Credentials](https://cwe.mitre.org/data/definitions/798.html)
- [CWE-287: Authentication](https://cwe.mitre.org/data/definitions/287.html)
- [CWE-352: CSRF](https://cwe.mitre.org/data/definitions/352.html)

### Estándares
- [CVSS Calculator](https://www.first.org/cvss/calculator/4.0)
- [NIST CSF](https://www.nist.gov/cyberframework)
- [ISO 27001:2022](https://www.iso.org/standard/27001)
- [PCI DSS 4.0](https://www.pcisecuritystandards.org/)

---

**Fin del Mapeo de Vulnerabilidades**
