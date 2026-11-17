package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
@io.swagger.v3.oas.annotations.tags.Tag(name = "CobrancaController")
@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RestController
@RequestMapping("/cobrancas")
public class CobrancaController {
    private final CobrancaService service;
    public CobrancaController(CobrancaService service) { this.service=service; }
    @io.swagger.v3.oas.annotations.Operation(summary = "Consultar cobranca criada apos conclusao do reparo")
    @GetMapping("/{orcamentoId}")
    public Cobranca buscar(@PathVariable UUID orcamentoId) { return service.buscar(orcamentoId); }
}
