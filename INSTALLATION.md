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

### ¿Cómo compruebo que la carpeta `lib` está completa?

Al finalizar los comandos anteriores deberías ver la carpeta `target/installer/app/lib` repleta de `.jar`. Es normal encontrar decenas de archivos, porque Maven copia tanto las dependencias directas como las transitivas. Para confirmar que no falta nada crítico revisa que estén, al menos, estos grupos:

* **JavaFX (con sufijo `-win`)**: `javafx-base-21-win.jar`, `javafx-graphics-21-win.jar`, `javafx-controls-21-win.jar` y `javafx-fxml-21-win.jar`. Si faltan los `-win` es señal de que no se descargaron los artefactos nativos y la interfaz no podrá arrancar.
* **Base de datos y reportes**: `sqlite-jdbc-3.42.0.0.jar` y `jasperreports-6.20.0.jar` (más sus complementos `commons-logging`, `itext`, etc.). Sin ellos, la inicialización de la base o los reportes bloquearán el *splash*.
* **Programador y automatización**: `quartz-2.3.2.jar`, `selenium-java-4.32.0.jar` y `selenium-devtools-v138-4.34.0.jar`, acompañados de los `client-combined`, `okhttp`, `gson`, `byte-buddy`, etc., que suelen aparecer juntos.
* **Iconografía**: `ikonli-javafx-12.3.1.jar` y `ikonli-fontawesome5-pack-12.3.1.jar`.

Si cualquiera de esos grupos falta, vuelve a ejecutar `mvn dependency:copy-dependencies ...` y verifica que no haya fallado por problemas de red. Cuando todo está presente, el instalador contará con el *classpath* completo y el arranque pasará del *splash* al login sin errores de clases faltantes.

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
3. Ejecución de `jpackage` con `Main` como clase principal, icono `src/main/resources/images/icono.ico`, y los módulos requeridos (`javafx.controls`, `javafx.fxml`, `javafx.graphics`, `java.sql`, `java.xml`, `java.naming`, `java.desktop`, `java.management`, `java.scripting`) para que el runtime embebido incluya todo lo que usan JasperReports, Quartz, Selenium y la capa de base de datos.
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

> **Importante:** la base de datos real (`gimnasio.db`) **no** se incluye dentro de `app/lib`. Esa carpeta solo contiene
> bibliotecas `.jar`. El archivo de datos se crea dinámicamente dentro de la carpeta `database/` que acompaña al ejecutable
> (es decir, en `database\gimnasio.db` relativo al directorio de instalación) o, si defines la variable de entorno
> `CONTROL_GIMNASIO_HOME`, en la ruta personalizada indicada. Por lo tanto, al instalar el sistema no necesitas copiar una base
> ya existente dentro del paquete. Solo asegúrate de que la aplicación tenga permisos para crearla en la primera ejecución.

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
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.xml,java.naming,java.desktop,java.management,java.scripting ^
  --java-options "-Dfile.encoding=UTF-8" ^
  --icon src\main\resources\images\icono.ico ^
  --dest target\installer ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser ^
  --win-menu-group "Tu Empresa" ^
  --win-upgrade-uuid 12345678-1234-1234-1234-123456789abc
```

> **Nota:** si omites módulos como `java.sql` o `java.desktop` el runtime generado por `jpackage` quedará “recortado” y, al iniciar, la aplicación se detendrá en el *splash* con errores como `java.lang.NoClassDefFoundError: java/sql/SQLException`. Mantén la lista completa o añade módulos adicionales según tus dependencias.

Sustituye parámetros como `--name`, `--win-menu-group`, `--win-upgrade-uuid` o el icono según tus necesidades.

### Diagnóstico del error `java/sql/SQLException`

Si el ejecutable se cierra justo después del *splash* y en `logs/app.log` aparece un rastro como:

```
[YYYY-MM-DD HH:MM:SS] ERROR: Fallo durante el splash
java.lang.NoClassDefFoundError: java/sql/SQLException
```

significa que el runtime embebido no contiene el módulo `java.sql`. Esto ocurre cuando `jpackage` se ejecuta sin el parámetro `--add-modules` completo (por ejemplo, porque se lanzó una versión anterior del script, se editó el comando manualmente o se reutilizó un instalador generado antes de actualizar la lista de módulos). Vuelve a generar el instalador con el script incluido en este repositorio o asegúrate de pasar explícitamente:

```
--add-modules javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.xml,java.naming,java.desktop,java.management,java.scripting
```

Al incluir `java.sql` el runtime vuelve a exponer `java.sql.SQLException` y la secuencia de arranque continuará hasta la ventana de autenticación.

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
