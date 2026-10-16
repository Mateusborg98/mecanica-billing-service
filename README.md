# MecÃ¢nica Billing Service â€” Tech Challenge Fase 4

MicrosserviÃ§o responsÃ¡vel pelos orÃ§amentos e pagamentos das ordens de serviÃ§o da oficina mecÃ¢nica.

Este projeto faz parte da arquitetura de microsserviÃ§os desenvolvida para o Tech Challenge da Fase 4.

## Responsabilidades

- Gerar orÃ§amentos com base nos itens identificados durante o diagnÃ³stico.
- Calcular e registrar os valores de peÃ§as e serviÃ§os.
- Manter as versÃµes dos orÃ§amentos.
- Disponibilizar o orÃ§amento para aprovaÃ§Ã£o ou recusa do cliente.
- Integrar o processamento de pagamentos com o Mercado Pago.
- Registrar cobranÃ§as e acompanhar a confirmaÃ§Ã£o dos pagamentos.
- Comunicar os resultados das operaÃ§Ãµes ao serviÃ§o de ordens de serviÃ§o.

## Dados sob responsabilidade do serviÃ§o

- OrÃ§amentos e suas versÃµes.
- Itens e valores registrados em cada orÃ§amento.
- AprovaÃ§Ãµes e recusas.
- CobranÃ§as.
- Pagamentos e identificadores da integraÃ§Ã£o com o Mercado Pago.

Os itens do orÃ§amento preservam as descriÃ§Ãµes e os preÃ§os utilizados na proposta, evitando que alteraÃ§Ãµes posteriores no catÃ¡logo modifiquem valores jÃ¡ acordados.

## IntegraÃ§Ã£o com os demais serviÃ§os

- mecanica-os-service: identificaÃ§Ã£o da OS e coordenaÃ§Ã£o do fluxo de orÃ§amento e pagamento.
- mecanica-execution-service: informaÃ§Ãµes do diagnÃ³stico e dos itens necessÃ¡rios ao reparo, conforme os contratos de integraÃ§Ã£o.

Cada microsserviÃ§o Ã© responsÃ¡vel por seus prÃ³prios dados. A comunicaÃ§Ã£o entre eles ocorre por contratos de API e mensagens, sem acesso direto ao banco de outro serviÃ§o.

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
O retorno de aprovação/recusa e a Saga completa ainda estão pendentes.
