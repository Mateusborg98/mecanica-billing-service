package br.com.techchallenge.mecanica.billing.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.EnviarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.infrastructure.config.NotificacaoOrcamentoConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificacaoConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(NotificacaoOrcamentoConfig.class)
        .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
        .withBean(BuscarOrcamentoPorIdUseCase.class, () -> mock(BuscarOrcamentoPorIdUseCase.class));

    @Test void desabilitadoNaoExigeServidorSmtp() {
        runner.run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(EnvioOrcamentoWorker.class));
    }

    @Test void habilitadoLigaFilaCasoDeUsoETransporte() {
        runner.withPropertyValues("app.notificacoes.enabled=true",
            "app.notificacoes.public-base-url=https://oficina.example", "app.notificacoes.from=oficina@example.test")
            .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EnvioOrcamentoWorker.class)
                .hasSingleBean(EnviarOrcamentoUseCase.class).hasSingleBean(NotificacaoOrcamentoMongoOutbox.class));
    }

    @Test void habilitadoSemTransporteFalhaNaInicializacao() {
        runner.withPropertyValues("app.notificacoes.enabled=true",
            "app.notificacoes.public-base-url=https://oficina.example", "app.notificacoes.from=oficina@example.test")
            .run(context -> assertThat(context).hasFailed());
    }
}