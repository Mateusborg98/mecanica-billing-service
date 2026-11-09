package br.com.techchallenge.mecanica.billing.infrastructure.notification;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import br.com.techchallenge.mecanica.billing.application.gateway.EnvioEmailGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
public final class EnvioOrcamentoWorker {
    private final NotificacaoOrcamentoMongoOutbox outbox;
    private final EnvioEmailGateway email;
    private final BuscarOrcamentoPorIdUseCase buscar;
    private final Clock clock;
    public EnvioOrcamentoWorker(NotificacaoOrcamentoMongoOutbox outbox,EnvioEmailGateway email,BuscarOrcamentoPorIdUseCase buscar,Clock clock) {
        this.outbox=outbox;this.email=email;this.buscar=buscar;this.clock=clock;
    }
    @Scheduled(fixedDelayString="${app.notificacoes.delay-ms:2000}")
    public void agendar() {enviarProximo();}
    public boolean enviarProximo() {
        var p=outbox.reservar(clock.instant());if(p==null)return false;
        try {
            if(buscar.executar(p.id()).getStatus()!=StatusOrcamento.AGUARDANDO_APROVACAO) {outbox.concluir(p,"CANCELADO");return true;}
            email.enviar(p.destinatario(),p.texto());outbox.concluir(p,"ENVIADO");return true;
        } catch(RuntimeException erro) {outbox.reagendar(p,clock.instant(),erro);return false;}
    }
}
