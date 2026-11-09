package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import br.com.techchallenge.mecanica.billing.application.usecase.CobrancaService;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
@RestController
@RequestMapping("/cobrancas")
public class CobrancaController {
    private final CobrancaService service;
    public CobrancaController(CobrancaService service) { this.service=service; }
    @GetMapping("/{orcamentoId}")
    public Cobranca buscar(@PathVariable UUID orcamentoId) { return service.buscar(orcamentoId); }
}
