package br.com.techchallenge.mecanica.billing.domain.orcamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

public class Orcamento {

    private final UUID id;
    private final UUID ordemId;
    private final List<ItemOrcamento> itens;
    private final Instant criadoEm;

    private StatusOrcamento status;

    public Orcamento(
            UUID ordemId,
            List<ItemOrcamento> itens,
            Instant criadoEm) {

        this(
                UUID.randomUUID(),
                ordemId,
                itens,
                criadoEm,
                StatusOrcamento.AGUARDANDO_APROVACAO);
    }

    public static Orcamento reconstituir(
            UUID id,
            UUID ordemId,
            List<ItemOrcamento> itens,
            Instant criadoEm,
            StatusOrcamento status) {

        return new Orcamento(id, ordemId, itens, criadoEm, status);
    }

    private Orcamento(
            UUID id,
            UUID ordemId,
            List<ItemOrcamento> itens,
            Instant criadoEm,
            StatusOrcamento status) {

        if (id == null) {
            throw new RegraNegocioException(
                    "Identificador do orçamento obrigatório");
        }

        if (ordemId == null) {
            throw new RegraNegocioException(
                    "Identificador da ordem obrigatório");
        }

        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException(
                    "Orçamento deve possuir pelo menos um item");
        }

        if (itens.stream().anyMatch(item -> item == null)) {
            throw new RegraNegocioException(
                    "Orçamento não pode possuir item nulo");
        }

        if (criadoEm == null) {
            throw new RegraNegocioException(
                    "Data de criação obrigatória");
        }

        if (status == null) {
            throw new RegraNegocioException(
                    "Status do orçamento obrigatório");
        }

        this.id = id;
        this.ordemId = ordemId;
        this.itens = List.copyOf(itens);
        this.criadoEm = criadoEm;
        this.status = status;
    }

    public BigDecimal calcularTotal() {
        return itens.stream()
                .map(ItemOrcamento::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void aprovar() {
        validarAguardandoAprovacao();
        this.status = StatusOrcamento.APROVADO;
    }

    public void recusar() {
        validarAguardandoAprovacao();
        this.status = StatusOrcamento.RECUSADO;
    }

    private void validarAguardandoAprovacao() {
        if (status != StatusOrcamento.AGUARDANDO_APROVACAO) {
            throw new RegraNegocioException(
                    "Orçamento já possui uma decisão: " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrdemId() {
        return ordemId;
    }

    public List<ItemOrcamento> getItens() {
        return itens;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }

        if (!(objeto instanceof Orcamento outro)) {
            return false;
        }

        return id.equals(outro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
