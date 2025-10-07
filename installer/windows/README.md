# Construir el instalador de Windows

El script `build-installer.ps1` automatiza los pasos para generar un `.exe` de Gimnasio Control con `jpackage`. Debe ejecutarse desde PowerShell (Windows 10/11) con permisos para escribir en el directorio `target/installer`.

## Requisitos previos

1. `JAVA_HOME` apuntando a un **JDK 21** que contenga `bin\jpackage.exe`.
2. `PATH` iniciado con `%JAVA_HOME%\bin` para que Windows utilice el JDK correcto.
3. `JAVAFX_JMODS` apuntando al directorio que contiene los archivos `*.jmod` de JavaFX (por ejemplo `C:\java\javafx-jmods-21`).
4. [Apache Maven](https://maven.apache.org/) instalado y disponible en el `PATH`.
5. PowerShell 5.1 o superior.

Puedes verificar rápidamente que el entorno está listo con:

```powershell
Get-Command mvn
Get-Command "$env:JAVA_HOME\bin\jpackage.exe"
Test-Path $env:JAVAFX_JMODS
```

Si alguno de los comandos devuelve `False` o un error, corrige las variables de entorno antes de continuar.

## Uso básico

```powershell
powershell -ExecutionPolicy Bypass -File installer/windows/build-installer.ps1
```

Al ejecutarlo sin parámetros el script utiliza valores por defecto:

| Parámetro       | Valor por defecto             | Descripción |
|-----------------|-------------------------------|-------------|
| `AppName`       | `GimnasioControl`              | Nombre de la aplicación e instalador. |
| `AppVersion`    | `1.0.0`                        | Se usa para el nombre del archivo `.exe` y metadatos. |
| `Vendor`        | `Gimnasio Control`             | Fabricante mostrado en el instalador y menú inicio. |
| `MainClass`     | `Main`                         | Clase principal del proyecto. |
| `IconPath`      | `src\main\resources\images\icono.ico` | Icono que verá el usuario. |
| `UpgradeUuid`   | Generado automáticamente       | Identificador para actualizaciones de Windows. |

### Personalización

Ejemplo con valores propios:

```powershell
powershell -ExecutionPolicy Bypass -File installer/windows/build-installer.ps1 \
  -AppName "ControlGimnasio" \
  -AppVersion "1.2.3" \
  -Vendor "Mi Empresa" \
  -IconPath "assets\branding\logo.ico" \
  -UpgradeUuid "0e0465fd-9cbf-4baf-8f62-4f2d4a0b7d63"
```

## Qué genera el script

* `target/installer/app/` con el `.jar`, dependencias y recursos necesarios.
* `target/installer/GimnasioControl-1.0.0.exe` (o el nombre según tus parámetros).
* Un archivo de log de jpackage en la consola para depuración.

Repite la ejecución cada vez que haya cambios en el código o dependencias.

## Launch4j como alternativa

Si prefieres crear un ejecutable ligero con una JVM empaquetada manualmente, abre el archivo `launch4j-config.xml` con Launch4j, ajusta rutas e iconos y genera el `.exe`. Posteriormente, empaca el directorio resultante con Inno Setup, NSIS u otra herramienta de instaladores.
