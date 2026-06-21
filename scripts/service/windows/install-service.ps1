$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BundleRoot = (Resolve-Path (Join-Path $ScriptDir "..\..\..")).Path
$WayangHome = if ($env:WAYANG_HOME) { $env:WAYANG_HOME } else { "$env:USERPROFILE\.wayang" }
$BundledWayangHome = Join-Path $BundleRoot ".wayang"

if ((Test-Path $BundledWayangHome) -and ($BundledWayangHome -ne $WayangHome)) {
    New-Item -ItemType Directory -Force -Path $WayangHome | Out-Null
    Copy-Item -Path (Join-Path $BundledWayangHome "*") -Destination $WayangHome -Recurse -Force
}

$WayangConfigDir = if ($env:WAYANG_CONFIG_DIR) { $env:WAYANG_CONFIG_DIR } else { Join-Path $WayangHome "config" }
$WayangLogDir = if ($env:WAYANG_LOG_DIR) { $env:WAYANG_LOG_DIR } else { Join-Path $WayangHome "logs" }
$WayangServerLogDir = if ($env:WAYANG_SERVER_LOG_DIR) { $env:WAYANG_SERVER_LOG_DIR } else { Join-Path $WayangLogDir "server" }
$WayangLogFilePath = if ($env:WAYANG_LOG_FILE_PATH) { $env:WAYANG_LOG_FILE_PATH } else { Join-Path $WayangServerLogDir "server.log" }
$WayangPluginsDir = if ($env:WAYANG_PLUGINS_DIR) { $env:WAYANG_PLUGINS_DIR } else { Join-Path $WayangHome "plugins" }
$WayangSecretsDir = if ($env:WAYANG_SECRETS_DIR) { $env:WAYANG_SECRETS_DIR } else { Join-Path $WayangHome "secrets" }
$WayangModelsDir = if ($env:WAYANG_MODELS_DIR) { $env:WAYANG_MODELS_DIR } else { Join-Path $WayangHome "models" }
$WayangMcpDir = if ($env:WAYANG_MCP_DIR) { $env:WAYANG_MCP_DIR } else { Join-Path $WayangHome "mcp" }
$WayangRunDir = if ($env:WAYANG_RUN_DIR) { $env:WAYANG_RUN_DIR } else { Join-Path $WayangHome "run" }
$WayangVectorDir = if ($env:WAYANG_VECTOR_DIR) { $env:WAYANG_VECTOR_DIR } else { Join-Path $WayangHome "vector" }
$GollekHome = if ($env:WAYANG_GOLLEK_HOME) { $env:WAYANG_GOLLEK_HOME } else { Join-Path $WayangHome "gollek" }
$legacyGollekHome = if ($env:GOLLEK_HOME) { $env:GOLLEK_HOME } else { "$env:USERPROFILE\.gollek" }

try {
    New-Item -ItemType Directory -Force -Path $GollekHome | Out-Null
    $ResolvedGollekHome = $GollekHome
} catch {
    New-Item -ItemType Directory -Force -Path $legacyGollekHome | Out-Null
    $ResolvedGollekHome = $legacyGollekHome
}

$WayangGollekModelsDir = if ($env:WAYANG_GOLLEK_MODELS_DIR) { $env:WAYANG_GOLLEK_MODELS_DIR } else { Join-Path $ResolvedGollekHome "models" }
$WayangGollekStorageDir = if ($env:WAYANG_GOLLEK_STORAGE_DIR) { $env:WAYANG_GOLLEK_STORAGE_DIR } else { Join-Path $ResolvedGollekHome "storage" }

@(
    $WayangConfigDir,
    $WayangServerLogDir,
    $WayangPluginsDir,
    $WayangSecretsDir,
    $WayangModelsDir,
    $WayangMcpDir,
    $WayangRunDir,
    $WayangVectorDir,
    $WayangGollekModelsDir,
    $WayangGollekStorageDir
) | ForEach-Object { New-Item -ItemType Directory -Force -Path $_ | Out-Null }

$WayangExecutable = if ($env:WAYANG_EXECUTABLE) { $env:WAYANG_EXECUTABLE } else { Join-Path $WayangHome "bin\wayang.exe" }
if (-not (Test-Path $WayangExecutable)) {
    throw "Wayang executable not found: $WayangExecutable"
}

$ServiceName = if ($env:WAYANG_SERVICE_NAME) { $env:WAYANG_SERVICE_NAME } else { "Wayang" }
$envArgs = @(
    "WAYANG_HOME=$WayangHome",
    "WAYANG_CONFIG_DIR=$WayangConfigDir",
    "WAYANG_LOG_DIR=$WayangLogDir",
    "WAYANG_SERVER_LOG_DIR=$WayangServerLogDir",
    "WAYANG_LOG_FILE_PATH=$WayangLogFilePath",
    "WAYANG_PLUGINS_DIR=$WayangPluginsDir",
    "WAYANG_SECRETS_DIR=$WayangSecretsDir",
    "WAYANG_MODELS_DIR=$WayangModelsDir",
    "WAYANG_MCP_DIR=$WayangMcpDir",
    "WAYANG_RUN_DIR=$WayangRunDir",
    "WAYANG_VECTOR_DIR=$WayangVectorDir",
    "WAYANG_GOLLEK_HOME=$GollekHome",
    "GOLLEK_HOME=$ResolvedGollekHome",
    "WAYANG_GOLLEK_MODELS_DIR=$WayangGollekModelsDir",
    "WAYANG_GOLLEK_STORAGE_DIR=$WayangGollekStorageDir"
)

if (Get-Service -Name $ServiceName -ErrorAction SilentlyContinue) {
    Stop-Service -Name $ServiceName -ErrorAction SilentlyContinue
    sc.exe delete $ServiceName | Out-Null
}

New-Service -Name $ServiceName -BinaryPathName "`"$WayangExecutable`"" -DisplayName "Wayang" -StartupType Automatic
Set-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Services\$ServiceName" -Name Environment -Type MultiString -Value $envArgs
Start-Service -Name $ServiceName
Write-Host "Installed Wayang Windows service '$ServiceName'"
