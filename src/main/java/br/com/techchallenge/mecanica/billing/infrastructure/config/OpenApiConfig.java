package br.com.techchallenge.mecanica.billing.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI serviceOpenApi() {
        return new OpenAPI().addServersItem(new Server().url("/"))
            .info(new Info().title("Mecanica - billing").version("v1")
                .description("Orcamentos, decisao publica, envio SMTP e pagamento. A cobranca surge depois da conclusao do reparo. Abra o checkoutUrl retornado pelo provedor; o webhook verifica o pagamento antes de notificar a OS. Operacoes administrativas exigem JWT de OPERADOR ou ADMINISTRADOR. Eventos RabbitMQ atualizam os outros servicos de forma assincrona; consulte o historico da OS para acompanhar."))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components().addSecuritySchemes("BearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}