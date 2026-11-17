# Envio de orçamento por SMTP

O operador solicita o envio; o cliente recebe um link público para consultar, aprovar ou recusar o orçamento. Aprovação autoriza o reparo. O pagamento ocorre após a conclusão e antes da entrega.

## Configuração

O envio fica desabilitado por padrão. Configure as variáveis no processo do Billing ou nos Secrets/ConfigMap do ambiente:

| Variável | Valor/uso |
| --- | --- |
| NOTIFICACOES_ENABLED | true |
| BILLING_PUBLIC_BASE_URL | URL HTTPS pública do Billing; localhost HTTP é permitido para testes |
| MAIL_FROM | Remetente autorizado pelo servidor SMTP |
| SPRING_MAIL_HOST | Host SMTP |
| SPRING_MAIL_PORT | Porta fornecida pelo provedor |
| SPRING_MAIL_USERNAME | Usuário, quando exigido |
| SPRING_MAIL_PASSWORD | Segredo local ou Kubernetes Secret; não versionar |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH | true quando o provedor exigir autenticação |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE | true para STARTTLS quando suportado pelo provedor |
| SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED | true quando STARTTLS for obrigatório |

Para SMTPS, use a configuração SSL do provedor em vez de presumir STARTTLS. Os timeouts de conexão, leitura e escrita são de 5 segundos. A aplicação precisa ser reiniciada após configurar as variáveis. Nunca use o SMTP de produção para testes automatizados.

## Solicitar e acompanhar

Com JWT de OPERADOR ou ADMINISTRADOR, execute `POST /orcamentos/{id}/notificacao` com JSON `{"destinatario":"cliente@example.com"}`. O orçamento deve estar aguardando aprovação. O retorno 202 indica agendamento, não entrega do e-mail.

Consulte `GET /orcamentos/{id}/notificacao`, também protegido. A resposta contém `orcamentoId`, `status`, `tentativas`, `ultimoErro` e `proximaTentativa`. Os estados são PENDENTE, ENVIADO e CANCELADO. ENVIADO significa aceitação pelo transporte SMTP; não comprova leitura nem chegada à caixa de entrada.

O link enviado aponta para `GET /orcamentos/{id}/aprovacao`. Esse GET não altera estado. Os botões enviam POST à mesma rota, usando os casos de uso existentes. A página escapa as descrições dos itens, desabilita cache e não envia referrer. As APIs JSON existentes continuam disponíveis.

## Persistência e falhas

Os pedidos são persistidos no MongoDB do Billing, na coleção `envios_orcamento`. Um worker consulta a cada 2 segundos. A reserva atômica de 60 segundos evita execução concorrente normal; falhas são reagendadas após 30 segundos. Reservas vencidas podem ser retomadas após reinício. O erro armazenado usa um código de diagnóstico sem mensagem bruta do provedor nem credenciais.

Repetir o pedido com o mesmo orçamento, destinatário e texto não agenda outro envio. Alterar destinatário/texto de um pedido já registrado retorna conflito. Se o orçamento já foi decidido quando o worker consulta, o envio é cancelado. Uma decisão simultânea ao envio pode resultar em mensagem com link para um orçamento já decidido; a página apresenta o estado atual e as regras de decisão permanecem protegidas.

SMTP não oferece transação conjunta com MongoDB: uma queda após aceitação pelo servidor e antes da confirmação local pode causar reenvio. A implementação oferece recuperação com possível duplicidade nessa janela, não exatamente uma entrega.

## Validação

Testes Java cobrem o protocolo SMTP em servidor local temporário, conteúdo da mensagem, fila MongoDB real, falha e nova tentativa, concorrência, expiração de reserva, cancelamento e página pública. O servidor local não envia mensagens externas.

O envio real permanece pendente de configurar um servidor SMTP e validar o recebimento em uma caixa de teste.
## Diagnostico e retomada

O launcher usa somente as variaveis da sessao. Nao carrega senha salva fora do repositorio.
Configure as variaveis **no mesmo terminal que inicia o Billing**, antes de executar o script.
Alterar variaveis em outro terminal ou depois de iniciar Java nao atualiza o processo em execucao.
O launcher informa se SMTP esta habilitado e rejeita configuracao obrigatoria ausente.
A inicializacao executa clean para evitar bytecode antigo gerado pela IDE.

- `PENDENTE`, zero tentativas: confira se o Billing iniciado e a instancia consultada sao os mesmos e se NOTIFICACOES_ENABLED=true.
- `AUTENTICACAO_SMTP`: confira usuario e credencial do provedor. No Gmail, use senha de aplicativo. Corrija as variaveis e reinicie o Billing; a fila persistida retoma automaticamente.
- `TRANSPORTE_SMTP`: verifique host, porta, TLS, rede e restricoes de remetente/destinatario do provedor.
- `PROCESSAMENTO_NOTIFICACAO`: falha interna anterior ou posterior ao transporte; investigar o orcamento e disponibilidade do banco.
- `proximaTentativa`: instante UTC da nova tentativa (30 segundos apos falha). A consulta nao expoe senha nem texto bruto do erro do provedor.
- `ENVIADO`: servidor SMTP aceitou. Confira spam, quarentena e regras do destinatario; isso nao e comprovante de entrega na caixa de entrada.

Nao recrie o orcamento nem limpe MongoDB para resolver autenticacao. Nao aprove/recuse o orcamento enquanto estiver testando o envio: uma decisao cancela a notificacao ainda pendente.

### Exemplo Gmail em PowerShell 7 (somente sessao)

```powershell
$env:NOTIFICACOES_ENABLED = 'true'
$env:SPRING_MAIL_HOST = 'smtp.gmail.com'
$env:SPRING_MAIL_PORT = '587'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = 'true'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = 'true'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED = 'true'
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE = 'false'
$env:SPRING_MAIL_USERNAME = Read-Host 'Conta remetente'
$env:MAIL_FROM = $env:SPRING_MAIL_USERNAME
$env:SPRING_MAIL_PASSWORD = Read-Host 'Senha de aplicativo do Gmail (sem espacos)' -MaskInput
$env:BILLING_PUBLIC_BASE_URL = Read-Host 'URL HTTPS atual do Billing (ou http://localhost:8082 para teste no mesmo computador)'
.\scripts\iniciar-billing-checkout.ps1 -Integrado
```

O launcher de checkout tambem exige as variaveis Mercado Pago/JWT descritas no roteiro de pagamento.
Depois de Started BillingApplication, solicite explicitamente POST /orcamentos/{id}/notificacao com JWT administrativo e destinatario; criar um orcamento sozinho nao envia e-mail.
Consulte GET na mesma rota ate ENVIADO ou obtenha ultimoErro para diagnostico.

### Evidencia automatizada de 16/11/2026

Servidor SMTP em loopback: envio desde a fila MongoDB, destinatario e link corretos, rejeicao 535, falha de autenticacao visivel, reinicio do worker com recuperacao e limpeza do erro apos sucesso. Esse teste nao usa Gmail e nao comprova recebimento externo.