# Checkout Pro via Orders

## Implementado

`POST /cobrancas/{orcamentoId}/checkout`, sem corpo, retorna `orderId` e `checkoutUrl`.
O caso de uso exige uma cobranca ja criada apos a conclusao do reparo; sem ela retorna 404.
O total vem exclusivamente do snapshot da cobranca. O adapter envia uma order online/manual
com um item consolidado, referencia externa e chave de idempotencia iguais ao UUID da cobranca.
O endpoint remoto e fixo: `https://api.mercadopago.com/v1/orders`.

O resultado fica na colecao Mongo `checkouts`, com chave unica por cobranca e atualizacao
`setOnInsert`. Chamadas posteriores retornam o mesmo checkout. Se a resposta HTTP for perdida
ou o Mongo falhar apos a criacao remota, a repeticao usa a mesma chave e o mesmo corpo no provedor.
Nao existe renovacao automatica de links expirados: nao gere uma nova chave manualmente sem
conciliar a order anterior. A recuperacao depende das garantias de idempotencia do provedor.

O adapter confere referencia, moeda, valor e dominio HTTPS da URL antes de salvar. Tem timeout
de conexao de 5s e requisicao de 15s; nao segue redirects. Falhas retornam 503 sem corpo remoto,
headers de autenticacao ou token. Token ausente nao impede iniciar o servico, mas impede criar checkout.

## Validacao local com credencial

No PowerShell em que MERCADO_PAGO_ACCESS_TOKEN ja esta configurada:

```powershell
Set-Location 'C:\Users\mateus.drago\Documents\Fase 4 - Tech challenge\mecanica-billing-service'
.\scripts\iniciar-billing-checkout.ps1
```

O script inicia apenas o Mongo do ambiente integrado, preservando os dados, e executa Billing
localmente em 8082 com mensageria desligada. Assim reutiliza uma cobranca criada pelo smoke de
conclusao. O banco isolado do `docker-compose.yml` deste repositorio nao contem esses dados.
Nao executar `docker compose config` com tokens configurados no ambiente de containers.

Em outro PowerShell, use o UUID retornado pelo smoke `validar-conclusao-cobranca.ps1`:

```powershell
$id = 'UUID-DO-ORCAMENTO-COM-REPARO-CONCLUIDO'
Invoke-RestMethod -Uri "http://localhost:8082/cobrancas/$id"
$checkout = Invoke-RestMethod -Method Post -Uri "http://localhost:8082/cobrancas/$id/checkout"
$checkout
Start-Process $checkout.checkoutUrl
```

Para parar o Billing, Ctrl+C no primeiro terminal. O Mongo fica disponivel; para parar sem apagar dados:
`docker compose -f ../mecanica-execution-service/compose.integration.yaml stop mongodb`.

## Limites desta entrega

A Public Key nao e necessaria neste fluxo de backend com redirecionamento.
Ainda faltam webhook autenticado, consulta autoritativa de pagamento no provedor, transicao para pago,
evento de pagamento e bloqueio/liberacao da entrega na OS. Criar link ou voltar do checkout NAO
confirma pagamento. A cobranca continua PENDENTE. Endpoints seguem locais, sem exposicao publica;
a autenticacao/autorizacao precisa ser tratada antes da publicacao.
Nenhuma compra real ou chamada autenticada ao Mercado Pago foi realizada nesta validacao.

## Evidencia

Em 20/10/2026: Maven verify com MongoDB real, 109 testes, zero falhas/erros/ignorados;
JaCoCo passou o limite de 80%. Testes HTTP usam servidor local e token ficticio, cobrindo corpo,
chave repetida, resposta invalida, valor/moeda/referencia, dominio, token ausente e falhas de rede.
Mongo real valida concorrencia e recuperacao por nova instancia do gateway.
No Windows foi necessario `-Djdk.net.unixdomain.tmpdir=C:\Windows\Temp` nesta maquina.

Fonte do contrato: https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-orders/create-order
