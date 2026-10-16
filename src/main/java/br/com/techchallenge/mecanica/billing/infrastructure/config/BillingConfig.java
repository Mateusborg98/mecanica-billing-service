package br.com.techchallenge.mecanica.billing.infrastructure.config;

import java.time.Clock;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.CriarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.RecusarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway.OrcamentoGatewayImpl;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;

@Configuration
public class BillingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OrcamentoPersistenceMapper orcamentoPersistenceMapper() {
        return new OrcamentoPersistenceMapper();
    }

    @Bean
    public OrcamentoGateway orcamentoGateway(
            OrcamentoMongoRepository repository,
            OrcamentoPersistenceMapper mapper, MongoTemplate template) {

        return new OrcamentoGatewayImpl(repository, mapper, template);
    }

    @Bean
    public CriarOrcamentoUseCase criarOrcamentoUseCase(
            OrcamentoGateway gateway,
            Clock clock) {

        return new CriarOrcamentoUseCase(gateway, clock);
    }

    @Bean
    public BuscarOrcamentoPorIdUseCase buscarOrcamentoPorIdUseCase(
            OrcamentoGateway gateway) {

        return new BuscarOrcamentoPorIdUseCase(gateway);
    }

    @Bean
    public AprovarOrcamentoUseCase aprovarOrcamentoUseCase(
            BuscarOrcamentoPorIdUseCase buscarUseCase,
            OrcamentoGateway gateway) {

        return new AprovarOrcamentoUseCase(buscarUseCase, gateway);
    }

    @Bean
    public RecusarOrcamentoUseCase recusarOrcamentoUseCase(
            BuscarOrcamentoPorIdUseCase buscarUseCase,
            OrcamentoGateway gateway) {

        return new RecusarOrcamentoUseCase(buscarUseCase, gateway);
    }
}
