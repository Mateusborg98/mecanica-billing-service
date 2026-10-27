# Publicacao do pagamento confirmado

O Billing salva `pagamentoEvento` no mesmo documento e na mesma atualizacao atomica que
`cobranca.status=PAGA` e o comprovante. Nao ha chamada ao broker dentro da confirmacao financeira.
O worker usa lease de 30s e reagenda em 30s se a publicacao falhar.

- Exchange duravel: `mecanica.events`.
- Routing key: `pagamento.confirmado.v1`.
- Fila duravel de destino: `os.pagamento.v1`.
- DLQ: `os.pagamento.v1.dlq`, exchange `mecanica.dlx`.
- Contrato: [pagamento-confirmado-v1.json](contracts/pagamento-confirmado-v1.json).
- eventId = cobrancaId = orcamentoId; ordemId e reparoId identificam a correlacao.
- Valor decimal e moeda sao o snapshot da cobranca; confirmadoEm registra a primeira confirmacao.

A mensagem e persistente e a publicacao exige publisher confirm e ausencia de retorno por falta
de rota. O worker e ativado por MESSAGING_ENABLED=true. Com mensageria desligada, o pagamento
pode ser confirmado e o evento fica pendente no Mongo ate ativar o worker.

A entrega e pelo menos uma vez: se houver falha depois do ACK do Rabbit e antes da gravacao no
Mongo, a mensagem sera repetida com o mesmo ID e conteudo. O consumidor da OS devera deduplicar
pelo eventId, conferir os vinculos e persistir recebimento e atualizacao da OS na mesma transacao.
Uma notificacao repetida do Mercado Pago nao recria nem rearma um evento ja publicado.

Limites: o consumidor da OS ainda nao foi implementado; publicar este evento nao libera a entrega.
Nao ha backfill automatico para pagamentos anteriores sem pagamentoEvento: reconciliar esses casos
antes de emitir eventos. Estornos/chargebacks continuam pendentes de fluxo proprio.

Testes cobrem contrato JSON, persistencia da mensagem, ACK/NACK/retorno sem rota, falha de gravacao
apos ACK, reentrega do webhook, publicadores concorrentes e recuperacao apos indisponibilidade.
