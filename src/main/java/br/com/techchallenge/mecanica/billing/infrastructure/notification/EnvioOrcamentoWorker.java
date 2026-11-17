package br.com.techchallenge.mecanica.billing.infrastructure.notification;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import br.com.techchallenge.mecanica.billing.application.gateway.EnvioEmailGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;

public final class EnvioOrcamentoWorker {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioOrcamentoWorker.class);
    private final NotificacaoOrcamentoMongoOutbox outbox;
    private final EnvioEmailGateway email;
    private final BuscarOrcamentoPorIdUseCase buscar;
    private final Clock clock;

    public EnvioOrcamentoWorker(NotificacaoOrcamentoMongoOutbox outbox, EnvioEmailGateway email,
            BuscarOrcamentoPorIdUseCase buscar, Clock clock) {
        this.outbox = outbox;
        this.email = email;
        this.buscar = buscar;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notificacoes.delay-ms:2000}")
    public void agendar() {
        enviarProximo();
    }

    public boolean enviarProximo() {
        var pendente = outbox.reservar(clock.instant());
        if (pendente == null) return false;
        try {
            if (buscar.executar(pendente.id()).getStatus() != StatusOrcamento.AGUARDANDO_APROVACAO) {
                outbox.concluir(pendente, "CANCELADO");
                return true;
            }
            email.enviar(pendente.destinatario(), pendente.texto());
            outbox.concluir(pendente, "ENVIADO");
            return true;
        } catch (RuntimeException erro) {
            outbox.reagendar(pendente, clock.instant(), erro);
            LOG.warn("Envio do orcamento {} pendente: {}. Nova tentativa em 30 segundos.",
                    pendente.id(), NotificacaoOrcamentoMongoOutbox.codigoErro(erro));
            return false;
        }
    }
}