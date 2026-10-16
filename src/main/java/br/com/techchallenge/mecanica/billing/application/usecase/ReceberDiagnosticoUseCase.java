package br.com.techchallenge.mecanica.billing.application.usecase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoDiagnosticoGateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
public class ReceberDiagnosticoUseCase {
    private final OrcamentoDiagnosticoGateway gateway;
    public ReceberDiagnosticoUseCase(OrcamentoDiagnosticoGateway gateway) { this.gateway = gateway; }
    public Orcamento executar(UUID reparoId, UUID ordemId, Instant ocorridoEm, List<ItemOrcamento> itens) {
        if (reparoId == null) throw new IllegalArgumentException("Referência do diagnóstico obrigatória.");
        var id = UUID.nameUUIDFromBytes(("mecanica:diagnostico:v1:" + reparoId).getBytes(StandardCharsets.UTF_8));
        var candidato = Orcamento.reconstituir(id, ordemId, itens, ocorridoEm, StatusOrcamento.AGUARDANDO_APROVACAO);
        return gateway.inserirOuRecuperar(candidato);
    }
}
