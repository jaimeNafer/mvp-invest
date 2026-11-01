#requires -Version 7.0
param([switch]$Build)

$ErrorActionPreference = 'Stop'
$backendPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backend'))
$privateConfiguration = if ($env:LASTRO_CONFIG_PATH) { $env:LASTRO_CONFIG_PATH } else { Join-Path $backendPath '../.local/mysql.properties' }
if (-not (Test-Path -LiteralPath $privateConfiguration)) {
    throw 'Configure .local/mysql.properties antes de iniciar o Lastro persistente.'
}
$jarPath = Join-Path $backendPath 'build/libs/lastro-backend-0.0.1-SNAPSHOT.jar'
Push-Location $backendPath
try {
    if ($Build) {
        Push-Location (Join-Path $backendPath '../frontend')
        try {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar o frontend.' }
            & npm.cmd run build
            if ($LASTEXITCODE -ne 0) { throw 'Falha no build do frontend.' }
        } finally { Pop-Location }
        & ./gradlew.bat bootJar -PincludeFrontend
        if ($LASTEXITCODE -ne 0) { throw 'Falha no build do backend.' }
    }
    if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Gere o pacote primeiro com -Build.' }
    $javaExecutable = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
    & $javaExecutable '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar $jarPath
    if ($LASTEXITCODE -ne 0) { throw 'A aplicação encerrou com erro. Verifique a conexão MySQL e a saída acima.' }
} finally { Pop-Location }
