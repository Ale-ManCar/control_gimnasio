# Guía para generar el instalador `.exe` con base de datos local

Esta guía explica cómo empaquetar **Control Gimnasio** para distribuirlo en una computadora con Windows y asegurar que funcione exactamente igual que en tu máquina de desarrollo. El objetivo es generar un ejecutable (`.exe`) que incluya todas las dependencias, la configuración y la base de datos SQLite local (`database/gimnasio.db`).

## 1. Preparar el entorno de construcción

1. Instala **JDK 21** (la misma versión utilizada en desarrollo) y agrega `JAVA_HOME` al `PATH`. Este JDK ya incluye la herramienta `jpackage` que permite crear instaladores por línea de comandos.
2. Instala **Maven 3.9+** y verifica que el comando `mvn -v` reconozca el JDK 21.
3. Instala **JavaFX SDK 21** solo si vas a ejecutar `jpackage` fuera de Maven (con Maven no es necesario descargarlo aparte, pero sí debes tener conexión para resolver las dependencias).
4. (Solo si eliges la ruta de Launch4j) descarga e instala **Launch4j** para envolver el `.jar` en un `.exe`.
5. (Opcional y únicamente para la ruta de Launch4j) instala **Inno Setup** si quieres generar un instalador `.exe` clásico que copie todo en `Archivos de programa` y cree accesos directos.

> **Recomendación**: Realiza la compilación en una máquina Windows limpia (o una máquina virtual) para validar que el paquete final no depende de herramientas que solo existen en la máquina de desarrollo.

## 2. Construir el `jar` ejecutable y copiar dependencias

Desde la raíz del proyecto (`control_gimnasio/`), ejecuta:

```bash
mvn clean package -DskipTests
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/libs
```

Esto genera:

- `target/gimnasio_control-1.0-SNAPSHOT.jar` (el jar principal).
- `target/libs/` con todas las dependencias requeridas en tiempo de ejecución (JavaFX, Selenium, SQLite, Quartz, JasperReports, Ikonli, etc.).

## 3. Armar la carpeta de distribución

Crea una carpeta temporal, por ejemplo `dist/`, con la siguiente estructura (todas las rutas relativas se respetan para que la aplicación encuentre los recursos igual que en desarrollo):

```
dist/
├── gimnasio_control-1.0-SNAPSHOT.jar
├── libs/                      # Copiar desde target/libs/
├── database/
│   └── gimnasio.db            # Copia exacta de la base local
├── backups/                   # Carpeta de respaldos (puede ir vacía pero debe existir)
├── CONFIGURACION.txt          # Mismo archivo que usas en desarrollo
├── gimnasio.bat               # Script existente para automatizaciones
└── driver/
    └── chromedriver.exe       # Misma versión configurada en WhatsAppService
```

Puntos clave:

- **Base de datos local**: la aplicación se conecta a `jdbc:sqlite:database/gimnasio.db`. Mantén esa ruta relativa para que no haya que editar código ni configuraciones.
- **Respaldo automático**: la carpeta `backups/` debe existir para que el proceso automático pueda escribir sin errores.
- **ChromeDriver**: respeta la ruta configurada en `WhatsAppService.java`. Si en producción lo ubicarás en otra ruta, actualiza la clase o crea un `.bat` que establezca la variable `webdriver.chrome.driver`.
- **Archivos adicionales**: incluye cualquier reporte Jasper (`.jasper`), plantillas, imágenes o recursos externos que el sistema utilice.

<a id="jpackage-cli"></a>
## 4. Generar el `.exe` únicamente con comandos (`jpackage`)

Para responder directamente a la pregunta de si se puede crear el `.exe` solo con comandos y sin Launch4j ni Inno Setup: **sí, se puede**. El flujo completo se apoya únicamente en `jlink` y `jpackage`, ambas herramientas incluidas en el JDK.

1. Asegúrate de tener la carpeta `dist/` lista (ver secciones anteriores).
2. Crea primero una imagen de runtime mínima que incluya los módulos de JavaFX que usa la aplicación:

   ```bash
   set DIST=%CD%\dist
   set RUNTIME_IMAGE=%DIST%\runtime

   "%JAVA_HOME%\bin\jlink" ^
     --module-path "%JAVA_HOME%\jmods";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar" ^
     --add-modules java.base,java.sql,java.desktop,javafx.controls,javafx.fxml ^
     --output "%RUNTIME_IMAGE%"
   ```

3. Con esa imagen lista, ejecuta `jpackage` para producir el instalador `.exe` auto contenido:

   ```bash
   "%JAVA_HOME%\bin\jpackage" ^
     --type exe ^
     --name "Control Gimnasio" ^
     --input "%DIST%" ^
     --main-jar gimnasio_control-1.0-SNAPSHOT.jar ^
     --main-class com.gimnasio.MainApp ^
     --app-version 1.0 ^
     --runtime-image "%RUNTIME_IMAGE%" ^
     --icon "%DIST%\icon.ico" ^
     --win-shortcut ^
     --win-menu ^
     --win-dir-chooser
   ```

4. El ejecutable generado (por ejemplo `Control Gimnasio-1.0.exe`) ya incluye la aplicación, la base de datos local, todas las librerías y el runtime, por lo que funcionará aunque la máquina del cliente no tenga Java instalado. Prueba el instalador en una máquina limpia para confirmar que todo opera igual que en tu entorno.

> **Consejo**: si no necesitas accesos directos ni asistentes gráficos, puedes sustituir `--type exe` por `--type app-image` para obtener directamente una carpeta auto contenida sin instalador. Luego puedes comprimirla y distribuirla.

## 5. Generar el `.exe` con Launch4j (ruta alternativa)

Si prefieres Launch4j (por ejemplo, porque ya lo usas en otros proyectos), puedes seguir empleándolo. Los pasos son:

1. Abre Launch4j y crea un nuevo proyecto.
2. Configura los campos principales:
   - **Output file**: `dist/gimnasio-control.exe` (o el nombre final definido en `CONFIGURACION.txt`).
   - **Jar**: `dist/gimnasio_control-1.0-SNAPSHOT.jar`.
   - **Don't wrap the jar, launch only**: *desactivado* (queremos que lo envuelva).
   - **Classpath**: agrega `libs/*.jar` para que todas las dependencias estén disponibles.
   - **Min JRE version**: `21`.
   - **Bundled JRE path**: apunta a una carpeta `jre/` dentro de `dist/` si quieres incluir un runtime de Java ya generado con `jlink` (ver sección 6).
   - **Custom icon**: selecciona el ícono deseado si ya tienes uno.
3. En la pestaña **JRE**, define argumentos JVM necesarios:
   - `--add-opens=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED`
   - `--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED`
   - `--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED`
   - `-Dfile.encoding=UTF-8`
4. Guarda la configuración (`.xml`) junto al proyecto para reutilizarla y haz clic en **Build Wrapper**. Launch4j generará `dist/gimnasio-control.exe`.

## 6. Incluir un runtime de Java **obligatorio** para un `.exe` auto contenido

Si el cliente no tiene (ni tendrá) Java instalado, debes entregar tu propia distribución del runtime. Tienes dos opciones:

### Opción A: `jlink` + Launch4j (mantener tu configuración actual)

1. **Descarga los módulos JavaFX nativos para Windows** ejecutando una compilación en esa plataforma (Maven colocará los `.jar` con sufijo `-win`).
2. Ejecuta en Windows el siguiente comando (ajusta rutas si usas un repositorio Maven diferente):

   ```bash
   "%JAVA_HOME%\bin\jlink" ^
     --module-path "%JAVA_HOME%\jmods";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar";"%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar" ^
     --add-modules java.base,java.sql,java.desktop,javafx.controls,javafx.fxml ^
     --output dist/jre
   ```

3. Copia la carpeta `dist/jre` junto con el resto de archivos. En Launch4j, establece **Bundled JRE path** en `jre` (ruta relativa) para que el ejecutable use ese runtime.

### Opción B: reutilizar el flujo 100 % por comandos (`jpackage`)

Si prefieres no usar Launch4j, aplica exactamente los comandos descritos en la [sección 4](#jpackage-cli). Ese flujo ya produce un instalador `.exe` auto contenido que empaqueta la aplicación junto con el runtime y funciona en equipos sin Java preinstalado.

## 7. Crear un instalador (`Setup.exe`) opcional

Si optaste por Launch4j y necesitas un instalador clásico (cuando no uses `jpackage`):

1. Abre Inno Setup y genera un script básico que copie toda la carpeta `dist/` a `C:\Program Files\Control Gimnasio`.
2. Crea accesos directos al `.exe` en el escritorio y en el menú Inicio.
3. Agrega una acción post-instalación para abrir la carpeta `driver/` o mostrar instrucciones para iniciar sesión en WhatsApp Web y escanear el código QR en el primer uso.
4. Incluye reglas para ejecutar el instalador como administrador (necesario si piensas instalar servicios de Windows).

## 8. Pasos posteriores en la máquina del cliente

1. Instala el paquete generado (copiando la carpeta `dist/` o utilizando el instalador de Inno Setup).
2. Ejecuta el `.exe` la primera vez manualmente para permitir el escaneo del código QR de WhatsApp Web.
3. Verifica que la carpeta `backups/` tenga permisos de escritura.
4. Agenda tareas programadas o servicios solo si necesitas automatizar la ejecución (`gimnasio.bat`).
5. Documenta credenciales, rutas y cualquier variable de entorno adicional (por ejemplo, si decides mover el `chromedriver.exe`).

## 9. Prueba en una máquina limpia

Antes de entregar al cliente:

1. Instala el paquete en una máquina virtual sin JDK ni Maven.
2. Lanza la aplicación y verifica:
   - Inicio de sesión y funciones principales.
   - Acceso a la base de datos (lectura/escritura en `database/gimnasio.db`).
   - Generación de respaldos en `backups/`.
   - Envío de mensajes por WhatsApp (si el cliente lo usará desde el inicio).
3. Documenta cualquier paso manual adicional detectado durante la prueba.

Con estos pasos tendrás un `.exe` autónomo que replica el comportamiento de tu entorno de desarrollo, incluyendo la base de datos local y todas las dependencias necesarias.
