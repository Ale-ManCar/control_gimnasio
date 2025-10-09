# Creación del instalador `.exe`

Este proyecto incluye un script que automatiza la construcción de un instalador de Windows mediante `jpackage`, sin depender de Launch4j ni Inno Setup.

## Requisitos previos

1. Windows 10/11 de 64 bits.
2. [Microsoft Visual C++ Redistributable 2015-2022](https://learn.microsoft.com/cpp/windows/latest-supported-vc-redist) instalado (requerido por JavaFX).
3. JDK 21 (o superior) para Windows, con las herramientas `mvn`, `jlink` y `jpackage` disponibles en el `PATH`.
   - Puedes usar el JDK de [Gluon](https://gluonhq.com/products/javafx/) o [Oracle](https://www.oracle.com/java/technologies/downloads/) siempre que incluya `jpackage`.
4. Acceso a internet la primera vez que ejecutes el script, para que Maven descargue las dependencias.

## Pasos

1. Abre una consola de **PowerShell** o **Command Prompt**.
2. Colócate en la carpeta raíz del proyecto.
3. Ejecuta el script:

   ```bat
   scripts\create-windows-exe.bat
   ```

   El script realizará las siguientes tareas:

   - Compilar el proyecto con Maven (`clean package`).
   - Copiar las dependencias de tiempo de ejecución en `target/app-libs`.
   - Generar una imagen de runtime mínima con `jlink` (Java SE).
   - Empaquetar el instalador `.exe` con `jpackage`, incorporando los directorios `database`, `backups`, `lib` y el archivo `CONFIGURACION.txt`.

4. El instalador resultante se guardará en la carpeta `dist`. El archivo tendrá un nombre similar a `ControlGimnasio-1.0.0.exe`.
5. Ejecuta el instalador en cualquier máquina Windows para instalar la aplicación. Se crearán accesos directos en el menú inicio y en el escritorio.

## Personalización

- Puedes actualizar el número de versión del instalador cambiando el valor de la propiedad `<app.release.version>` en el `pom.xml`.
- Si necesitas incluir archivos adicionales, añádelos en el bloque correspondiente del script (`scripts/create-windows-exe.bat`).
- Para generar solo la imagen portable (sin instalador), puedes cambiar `--type exe` por `--type app-image` en el script.

## Solución de problemas

- **`jlink` o `jpackage` no se reconocen**: asegúrate de ejecutar el script con un JDK completo (no un JRE) y que la variable `JAVA_HOME` apunte a él.
- **Errores de librerías nativas JavaFX**: verifica que el instalador se ejecute en una arquitectura de 64 bits y que no se hayan eliminado las carpetas `lib`, `database` o `backups`.
- **Base de datos vacía**: el instalador copia la carpeta `database` tal como se encuentra en el repositorio. Si necesitas datos iniciales, colócalos en esa carpeta antes de empaquetar.

Con estos pasos obtendrás un instalador `.exe` funcional del sistema, idéntico al que ejecutas actualmente desde tu entorno de desarrollo.
