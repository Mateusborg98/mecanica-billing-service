# Validação local — 14/10/2026

## Billing
- Maven verify: 64 testes, nenhuma falha, nenhum teste ignorado, incluindo MongoDB real.
- Aprovação e recusa em duas threads: apenas uma decisão persistida.
- Resposta HTTP 409 para conflito de atualização coberta por teste.
- JaCoCo: 204 de 221 linhas, 92,31%, sem exclusões de classes.
- Imagem Docker construída localmente.

## Execution
- Maven clean verify: 5 testes, nenhuma falha.
- JaCoCo: 31 de 33 linhas, 93,94%, sem exclusões de classes.
- Imagem Docker construída; health UP com PostgreSQL independente.
- Inicialização Java direta nesta sessão Windows falhou em UnixDomainSockets.connect;
  a mesma aplicação iniciou com sucesso no contêiner. Não alterar regras de negócio
  para contornar esse problema de ambiente.

## Próximos blocos
1. Persistência e API do reparo, catálogo e reservas de estoque no Execution.
2. Separação efetiva das responsabilidades ainda duplicadas no monólito OS.
3. Mensageria, Saga e compensações, com idempotência e publicação confiável de eventos.
4. Mercado Pago: cobrança após reparo, confirmação de pagamento antes de entregar.
5. BDD completo, análise estática, autenticação entre serviços, Kubernetes e observabilidade.
6. Publicação e validação dos workflows, evidências, vídeo e documentação de entrega.

Os workflows de CI foram criados localmente, mas não executados no GitHub.
A cobertura mede o código existente, não a conclusão do escopo do desafio.
