param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$AppVersion = "1.1.0",
    [string]$OutputDir = "dist",
    [string]$JavaFxModulePath
)

$ErrorActionPreference = "Stop"

if (-not $JdkPath) {
    throw "JAVA_HOME no está configurada. Define JAVA_HOME apuntando a un JDK que incluya jpackage (JDK 14+)."
}

$projectRoot = Resolve-Path "$PSScriptRoot/.."
$targetDir = Join-Path $projectRoot "target"
$resourceStagingDir = Join-Path $targetDir "jpackage-resources"

$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { "mvn.cmd" }

Write-Host "Compilando el proyecto y generando el JAR con dependencias..."
& $mvnCmd clean package -DskipTests | Write-Output
if ($LASTEXITCODE -ne 0) {
    throw "La compilación con Maven finalizó con código $LASTEXITCODE. Revisa los mensajes anteriores para más detalles."
}

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

$jpackageArgs = @(
    "--type", "exe",
    "--name", "ControlGimnasio",
    "--app-version", $AppVersion,
    "--input", (Resolve-Path $targetDir),
    "--main-jar", (Split-Path -Leaf $mainJar),
    "--main-class", "Main",
    "--dest", (Resolve-Path $OutputDir),
    "--win-shortcut",
    "--win-menu",
    "--vendor", "Control Gimnasio",
    "--icon", (Resolve-Path $iconPath),
    "--java-options", "--add-opens java.base/java.lang=ALL-UNNAMED",
    "--java-options", "--add-opens java.base/java.time=ALL-UNNAMED",
    "--jlink-options", "--strip-debug --no-header-files --no-man-pages"
)

if ($JavaFxModulePath) {
    $resolvedJavaFxPath = Resolve-Path $JavaFxModulePath -ErrorAction Stop
    $jpackageArgs += @("--module-path", $resolvedJavaFxPath)
    $jpackageArgs += @("--add-modules", "javafx.controls,javafx.fxml")
    Write-Host "Usando JavaFX module path" $resolvedJavaFxPath
} else {
    Write-Host "No se especificó JavaFxModulePath; se asume que las dependencias de JavaFX están incluidas en el JAR con dependencias."
}

if (Test-Path $resourceStagingDir) {
    Remove-Item $resourceStagingDir -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $resourceStagingDir | Out-Null

$resourcesToBundle = @("database", "CONFIGURACION.txt")

foreach ($resource in $resourcesToBundle) {
    $sourcePath = Join-Path $projectRoot $resource
    if (-not (Test-Path $sourcePath)) {
        throw "No se encontró el recurso '$resource' en el proyecto."
    }

    $destinationPath = Join-Path $resourceStagingDir $resource
    if ((Get-Item $sourcePath).PSIsContainer) {
        Copy-Item $sourcePath -Destination $destinationPath -Recurse -Force
    } else {
        $destinationParent = Split-Path $destinationPath -Parent
        if ($destinationParent) {
            New-Item -ItemType Directory -Force -Path $destinationParent | Out-Null
        }
        Copy-Item $sourcePath -Destination $destinationPath -Force
    }
}

$jpackageArgs += @("--resource-dir", (Resolve-Path $resourceStagingDir))
$jpackageArgs += "--win-per-user-install"

Write-Host "Ejecutando jpackage para generar el instalador .exe..."
& $jpackage @jpackageArgs

if ($LASTEXITCODE -ne 0) {
    throw "jpackage terminó con código $LASTEXITCODE. Consulta el detalle del error impreso arriba."
}

$generatedExe = Get-ChildItem -Path $OutputDir -Filter '*.exe' -File -ErrorAction SilentlyContinue
if (-not $generatedExe) {
    throw "No se encontró ningún instalador .exe en '$OutputDir'. Verifica la salida de jpackage para detectar el problema."
}

Write-Host "Instalador generado en" ($generatedExe.FullName)