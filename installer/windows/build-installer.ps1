[CmdletBinding()]
param(
    [string]$AppName = "GimnasioControl",
    [string]$AppVersion = "1.0.0",
    [string]$Vendor = "Gimnasio Control",
    [string]$MainClass = "Main",
    [string]$IconPath = "src\main\resources\images\icono.ico",
    [string]$UpgradeUuid = "",
    [string]$JavaFxJmods = $env:JAVAFX_JMODS
)

$ErrorActionPreference = 'Stop'

function Assert-FileExists {
    param([string]$Path, [string]$Message)
    if (-not (Test-Path $Path)) {
        throw $Message
    }
}

function Invoke-CommandChecked {
    param([string]$Command, [string[]]$Arguments)
    Write-Host "→ $Command $($Arguments -join ' ')"
    $process = Start-Process -FilePath $Command -ArgumentList $Arguments -NoNewWindow -PassThru -Wait
    if ($process.ExitCode -ne 0) {
        throw "El comando '$Command' terminó con código $($process.ExitCode)."
    }
}

$projectRoot = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $projectRoot

Assert-FileExists -Path $env:JAVA_HOME -Message "JAVA_HOME no está definido o apunta a una ruta inexistente."
Assert-FileExists -Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe') -Message "No se encontró jpackage en %JAVA_HOME%\bin. Instala un JDK 21 completo."

if (-not $JavaFxJmods) {
    throw "Debes definir la variable de entorno JAVAFX_JMODS apuntando a los jmods de JavaFX (por ejemplo C:\\java\\javafx-jmods-21)."
}
Assert-FileExists -Path $JavaFxJmods -Message "La ruta definida en JAVAFX_JMODS no existe: $JavaFxJmods"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "No se encontró Maven en el PATH. Instala Maven y agrégalo al PATH."
}

$installerRoot = Join-Path $projectRoot 'target\installer'
$appDir = Join-Path $installerRoot 'app'
$libDir = Join-Path $appDir 'lib'

if (Test-Path $appDir) {
    Remove-Item $appDir -Recurse -Force
}
New-Item -ItemType Directory -Path $libDir -Force | Out-Null

Invoke-CommandChecked -Command 'mvn' -Arguments @('clean', 'package', '--batch-mode')
Invoke-CommandChecked -Command 'mvn' -Arguments @('dependency:copy-dependencies', '-DincludeScope=runtime', "-DoutputDirectory=$libDir", '--batch-mode')

$artifact = Join-Path $projectRoot 'target\gimnasio_control-1.0-SNAPSHOT.jar'
Assert-FileExists -Path $artifact -Message "No se generó el jar esperado en target/. Verifica que Maven haya terminado correctamente."

$mainJar = Join-Path $appDir ("{0}.jar" -f $AppName)
Copy-Item $artifact $mainJar -Force

$extraItems = @('CONFIGURACION.txt', 'lib', 'backups')
foreach ($item in $extraItems) {
    $source = Join-Path $projectRoot $item
    if (Test-Path $source) {
        Copy-Item $source $appDir -Recurse -Force
    }
}

$imagesDir = Join-Path $projectRoot 'src\main\resources\images'
if (Test-Path $imagesDir) {
    Copy-Item $imagesDir (Join-Path $appDir 'images') -Recurse -Force
}

$resolvedIcon = Resolve-Path (Join-Path $projectRoot $IconPath)
$uuid = if ([string]::IsNullOrWhiteSpace($UpgradeUuid)) { ([guid]::NewGuid()).ToString() } else { $UpgradeUuid }

$modulePath = (Join-Path $env:JAVA_HOME 'jmods') + ';' + (Resolve-Path $JavaFxJmods)
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'

$requiredModules = 'javafx.controls,javafx.fxml,javafx.graphics,java.sql,java.xml,java.naming,java.desktop,java.management,java.scripting'

$jpackageArgs = @(
    '--type', 'exe',
    '--name', $AppName,
    '--app-version', $AppVersion,
    '--vendor', $Vendor,
    '--input', $appDir,
    '--main-jar', (Split-Path $mainJar -Leaf),
    '--main-class', $MainClass,
    '--class-path', 'lib\\*',
    '--module-path', $modulePath,
    '--add-modules', $requiredModules,
    '--java-options', '-Dfile.encoding=UTF-8',
    '--icon', $resolvedIcon,
    '--dest', $installerRoot,
    '--win-dir-chooser',
    '--win-shortcut',
    '--win-menu',
    '--win-menu-group', $Vendor,
    '--win-upgrade-uuid', $uuid
)

Write-Host "Ejecutando jpackage..."
Invoke-CommandChecked -Command $jpackage -Arguments $jpackageArgs

Write-Host "Instalador generado en:" (Join-Path $installerRoot ("{0}-{1}.exe" -f $AppName, $AppVersion))
Write-Host "UUID de actualización utilizado: $uuid"
