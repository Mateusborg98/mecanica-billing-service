# Mecânica Billing Service — Fase 4

Microsserviço Java 21 / Spring Boot responsável pelos orçamentos e pagamentos. Possui banco MongoDB próprio e integra OS e Execution por eventos RabbitMQ, sem acessar seus bancos.

## Fluxo implementado

Diagnóstico gera orçamento idempotente. O operador pode solicitar o envio por SMTP; o cliente consulta o link público e aprova ou recusa. Aprovação libera a execução. A conclusão cria a cobrança; o checkout Mercado Pago gera o link de pagamento. O webhook autenticado consulta o provedor e publica o pagamento confirmado para a OS, que exige pagamento antes da entrega.

As operações administrativas exigem JWT de OPERADOR ou ADMINISTRADOR. Links de orçamento, aprovação e checkout são públicos conforme a decisão do projeto. O webhook possui autenticação própria. Consulte [permissões](docs/seguranca-e-permissoes.md).

## Arquitetura e recuperação

A Saga usa coreografia para manter os serviços independentes: cada participante confirma sua transação local e comunica o resultado por eventos. Outbox, confirmações do broker, consumidores idempotentes e reprocessamento de DLQ permitem retomar falhas sem duplicar os efeitos de negócio.

Recusa/cancelamento antes da execução cancela o orçamento e libera reservas. Falhas definitivas ficam pendentes para correção e reprocessamento; o operador pode cancelar explicitamente antes do início. Após execução física, a recuperação prossegue para concluir cobrança/pagamento, sem simular desfazimento do reparo. Consulte [Saga](docs/saga.md), [política de falhas](docs/politica-falhas-saga.md) e [DLQ](docs/reprocessamento-dlq.md).

## Execução e testes

Use Java 21 e o Maven Wrapper: `./mvnw clean verify` (PowerShell: `.\mvnw.cmd clean verify`). A verificação inclui testes, JaCoCo com mínimo de 80% e PMD. Os testes com MongoDB real exigem `RUN_MONGO_TESTS=true` e `TEST_MONGO_URI` apontando para um banco de teste; nunca use dados de produção.

O BDD distribuído está no repositório mecanica-execution-service, em `tests/fluxo-distribuido`, e utiliza os três serviços, bancos e RabbitMQ em Docker, com provedor de pagamento simulado. Não substitui a compra real de teste.

## Guias

- [Envio SMTP e página de aprovação](docs/envio-orcamento-smtp.md).
- [Integração diagnóstico/orçamento](docs/integracao-diagnostico-rabbitmq.md).
- [Decisão do orçamento](docs/decisao-orcamento-rabbitmq.md).
- [Checkout Mercado Pago](docs/checkout-mercado-pago-orders.md) e [webhook](docs/webhook-mercado-pago.md).
- [Pagamento confirmado](docs/pagamento-confirmado-rabbitmq.md).
- [Roteiro de compra real de teste](docs/roteiro-pagamento-real.md).
- [Kubernetes](k8s/README.md) e [qualidade](docs/qualidade-estatica.md).

## Entrega e limites

CI e CD possuem workflows próprios; os manifests Kubernetes estão preparados. A infraestrutura compartilhada está em `mecanica-os-service/infra/fase4`. Documentos de validação com data registram evidências históricas, não o estado atual completo.

Permanecem externas à validação local: compra real de teste Mercado Pago, recebimento via SMTP real, deploy/CD em AWS e evidências de Datadog no ambiente entregue. Preparação de código e manifests não comprova essas etapas.