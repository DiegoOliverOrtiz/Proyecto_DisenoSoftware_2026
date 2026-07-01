# Guía de ejecución del proyecto

Este proyecto está compuesto por tres componentes:

- Backend principal: expone la API en el puerto 8080.
- Backend de usuarios: expone la API en el puerto 8081.
- Frontend: se ejecuta en el puerto 4200.

## Prerrequisitos

Antes de arrancar la aplicación, se debe tener instalado lo siguiente:

- Java 21 para el backend principal.
- Java 17 para el backend de usuarios.
- Maven.
- Node.js 20 o superior con npm.
- MySQL Server 8 o superior.
- SQL Server Developer o Express, junto con SQL Server Management Studio.
- Git.

También es importante dejar libres los puertos 8080, 8081 y 4200.


## 1. Preparar MySQL

1. Tener instalado MySQL Server.
2. Crear una base de datos llamada `esientradas`.
3. Crear un usuario de aplicación y asignar permisos sobre esa base de datos.

Ejemplo de SQL para MySQL:

```sql
CREATE DATABASE esientradas;
CREATE USER 'entradas_app'@'localhost' IDENTIFIED BY 'Cambiar123';
GRANT ALL PRIVILEGES ON esientradas.* TO 'entradas_app'@'localhost';
FLUSH PRIVILEGES;
```

## 2. Preparar SQL Server

1. Tener instalado SQL Server y SQL Server Management Studio.
2. Crear una base de datos llamada esiusuarios.
3. Crear un login de aplicación con una contraseña segura.
4. Hay que asegurarse de que el motor de SQL Server esté disponible en localhost:1433.

Ejemplo de SQL para SQL Server:

```sql
CREATE DATABASE esiusuarios;
GO

CREATE LOGIN usuarios_app WITH PASSWORD = 'UsuariosApp!2026';
GO

USE esiusuarios;
CREATE USER usuarios_app FOR LOGIN usuarios_app;
ALTER ROLE db_owner ADD MEMBER usuarios_app;
GO
```

## 3. Configurar variables de entorno

El backend principal usa variables de entorno para la conexión a MySQL y para la comunicación con el servicio de usuarios.
En PowerShell, desde la raíz del proyecto, hay que ejecutar:

```powershell
$env:MYSQL_URL="jdbc:mysql://localhost:3306/esientradas?serverTimezone=UTC&autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true"
$env:MYSQL_USER="entradas_app"
$env:MYSQL_PASSWORD="Cambiar123"
$env:ESIUSUARIOS_URL="http://localhost:8081"
$env:FRONTEND_BASE_URL="http://localhost:4200"
```
Para el backend de usuarios, se debe crear un archivo .env en la carpeta usuarios a partir del ejemplo existente y ajustar los valores:

SQLSERVER_HOST=localhost
SQLSERVER_PORT=1433
SQLSERVER_DATABASE=esiusuarios
SQLSERVER_USER=usuarios_app
SQLSERVER_PASSWORD=UsuariosApp!2026


## 4. Arrancar el backend de usuarios

Abrir una terminal y ejecutar:

```bash
cd esiusuarios\usuarios
Copy-Item .env.example .env
.\start-dev.ps1
```
Este servicio quedará disponible en: http://localhost:8081


## 5. Arrancar el backend principal

Abrir otra terminal y ejecutar:

```bash
cd Dise-oSW
.\mvnw.cmd -Pdev spring-boot:run
```
Este servicio quedará disponible en: http://localhost:8080


## 6. Arrancar el frontend

Abrir una tercera terminal y ejecutar:

```bash
cd Frontend-Dise-oSoftware
npm install
npm start
```
La interfaz quedará disponible en: http://localhost:4200


## 7. Verificación final
Si todo está correctamente configurado, el sistema debería ya de estar en ejecución y se debería poder interactuar desde el frontend simulando la reserva de entradas, su posterior compra y el sistema de correos electrónicos que envía las entradas o sirve para configurar una cuenta de usuario.

Notas importantes:

Las tablas de la base de datos se crean automáticamente al iniciar la aplicación cuando corresponde.
Si se quiere probar el flujo de pago, es necesario configurar las claves de Stripe.
Si alguno de los puertos está ocupado, debe liberarse antes de volver a arrancar el servicio o configurar unos puertos diferentes modificando server-port en application-dev.properties.
