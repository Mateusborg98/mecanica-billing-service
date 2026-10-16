package br.com.techchallenge.mecanica.billing.infrastructure.messaging;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.usecase.ReceberDiagnosticoUseCase;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.OrcamentoDiagnosticoMongoGateway;
@Configuration
@ConditionalOnProperty(name="integration.messaging.enabled", havingValue="true")
public class BillingMessagingConfig {
    @Bean public ReceberDiagnosticoUseCase receberDiagnosticoUseCase(MongoTemplate mongo, OrcamentoPersistenceMapper mapper) {
        return new ReceberDiagnosticoUseCase(new OrcamentoDiagnosticoMongoGateway(mongo, mapper));
    }
    @Bean public DiagnosticoListener diagnosticoListener(ReceberDiagnosticoUseCase useCase) { return new DiagnosticoListener(useCase); }
}
