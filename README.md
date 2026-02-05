# HashPass

## 👥 Miembros del Equipo
| Nombre y Apellidos | Correo URJC | Usuario GitHub |
|:--- |:--- |:--- |
| Iván Tabares Rico | i.tabares.2024@alumnos.urjc.es | ivvaann29 |
| Yago Contreras Nevares | y.contreras.2024@alumnos.urjc.es | yagoo-cn |
| Iker Marín López | i.marin.2024@alumnos.urjc.es | IML15 |
| Carlos Javier González Ledo | cj.gonzalezl.2024@alumnos.urjc.es | k4r0n22 |

---

## 🎭 **Preparación: Definición del Proyecto**

### **Descripción del Tema**
HashPass es Password Manager diseñado para proteger los credenciales de los usuarios. El objetivo principal es almacenar y organizar credenciales bajo un cifrado robusto.

### **Entidades**
Indicar las entidades principales que gestionará la aplicación y las relaciones entre ellas:

1. **[Entidad 1]**: Usuario
2. **[Entidad 2]**: Credencial (Contraseña)
3. **[Entidad 3]**: Plan (Suscripción)
4. **[Entidad 4]**: Registro de Actividad (Log)

**Relaciones entre entidades:**
- Un **Usuario** puede tener muchas **Credenciales** (1:N).
- Un **Usuario** tiene asignado un único **Plan** activo (1:1).
- Un **Usuario** genera múltiples **Registros de Actividad** (1:N).

### **Permisos de los Usuarios**
Describir los permisos de cada tipo de usuario e indicar de qué entidades es dueño:

* **Usuario Anónimo**: 
  - Permisos: Visualización de la Landing Page, consulta de Planes de precios, acceso a Login y Registro.
  - No es dueño de ninguna entidad.

* **Usuario Registrado**: 
  - Permisos: Gestión completa de su Bóveda (Crear, Leer, Editar, Borrar credenciales), gestión de su perfil (avatar, email, contraseña maestra), visualización de su Panel Principal.
  - Es dueño de: Sus **Credenciales** y su **Perfil de Usuario**.

* **Administrador**: 
  - Permisos: Visualización del listado global de usuarios, capacidad para eliminar cuentas, visualización de estadísticas globales del sistema.
  - Es dueño de: Gestión de **Usuarios**.

### **Imágenes**
Indicar qué entidades tendrán asociadas una o varias imágenes:

- **[Entidad con imágenes 1]**: Usuario - Un avatar de perfil personalizado (subido por el usuario o generado por API).
- **[Entidad con imágenes 1]**: Credencial - Icono o logotipo del servicio asociado (ej: Logo de Netflix, Google, etc.).

---

## 🛠 **Práctica 1: Maquetación de páginas con HTML y CSS**

### **Vídeo de Demostración**
📹 **[Enlace al vídeo en YouTube](https://www.youtube.com/watch?v=x91MPoITQ3I)**
> Vídeo mostrando las principales funcionalidades de la aplicación web.

### **Diagrama de Navegación**
Diagrama que muestra cómo se navega entre las diferentes páginas de la aplicación:

![Diagrama de Navegación](images/navigation-diagram.png)

> En el diagrama se observa la navegación dividida por roles: los usuarios anónimos acceden a las páginas públicas (azul), los registrados gestionan su información en el área privada (amarillo), y los administradores tienen un flujo exclusivo (verde) para la gestión global de usuarios.

### **Capturas de Pantalla y Descripción de Páginas**

#### **1. Página Principal / Home**
![Página Principal](images/home-page.png)

> Página de inicio que muestra la seguridad que ofrece el gestor con una previsualización del perfil de usuario. Incluye botones de navegación como el de los planes y acceso a registro/login para usuarios no autenticados.

#### **2. Planes de Subscripción**
![Página Principal](images/plan.png)

> Vista comparativa de los tres niveles de suscripción (Gratuito, Premium, Platinum).

#### **3. Inicio de sesión - Fase 1**
![Página Principal](images/login.png)

> Paso inicial de la autenticación donde el usuario introduce su correo electrónico para validar su identidad en el sistema.

#### **4. Inicio de sesión - Fase 2**
![Página Principal](images/password_login.png)

> Segunda fase del login que solicita la Contraseña Maestra para descifrar la bóveda, con ayudas visuales sobre seguridad.

#### **5. Página de Registro**
![Página Principal](images/register.png)

> Formulario de alta para nuevos usuarios que establece las credenciales de acceso y la clave maestra de cifrado.

#### **6. Panel Principal dentro de la sesión**
![Página Principal](images/dashboard.png)

> Centro de control privado con estadísticas de seguridad, historial de actividad reciente y menú lateral de navegación.

#### **7. Bóveda de contraseñas**
![Página Principal](images/passwords.png)

> Listado completo de credenciales con buscador, filtros y botones de acción rápida para copiar datos o editar.

#### **8. Añadir nuevos credenciales**
![Página Principal](images/new_credential.png)

> Formulario para añadir credenciales con medidor de fortaleza de contraseña en tiempo real y generador de claves aleatorias.

#### **9. Ver info/Editar Credenciales**
![Página Principal](images/info_credential.png)

> Vista de detalle que permite visualizar, modificar o eliminar la información de una credencial existente.

#### **10. Info de usuario - Sesión de usuario**
![Página Principal](images/user.png)

> Ficha de perfil integrada en la navegación que muestra información personal y el estado de la cuenta del usuario.

#### **11. Configuración Básica de usuario**
![Página Principal](images/config_user.png)

> Formulario de gestión de perfil para actualizar datos personales como avatar y correo electrónico.

#### **12. Configuración de seguridad de usuario**
![Página Principal](images/security_user.png)

> Panel crítico para cambiar la Contraseña Maestra, activar el doble factor (2FA) o eliminar la cuenta.

#### **13. Página de Administrador**
![Página Principal](images/admin.png)

> Panel de gestión global con métricas del sistema y tabla de administración para filtrar y moderar usuarios.

#### **14. Info de usuario - Sesión de admin**
![Página Principal](images/admin_user_detail.png)

> Vista técnica de un usuario para que el administrador gestione bloqueos sin acceder a la sesión personal.

### **Participación de Miembros en la Práctica 1**

#### **Alumno 1 - Iván Tabares Rico**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 2 - Yago Contreras Nevares**

[Creación de la parte del usuario donde almacena las contraseñas y la configuración]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Creación de la pantalla de entradas](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)  |
|2| [Añadido entradas de ejemplo en la pantalla de contraseñas](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435)  |
|3| [Añadido el menu de configuración y creada la página de configuración de usuario](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c),[security_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [config_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [dashboard.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c)     |
|4| [Añadido avatar en la parte superior con menú](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [add-password.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862),[config_user](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [dashboard.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [password-login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862),[passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [security_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862)  |
|5| [Añadida pantalla de edición de las entradas](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5)  | [info-passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5)  |

---

#### **Alumno 3 - Iker Marín López**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 4 - Carlos Javier González Ledo**

Desarrollo del Landing, Planes, ventana de añadir credenciales y user info

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Estructura base y estilos globales](https://github.com/DWS-2026/dws-2026-project-base/commit/097e055bf6b95dc60234a08f45ac8497fcd6c997)  | [index.html](https://github.com/DWS-2026/dws-2026-project-base/commit/097e055bf6b95dc60234a08f45ac8497fcd6c997#diff-549b2dc8686b76d37f8a6dff513ea97116a51f530648cbc068dc74ac4b040cf01) ; [plan.html](https://github.com/DWS-2026/dws-2026-project-base/commit/097e055bf6b95dc60234a08f45ac8497fcd6c997#diff-6837ab50303d15d016c29d0fc62dd6f5a4f3f3da2f44a157b94d9bb6a0697c08) ; [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/097e055bf6b95dc60234a08f45ac8497fcd6c997#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)   |
|2| [Mejoras visuales del Landing](https://github.com/DWS-2026/dws-2026-project-base/commit/fe3dcb71b586cb762f5b4b498950b2aa7acc4a6a)  | [index.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe3dcb71b586cb762f5b4b498950b2aa7acc4a6a#diff-549b2dc8686b76d37f8a6dff513ea97116a51f530648cbc068dc74ac4b040cf0)   |
|3| [Formulario de credenciales](https://github.com/DWS-2026/dws-2026-project-base/commit/5a47007d51ab6e6f7c500ae25aeb3d76ab5278f0)  | [add-password.html](https://github.com/DWS-2026/dws-2026-project-base/commit/5a47007d51ab6e6f7c500ae25aeb3d76ab5278f0#diff-9c5cf199d553e3ac6d394b8916fd092d9b79e1494ad2b2838c1fdf08f5205504)   |
|4| [Corrección HTML, Footers y Grid de Planes](https://github.com/DWS-2026/dws-2026-project-base/commit/fa5bb353834f262e66da30a4af46756f0304de42)  | [plan.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fa5bb353834f262e66da30a4af46756f0304de42#diff-6837ab50303d15d016c29d0fc62dd6f5a4f3f3da2f44a157b94d9bb6a0697c08)   |
|5| [Implementación de Perfil de Usuario y Sidebar](https://github.com/DWS-2026/dws-2026-project-base/commit/1bc9e408fdb853e6ce1eeb2df7c8ae63851b7697)  | [user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/1bc9e408fdb853e6ce1eeb2df7c8ae63851b7697#diff-07ca067c7f10dd431a799ca65e567aee6d5dcb91551e0201025f00e7f8d711ef) ; [admin_user_detail.html](https://github.com/DWS-2026/dws-2026-project-base/commit/1bc9e408fdb853e6ce1eeb2df7c8ae63851b7697#diff-9c4b29dff9ebe05f278c061d746bee68658653ea2427b10ff2b5735b07178142)  |


---

## 🛠 **Práctica 2: Web con HTML generado en servidor**

### **Vídeo de Demostración**
📹 **[Enlace al vídeo en YouTube](https://www.youtube.com/watch?v=x91MPoITQ3I)**
> Vídeo mostrando las principales funcionalidades de la aplicación web.

### **Navegación y Capturas de Pantalla**

#### **Diagrama de Navegación**

Solo si ha cambiado.

#### **Capturas de Pantalla Actualizadas**

Solo si han cambiado.

### **Instrucciones de Ejecución**

#### **Requisitos Previos**
- **Java**: versión 21 o superior
- **Maven**: versión 3.8 o superior
- **MySQL**: versión 8.0 o superior
- **Git**: para clonar el repositorio

#### **Pasos para ejecutar la aplicación**

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/[usuario]/[nombre-repositorio].git
   cd [nombre-repositorio]
   ```

2. **AQUÍ INDICAR LO SIGUIENTES PASOS**

#### **Credenciales de prueba**
- **Usuario Admin**: usuario: `admin`, contraseña: `admin`
- **Usuario Registrado**: usuario: `user`, contraseña: `user`

### **Diagrama de Entidades de Base de Datos**

Diagrama mostrando las entidades, sus campos y relaciones:

![Diagrama Entidad-Relación](images/database-diagram.png)

> [Descripción opcional: Ej: "El diagrama muestra las 4 entidades principales: Usuario, Producto, Pedido y Categoría, con sus respectivos atributos y relaciones 1:N y N:M."]

### **Diagrama de Clases y Templates**

Diagrama de clases de la aplicación con diferenciación por colores o secciones:

![Diagrama de Clases](images/classes-diagram.png)

> [Descripción opcional del diagrama y relaciones principales]

### **Participación de Miembros en la Práctica 2**

#### **Alumno 1 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 2 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 3 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 4 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

## 🛠 **Práctica 3: Incorporación de una API REST a la aplicación web, análisis de vulnerabilidades y contramedidas**

### **Vídeo de Demostración**
📹 **[Enlace al vídeo en YouTube](https://www.youtube.com/watch?v=x91MPoITQ3I)**
> Vídeo mostrando las principales funcionalidades de la aplicación web.

### **Documentación de la API REST**

#### **Especificación OpenAPI**
📄 **[Especificación OpenAPI (YAML)](/api-docs/api-docs.yaml)**

#### **Documentación HTML**
📖 **[Documentación API REST (HTML)](https://raw.githack.com/[usuario]/[repositorio]/main/api-docs/api-docs.html)**

> La documentación de la API REST se encuentra en la carpeta `/api-docs` del repositorio. Se ha generado automáticamente con SpringDoc a partir de las anotaciones en el código Java.

### **Diagrama de Clases y Templates Actualizado**

Diagrama actualizado incluyendo los @RestController y su relación con los @Service compartidos:

![Diagrama de Clases Actualizado](images/complete-classes-diagram.png)

#### **Credenciales de Usuarios de Ejemplo**

| Rol | Usuario | Contraseña |
|:---|:---|:---|
| Administrador | admin | admin123 |
| Usuario Registrado | user1 | user123 |
| Usuario Registrado | user2 | user123 |

### **Participación de Miembros en la Práctica 3**

#### **Alumno 1 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 2 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 3 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |

---

#### **Alumno 4 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Descripción commit 1](URL_commit_1)  | [Archivo1](URL_archivo_1)   |
|2| [Descripción commit 2](URL_commit_2)  | [Archivo2](URL_archivo_2)   |
|3| [Descripción commit 3](URL_commit_3)  | [Archivo3](URL_archivo_3)   |
|4| [Descripción commit 4](URL_commit_4)  | [Archivo4](URL_archivo_4)   |
|5| [Descripción commit 5](URL_commit_5)  | [Archivo5](URL_archivo_5)   |
