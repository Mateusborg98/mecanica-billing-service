$ErrorActionPreference = "Stop"
$tools = Join-Path $env:LOCALAPPDATA 'MecanicaDevTools'
$stateFile = Join-Path $tools 'webhook-processes.json'
if (!(Test-Path $stateFile)) { Write-Output 'Nenhum tunel registrado.'; exit 0 }
$records = @(Get-Content $stateFile -Raw | ConvertFrom-Json)
foreach ($record in $records) {
    $running = Get-Process -Id $record.Id -ErrorAction SilentlyContinue
    if ($running -and $running.StartTime.ToUniversalTime().ToString('o') -eq $record.Started) {
        Stop-Process -Id $record.Id
    }
}
Remove-Item -LiteralPath $stateFile
$urlFile = Join-Path $tools 'webhook-url.txt'
if (Test-Path $urlFile) { Remove-Item -LiteralPath $urlFile }
Write-Output 'Tunel HTTPS e proxy encerrados. Billing e Mongo permanecem no estado anterior.'
