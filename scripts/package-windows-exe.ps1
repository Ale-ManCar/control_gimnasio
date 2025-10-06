param(
    [string]$JdkPath = $env:JAVA_HOME,
    [string]$MavenExecutable,
    [string]$AppVersion = "1.1.0",
    [string]$OutputDir = "dist",
    [string]$JavaFxModulePath
)

$ErrorActionPreference = "Stop"

function Resolve-JdkPath {
    param(
        [string]$InitialPath
    )

    $candidates = @()

    if ($InitialPath) {
        $candidates += $InitialPath
    }

    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }

    if ($env:JDK_HOME) {
        $candidates += $env:JDK_HOME
    }

    $candidates += @(
        'C:\\java\\jdk-21',
        'C:\\Program Files\\Java\\jdk-21',
        'C:\\Program Files\\Java\\jdk-21.0.1',
        'C:\\Program Files\\Eclipse Adoptium\\jdk-21',
        'C:\\Program Files (x86)\\Java\\jdk-21'
    )

    foreach ($candidate in $candidates | Where-Object { $_ }) {
        try {
            $resolved = (Resolve-Path $candidate -ErrorAction Stop).Path
            $jpackage = Join-Path $resolved "bin/jpackage.exe"
            if (Test-Path $jpackage) {
                return $resolved
            }
        } catch {
            continue
        }
    }

    throw "No se pudo localizar un JDK con jpackage. Define JAVA_HOME o usa el parámetro -JdkPath apuntando al directorio del JDK (por ejemplo C:\\java\\jdk-21)."
}

function Resolve-MavenExecutable {
    param(
        [string]$ExplicitPath
    )

    if ($ExplicitPath) {
        try {
            return (Resolve-Path $ExplicitPath -ErrorAction Stop).Path
        } catch {
            throw "No se encontró Maven en '$ExplicitPath'. Verifica la ruta especificada en -MavenExecutable."
        }
    }

    $command = Get-Command mvn -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $commandCmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($commandCmd) {
        return $commandCmd.Source
    }

    $candidates = @()

    if ($env:MAVEN_HOME) {
        $candidates += (Join-Path $env:MAVEN_HOME "bin/mvn.cmd")
        $candidates += (Join-Path $env:MAVEN_HOME "bin/mvn")
    }

    if ($env:M2_HOME) {
        $candidates += (Join-Path $env:M2_HOME "bin/mvn.cmd")
        $candidates += (Join-Path $env:M2_HOME "bin/mvn")
    }

    $candidates += @(
        'C:\\Maven\\apache-maven-3.9.11\\bin\\mvn.cmd',
        'C:\\Program Files\\Apache\\maven\\bin\\mvn.cmd'
    )

    foreach ($candidate in $candidates | Where-Object { $_ }) {
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate -ErrorAction Stop).Path
        }
    }

    throw "No se pudo encontrar Maven en el PATH ni en ubicaciones comunes. Instálalo o indica la ruta con -MavenExecutable (por ejemplo C:\\Maven\\apache-maven-3.9.11\\bin\\mvn.cmd)."
}

$JdkPath = Resolve-JdkPath -InitialPath $JdkPath
$mvnCmd = Resolve-MavenExecutable -ExplicitPath $MavenExecutable

Write-Host "Usando JDK en" $JdkPath
Write-Host "Usando Maven en" $mvnCmd

$projectRoot = Resolve-Path "$PSScriptRoot/.."
$targetDir = Join-Path $projectRoot "target"
$resourceStagingDir = Join-Path $targetDir "jpackage-resources"

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

try {
    $jpackageVersion = & $jpackage --version 2>&1
} catch {
    throw "No se pudo ejecutar '$jpackage --version'. Verifica que el JDK esté instalado correctamente."
}

if ($LASTEXITCODE -ne 0) {
    throw "'$jpackage --version' finalizó con código $LASTEXITCODE. Revisa la instalación del JDK."
}

Write-Host "Usando jpackage en" $jpackage "(versión $jpackageVersion)"

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

$resolvedJavaFxPath = $null

if ($JavaFxModulePath) {
    $resolvedJavaFxPath = Resolve-Path $JavaFxModulePath -ErrorAction Stop
} else {
    $javaFxCandidates = @()

    if ($env:JAVA_FX_MODULE_PATH) {
        $javaFxCandidates += $env:JAVA_FX_MODULE_PATH
    }

    if ($env:JAVA_FX_SDK_LIB) {
        $javaFxCandidates += $env:JAVA_FX_SDK_LIB
    }

    if ($env:JAVA_FX_SDK) {
        $javaFxCandidates += (Join-Path $env:JAVA_FX_SDK "lib")
    }

    $javaFxCandidates += @(
        'C:\\java\\javafx-sdk-21\\lib',
        'C:\\javafx-sdk-21\\lib'
    )

    foreach ($candidate in $javaFxCandidates) {
        if (-not $candidate) { continue }
        if (Test-Path $candidate) {
            try {
                $resolvedJavaFxPath = Resolve-Path $candidate -ErrorAction Stop
                break
            } catch {
                continue
            }
        }
    }
}

if (-not $resolvedJavaFxPath) {
    throw "No se pudo localizar el SDK de JavaFX. Indica la carpeta 'lib' con -JavaFxModulePath (por ejemplo C:\\java\\javafx-sdk-21\\lib) o define la variable de entorno JAVA_FX_MODULE_PATH."
}

$jpackageArgs += @("--module-path", $resolvedJavaFxPath)
$jpackageArgs += @("--add-modules", "javafx.controls,javafx.fxml")
Write-Host "Usando JavaFX module path" $resolvedJavaFxPath

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
