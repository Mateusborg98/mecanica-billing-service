package br.com.techchallenge.mecanica.billing.presentation.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.techchallenge.mecanica.billing.application.usecase.AprovarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.BuscarOrcamentoPorIdUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.CriarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.application.usecase.RecusarOrcamentoUseCase;
import br.com.techchallenge.mecanica.billing.presentation.dto.CriarOrcamentoRequest;
import br.com.techchallenge.mecanica.billing.presentation.dto.OrcamentoResponse;
import br.com.techchallenge.mecanica.billing.presentation.mapper.OrcamentoPresentationMapper;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private final CriarOrcamentoUseCase criarUseCase;
    private final BuscarOrcamentoPorIdUseCase buscarUseCase;
    private final AprovarOrcamentoUseCase aprovarUseCase;
    private final RecusarOrcamentoUseCase recusarUseCase;
    private final OrcamentoPresentationMapper mapper;

    public OrcamentoController(
            CriarOrcamentoUseCase criarUseCase,
            BuscarOrcamentoPorIdUseCase buscarUseCase,
            AprovarOrcamentoUseCase aprovarUseCase,
            RecusarOrcamentoUseCase recusarUseCase,
            OrcamentoPresentationMapper mapper) {

        this.criarUseCase = criarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.aprovarUseCase = aprovarUseCase;
        this.recusarUseCase = recusarUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<OrcamentoResponse> criar(
            @Valid @RequestBody CriarOrcamentoRequest request) {

        var orcamento = criarUseCase.executar(mapper.toInput(request));

        return ResponseEntity
                .created(URI.create("/orcamentos/" + orcamento.getId()))
                .body(mapper.toResponse(orcamento));
    }

    @GetMapping("/{id}")
    public OrcamentoResponse buscar(@PathVariable UUID id) {
        return mapper.toResponse(buscarUseCase.executar(id));
    }

    @PostMapping("/{id}/aprovar")
    public OrcamentoResponse aprovar(@PathVariable UUID id) {
        return mapper.toResponse(aprovarUseCase.executar(id));
    }

    @PostMapping("/{id}/recusar")
    public OrcamentoResponse recusar(@PathVariable UUID id) {
        return mapper.toResponse(recusarUseCase.executar(id));
    }
}
