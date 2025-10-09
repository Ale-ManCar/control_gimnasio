# Empaquetado en Ejecutable (.exe)

Esta guía describe cómo construir un instalador `.exe` para el sistema `control_gimnasio` usando `jpackage` en Windows.

## Requisitos previos

1. **Java Development Kit 21** instalado en `C:\java\jdk-21` (`JAVA_HOME`).
2. **JavaFX jmods 21** en `C:\java\javafx-jmods-21` (`JAVAFX_JMODS`).
3. El directorio `C:\java\jdk-21\bin` incluido en la variable de entorno `Path`.
4. Maven 3.9 o superior en el `Path` para compilar el proyecto.
5. Icono `.ico` opcional para personalizar el instalador (por ejemplo, `recursos\gimnasio.ico`).

## 1. Compilar el proyecto

```powershell
mvn clean package -DskipTests
```

Esto genera el artefacto `target\gimnasio_control-1.0-SNAPSHOT.jar` con los recursos (`.fxml`, `.css`, `.jrxml`, etc.) embebidos.

## 2. Copiar dependencias externas

`jpackage` no descarga automáticamente las librerías en tiempo de ejecución (Selenium, Quartz, Ikonli, etc.), por lo que deben copiarse manualmente. Utiliza el plugin `dependency` de Maven para reunirlas en una carpeta `libs`:

```powershell
mvn dependency:copy-dependencies ^
  -DincludeScope=runtime ^
  -DoutputDirectory=dist\app\libs
```

Al terminar tendrás todos los `.jar` que la aplicación necesita dentro de `dist\app\libs`.

## 3. Preparar archivos adicionales

1. Crea `dist\app` y copia el JAR principal generado en el paso 1.
2. Copia la carpeta `database` completa (`database\gimnasio.db`) dentro de `dist\app`. En la versión instalada, la aplicación detectará ese archivo y lo copiará automáticamente a `%LOCALAPPDATA%\ControlGimnasio\gimnasio.db` (o `~/.control_gimnasio/gimnasio.db` en otros sistemas) para evitar errores de permisos en `Archivos de programa`.
3. Si deseas distribuir controladores externos (por ejemplo `driver\chromedriver.exe`), colócalos dentro de subcarpetas en `dist\app`. La aplicación detecta automáticamente `driver\chromedriver.exe` junto al ejecutable o usa la variable de entorno `CONTROL_GIMNASIO_CHROMEDRIVER` si la defines.

Una estructura típica queda así:

```
dist\app\
 ├─ gimnasio_control-1.0-SNAPSHOT.jar
 ├─ libs\*.jar
 ├─ database\gimnasio.db
 └─ driver\chromedriver.exe
```

> 💡 Para conservar la sesión de WhatsApp Web en un directorio específico puedes establecer `CONTROL_GIMNASIO_WHATSAPP_SESSION`. Si no se define, el sistema usa `%LOCALAPPDATA%\ControlGimnasio\whatsapp_session` (Windows) o `~/.control_gimnasio/whatsapp_session`.

## 4. Crear una imagen de runtime con JavaFX

El ejecutable falla si no incluye los módulos JavaFX nativos. Genera primero una imagen de runtime usando `jlink`, combinando los módulos del JDK y de JavaFX:

```powershell
"%JAVA_HOME%\bin\jlink" ^
  --module-path "%JAVA_HOME%\jmods;%JAVAFX_JMODS%" ^
  --add-modules java.base,java.logging,java.sql,javafx.controls,javafx.fxml ^
  --output dist\runtime
```

Si tu aplicación usa otros módulos Java (por ejemplo `java.desktop` o `java.naming`), agrégalos en `--add-modules`.

## 5. Ejecutar `jpackage`

Con el runtime personalizado listo, genera el instalador:

```powershell
"%JAVA_HOME%\bin\jpackage" ^
  --type exe ^
  --name "Control Gimnasio" ^
  --app-version 1.0.0 ^
  --input dist\app ^
  --main-jar gimnasio_control-1.0-SNAPSHOT.jar ^
  --class-path "libs/*" ^
  --main-class Main ^
  --runtime-image dist\runtime ^
  --module-path "%JAVAFX_JMODS%" ^
  --add-modules javafx.controls,javafx.fxml ^
  --win-menu ^
  --win-shortcut
```

### Ajustes opcionales

- Para incluir un icono personalizado: agrega `--icon dist\gimnasio.ico`.
- Si deseas firmar digitalmente el instalador, usa `--win-sign` con los parámetros de certificado.
- Para especificar proveedor: `--vendor "Nombre de tu organización"`.

## 6. Probar el instalador

El ejecutable generado aparecerá en el directorio actual, con nombre similar a `Control Gimnasio-1.0.0.exe`. Instálalo en una máquina de prueba y verifica que:

- La aplicación inicia correctamente.
- Los reportes, conexión a base de datos y módulos opcionales (WhatsApp, Selenium) funcionan. Si la aplicación se cierra después del splash, revisa `%LOCALAPPDATA%\ControlGimnasio\` y confirma que `gimnasio.db` se copió correctamente o ejecuta el acceso directo "Control Gimnasio" desde una consola (`cmd`) para ver el error detallado.
- Las rutas relativas a recursos funcionan sin necesidad de editar archivos.

## 5. Distribución

Comparte el `.exe` resultante o súbelo a la plataforma de distribución deseada. Mantén un registro de las versiones instaladas para facilitar futuras actualizaciones.

> **Nota:** Si necesitas reducir el tamaño del instalador, considera crear una imagen de runtime personalizada con `jlink` antes de `jpackage`, incluyendo únicamente los módulos Java necesarios.
