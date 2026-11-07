# Mecânica Billing Service — Tech Challenge Fase 4

Microsserviço responsável pelos orçamentos e pagamentos das ordens de serviço da oficina mecânica.

Este projeto faz parte da arquitetura de microsserviços desenvolvida para o Tech Challenge da Fase 4.

## Responsabilidades

- Gerar orçamentos com base nos itens identificados durante o diagnóstico.
- Calcular e registrar os valores de peças e serviços.
- Manter as versões dos orçamentos.
- Disponibilizar o orçamento para aprovação ou recusa do cliente.
- Integrar o processamento de pagamentos com o Mercado Pago.
- Registrar cobranças e acompanhar a confirmação dos pagamentos.
- Comunicar os resultados das operações ao serviço de ordens de serviço.

## Dados sob responsabilidade do serviço

- Orçamentos e suas versões.
- Itens e valores registrados em cada orçamento.
- Aprovações e recusas.
- Cobranças.
- Pagamentos e identificadores da integração com o Mercado Pago.

Os itens do orçamento preservam as descrições e os preços utilizados na proposta, evitando que alterações posteriores no catálogo modifiquem valores já acordados.

## Integração com os demais serviços

- mecanica-os-service: identificação da OS e coordenação do fluxo de orçamento e pagamento.
- mecanica-execution-service: informações do diagnóstico e dos itens necessários ao reparo, conforme os contratos de integração.

Cada microsserviço é responsável por seus próprios dados. A comunicação entre eles ocorre por contratos de API e mensagens, sem acesso direto ao banco de outro serviço.

## Status do projeto

Em desenvolvimento. As responsabilidades descritas representam a arquitetura planejada para a Fase 4.
## Avanço de 14/10/2026

Proteção atômica contra aprovação/recusa concorrentes, retorno HTTP 409, testes com
MongoDB real, Dockerfile e CI com JaCoCo e mínimo de 80% de cobertura de linhas.
O CI foi criado localmente; sua execução no GitHub depende de publicar estas alterações.
Consulte [a decisão de fluxo](docs/decisoes-fluxo.md).

Pagamento, Mercado Pago e Saga completa ainda estão pendentes. O primeiro fluxo de mensageria, diagnóstico para orçamento, já foi implementado.

## Integração de diagnóstico e orçamento — 16/10/2026

Execution publica diagnósticos via outbox/RabbitMQ; Billing cria orçamentos de forma
idempotente. Veja [fluxo, execução local e limites](docs/integracao-diagnostico-rabbitmq.md).
O retorno de aprovação/recusa está implementado; a Saga completa permanece pendente.

## Aprovação e recusa integradas

O Billing publica a decisão do orçamento por RabbitMQ. O Execution libera o reparo aprovado ou cancela o recusado, devolvendo reservas sem duplicidade. Consulte [o fluxo, a validação e os limites](docs/decisao-orcamento-rabbitmq.md).

## Conclusão e cobrança pendente

A conclusão do reparo chega ao Billing por RabbitMQ e cria uma única cobrança interna PENDENTE, com o valor do orçamento aprovado. Não há cobrança real no Mercado Pago nesta etapa. Consulte [fluxo, falhas e demonstração](docs/conclusao-e-cobranca.md).

## Checkout Mercado Pago

Geracao idempotente de link via Checkout Pro / Orders: [guia e validacao](docs/checkout-mercado-pago-orders.md). Confirmacao automatica implementada com [webhook autenticado e consulta ao provedor](docs/webhook-mercado-pago.md); validacao remota e integracao com a entrega ainda pendentes.

## Evento de pagamento e proximas validacoes

[Publicacao confiavel do pagamento](docs/pagamento-confirmado-rabbitmq.md) e [pendencias para o fim de semana](docs/pendencias-validacao-2026-10-27.md). O consumidor da OS e a liberacao da entrega continuam pendentes.

[Progresso e valor da OS](docs/progresso-os.md) — contratos, validação integrada e limitações atuais.


## Saga e recuperação de falhas

A integração usa Saga por coreografia. Consulte [etapas, compensação de reservas, roteiro de falha e limites pendentes](docs/saga.md).


## Cancelamento integrado

Cancelamentos antes do início propagam para OS e Billing, liberam reservas e toleram reentregas. Veja [regras, contrato e roteiro de validação](docs/cancelamento-integrado.md).

- [Reprocessamento auditável de DLQ](docs/reprocessamento-dlq.md): operação local, limites e evidências.

- [Análise estática e critérios de aprovação](docs/qualidade-estatica.md): regras PMD, execução e relatórios.

## Validação externa pendente

[Roteiro de pagamento real de teste](docs/roteiro-pagamento-real.md), incluindo a opção -Integrado do launcher para confirmar pagamento e liberar entrega nos três serviços.

[Validação local de 05/11/2026](docs/validacao-2026-11-05.md): OS, Billing e BDD distribuído; limites e pendências explícitos.

## Permissões de acesso

As operações administrativas exigem JWT de OPERADOR ou ADMINISTRADOR. Consulte [a matriz de permissões e configuração local](docs/seguranca-e-permissoes.md). As rotas públicas de acompanhamento, orçamento e checkout permanecem conforme o contrato documentado.


## Preparação de implantação da Fase 4

Os [manifests Kubernetes](k8s/README.md) e o workflow CD estão preparados. A infraestrutura compartilhada e a estimativa de custos ficam em mecanica-os-service/infra/fase4/README.md. A preparação local não comprova provisionamento AWS ou execução remota do CD.
