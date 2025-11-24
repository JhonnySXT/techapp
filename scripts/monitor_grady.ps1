Param(
    [int]$IntervalSeconds = 10,
    [int]$MaxIterations = 0
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gradleWrapper = Join-Path $repoRoot "gradlew"

if (-not (Test-Path $gradleWrapper)) {
    throw "Не найден gradlew по пути $gradleWrapper"
}

Write-Host "Игорь наблюдает за агентом Грэди. Интервал: $IntervalSeconds c."

$iteration = 0

while ($true) {
    $iteration++
    $timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ssK"

    try {
        $statusOutput = & $gradleWrapper --status 2>&1
        $exitCode = $LASTEXITCODE
    } catch {
        $statusOutput = $_.Exception.Message
        $exitCode = 1
    }

    if ($exitCode -eq 0) {
        Write-Host "[$timestamp] Грэди: $statusOutput"
    } else {
        Write-Warning "[$timestamp] Не удалось получить статус Грэди: $statusOutput"
    }

    if ($MaxIterations -gt 0 -and $iteration -ge $MaxIterations) {
        break
    }

    Start-Sleep -Seconds $IntervalSeconds
}

Write-Host "Мониторинг завершен. Грэди продолжает работу самостоятельно."






