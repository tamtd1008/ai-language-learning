param(
    [switch]$MockAi,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    docker info --format '{{.ServerVersion}}'
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Linux engine is unavailable. Open Docker Desktop, wait for it to start, and retry.'
    }
    if (-not (Test-Path -LiteralPath '.env')) {
        Copy-Item -LiteralPath '.env.example' -Destination '.env'
        Write-Host 'Created .env from .env.example. Review the demo credentials and JWT_SECRET before use.'
        return
    }

    $composeArgs = @('compose', '--profile', 'local-speech', 'up', '-d', '--wait')
    if ($Rebuild) {
        $composeArgs += '--build'
    }
    $composeArgs += @('postgres', 'whisper', 'piper')
    docker @composeArgs
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL, Whisper or Piper did not become healthy.' }

    $env:SPEECH_STT_PROVIDER = 'whisper'
    $env:SPEECH_TTS_PROVIDER = 'piper'
    $env:LOGGING_FILE_NAME = Join-Path $PSScriptRoot 'backend-demo.log'
    Push-Location backend
    try {
        Write-Host 'Demo URL: http://localhost:8080/demo/index.html'
        if ($MockAi) {
            $env:AI_PROVIDER = 'mock'
        }
        .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'Spring Boot did not start.' }
    } finally {
        Pop-Location
    }
} finally {
    Pop-Location
}
