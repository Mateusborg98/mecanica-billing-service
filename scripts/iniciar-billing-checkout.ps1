param([switch]$Integrado)
$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:MERCADO_PAGO_ACCESS_TOKEN)) {
    throw "Configure MERCADO_PAGO_ACCESS_TOKEN nesta sessao antes de iniciar. Nao envie o token no chat."
}
if ([string]::IsNullOrWhiteSpace($env:JWT_PUBLIC_KEY)) {
    $publicFile=Join-Path $env:LOCALAPPDATA 'MecanicaDevTools/os-integration-public.env'
    if (-not (Test-Path -LiteralPath $publicFile)) {
        throw 'Configure JWT_PUBLIC_KEY/JWT_ISSUER ou execute iniciar-integracao-os.ps1 primeiro.'
    }
    $line=[IO.File]::ReadAllText($publicFile).Trim()
    if (-not $line.StartsWith("OS_INTEGRATION_PUBLIC_KEY='") -or -not $line.EndsWith("'")) {
        throw 'Arquivo de chave publica local invalido.'
    }
    $env:JWT_PUBLIC_KEY=$line.Substring(27,$line.Length-28)
    $env:JWT_ISSUER='mecanica-integration-local'
}
$repo = Split-Path $PSScriptRoot -Parent
$compose = Join-Path (Split-Path $repo -Parent) "mecanica-execution-service/compose.integration.yaml"
docker compose -f $compose up -d --wait mongodb
if ($LASTEXITCODE -ne 0) { throw "MongoDB integrado indisponivel." }
$env:MONGODB_URI = "mongodb://billing_local:billing_local_password@localhost:17018/mecanica_billing?authSource=admin"
if ($Integrado) {
    docker compose -f $compose up -d --wait rabbitmq
    if ($LASTEXITCODE -ne 0) { throw "RabbitMQ integrado indisponivel." }
    docker compose -f $compose stop billing
    if ($LASTEXITCODE -ne 0) { throw "Nao foi possivel parar o Billing do Compose." }
    $env:MESSAGING_ENABLED = "true"
    $env:RABBITMQ_HOST = "127.0.0.1"
    $env:RABBITMQ_PORT = "5673"
    $env:RABBITMQ_USERNAME = "mecanica_local"
    $env:RABBITMQ_PASSWORD = "mecanica_local_password"
    Write-Host "Integracao RabbitMQ habilitada. OS e Execution devem estar ativos no Compose."
} else {
    $env:MESSAGING_ENABLED = "false"
}
if ([string]::IsNullOrWhiteSpace($env:MERCADO_PAGO_WEBHOOK_SECRET)) {
    $env:MERCADO_PAGO_WEBHOOK_ENABLED = "false"
    Write-Host "Checkout habilitado. Webhook desabilitado: configure MERCADO_PAGO_WEBHOOK_SECRET para testar notificacoes."
} else {
    $env:MERCADO_PAGO_WEBHOOK_ENABLED = "true"
    Write-Host "Checkout e webhook habilitados localmente."
}
$env:SERVER_PORT = "8082"
$env:SERVER_ADDRESS = "127.0.0.1"
# Windows: use a short socket path for the JDK HTTP client. Preserve existing JVM options.
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$env:SystemRoot\Temp"
Push-Location $repo
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Billing encerrou com erro." }
} finally { Pop-Location }
