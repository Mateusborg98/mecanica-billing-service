package br.com.techchallenge.mecanica.billing.domain.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

class OrcamentoTest {

    private static final Instant CRIADO_EM =
            Instant.parse("2026-09-28T12:00:00Z");

    @Test
    void deveCriarOrcamentoAguardandoAprovacao() {
        UUID ordemId = UUID.randomUUID();

        var orcamento = new Orcamento(
                ordemId, List.of(peca()), CRIADO_EM);

        assertNotNull(orcamento.getId());
        assertEquals(ordemId, orcamento.getOrdemId());
        assertEquals(CRIADO_EM, orcamento.getCriadoEm());
        assertEquals(
                StatusOrcamento.AGUARDANDO_APROVACAO,
                orcamento.getStatus());
    }

    @Test
    void deveSomarSubtotaisDePecasEServicos() {
        var orcamento = new Orcamento(
                UUID.randomUUID(),
                List.of(peca(), servico()),
                CRIADO_EM);

        assertEquals(
                new BigDecimal("170.00"),
                orcamento.calcularTotal());
    }

    @Test
    void deveAprovarOrcamentoPendente() {
        var orcamento = novoOrcamento();

        orcamento.aprovar();

        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
    }

    @Test
    void deveRecusarOrcamentoPendente() {
        var orcamento = novoOrcamento();

        orcamento.recusar();

        assertEquals(StatusOrcamento.RECUSADO, orcamento.getStatus());
    }

    @Test
    void deveImpedirRecusaDeOrcamentoAprovado() {
        var orcamento = novoOrcamento();
        orcamento.aprovar();

        assertThrows(RegraNegocioException.class, orcamento::recusar);
        assertEquals(StatusOrcamento.APROVADO, orcamento.getStatus());
    }

    @Test
    void deveImpedirAprovacaoDeOrcamentoRecusado() {
        var orcamento = novoOrcamento();
        orcamento.recusar();

        assertThrows(RegraNegocioException.class, orcamento::aprovar);
        assertEquals(StatusOrcamento.RECUSADO, orcamento.getStatus());
    }

    @Test
    void devePreservarItensQuandoListaOriginalForAlterada() {
        var itens = new ArrayList<>(List.of(peca()));
        var orcamento = new Orcamento(
                UUID.randomUUID(), itens, CRIADO_EM);

        itens.clear();

        assertEquals(1, orcamento.getItens().size());
        assertEquals(new BigDecimal("50.00"), orcamento.calcularTotal());
    }

    @Test
    void deveImpedirAlteracaoDosItensPeloGetter() {
        var orcamento = novoOrcamento();

        assertThrows(
                UnsupportedOperationException.class,
                () -> orcamento.getItens().clear());
    }

    @Test
    void deveRejeitarOrdemAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new Orcamento(null, List.of(peca()), CRIADO_EM));
    }

    @Test
    void deveRejeitarListaAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new Orcamento(UUID.randomUUID(), null, CRIADO_EM));
    }

    @Test
    void deveRejeitarListaVazia() {
        assertThrows(
                RegraNegocioException.class,
                () -> new Orcamento(
                        UUID.randomUUID(), List.of(), CRIADO_EM));
    }

    @Test
    void deveRejeitarItemNulo() {
        assertThrows(
                RegraNegocioException.class,
                () -> new Orcamento(
                        UUID.randomUUID(),
                        Arrays.asList(peca(), null),
                        CRIADO_EM));
    }

    @Test
    void deveRejeitarDataAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new Orcamento(
                        UUID.randomUUID(), List.of(peca()), null));
    }

    private Orcamento novoOrcamento() {
        return new Orcamento(
                UUID.randomUUID(), List.of(peca()), CRIADO_EM);
    }

    private ItemOrcamento peca() {
        return new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "Filtro",
                2,
                new BigDecimal("25.00"));
    }

    private ItemOrcamento servico() {
        return new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.SERVICO,
                "Alinhamento",
                1,
                new BigDecimal("120.00"));
    }
}
