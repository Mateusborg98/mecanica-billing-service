# Diagnostico temporario do checkout Orders

Java 21, usando o classpath do Billing. Nao exige parar a aplicacao. Executar da raiz do Billing:

    .\scripts\diagnosticar-checkout.ps1 -Modo preview
    .\scripts\diagnosticar-checkout.ps1 -Modo consultar
    .\scripts\diagnosticar-checkout.ps1 -Modo criar-teste

Preview nao acessa rede. Os demais modos leem MERCADO_PAGO_ACCESS_TOKEN da sessao, sem gravar ou imprimir. Consultar le a order de teste ORDTST01M7JQT3KPM7FQN4GZSDW2WV3B. Criar-teste consulta primeiro a original e cria uma operacao remota independente de R$ 50 com o payload minimo do Billing, sem MongoDB, RabbitMQ ou casos de uso da aplicacao. Nunca confirma pagamento. Utilizar somente credencial do ambiente Teste.

A referencia externa com prefixo diagnostico nao corresponde a uma cobranca local. Esse checkout nao valida Saga, entrega ou webhook financeiro da OS. Nao usa nenhum dado de cartao. Se houver webhook dessa operacao isolada, nao representa pagamento da cobranca original.

A chave de idempotencia fica em target/diagnostico-checkout-id.txt. Preservar esse arquivo, nao executar clean entre repeticoes e nao alterar o payload mantendo a chave. Em timeout, repetir o mesmo comando reutiliza a chave. O resultado mostra apenas campos permitidos da order, HTTP e x-request-id. Nao salvar resposta integral, que pode conter client_token e dados pessoais.

Comparacao em 16/11/2026: POST /v1/orders, type online, processing_mode manual, total_amount string, items com title/quantity/unit_price e X-Idempotency-Key correspondem ao contrato documentado. Public Key nao participa. O exemplo da documentacao inclui payer.email; a tabela o considera condicional, portanto sua ausencia no Billing nao comprova erro. Nao alteramos o payload da aplicacao por hipotese.

Fonte: https://www.mercadopago.com.br/developers/pt/docs/checkout-pro-orders/create-order

Verificacao realizada: compilacao e preview local, sem rede. A consulta autenticada e a criacao remota dependem de execucao pelo responsavel no terminal com o token; nao foram executadas pelo agente.