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
@RestController
@ConditionalOnProperty(name="app.notificacoes.enabled",havingValue="true")
@RequestMapping("/orcamentos/{id}/notificacao")
public class NotificacaoOrcamentoController {
    public record Request(@NotBlank @Email String destinatario) {}
    private final EnviarOrcamentoUseCase enviar;
    private final NotificacaoOrcamentoGateway gateway;
    public NotificacaoOrcamentoController(EnviarOrcamentoUseCase enviar,NotificacaoOrcamentoGateway gateway) {this.enviar=enviar;this.gateway=gateway;}
    @PostMapping public ResponseEntity<NotificacaoOrcamentoGateway.Situacao> enviar(@PathVariable UUID id,@Valid @RequestBody Request request) {
        return ResponseEntity.accepted().body(enviar.executar(id,request.destinatario()));
    }
    @GetMapping public NotificacaoOrcamentoGateway.Situacao consultar(@PathVariable UUID id) {return gateway.buscar(id);}
}
