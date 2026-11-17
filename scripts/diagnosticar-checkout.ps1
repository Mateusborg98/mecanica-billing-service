param([ValidateSet('preview','consultar','criar-teste')][string]$Modo='preview')
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\mvnw.cmd -B -ntp dependency:build-classpath '-Dmdep.outputFile=target/diagnostico-classpath.txt' '-Dmdep.includeScope=runtime'
    if($LASTEXITCODE -ne 0){throw 'Falha ao preparar classpath do diagnostico.'}
    $classpath=(Get-Content target/diagnostico-classpath.txt -Raw).Trim()
    & java -cp $classpath tools/diagnostico-checkout/DiagnosticoCheckout.java "--$Modo"
    if($LASTEXITCODE -ne 0){throw 'Diagnostico nao concluido; preserve a chave e o registro da tentativa.'}
} finally { Pop-Location }