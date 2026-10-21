$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($env:MERCADO_PAGO_ACCESS_TOKEN)) {
    throw "Configure MERCADO_PAGO_ACCESS_TOKEN nesta sessao antes de iniciar. Nao envie o token no chat."
}
$repo = Split-Path $PSScriptRoot -Parent
$compose = Join-Path (Split-Path $repo -Parent) "mecanica-execution-service/compose.integration.yaml"
docker compose -f $compose up -d --wait mongodb
if ($LASTEXITCODE -ne 0) { throw "MongoDB integrado indisponivel." }
$env:MONGODB_URI = "mongodb://billing_local:billing_local_password@localhost:17018/mecanica_billing?authSource=admin"
$env:MESSAGING_ENABLED = "false"
$env:SERVER_PORT = "8082"
$env:SERVER_ADDRESS = "127.0.0.1"
# Windows: use a short socket path for the JDK HTTP client. Preserve existing JVM options.
$env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$env:SystemRoot\Temp"
Push-Location $repo
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Billing encerrou com erro." }
} finally { Pop-Location }
