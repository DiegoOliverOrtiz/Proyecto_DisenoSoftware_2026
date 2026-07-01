# Guía de ejecución del proyecto

Este proyecto está compuesto por tres componentes:

- Backend principal: expone la API en el puerto 8080.
- Backend de usuarios: expone la API en el puerto 8081.
- Frontend: se ejecuta en el puerto 4200.

## Prerrequisitos

Antes de arrancar la aplicación, asegúrate de tener instalado lo siguiente:

- Java 21 para el backend principal.
- Java 17 para el backend de usuarios.
- Maven.
- Node.js 20 o superior con npm.
- MySQL Server 8 o superior.
- SQL Server Developer o Express, junto con SQL Server Management Studio.
- Git.

También es importante dejar libres los puertos 8080, 8081 y 4200.

## 1. Preparar MySQL

1. Instala MySQL Server.
2. Crea una base de datos llamada `esientradas`.
3. Crea un usuario de aplicación y asigna permisos sobre esa base de datos.

Ejemplo de SQL para MySQL:

```sql
CREATE DATABASE esientradas;
CREATE USER 'entradas_app'@'localhost' IDENTIFIED BY 'Cambiar123';
GRANT ALL PRIVILEGES ON esientradas.* TO 'entradas_app'@'localhost';
FLUSH PRIVILEGES;
