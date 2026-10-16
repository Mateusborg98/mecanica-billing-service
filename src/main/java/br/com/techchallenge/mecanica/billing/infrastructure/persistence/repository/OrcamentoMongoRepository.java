package br.com.techchallenge.mecanica.billing.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.OrcamentoDocument;

public interface OrcamentoMongoRepository
        extends MongoRepository<OrcamentoDocument, String> {
}
