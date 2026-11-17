package br.com.techchallenge.mecanica.billing.domain.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

class OrcamentoReconstituicaoTest {

    private static final Instant CRIADO_EM =
            Instant.parse("2026-10-06T12:00:00Z");

    @ParameterizedTest
    @EnumSource(StatusOrcamento.class)
    void devePreservarDadosEStatusAoReconstituir(
            StatusOrcamento status) {

        UUID id = UUID.randomUUID();
        UUID ordemId = UUID.randomUUID();
        var itens = itensValidos();

        var orcamento = Orcamento.reconstituir(
                id, ordemId, itens, CRIADO_EM, status);

        assertEquals(id, orcamento.getId());
        assertEquals(ordemId, orcamento.getOrdemId());
        assertEquals(itens, orcamento.getItens());
        assertEquals(CRIADO_EM, orcamento.getCriadoEm());
        assertEquals(status, orcamento.getStatus());
        assertEquals(
                new BigDecimal("50.00"),
                orcamento.calcularTotal());
    }

    @Test
    void deveRejeitarIdentificadorAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> Orcamento.reconstituir(
                        null,
                        UUID.randomUUID(),
                        itensValidos(),
                        CRIADO_EM,
                        StatusOrcamento.APROVADO));
    }

    @Test
    void deveRejeitarStatusAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> Orcamento.reconstituir(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        itensValidos(),
                        CRIADO_EM,
                        null));
    }

    @Test
    void deveManterRegraDeOrcamentoAprovadoAposReconstituicao() {
        var orcamento = Orcamento.reconstituir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                itensValidos(),
                CRIADO_EM,
                StatusOrcamento.APROVADO);

        assertThrows(
                RegraNegocioException.class,
                orcamento::recusar);

        assertEquals(
                StatusOrcamento.APROVADO,
                orcamento.getStatus());
    }

    @Test
    void devePermitirAprovarOrcamentoPendenteReconstituido() {
        var orcamento = Orcamento.reconstituir(
                UUID.randomUUID(),
                UUID.randomUUID(),
                itensValidos(),
                CRIADO_EM,
                StatusOrcamento.AGUARDANDO_APROVACAO);

        orcamento.aprovar();

        assertEquals(
                StatusOrcamento.APROVADO,
                orcamento.getStatus());
    }

    private List<ItemOrcamento> itensValidos() {
        return List.of(new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "Filtro",
                2,
                new BigDecimal("25.00")));
    }
}
