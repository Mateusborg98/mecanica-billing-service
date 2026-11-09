package br.com.techchallenge.mecanica.billing.infrastructure.persistence.gateway;
import java.time.Instant;
import java.util.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import br.com.techchallenge.mecanica.billing.application.dto.ConclusaoReparoInput;
import br.com.techchallenge.mecanica.billing.application.gateway.CobrancaGateway;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.document.*;
import br.com.techchallenge.mecanica.billing.infrastructure.persistence.mapper.OrcamentoPersistenceMapper;
public class CobrancaMongoGateway implements CobrancaGateway {
    private final MongoTemplate mongo;
    private final OrcamentoPersistenceMapper mapper;
    public CobrancaMongoGateway(MongoTemplate mongo,OrcamentoPersistenceMapper mapper) { this.mongo=mongo; this.mapper=mapper; }
    public Cobranca criarOuRecuperar(ConclusaoReparoInput input,Instant registradaEm) {
        var id=input.orcamentoId().toString();
        var orcamento=mongo.findById(id,OrcamentoDocument.class);
        if(orcamento==null || !"APROVADO".equals(orcamento.status())
                || !input.reparoId().toString().equals(orcamento.reparoId()) || !input.ordemId().toString().equals(orcamento.ordemId()))
            throw new IllegalArgumentException("Conclusão exige orçamento aprovado do mesmo reparo e ordem.");
        if(input.ocorridoEm().isBefore(orcamento.criadoEm()))
            throw new IllegalArgumentException("Conclusão anterior ao diagnóstico.");
        if(orcamento.cobranca()!=null) return conferirReentrega(orcamento,input);
        var cobranca=new CobrancaDocument(input.eventId().toString(),input.reparoId().toString(),input.ordemId().toString(),
                mapper.toDomain(orcamento).calcularTotal(),"BRL","PENDENTE",input.ocorridoEm(),
                registradaEm.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        // Charge and receipt marker are embedded in the approved budget, in one atomic update.
        var query=Query.query(Criteria.where("_id").is(id).and("status").is("APROVADO")
                .and("reparoId").is(input.reparoId().toString()).and("ordemId").is(input.ordemId().toString()).and("cobranca").is(null));
        var salvo=mongo.findAndModify(query,new Update().set("cobranca",cobranca),
                FindAndModifyOptions.options().returnNew(true),OrcamentoDocument.class);
        if(salvo==null) salvo=mongo.findById(id,OrcamentoDocument.class);
        if(salvo==null || salvo.cobranca()==null) throw new IllegalStateException("Cobrança não persistida; repetir processamento.");
        return conferirReentrega(salvo,input);
    }
    private Cobranca conferirReentrega(OrcamentoDocument doc,ConclusaoReparoInput input) {
        var c=doc.cobranca();
        if(!input.eventId().toString().equals(c.eventoId()) || !input.reparoId().toString().equals(c.reparoId())
                || !input.ordemId().toString().equals(c.ordemId()) || !input.ocorridoEm().equals(c.reparoConcluidoEm()))
            throw new IllegalArgumentException("Conclusão repetida com conteúdo diferente.");
        return toDomain(doc);
    }
    public Optional<Cobranca> buscar(UUID orcamentoId) {
        return Optional.ofNullable(mongo.findById(orcamentoId.toString(),OrcamentoDocument.class))
                .filter(doc->doc.cobranca()!=null).map(this::toDomain);
    }
    private Cobranca toDomain(OrcamentoDocument doc) {
        var c=doc.cobranca();
        return new Cobranca(UUID.fromString(doc.id()),UUID.fromString(c.ordemId()),UUID.fromString(c.reparoId()),
                c.valorTotal(),c.moeda(),Cobranca.Status.valueOf(c.status()),c.reparoConcluidoEm(),c.criadaEm());
    }
}
