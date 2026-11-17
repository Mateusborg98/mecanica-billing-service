package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import br.com.techchallenge.mecanica.billing.application.usecase.EnviarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoOrcamentoGateway;
@io.swagger.v3.oas.annotations.tags.Tag(name = "NotificacaoOrcamentoController")
@RestController
@ConditionalOnProperty(name="app.notificacoes.enabled",havingValue="true")
@RequestMapping("/orcamentos/{id}/notificacao")
public class NotificacaoOrcamentoController {
    public record Request(@NotBlank @Email String destinatario) {}
    private final EnviarOrcamentoUseCase enviar;
    private final NotificacaoOrcamentoGateway gateway;
    public NotificacaoOrcamentoController(EnviarOrcamentoUseCase enviar,NotificacaoOrcamentoGateway gateway) {this.enviar=enviar;this.gateway=gateway;}
    @io.swagger.v3.oas.annotations.Operation(summary = "Solicitar envio do orcamento por e-mail")
    @PostMapping public ResponseEntity<NotificacaoOrcamentoGateway.Situacao> enviar(@PathVariable UUID id,@Valid @RequestBody Request request) {
        return ResponseEntity.accepted().body(enviar.executar(id,request.destinatario()));
    }
    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar envio, tentativas e erro sanitizado")
    @GetMapping public NotificacaoOrcamentoGateway.Situacao consultar(@PathVariable UUID id) {return gateway.buscar(id);}
}
