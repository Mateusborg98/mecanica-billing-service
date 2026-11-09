# Kubernetes — mecanica-billing-service

Base da Fase 4, namespace mecanica-fase4. Imagem mecanica-billing-service:release é um marcador: substitua pelo URI ECR e SHA do commit antes do apply. O workflow CD faz essa substituição. Não há credenciais versionadas.

Dependências: cluster, metrics-server, EBS CSI, bancos/broker persistentes e Secret mecanica-billing-service-secret. A infraestrutura compartilhada e o roteiro completo estão em mecanica-os-service/infra/fase4/README.md. O deploy de cada API permanece independente.

Renderização local: kubectl kustomize k8s. Implantação só depois de criar os Secrets descritos no roteiro. O Service é ClusterIP; use port-forward para a demonstração. Readiness verifica banco e RabbitMQ; liveness verifica apenas a aplicação para evitar reinícios causados por indisponibilidade externa.

O HPA permite 1–2 réplicas por API e exige metrics-server. A capacidade do nó é limitada: validar uso real antes de demonstrar escalabilidade. Não há alta disponibilidade dos bancos ou do cluster de um nó.

## SMTP opcional

O Deployment já importa `mecanica-billing-service-config` e `mecanica-billing-service-secret` por envFrom. Para habilitar notificações, adicione ao ConfigMap `NOTIFICACOES_ENABLED`, `BILLING_PUBLIC_BASE_URL`, `MAIL_FROM`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` e as opções de TLS/autenticação do provedor. Guarde usuário/senha no Secret existente, preservando suas outras chaves. Consulte [configuração e roteiro SMTP](../docs/envio-orcamento-smtp.md). A URL deve ser alcançável pelo cliente; Service ClusterIP não é URL pública. Reinicie o Deployment após alterar ConfigMap/Secret.