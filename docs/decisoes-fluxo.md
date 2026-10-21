# Fluxo aprovado em 14/10/2026

1. Cliente aprova o orçamento.
2. A aprovação libera a execução do reparo.
3. Após a conclusão do reparo, ocorre o pagamento.
4. A entrega depende da confirmação do pagamento.

A autorização do orçamento não equivale a pagamento. A integração entre Execution e Billing já contempla diagnóstico, decisão do orçamento e conclusão com cobrança pendente. A integração da OS, o pagamento real e a barreira de entrega ainda precisam ser implementados.

## Concorrência no orçamento

Um orçamento nasce AGUARDANDO_APROVACAO. A persistência insere novos documentos sem
sobrescrever existentes. Aprovar e recusar atualizam apenas o status, com uma condição
atômica que exige o estado pendente. Uma decisão concorrente obsoleta recebe HTTP 409.
Uma tentativa sequencial detectada pelo domínio continua recebendo HTTP 400.

Para executar os testes com MongoDB local no PowerShell:

```powershell
docker compose up -d
$env:RUN_MONGO_TESTS = "true"
.\mvnw.cmd clean verify
Remove-Item Env:RUN_MONGO_TESTS
docker compose down
```
