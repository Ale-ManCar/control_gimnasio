# Preguntas frecuentes

## ¿Por qué el instalador se abre pero la aplicación no muestra nada?

Si el instalador crea el acceso directo pero al hacer doble clic no se abre la aplicación, revisa que los datos iniciales se hayan copiado a la carpeta de usuario:

1. Abre el Explorador de archivos y navega a `%LOCALAPPDATA%\\ControlGimnasio`.
2. Verifica que existan los archivos `gimnasio.db` y `CONFIGURACION.txt`.
3. Si no están, elimina la aplicación e instala de nuevo asegurándote de no mover ni renombrar la carpeta `dist` antes de ejecutar el instalador.
4. Si los archivos están presentes pero la app sigue sin abrir, ejecuta `ControlGimnasio.exe` desde la línea de comandos para ver si muestra algún mensaje de error adicional.

## ¿Qué hago si el ejecutable muestra "Failed to launch JVM"?

Ese mensaje indica que el runtime incluido en el instalador no contiene los módulos de JavaFX. Genera nuevamente el `.exe` indicando la carpeta `lib` del SDK de JavaFX (por ejemplo `C:\java\javafx-sdk-21\lib`) mediante `-JavaFxModulePath` o configurando la variable `JAVA_FX_MODULE_PATH` antes de ejecutar el script.

## ¿Cómo verifico si `jpackage` está instalado?

Ejecuta en PowerShell:

```powershell
jpackage --version
```

Si el comando devuelve un número de versión (por ejemplo `21.0.1`), significa que `jpackage` está disponible en el `PATH`. El script de empaquetado mostrará esa misma versión cuando lo ejecute, así podrás confirmar cuál binario está usando. Si ves un error indicando que el comando no se reconoce, ejecuta la herramienta directamente desde el JDK:

```powershell
& "$env:JAVA_HOME\bin\jpackage.exe" --version
```

Si este segundo comando también falla, revisa que tengas instalado un JDK 14 o superior y que `JAVA_HOME` apunte a esa carpeta.

## ¿Qué hago si el instalador falla por JavaFX?

Puedes proporcionar la ruta del SDK de JavaFX al script de empaquetado:

```powershell
./scripts/package-windows-exe.ps1 -AppVersion 1.1.0 -JavaFxModulePath "C:\\java\\javafx-sdk-21\\lib"
```

Esto añade los módulos necesarios al comando `jpackage` para que el ejecutable resultante incluya todas las dependencias.

Si además tienes Maven o el JDK instalados fuera del `PATH`, también puedes indicarlos explícitamente:

```powershell
./scripts/package-windows-exe.ps1 -JdkPath "C:\\java\\jdk-21" -MavenExecutable "C:\\Maven\\apache-maven-3.9.11\\bin\\mvn.cmd"
```

El script validará las rutas, mostrará cuáles está utilizando y seguirá con el empaquetado sin necesidad de mover las instalaciones a carpetas estándar.
