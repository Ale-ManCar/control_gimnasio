# Generar instalador de Windows sin requerir Java preinstalado

Este proyecto se puede distribuir como un `.exe` que incluye su propio runtime de Java. Para lograrlo se usa [jpackage](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html), disponible en JDK 14 o superior.

## Requisitos previos

1. **JDK 21** (o superior) instalado en Windows y con `JAVA_HOME` apuntando a su carpeta. El script intentará localizarlo automáticamente si está en rutas comunes como `C:\java\jdk-21` o `C:\Program Files\Java\jdk-21`, pero la forma más fiable es configurar `JAVA_HOME`. El JDK debe incluir la herramienta `jpackage` (las descargas de Oracle u OpenJDK como Temurin lo incluyen). Puedes comprobarlo ejecutando `jpackage --version` en PowerShell; si no está en el `PATH`, usa `& "$env:JAVA_HOME\bin\jpackage.exe" --version` para verificar que el ejecutable existe. El script mostrará la ruta y la versión detectada antes de crear el instalador, así sabrás exactamente qué `jpackage` se utiliza.
2. **Maven 3.8+** disponible en la variable de entorno `PATH`. Si no está en el `PATH`, puedes indicar la ruta completa al ejecutable (`mvn.cmd`) mediante el nuevo parámetro `-MavenExecutable`.
3. Acceso a PowerShell 5+.

> ⚠️ El empaquetado `.exe` solo se puede generar desde Windows. Para otros sistemas operativos utiliza el formato nativo correspondiente (`.pkg`, `.dmg`, `.deb`, `.rpm`, etc.).

## Pasos

1. Abrir una consola de PowerShell en la raíz del proyecto (`control_gimnasio`).
2. Ejecutar el script de empaquetado:
   ```powershell
   .\scripts\package-windows-exe.ps1 -AppVersion 1.1.0
   ```
   - El parámetro `-AppVersion` es opcional y permite versionar el instalador.
   - El script ejecuta `mvn clean package` para generar un JAR con todas las dependencias y luego invoca `jpackage`.
   - Antes de llamar a `jpackage`, el script copia la base de datos inicial (`database/gimnasio.db`) y el archivo `CONFIGURACION.txt` a una carpeta temporal para que queden incluidos dentro del instalador.
   - Es obligatorio contar con un SDK de JavaFX para Windows. Proporciona su carpeta `lib` mediante `-JavaFxModulePath` (por ejemplo `-JavaFxModulePath "C:\\java\\javafx-sdk-21\\lib"`). El script intentará detectarlo automáticamente consultando `JAVA_FX_MODULE_PATH`, `JAVA_FX_SDK_LIB`, `JAVA_FX_SDK` o rutas comunes como `C:\java\javafx-sdk-21\lib`; si no logra encontrarlo, detendrá el proceso para evitar crear un instalador incompleto.
   - Si tu instalación de Maven no está en el `PATH`, pasa su ruta completa: `.\scripts\package-windows-exe.ps1 -MavenExecutable "C:\\Maven\\apache-maven-3.9.11\\bin\\mvn.cmd"`.
   - Si alguna de las etapas falla (por ejemplo, porque Maven no puede descargar dependencias o `jpackage` detecta un error de configuración), el script se detendrá y mostrará el código de salida correspondiente. Corrige el problema indicado antes de volver a ejecutarlo.
3. El instalador se generará dentro de la carpeta `dist/` con un nombre similar a `ControlGimnasio-1.1.0.exe`.
4. Entrega ese archivo al cliente. El instalador incluye un runtime de Java personalizado, por lo que no se necesita instalar Java en las máquinas destino y además se instala en modo *per-user*, ubicándose en `%LOCALAPPDATA%\Programs\ControlGimnasio`. Durante la primera ejecución la aplicación copia la base de datos inicial y `CONFIGURACION.txt` a `%LOCALAPPDATA%\ControlGimnasio` (o `~/.control_gimnasio` en otros sistemas), un directorio con permisos de escritura donde también se almacenan los respaldos automáticos.

## Contenido del instalador

El instalador crea los accesos directos en el menú inicio y (opcionalmente) en el escritorio. Además, durante la instalación copia un runtime reducido de Java preparado específicamente para esta aplicación, la base de datos SQLite inicial y el archivo `CONFIGURACION.txt`. En la primera ejecución estos archivos se colocan automáticamente en la carpeta de datos del usuario (`%LOCALAPPDATA%\ControlGimnasio`), por lo que el programa puede leerlos y actualizarlos sin pasos manuales.

Si necesitas personalizar el nombre del acceso directo, iconos u otras opciones de jpackage, edita el script `scripts/package-windows-exe.ps1`.

## Solución de problemas

- **Al abrir el `.exe` aparece "Failed to launch JVM"**: el instalador se creó sin los módulos de JavaFX. Repite el empaquetado indicando la ruta `lib` del SDK (por ejemplo `-JavaFxModulePath "C:\\java\\javafx-sdk-21\\lib"`) o configurando `JAVA_FX_MODULE_PATH` antes de ejecutar el script.
- **La aplicación instalada no abre**: confirma que, tras el primer inicio, exista la carpeta `%LOCALAPPDATA%\ControlGimnasio` con los archivos `gimnasio.db` y `CONFIGURACION.txt`. Si faltan, elimina la carpeta `%LOCALAPPDATA%\ControlGimnasio` y vuelve a ejecutar la aplicación para que copie nuevamente los recursos empaquetados. También verifica que el instalador se haya generado con la versión más reciente del script `package-windows-exe.ps1`, que incluye estos recursos dentro del ejecutable.
