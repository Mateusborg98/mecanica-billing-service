$ErrorActionPreference = "Stop"
$tools = Join-Path $env:LOCALAPPDATA 'MecanicaDevTools'
$binary = Join-Path $tools 'cloudflared.exe'
$stateFile = Join-Path $tools 'webhook-processes.json'
if (Test-Path $stateFile) { throw 'Execute encerrar-webhook-https.ps1 antes de abrir outro tunel.' }
if (!(Test-Path $binary)) { throw 'Cloudflared nao encontrado em LOCALAPPDATA/MecanicaDevTools.' }
$proxyScript = Join-Path $PSScriptRoot 'webhook_proxy.py'
$python = (Get-Command python).Source
$proxy = Start-Process -FilePath $python -ArgumentList @('-u', ('"' + $proxyScript + '"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $tools 'webhook-proxy.log') -RedirectStandardError (Join-Path $tools 'webhook-proxy-error.log')
$processes = @(@{ Id=$proxy.Id; Started=$proxy.StartTime.ToUniversalTime().ToString('o') })
$processes | ConvertTo-Json | Set-Content -Encoding UTF8 $stateFile
Start-Sleep -Seconds 1
if ($proxy.HasExited) { throw 'Proxy nao iniciou. Consulte webhook-proxy-error.log e execute encerrar-webhook-https.ps1.' }
$tunnel = Start-Process -FilePath $binary -ArgumentList @('tunnel','--url','http://127.0.0.1:8084','--no-autoupdate') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $tools 'webhook-tunnel.log') -RedirectStandardError (Join-Path $tools 'webhook-tunnel-error.log')
$processes += @{ Id=$tunnel.Id; Started=$tunnel.StartTime.ToUniversalTime().ToString('o') }
$processes | ConvertTo-Json | Set-Content -Encoding UTF8 $stateFile
for ($attempt=0; $attempt -lt 30; $attempt++) {
    Start-Sleep -Seconds 1
    $log = Get-Content (Join-Path $tools 'webhook-tunnel-error.log') -Raw -ErrorAction SilentlyContinue
    if ($log -match 'https://[a-z0-9-]+\.trycloudflare\.com') {
        $url = $Matches[0] + '/webhooks/mercado-pago'
        $origin = $Matches[0]
        $status = & curl.exe --silent --max-time 5 --output NUL --write-out '%{http_code}' ($origin + '/orcamentos')
        if ($LASTEXITCODE -eq 0 -and $status -eq '404') {
            $hookStatus = & curl.exe --silent --max-time 5 --output NUL --write-out '%{http_code}' --request POST ($url + '?data.id=ORDTEST&type=order')
            if ($LASTEXITCODE -eq 0 -and $hookStatus -eq '401') {
                $url | Set-Content (Join-Path $tools 'webhook-url.txt')
                Write-Output $url
                exit 0
            }
        }
    }
    if ($tunnel.HasExited) { throw 'Tunel encerrou. Consulte webhook-tunnel-error.log.' }
}
& (Join-Path $PSScriptRoot 'encerrar-webhook-https.ps1')
throw 'URL HTTPS nao ficou acessivel; o tunel foi encerrado. Consulte webhook-tunnel-error.log.'
