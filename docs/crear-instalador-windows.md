# Generar instalador de Windows sin requerir Java preinstalado

Este proyecto se puede distribuir como un `.exe` que incluye su propio runtime de Java. Para lograrlo se usa [jpackage](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html), disponible en JDK 14 o superior.

## Requisitos previos

1. **JDK 21** (o superior) instalado en Windows y con `JAVA_HOME` apuntando a su carpeta. El JDK debe incluir la herramienta `jpackage` (las descargas de Oracle u OpenJDK como Temurin lo incluyen).
2. **Maven 3.8+** disponible en la variable de entorno `PATH`.
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
3. El instalador se generará dentro de la carpeta `dist/` con un nombre similar a `ControlGimnasio-1.1.0.exe`.
4. Entrega ese archivo al cliente. El instalador incluye un runtime de Java personalizado, por lo que no se necesita instalar Java en las máquinas destino.

## Contenido del instalador

El instalador crea los accesos directos en el menú inicio y (opcionalmente) en el escritorio. Además, durante la instalación copia un runtime reducido de Java preparado específicamente para esta aplicación, por lo que funciona aunque el usuario no tenga Java instalado previamente.

Si necesitas personalizar el nombre del acceso directo, iconos u otras opciones de jpackage, edita el script `scripts/package-windows-exe.ps1`.
