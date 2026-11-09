# Validação final do pagamento de teste

Este roteiro fica pendente até a compra ser concluída pelo usuário. O BDD com provedor simulado não substitui esta evidência. Não reutilize as orders ou os túneis antigos.

## Preparar o ambiente

1. No Execution, execute `./scripts/iniciar-integracao-os.ps1` para iniciar OS, Execution, Billing e dependências. Preserve os volumes.
2. No Billing, configure na sessão local `MERCADO_PAGO_ACCESS_TOKEN` e `MERCADO_PAGO_WEBHOOK_SECRET` da mesma aplicação de teste. Não coloque os valores no Git, nos relatórios ou no chat.
3. Execute `./scripts/iniciar-billing-checkout.ps1 -Integrado`. Essa opção interrompe somente o Billing do Compose para evitar dois processos sobre a mesma base, inicia o Billing em `127.0.0.1:8082` e conecta o RabbitMQ local. OS e Execution continuam nas portas 18081/18083.
4. Em outro terminal, execute `./scripts/iniciar-webhook-https.ps1`. Cadastre a URL HTTPS nova no Mercado Pago, para evento Order, e confirme que o segredo configurado no Billing corresponde a esse cadastro. Se mudar o segredo, reinicie o Billing.

## Criar uma operação nova

Use Swagger/Postman para percorrer as APIs. Envie JWT de operador nas chamadas administrativas da OS e do Execution; o helper autenticacao-local.ps1 do Execution fornece os headers no ambiente local. Consulte seguranca-e-permissoes.md. Os dados não devem ser inseridos diretamente no banco.

1. Abra uma OS autenticada em `POST http://localhost:18081/ordens-servico`, com `placa` e `matriculaOperador`. Guarde o `id`.
2. Consulte `GET /ordens-servico/{id}/reparo` até `publicado=true`; guarde `reparoId`.
3. No Execution, cadastre o serviço/peça, faça entrada de estoque quando necessário, registre `POST /reparos/{reparoId}/diagnostico` e reserve as peças via `/reserva`.
4. Consulte o orçamento criado no Billing local. O identificador determinístico do orçamento é o UUID Java `UUID.nameUUIDFromBytes(("mecanica:diagnostico:v1:" + reparoId).getBytes(StandardCharsets.UTF_8))`. Use o mesmo identificador que o fluxo integrado já devolve/consulta; não invente uma order do provedor.
5. Aprove o orçamento em `POST http://localhost:8082/orcamentos/{orcamentoId}/aprovar`. Aguarde o reparo `NA_FILA`, inicie e conclua no Execution.
6. Confirme OS `FINALIZADA` e cobrança `PENDENTE`. Tentar entregar nesse momento deve retornar HTTP 422.

## Gerar checkout e concluir a compra

No PowerShell, usando o orçamento desta operação:

```powershell
$orcamentoId = 'ID-DO-ORCAMENTO-NOVO'
$checkout = Invoke-RestMethod -Method Post -Uri "http://localhost:8082/cobrancas/$orcamentoId/checkout"
$checkout.checkoutUrl
```

Abra a URL retornada e conclua a compra com a conta compradora de teste. Guarde `checkout.orderId`; ele é o identificador usado em `/v1/orders/{id}`, diferente do orçamento, do reparo e de uma preferência.

Se a consulta da order retornar `order_not_found` ou o checkout expirar, pare essa tentativa e confira aplicação/conta/ambiente com as credenciais usadas na criação. Preserve o registro anterior; não force `PAGA` nem troque a chave de idempotência para contornar o problema.

## Evidência necessária

- Webhook recebido por HTTPS e aceito pelo Billing. HTTP 200 sozinho comprova apenas recebimento durável.
- Consulta do Billing ao provedor confirma order, referência, BRL, valor total e valor pago integral; cobrança passa para `PAGA`.
- Evento PAGAMENTO aparece em `GET http://localhost:18081/ordens-servico/{id}/historico`.
- Entrega passa a ser aceita e acompanhamento retorna `ENTREGUE`.
- Reenvio da mesma notificação não duplica o pagamento no histórico.

Registre somente IDs de negócio, horários, estados, valores e resultados HTTP. Tokens, segredo de assinatura e JWT não entram na evidência. Estorno/chargeback não estão cobertos por essa validação.

## Encerrar

Encerre o túnel com `./scripts/encerrar-webhook-https.ps1` e pare o Billing local com Ctrl+C. Para retornar ao Billing em Docker, execute no Execution `docker compose -f compose.integration.yaml up -d billing`. Para encerrar todo o ambiente integrado, use `docker compose -f compose.integration.yaml --profile os down`, sem `-v`.
