# Continuidade e validacao externa pendente — 27/10/2026

O usuario decidiu adiar o teste real do Mercado Pago para o fim de semana e continuar a implementacao
com testes locais. O tunel temporario foi encerrado; nao reutilizar URLs antigas.

## Estado verificado

- O checkout anterior abriu, depois mostrou expiracao. GET da order retornou order_not_found.
  A causa nao foi determinada; o erro nao prova ausencia de pagamento.
- Uma nova operacao independente foi criada em 27/10, preservando a anterior.
- Novo orcamento/cobranca: 2617426a-718f-374d-842b-a4a75bb47e86.
- Novo reparo: 43c7a1b8-dd57-430e-8523-a623fbb42de7.
- Nova OS de teste (identificador externo ao servico OS): 0eaab857-d644-4b3d-97d7-aae55a2eb924.
- Valor: R$ 51,00. Nova order: ORDTST01M5YTGH5EV7V3WXBTFA6NA494.
- Criacao remota retornou checkoutUrl. Consulta autenticada dessa nova order ainda nao foi validada.
- Nao foi comprovado recebimento de webhook real nem pagamento aprovado pelo provedor.

## Roteiro do fim de semana

1. Confirmar credenciais de teste da mesma aplicacao vendedora; nao compartilhar tokens.
2. Iniciar Billing local pelo script iniciar-billing-checkout.ps1, com Access Token e segredo.
3. Iniciar iniciar-webhook-https.ps1 e cadastrar a URL nova no painel Mercado Pago.
4. Consultar a order mais recente usando o token da criacao. Registrar apenas HTTP/status/request-id,
   sem headers de autenticacao. Se retornar 404, investigar conta/aplicacao/ambiente antes de pagar.
5. Se a order/checkout ja estiver expirada, criar outra operacao independente de teste; nao excluir
   comprovantes, trocar chaves de idempotencia ou reutilizar cegamente o checkout antigo.
6. Usar conta comprador e cartao de teste oficiais. Verificar recebimento na inbox e status PAGA.
7. Acompanhar pagamentoEvento e fila os.pagamento.v1 com mensageria habilitada.
8. Encerrar o tunel e os processos do ambiente quando terminar, preservando volumes.

## Trabalho que pode avancar sem credenciais

- Publicacao confiavel de PagamentoConfirmado pelo Billing: implementada nesta etapa.
- Separacao do servico OS: ainda contem partes de estoque, servicos, pecas e orcamento do monolito.
  Seu checkout local tem arquivos anteriores nao versionados; preservar e revisar antes de consolidar.
- Integracao OS -> Execution: criar reparo a partir da OS e salvar correlacao duravel.
- Consumidor de pagamento na OS: inbox idempotente, correlacao de reparo/orcamento e atualizacao transacional.
- Entrega: exigir reparo concluido e pagamento confirmado; bloquear sem os dois requisitos.
- Testes integrados dos tres servicos e documentacao dos contratos.
- AWS Academy/Datadog e custos: manter limite combinado de US$ 50; provisionamento ainda pendente.

Esses itens sao um plano de continuidade tecnica, nao uma nova auditoria completa do enunciado.
