# Guía completa para generar un instalador `.exe` sin errores de JVM

Este repositorio contiene una aplicación JavaFX empaquetada con Maven. Para distribuirla como ejecutable en Windows necesitas:

1. Un **JDK 21** configurado correctamente.
2. Las librerías JavaFX en formato **jmods** (requeridas por `jpackage`).
3. Un `.jar` con todas las dependencias de tu aplicación.
4. Un comando o script que invoque `jpackage` y agrupe todo en un instalador.

Los apartados siguientes detallan cada paso y te proporcionan un script reutilizable junto con una configuración opcional para Launch4j.

## 1. Preparar el entorno de construcción

1. Instala **JDK 21** (por ejemplo en `C:\java\jdk-21`) y define las variables de entorno en *Panel de control → Sistema → Configuración avanzada del sistema → Variables de entorno*:
   - `JAVA_HOME=C:\java\jdk-21`
   - Añade `%JAVA_HOME%\bin` al **inicio** de `PATH`.
2. Descarga el paquete **JavaFX jmods** que coincida con tu versión (por ejemplo `javafx-jmods-21.zip`) desde [https://gluonhq.com/products/javafx/](https://gluonhq.com/products/javafx/) y descomprímelo, por ejemplo en `C:\java\javafx-jmods-21`.
3. Crea la variable `JAVAFX_JMODS=C:\java\javafx-jmods-21` y comprueba todo desde una ventana de *Command Prompt*:

   ```bat
   echo %JAVA_HOME%
   dir %JAVA_HOME%\bin
   where java
   where jpackage
   echo %JAVAFX_JMODS%
   dir %JAVAFX_JMODS%
   ```

   Las rutas deben apuntar a los directorios que acabas de configurar. Si `where java` muestra otras instalaciones (por ejemplo `javapath`), deja tu JDK 21 al inicio del `PATH` para que tenga prioridad.

4. Verifica que Maven esté disponible (`mvn -v`). Si no lo tienes, instala [Apache Maven](https://maven.apache.org/download.cgi) y añade su carpeta `bin` al `PATH`.

## 2. Generar el `.jar` con dependencias

El proyecto ya define la clase principal `Main` en `src/main/java/Main.java`. Para producir un `jar` listo para empaquetar ejecuta en la raíz del repositorio:

```bat
mvn clean package
mvn dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\installer\app\lib
```

Los comandos anteriores generan:

* `target/gimnasio_control-1.0-SNAPSHOT.jar`: el artefacto principal.
* `target/installer/app/lib`: todas las dependencias externas (JavaFX, JasperReports, Selenium, etc.) listas para que `jpackage` las incluya en el classpath.

## 3. Script automatizado con `jpackage`

Para evitar errores al lanzar la JVM se incluye el script `installer/windows/build-installer.ps1`. Ejecuta una terminal de **PowerShell** y lanza:

```powershell
cd RUTA\AL\PROYECTO
powershell -ExecutionPolicy Bypass -File installer/windows/build-installer.ps1 \
  -AppVersion "1.0.0" \
  -Vendor "Tu Empresa" \
  -UpgradeUuid "12345678-1234-1234-1234-123456789abc"
```

El script realiza automáticamente:

1. Validaciones de entorno (`JAVA_HOME`, `JAVAFX_JMODS`, Maven y `jpackage`).
2. Construcción del `jar` y copiado de dependencias, archivos de configuración (`CONFIGURACION.txt`), carpeta `lib/`, recursos gráficos y respaldos necesarios.
3. Ejecución de `jpackage` con `Main` como clase principal, icono `src/main/resources/images/icono.ico`, módulos JavaFX y opciones para crear accesos directos del menú de inicio.
4. Generación de un instalador en `target/installer/GimnasioControl-1.0.0.exe` (puedes ajustar el nombre con los parámetros del script).

Revisa `installer/windows/README.md` para conocer todos los parámetros personalizables del script.

### Resultado del directorio `target/installer`

```
target/installer/
├─ app/                (archivos que usará jpackage)
│  ├─ GimnasioControl.jar
│  ├─ CONFIGURACION.txt (si existe en el proyecto)
│  ├─ lib/             (dependencias externas)
│  ├─ images/          (recursos estáticos)
│  └─ backups/         (copias de seguridad, si las tuvieras)
└─ GimnasioControl-1.0.0.exe
```

Prueba el `.exe` final en una máquina Windows limpia para confirmar que la JVM embebida funciona correctamente.

## 4. Personalizar o depurar `jpackage`

Si necesitas ejecutar `jpackage` manualmente (por ejemplo para depurar), puedes basarte en el comando que genera el script:

```bat
jpackage ^
  --type exe ^
  --name GimnasioControl ^
  --input target\installer\app ^
  --main-jar GimnasioControl.jar ^
  --main-class Main ^
  --class-path lib\* ^
  --module-path %JAVAFX_JMODS%;%JAVA_HOME%\jmods ^
  --add-modules javafx.controls,javafx.fxml ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --icon src\main\resources\images\icono.ico ^
  --dest target\installer ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-menu-group "Tu Empresa" ^
  --win-upgrade-uuid 12345678-1234-1234-1234-123456789abc
```

Sustituye parámetros como `--name`, `--win-menu-group`, `--win-upgrade-uuid` o el icono según tus necesidades.

## 5. Alternativa: Launch4j con JVM embebida

Si prefieres un ejecutable ligero que delegue en un JRE empacado manualmente, utiliza [Launch4j](https://launch4j.sourceforge.net/). Este repositorio incluye la plantilla `installer/windows/launch4j-config.xml` que espera la siguiente estructura:

```
dist/
├─ GimnasioControl.exe         (resultado de Launch4j)
├─ GimnasioControl.jar         (renombrado desde target/gimnasio_control-1.0-SNAPSHOT.jar)
├─ lib/                        (dependencias externas)
├─ jre/                        (JRE o JDK recortado con jlink)
└─ recursos adicionales
```

Abre el archivo XML en la interfaz de Launch4j para ajustar datos como el `outfile`, la ruta del icono o el identificador de tu empresa. Después puedes empaquetar la carpeta `dist/` con instaladores como Inno Setup o NSIS.

---

Con estas herramientas y archivos dispones de todo lo necesario para generar un `.exe` estable y listo para distribución sin errores de tipo “Failed to launch JVM”.
