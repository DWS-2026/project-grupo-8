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
4. **[Entidad 4]**: Reviews

**Relaciones entre entidades:**
- Un **Usuario** puede tener muchas **Credenciales** (1:N).
- Un **Usuario** tiene asignado un único **Plan** activo (1:1).
- Un **Usuario** puede generar múltiples **reviews** (1:N).

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
📹 **[Enlace al vídeo en YouTube](https://youtu.be/lHgOssbFNRo)**
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

#### **15. Reseñas y Comentarios**
![Página Principal](images/reviews.png)

> Sección de comentarios donde se pueden añadir reseñas.

#### **16. Página de Pago**
![Página Principal](images/payment.png)

> Pantalla de Pago con targeta

### **Participación de Miembros en la Práctica 1**

#### **Alumno 1 - Iván Tabares Rico**

Creacion de la pantalla de adminnistrador, ajustes de seguridad de usuario, y panel principal de usuario

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Creacion security_user.html, y cambios en config_user.html](https://github.com/DWS-2026/project-grupo-8/commit/7b74183b0a515468959e7963fad2a1827bb14188)  | [config_user.html](https://github.com/DWS-2026/project-grupo-8/commit/7b74183b0a515468959e7963fad2a1827bb14188#diff-7f36781ce5c980cd24233129f5c7329719e0963707cd30e7e217a5a8c29a7878), [security_user.html](https://github.com/DWS-2026/project-grupo-8/commit/7b74183b0a515468959e7963fad2a1827bb14188#diff-5087fd5a56ddd20d1d916e8043ba0552baa7e4a8c60d1ffd1c49cbf4f31b0b3c)   |
|2| [Creacion pantalla admin.html y modificacion css](https://github.com/DWS-2026/project-grupo-8/commit/bfff8d51b0da97c16eccc775e196cab49dd16079)  | [admin.html](https://github.com/DWS-2026/project-grupo-8/commit/bfff8d51b0da97c16eccc775e196cab49dd16079#diff-9e792440d333205e8cbb842f9a84fa08648e8a69151f8e67ca418a5326ab03b7), [styles.css](https://github.com/DWS-2026/project-grupo-8/commit/bfff8d51b0da97c16eccc775e196cab49dd16079#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)   |
|3| [Creacion de dashboard.html y modificacion de passwords.html](https://github.com/DWS-2026/project-grupo-8/commit/a1a46ffa5b19c14c4062d4985a740f50be8d6d86)  | [dashboard.html](https://github.com/DWS-2026/project-grupo-8/commit/a1a46ffa5b19c14c4062d4985a740f50be8d6d86#diff-c53908a5b164e37da45cff45b26de99931e4221295d917eb6b0b0720a428ab56), [passwords.html](https://github.com/DWS-2026/project-grupo-8/commit/a1a46ffa5b19c14c4062d4985a740f50be8d6d86#diff-e3ae49905403e99b618ba1a7b002317ee3c559cf8db694a16f52b6a9d73b18cc).   |
|4| [Mejoras en pantalla dashboard.html](https://github.com/DWS-2026/project-grupo-8/commit/0425f958fe3d9dfeb15c27f8c94d4a381aca0501)  | [dashboard.html](https://github.com/DWS-2026/project-grupo-8/commit/0425f958fe3d9dfeb15c27f8c94d4a381aca0501#diff-c53908a5b164e37da45cff45b26de99931e4221295d917eb6b0b0720a428ab56)   |
|5| [Arreglo de varias interacciones entre pantallas](https://github.com/DWS-2026/project-grupo-8/commit/5378eb08680651325ea8a7cd753e07d0c778ca18)  | [dashboard.html](https://github.com/DWS-2026/project-grupo-8/commit/5378eb08680651325ea8a7cd753e07d0c778ca18#diff-c53908a5b164e37da45cff45b26de99931e4221295d917eb6b0b0720a428ab56), [config_user.html](https://github.com/DWS-2026/project-grupo-8/commit/5378eb08680651325ea8a7cd753e07d0c778ca18#diff-7f36781ce5c980cd24233129f5c7329719e0963707cd30e7e217a5a8c29a7878), [security_user.html](https://github.com/DWS-2026/project-grupo-8/commit/5378eb08680651325ea8a7cd753e07d0c778ca18#diff-5087fd5a56ddd20d1d916e8043ba0552baa7e4a8c60d1ffd1c49cbf4f31b0b3c).   |

---

#### **Alumno 2 - Yago Contreras Nevares**

Creación de la parte del usuario donde almacena las contraseñas y la configuración

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Creación de la pantalla de entradas](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/0e19e25d5fdb976d29cb0764e4750d590c21b338#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)  |
|2| [Añadido entradas de ejemplo en la pantalla de contraseñas](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/2a11f647af0b0f7517fdb30ed8473713b9613435)  |
|3| [Añadido el menu de configuración y creada la página de configuración de usuario](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c),[security_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [config_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c), [dashboard.html](https://github.com/DWS-2026/dws-2026-project-base/commit/cdd80172c1835e7a6b7558f203317788f3515a6c)     |
|4| [Añadido avatar en la parte superior con menú](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862)  | [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [add-password.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862),[config_user](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [dashboard.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [password-login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862),[passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862), [security_user.html](https://github.com/DWS-2026/dws-2026-project-base/commit/fe6f6fcedde5e4532285d7cf8498b43e29378862)  |
|5| [Añadida pantalla de edición de las entradas](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5)  | [info-passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5), [passwords.html](https://github.com/DWS-2026/dws-2026-project-base/commit/4767f1a7c9c9a87f9aab8881bdc97824ef8e62b5)  |

---

#### **Alumno 3 - Iker Marín López**

Desarrollo de las páginas de login (tanto login inicial como password-login) y página de registro de nuevo usuario.

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Creación de la página de login](https://github.com/DWS-2026/dws-2026-project-base/commit/ef64e0ca1f4e95e561d46f90a3c7af7dc7ad0563)  | [login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/ef64e0ca1f4e95e561d46f90a3c7af7dc7ad0563#diff-85824d9587d70e9bd6d79b06f604b7ab4b0112ed85567dede3dfaa14c6896da9)   |
|2| [Creación de password-login.html y modificación de login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/ed886cb7cc02591dd4511533b417b84abf812a50)  | [password-login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/ed886cb7cc02591dd4511533b417b84abf812a50#diff-d406f429d968667a13d79f5a46fcd7f26f4a6031a95fd504b872809ea3c8ae3a), [login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/ed886cb7cc02591dd4511533b417b84abf812a50#diff-85824d9587d70e9bd6d79b06f604b7ab4b0112ed85567dede3dfaa14c6896da9)   |
|3| [Creación de la página register.html y ajute del login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/70ba131fe5120ae05225ad8b235f66d8fbf304d1)  | [register.html](https://github.com/DWS-2026/dws-2026-project-base/commit/70ba131fe5120ae05225ad8b235f66d8fbf304d1#diff-ff0ce1d67eb3e47366a2d74b726fe9aaf27723281fa8156be7ef781ad6eec4b0), [login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/70ba131fe5120ae05225ad8b235f66d8fbf304d1#diff-85824d9587d70e9bd6d79b06f604b7ab4b0112ed85567dede3dfaa14c6896da9)   |
|4| [Cambios importnates en password-login.html, login.html, register.html y styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/13275d02aaa386e7b96226beefea5d0ea99e92bf)  | [password-login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/13275d02aaa386e7b96226beefea5d0ea99e92bf#diff-d406f429d968667a13d79f5a46fcd7f26f4a6031a95fd504b872809ea3c8ae3a), [login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/13275d02aaa386e7b96226beefea5d0ea99e92bf#diff-85824d9587d70e9bd6d79b06f604b7ab4b0112ed85567dede3dfaa14c6896da9), [regsiter.html](https://github.com/DWS-2026/dws-2026-project-base/commit/13275d02aaa386e7b96226beefea5d0ea99e92bf#diff-ff0ce1d67eb3e47366a2d74b726fe9aaf27723281fa8156be7ef781ad6eec4b0), [styles.css](https://github.com/DWS-2026/dws-2026-project-base/commit/13275d02aaa386e7b96226beefea5d0ea99e92bf#diff-506553736ec2a7007edf02fc6f203dc94a6f62054042f9148957f96270a7db22)   |
|5| [Creación de nuevo footer y cambio de este en muchos de los archivos .html](https://github.com/DWS-2026/dws-2026-project-base/commit/01efce47a83f7386b7cfeaec8bdf5a75bd0dfd21)  | [password-login.html](https://github.com/DWS-2026/dws-2026-project-base/commit/01efce47a83f7386b7cfeaec8bdf5a75bd0dfd21#diff-d406f429d968667a13d79f5a46fcd7f26f4a6031a95fd504b872809ea3c8ae3a), [register.html](https://github.com/DWS-2026/dws-2026-project-base/commit/01efce47a83f7386b7cfeaec8bdf5a75bd0dfd21#diff-ff0ce1d67eb3e47366a2d74b726fe9aaf27723281fa8156be7ef781ad6eec4b0)   |

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
📹 **[Enlace al vídeo en YouTube](https://youtu.be/FzdeYResrmw?si=tp4GaUr3YkSWezOw)**
> Vídeo mostrando las principales funcionalidades de la aplicación web.

### **Navegación y Capturas de Pantalla**

#### **Diagrama de Navegación**

Solo si ha cambiado.

#### **Capturas de Pantalla Actualizadas**

#### **1. Añadir credencial**
![Añadir credencial](images/add_credential_new.png)

> Se han añadido imagenes a las credenciales de forma opcional

#### **2. Informacion de las credenciales**
![Info credencial](images/info_credentials_new.png)

> Se han añadido imagenes a las credenciales de forma opcional

#### **3. Admin**
![Admin](images/admin_new.png)

> Se ha añadido la opcion de gestionar planes desde el admin

#### **4. Error**
![Error](images/error_403_new.png)

> Se han añadido paginas de error, tanto 403, 404, como 500 

### **Instrucciones de Ejecución**

Ejecutar en terminal el siguiente comando si el usuario que desea uasr la aplicación usa docker para la BBDD: 

<docker run --rm -e MYSQL_ROOT_PASSWORD='SS$pgmHbJ8&Mbv1' -e MYSQL_DATABASE=hashpass -p 3306:3306 -d mysql:9.6>



#### **Requisitos Previos**
- **Java**: versión 25 o superior
- **Maven**: versión 3.8 o superior
- **MySQL**: versión 8.0 o superior
- **Git**: para clonar el repositorio

#### **Pasos para ejecutar la aplicación**

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/[usuario]/[nombre-repositorio].git
   cd [nombre-repositorio]
   ```

2. **Te metes desde vscode a esa carpeta y ejecutas el código**
3. **Abres el enlace https://localhost:8443**

#### **Credenciales de prueba**
- **Usuario Admin**: usuario: `adminhashpass@gmail.com`, contraseña: `admin123`
- **Usuario Registrado 1**: usuario: `demo1@hashpass.local`, contraseña: `Demo123!`
- **Usuario Registrado 2**: usuario: `demo2@hashpass.local`, contraseña: `Demo123!`

### **Diagrama de Entidades de Base de Datos**

Diagrama mostrando las entidades, sus campos y relaciones:

![Diagrama Entidad-Relación](images/database-diagram.png)

> El diagrama muestra las 4 entidades principales: Usuario, Planes, Credenciales y reviews, con sus respectivos atributos y relaciones 1:N y N:M.

### **Diagrama de Clases y Templates**

Diagrama de clases de la aplicación con diferenciación por colores o secciones:

![Diagrama de Clases](images/classes-diagram.png)

> [Descripción opcional del diagrama y relaciones principales]

### **Participación de Miembros en la Práctica 2**

#### **Alumno 1 - [Nombre Completo]**

[Descripción de las tareas y responsabilidades principales del alumno en el proyecto]

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Lógica de cifrado AES y seguridad en el acceso y gestión de contraseñas](https://github.com/DWS-2026/project-grupo-8/commit/df6c10dd8290c2c601214ff5b48b3fb0bb1cf1eb)  | [AuthService.java, EntryService.java, UserSession.java y MainController.java](https://github.com/DWS-2026/project-grupo-8/commit/df6c10dd8290c2c601214ff5b48b3fb0bb1cf1eb#diff-d0b2e9f5a02a6c7f254390113eaf871522c8e0b8b39a4de7e58d6e786a06f2aa)   |
|2| [Creacion EntryService](https://github.com/DWS-2026/project-grupo-8/commit/0c28aa5d70436de9afdffe05fd5a5d49bb935100)  | [EntryService.java entre otros](https://github.com/DWS-2026/project-grupo-8/commit/0c28aa5d70436de9afdffe05fd5a5d49bb935100#diff-d0b2e9f5a02a6c7f254390113eaf871522c8e0b8b39a4de7e58d6e786a06f2aa)   |
|3| [creacion de un controller por cada una de las 4 entidades](https://github.com/DWS-2026/project-grupo-8/commit/3ae7434948b6ae41dce6ede536d85484573aef3f)  | [Todos los controller.java](https://github.com/DWS-2026/project-grupo-8/commit/3ae7434948b6ae41dce6ede536d85484573aef3f#diff-a5a1e31ae899215bc47ca93d9cb6fb97ec97b4b854fa5c5904c0dda4676c064f)   |
|4| [Fecha ultimo login, e intentos fallidos recientes, y diseño pantalla admin](https://github.com/DWS-2026/project-grupo-8/commit/fc04cc78a37389414e1f91d814c53203dc56eb9b)  | [AuthService.java entre otros](https://github.com/DWS-2026/project-grupo-8/commit/fc04cc78a37389414e1f91d814c53203dc56eb9b#diff-dbd1337d00de64e54f39aff7aeb4dfefae9a6b8f3f1879a73f6cca630c8b1a27)   |
|5| [Reviews edit and delete](https://github.com/DWS-2026/project-grupo-8/commit/96bdc93d0f9c536288b1ee5163f9463ff6dc3ff9)  | [ReviewController.java](https://github.com/DWS-2026/project-grupo-8/commit/96bdc93d0f9c536288b1ee5163f9463ff6dc3ff9#diff-459cf7c77e2a69042e385e3659d058ad0e1511dbc82b5e5d7ab58aafa7fb75ca)   |

---

#### **Alumno 2 - Yago Contreras Nevares**

Creación general de controllers, services, entidades, ... sobre todo la creación del login, imágenes, seguridad, algo de las reviews, entre otras cosas 

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Cambio del UserSesion a UserService para pedir al usuario logueado](https://github.com/DWS-2026/project-grupo-8/commit/5cbd7599491df0c2eda13024f7e21309a063e9aa)  | [PlanController.java entre otros](https://github.com/DWS-2026/project-grupo-8/commit/5cbd7599491df0c2eda13024f7e21309a063e9aa)   |
|2| [Cambio de planes para hacerlos de forma dinámica desde la base de datos](https://github.com/DWS-2026/project-grupo-8/commit/f7a7921a44d053f48dc00bacdeaa14910125c03d)  | [PlanController.java entre otros](https://github.com/DWS-2026/project-grupo-8/commit/f7a7921a44d053f48dc00bacdeaa14910125c03d)   |
|3| [Creacion de reviews](https://github.com/DWS-2026/project-grupo-8/commit/8bc4849b61f8f21fbf8d17a8962b25e440d31dc6)  | [ReviewController.java entre otras](https://github.com/DWS-2026/project-grupo-8/commit/8bc4849b61f8f21fbf8d17a8962b25e440d31dc6)   |
|4| [Creación de imágenes](https://github.com/DWS-2026/project-grupo-8/commit/43077c90b2d40e6ab8fde4cdc5571800eb7f32a0)  | [ImagesService.java entre otros](https://github.com/DWS-2026/project-grupo-8/commit/43077c90b2d40e6ab8fde4cdc5571800eb7f32a0)   |
|5| [Redirecciones, admin, reviews](https://github.com/DWS-2026/project-grupo-8/commit/501226fb0e428d38281e85bd35078d1015088936)  | [admin.html entre otros](https://github.com/DWS-2026/project-grupo-8/commit/501226fb0e428d38281e85bd35078d1015088936)   |

---

#### **Alumno 3 - Iker Marín López**

Desarrollo general de la aplicación (entidades, servicios, ...) sobre todo en la creación de la misma. También el desarrollo en profundidad de la entidad de "planes" y traducción final de los comentarios del código. 

| Nº    | Commits      | Files      |
|:------------: |:------------:| :------------:|
|1| [Desarrollo general de inicio de proyecto](https://github.com/DWS-2026/dws-2026-project-base/commit/9ca216f6a700fd46b00366232232542ff0aab991)  | [Plan.java entre otros](https://github.com/DWS-2026/dws-2026-project-base/commit/9ca216f6a700fd46b00366232232542ff0aab991#diff-e5158bd2a8137474bfca510dd2cc98ce30a0fd72a252e9bf83fad523d0a92006)   |
|2| [Migración del código de thymeleaf a mustache](https://github.com/DWS-2026/dws-2026-project-base/commit/cc6ffaf925a65645c790b86edbd5af3b48f3390a)  | [footerPublic.html entre otros](https://github.com/DWS-2026/dws-2026-project-base/commit/cc6ffaf925a65645c790b86edbd5af3b48f3390a#diff-59bf28b7dc0c557e15f7b4de4c7ed54ad71c36aa2f9138c06e730ef340cf012b) |
|3| [Creación de un controler y un service para el login y register](https://github.com/DWS-2026/dws-2026-project-base/commit/3e940ce7a3f2b9af760cae60803e11687b0c95f4)  | [AuthController.java entre otros](https://github.com/DWS-2026/dws-2026-project-base/commit/3e940ce7a3f2b9af760cae60803e11687b0c95f4#diff-34a6338c5c4a2ca112ab109ce48a5227cd95f7edca3f6e7c1869bf8ae9ddbbe0)   |
|4| [Ajustes del Dashboard, cambio en BBDD para los planes, creación de planes, ...](https://github.com/DWS-2026/dws-2026-project-base/commit/bd808d29b0c2b15419dda3899c898605aa5c7868)  | [PlanController.java entre otros](https://github.com/DWS-2026/dws-2026-project-base/commit/bd808d29b0c2b15419dda3899c898605aa5c7868#diff-a5a1e31ae899215bc47ca93d9cb6fb97ec97b4b854fa5c5904c0dda4676c064f)   |
|5| [Traducción de comentarios](https://github.com/DWS-2026/dws-2026-project-base/commit/fecca6862b8fb5bf099fdcd2abf7734d3375f37f)  | [EntryService.java entre otros](https://github.com/DWS-2026/dws-2026-project-base/commit/fecca6862b8fb5bf099fdcd2abf7734d3375f37f#diff-d0b2e9f5a02a6c7f254390113eaf871522c8e0b8b39a4de7e58d6e786a06f2aa)   |

---

#### **Alumno 4 - [Carlos  Javier González Ledo]**

Desarrollo general del backend, configuración de base de datos, autenticación, gestión segura de contraseñas y panel de administración. Además de varios arreglos y mejoras en Entidades. 

| Nº | Commits | Files |
|:---:|:---|:---|
| 1 | [Configuración de BBDD y sistema de autenticación base](https://github.com/DWS-2026/project-grupo-8/commit/10c4889f66fa7fc361add47b5575b86a152f58c5) | [MainController.java, entre otros](https://github.com/DWS-2026/project-grupo-8/commit/10c4889f66fa7fc361add47b5575b86a152f58c5#diff-96e557d647d64a5cc6924ffd0e4d55121017fde20b9edc09c914b3adfcd1650b) |
| 2 | [Implementación de estadísticas dinámicas en el dashboard](https://github.com/DWS-2026/project-grupo-8/commit/535e0b7ba0cbf3ba87b7d6c2c175cff17fcfebbf) | [User.java, entre otros](https://github.com/DWS-2026/project-grupo-8/commit/535e0b7ba0cbf3ba87b7d6c2c175cff17fcfebbf#diff-6254fbff4673d7fa4e920913e0adb7faec60eb11a491939145ac07ea50a6f84b) |
| 3 | [Desarrollo de la gestión completa de credenciales](https://github.com/DWS-2026/project-grupo-8/commit/0d4bd773c3d8758b6b28c8756133015adca955ce) | [MainController.java, entre otros](https://github.com/DWS-2026/project-grupo-8/commit/0d4bd773c3d8758b6b28c8756133015adca955ce#diff-96e557d647d64a5cc6924ffd0e4d55121017fde20b9edc09c914b3adfcd1650b) |
| 4 | [Creación del panel de administración y control de roles](https://github.com/DWS-2026/project-grupo-8/commit/9c19a7a239b273e8913f8817905dc3b7a12be642) | [UserController.java, entre otros](https://github.com/DWS-2026/project-grupo-8/commit/9c19a7a239b273e8913f8817905dc3b7a12be642#diff-a1ba98464931c168a2577cb7752cdd0161551ca079c4fecb083f57cffba56da6) |
| 5 | [Filtrado/ordenación y seguridad de cuenta](https://github.com/DWS-2026/project-grupo-8/commit/b1ca28500b9f5b049f5767639178a794584f39b8) | [UserController.java, entre otros](https://github.com/DWS-2026/project-grupo-8/commit/b1ca28500b9f5b049f5767639178a794584f39b8#diff-a1ba98464931c168a2577cb7752cdd0161551ca079c4fecb083f57cffba56da6) |

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
