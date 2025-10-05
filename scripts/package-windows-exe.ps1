param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$AppVersion = "1.1.0",
    [string]$OutputDir = "dist"
)

$ErrorActionPreference = "Stop"

if (-not $JdkPath) {
    throw "JAVA_HOME no está configurada. Define JAVA_HOME apuntando a un JDK que incluya jpackage (JDK 14+)."
}

$projectRoot = Resolve-Path "$PSScriptRoot/.."
$targetDir = Join-Path $projectRoot "target"

$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { "mvn.cmd" }

Write-Host "Compilando el proyecto y generando el JAR con dependencias..."
& $mvnCmd clean package -DskipTests | Write-Output

$mainJar = Join-Path $targetDir "control-gimnasio.jar"
if (-not (Test-Path $mainJar)) {
    throw "No se encontró $mainJar. Verifica que la fase de empaquetado se ejecutó correctamente."
}

$jpackage = Join-Path $JdkPath "bin/jpackage.exe"
if (-not (Test-Path $jpackage)) {
    throw "No se encontró jpackage en $jpackage. Asegúrate de usar un JDK completo (no solo un JRE)."
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$iconPath = Join-Path $projectRoot "src/main/resources/images/icono.ico"

Write-Host "Ejecutando jpackage para generar el instalador .exe..."
& $jpackage `
    --type exe `
    --name "ControlGimnasio" `
    --app-version $AppVersion `
    --input $targetDir `
    --main-jar (Split-Path -Leaf $mainJar) `
    --main-class Main `
    --dest (Resolve-Path $OutputDir) `
    --win-shortcut `
    --win-menu `
    --vendor "Control Gimnasio" `
    --icon $iconPath `
    --add-modules "javafx.controls,javafx.fxml" `
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" `
    --java-options "--add-opens java.base/java.time=ALL-UNNAMED" `
    --jlink-options "--strip-debug --no-header-files --no-man-pages"

Write-Host "Instalador generado en" (Resolve-Path $OutputDir)
