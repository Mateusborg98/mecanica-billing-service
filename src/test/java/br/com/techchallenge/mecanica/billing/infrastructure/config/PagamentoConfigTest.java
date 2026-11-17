package br.com.techchallenge.mecanica.billing.infrastructure.config;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
import br.com.techchallenge.mecanica.billing.infrastructure.payment.PagamentoInboxWorker;
import br.com.techchallenge.mecanica.billing.presentation.controller.MercadoPagoWebhookController;
class PagamentoConfigTest {
    ApplicationContextRunner context() { return new ApplicationContextRunner()
        .withBean(MongoTemplate.class,()->mock(MongoTemplate.class))
        .withBean(CobrancaService.class,()->mock(CobrancaService.class))
        .withBean(Clock.class,Clock::systemUTC)
        .withUserConfiguration(PagamentoConfig.class,MercadoPagoWebhookController.class); }
    @Test void desabilitadoNaoExpoeWebhookNemAgendaConsulta() {
        context().run(ctx->{ assertNull(ctx.getStartupFailure());
            assertTrue(ctx.getBeansOfType(MercadoPagoWebhookController.class).isEmpty());
            assertTrue(ctx.getBeansOfType(PagamentoInboxWorker.class).isEmpty()); });
    }
    @Test void habilitadoTemWorkerMesmoComMensageriaDesligada() {
        context().withPropertyValues("mercado-pago.webhook.enabled=true","mercado-pago.webhook.secret=fake","mercado-pago.access-token=fake")
            .run(ctx->{ assertNull(ctx.getStartupFailure());
                assertEquals(1,ctx.getBeansOfType(MercadoPagoWebhookController.class).size());
                assertEquals(1,ctx.getBeansOfType(PagamentoInboxWorker.class).size()); });
    }
}
