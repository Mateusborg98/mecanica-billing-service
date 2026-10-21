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
