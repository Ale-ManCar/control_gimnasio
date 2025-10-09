@echo off
setlocal EnableDelayedExpansion

REM Directory resolution -----------------------------------------------------
set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
cd /d "%PROJECT_DIR%"

REM Resolve Maven coordinates -------------------------------------------------
for /f "delims=" %%i in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.artifactId') do set "ARTIFACT_ID=%%i"
for /f "delims=" %%i in ('mvn -q -DforceStdout help:evaluate -Dexpression=project.version') do set "PROJECT_VERSION=%%i"
for /f "delims=" %%i in ('mvn -q -DforceStdout help:evaluate -Dexpression=app.release.version') do set "APP_VERSION=%%i"
if not defined APP_VERSION set "APP_VERSION=1.0.0"

set "TARGET_DIR=%PROJECT_DIR%\target"
set "LIB_DIR=%TARGET_DIR%\app-libs"
set "INPUT_DIR=%TARGET_DIR%\jpackage-input"
set "RUNTIME_IMAGE=%TARGET_DIR%\runtime-image"
set "RESOURCE_DIR=%TARGET_DIR%\jpackage-resources"
set "DIST_DIR=%PROJECT_DIR%\dist"

REM Build the project --------------------------------------------------------
call mvn -B -DskipTests clean package
if errorlevel 1 (
    echo Maven build failed.
    exit /b 1
)

if not exist "%LIB_DIR%" (
    echo Runtime dependency directory not found: %LIB_DIR%
    echo Make sure the Maven build ran successfully.
    exit /b 1
)

REM Prepare input directory for jpackage -------------------------------------
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"

set "MAIN_JAR="
if exist "%TARGET_DIR%\%ARTIFACT_ID%-%PROJECT_VERSION%.jar" (
    set "MAIN_JAR=%ARTIFACT_ID%-%PROJECT_VERSION%.jar"
) else (
    for %%f in ("%TARGET_DIR%\%ARTIFACT_ID%-*.jar") do (
        set "MAIN_JAR=%%~nxf"
        goto foundJar
    )
)
:foundJar
if not defined MAIN_JAR (
    echo Could not find the application JAR inside %TARGET_DIR%.
    exit /b 1
)

copy "%TARGET_DIR%\%MAIN_JAR%" "%INPUT_DIR%" >nul
xcopy "%LIB_DIR%" "%INPUT_DIR%" /E /I /Y >nul

REM Create custom runtime image ----------------------------------------------
if exist "%RUNTIME_IMAGE%" rmdir /s /q "%RUNTIME_IMAGE%"
jlink --strip-debug --no-header-files --no-man-pages --compress=2 --add-modules java.se --output "%RUNTIME_IMAGE%"
if errorlevel 1 (
    echo jlink failed. Ensure you are running this script with a JDK that includes jlink.
    exit /b 1
)

REM Prepare external resources -----------------------------------------------
if exist "%RESOURCE_DIR%" rmdir /s /q "%RESOURCE_DIR%"
mkdir "%RESOURCE_DIR%"

if exist "%PROJECT_DIR%\CONFIGURACION.txt" copy "%PROJECT_DIR%\CONFIGURACION.txt" "%RESOURCE_DIR%" >nul
if exist "%PROJECT_DIR%\database" xcopy "%PROJECT_DIR%\database" "%RESOURCE_DIR%\database" /E /I /Y >nul
if exist "%PROJECT_DIR%\backups" xcopy "%PROJECT_DIR%\backups" "%RESOURCE_DIR%\backups" /E /I /Y >nul
if exist "%PROJECT_DIR%\lib" xcopy "%PROJECT_DIR%\lib" "%RESOURCE_DIR%\lib" /E /I /Y >nul

REM Ensure output directory exists ------------------------------------------
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

REM Package the Windows installer -------------------------------------------
jpackage ^
  --type exe ^
  --name "ControlGimnasio" ^
  --app-version "%APP_VERSION%" ^
  --description "Sistema de control de gimnasio" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class Main ^
  --runtime-image "%RUNTIME_IMAGE%" ^
  --resource-dir "%RESOURCE_DIR%" ^
  --dest "%DIST_DIR%" ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "ControlGimnasio"

if errorlevel 1 (
    echo jpackage failed. Review the output above for details.
    exit /b 1
)

echo.
echo Instalador generado en: %DIST_DIR%
endlocal
