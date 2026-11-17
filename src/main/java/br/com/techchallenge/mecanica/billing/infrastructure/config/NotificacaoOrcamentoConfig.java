package br.com.techchallenge.mecanica.billing.infrastructure.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import br.com.techchallenge.mecanica.billing.application.usecase.*;
import br.com.techchallenge.mecanica.billing.infrastructure.notification.*;
@Configuration
@EnableScheduling
@ConditionalOnProperty(name="app.notificacoes.enabled",havingValue="true")
public class NotificacaoOrcamentoConfig {
    @Bean NotificacaoOrcamentoMongoOutbox notificacaoOutbox(MongoTemplate mongo) {return new NotificacaoOrcamentoMongoOutbox(mongo);}
    @Bean EnviarOrcamentoUseCase enviarOrcamento(BuscarOrcamentoPorIdUseCase buscar,NotificacaoOrcamentoMongoOutbox outbox,
            @Value("${app.notificacoes.public-base-url}") String base) {
        return new EnviarOrcamentoUseCase(buscar,outbox,base,Clock.systemUTC());
    }
    @Bean EnvioOrcamentoWorker envioOrcamentoWorker(NotificacaoOrcamentoMongoOutbox outbox,JavaMailSender sender,
            BuscarOrcamentoPorIdUseCase buscar,@Value("${app.notificacoes.from}") String from) {
        return new EnvioOrcamentoWorker(outbox,new SmtpEmailGateway(sender,from),buscar,Clock.systemUTC());
    }
}
