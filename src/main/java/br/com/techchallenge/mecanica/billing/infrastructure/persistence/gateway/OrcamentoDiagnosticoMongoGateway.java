package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoDiagnosticoGateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
public class OrcamentoDiagnosticoMongoGateway implements OrcamentoDiagnosticoGateway {
    private final MongoTemplate mongo;
    private final OrcamentoPersistenceMapper mapper;
    public OrcamentoDiagnosticoMongoGateway(MongoTemplate mongo, OrcamentoPersistenceMapper mapper) {
        this.mongo = mongo; this.mapper = mapper;
    }
    public Orcamento inserirOuRecuperar(Orcamento candidato) {
        try { mongo.insert(mapper.toDocument(candidato)); return candidato; }
        catch (DuplicateKeyException exception) {
            var documento = mongo.findById(candidato.getId().toString(), OrcamentoDocument.class);
            if (documento == null) throw exception;
            var existente = mapper.toDomain(documento);
            if (!existente.getOrdemId().equals(candidato.getOrdemId()) || !mesmosItens(existente, candidato))
                throw new IllegalArgumentException("Diagnóstico repetido com conteúdo diferente.");
            return existente;
        }
    }
    private boolean mesmosItens(Orcamento a, Orcamento b) {
        if (a.getItens().size() != b.getItens().size()) return false;
        for (int n = 0; n < a.getItens().size(); n++) {
            var x = a.getItens().get(n); var y = b.getItens().get(n);
            if (!x.referenciaId().equals(y.referenciaId()) || x.tipo() != y.tipo() || !x.descricao().equals(y.descricao())
                    || x.quantidade() != y.quantidade() || x.valorUnitario().compareTo(y.valorUnitario()) != 0) return false;
        }
        return true;
    }
}
