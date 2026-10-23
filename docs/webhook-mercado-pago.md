# Confirmacao de pagamento via webhook — 23/10/2026

## Fluxo implementado

1. Mercado Pago envia `POST /webhooks/mercado-pago?data.id=ORD...&type=order`.
2. Controller valida HMAC-SHA256 usando `x-signature`, `x-request-id`, `data.id` e o segredo do webhook.
3. O aviso fica na colecao Mongo `notificacoes_pagamento` antes de responder HTTP 200.
4. Worker consulta `GET https://api.mercadopago.com/v1/orders/{id}` com o Access Token do backend.
5. Caso de uso compara order, referencia externa, checkout salvo, moeda, total e total pago.
6. Apenas `processed` / `accredited`, com pagamento integral igual a cobranca, muda PENDENTE para PAGA.

O corpo recebido no webhook nao e fonte de status nem valor. O segredo de assinatura e diferente
do Access Token. Nenhum deles e persistido na fila ou impresso nos erros. A implementacao preserva
maiúsculas/minúsculas de data.id conforme o contrato do SDK Java consultado em 23/10/2026.
IDs repetidos em query ou headers obrigatorios sao rejeitados. Assinatura invalida retorna 401;
persistencia indisponivel retorna 503, permitindo reenvio pelo Mercado Pago.

A assinatura inclui ts, mas nao impomos janela de idade nesta etapa: reentregas tardias continuam
aceitas. Elas consultam o estado atual no provedor e passam pela mesma confirmacao idempotente.
Nao confiamos em um status antigo enviado no corpo, mesmo que a assinatura seja valida.

## Persistencia e recuperacao

A fila usa `_id=orderId`, versao crescente por notificacao e lease de 30 segundos por worker.
A consulta tem timeout de 15 segundos; falhas reagendam para 30 segundos depois. Um novo aviso
durante a consulta incrementa a versao, impedindo que o worker o marque como processado por engano.
Uma lease expirada pode ser retomada apos reinicio. Nao ha limite automatico de tentativas;
`tentativas`, `ultimoErro`, `proximaTentativa` e `pendente` ficam disponiveis para diagnostico.
Uma divergencia de checkout/valor/moeda exige investigar os dados, nao forcar a cobranca para PAGA.

A atualizacao de `cobranca.status` e o comprovante `pagamento` ocorrem no mesmo documento do
orcamento, atomicamente. Repeticoes nao alteram a data da primeira confirmacao. Um erro entre
essa atualizacao e a confirmacao da fila resulta em reprocessamento seguro.
`GET /cobrancas/{orcamentoId}` mostra PAGA; nova criacao de checkout para essa cobranca e recusada.

## Configuracao local

No PowerShell que iniciara o Billing, configure Access Token e segredo do webhook apenas no ambiente:

```powershell
$env:MERCADO_PAGO_ACCESS_TOKEN = 'SEU-TOKEN'
$env:MERCADO_PAGO_WEBHOOK_SECRET = 'SEGREDO-GERADO-NA-CONFIGURACAO-DE-WEBHOOKS'
.\scripts\iniciar-billing-checkout.ps1
```

O script habilita o endpoint e o worker quando ha segredo; sem segredo, preserva o teste de checkout
com webhook desligado. Em execucao fora do script use tambem `MERCADO_PAGO_WEBHOOK_ENABLED=true`.
O worker independe de RabbitMQ; o script continua com mensageria desligada e banco integrado na 17018.
Nao copie credenciais para este documento, testes ou mensagens.

Para o teste remoto ainda e necessario disponibilizar uma URL HTTPS que encaminhe exclusivamente
`POST /webhooks/mercado-pago` ao Billing. Localhost nao e acessivel pelo Mercado Pago. Nao encaminhe
a API inteira, pois seus endpoints administrativos ainda nao possuem autenticacao nesta etapa.
No painel do Mercado Pago selecione o evento **Order (Mercado Pago)**. A chave secreta gerada nessa
configuracao e a usada em MERCADO_PAGO_WEBHOOK_SECRET. Um simulador com ID ficticio pode retornar
200 por recebimento duravel, mas nao confirma pagamento: a consulta da order real precisa funcionar.

## Validacao e limites

Validacao local: 134 testes, zero falhas/erros/ignorados, cobertura de linhas 90.02% (limite de 80%).

Testes incluem assinatura independente calculada com Python, headers adulterados, indisponibilidade,
consulta HTTP simulada, Mongo real, concorrencia, reinicio, repeticoes e aviso durante processamento.
Um teste integrado percorre controller -> fila Mongo -> GET HTTP simulado -> PAGA -> GET da cobranca.
Todos os dados e segredos de teste sao ficticios e usam bancos exclusivos descartados ao final.

Pendente: validar webhook recebido do Mercado Pago por HTTPS com o segredo real; publicar evento de
pagamento com outbox e integrar a liberacao de entrega no servico de OS. Nenhuma entrega e liberada
por este commit. O estado PAGA registra a confirmacao inicial: estornos e chargebacks posteriores
ainda precisam de fluxo proprio antes de usar esse estado como autorizacao de entrega em producao.
Nao ha endpoint manual para marcar pagamento como aprovado. Nao foram realizados pagamentos reais.

## Referencias

- https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-orders/notifications
- https://www.mercadopago.com.br/developers/pt/reference/online-payments/checkout-pro/get-order/get
- https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-orders/payment-management/status/order-status
- https://github.com/mercadopago/sdk-java/blob/master/src/main/java/com/mercadopago/webhook/WebhookSignatureValidator.java
