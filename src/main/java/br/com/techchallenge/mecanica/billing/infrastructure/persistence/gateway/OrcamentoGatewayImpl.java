package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import br.com.techchallenge.mecanica.billing.application.exception.ConflitoOrcamentoException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.application.gateway.OrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.orcamento.Orcamento;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository.OrcamentoMongoRepository;

public class OrcamentoGatewayImpl implements OrcamentoGateway {

    private final OrcamentoMongoRepository repository;
    private final OrcamentoPersistenceMapper mapper;
    private final MongoTemplate template;

    public OrcamentoGatewayImpl(
            OrcamentoMongoRepository repository,
            OrcamentoPersistenceMapper mapper, MongoTemplate template) {

        this.repository = Objects.requireNonNull(repository);
        this.mapper = Objects.requireNonNull(mapper);
        this.template = Objects.requireNonNull(template);
    }

    @Override
    public void salvar(Orcamento orcamento) {
        if (orcamento.getStatus() == StatusOrcamento.AGUARDANDO_APROVACAO) {
            try {
                repository.insert(mapper.toDocument(orcamento));
            } catch (DuplicateKeyException exception) {
                throw new ConflitoOrcamentoException(orcamento.getId());
            }
            return;
        }
        // A decisão só pode substituir o estado pendente, atomicamente no MongoDB.
        var query = Query.query(Criteria.where("_id").is(orcamento.getId().toString())
                .and("status").is(StatusOrcamento.AGUARDANDO_APROVACAO.name()));
        var update = new Update().set("status", orcamento.getStatus().name());
        var result = template.updateFirst(query, update, OrcamentoDocument.class);
        if (result.getMatchedCount() != 1) {
            throw new ConflitoOrcamentoException(orcamento.getId());
        }
    }

    @Override
    public Optional<Orcamento> buscarPorId(UUID id) {
        return repository.findById(id.toString())
                .map(mapper::toDomain);
    }
}
