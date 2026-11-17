package br.com.techchallenge.mecanica.billing.domain.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;

class ItemOrcamentoTest {

    @Test
    void deveCalcularSubtotalDasPecas() {
        var item = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "Filtro",
                2,
                new BigDecimal("25.50"));

        assertEquals(
                new BigDecimal("51.00"),
                item.calcularSubtotal());
    }

    @Test
    void deveCalcularSubtotalDoServico() {
        var item = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.SERVICO,
                "Alinhamento",
                1,
                new BigDecimal("120.00"));

        assertEquals(
                new BigDecimal("120.00"),
                item.calcularSubtotal());
    }

    @Test
    void deveRemoverEspacosNasExtremidadesDaDescricao() {
        var item = new ItemOrcamento(
                UUID.randomUUID(),
                TipoItemOrcamento.PECA,
                "  Filtro de óleo  ",
                1,
                new BigDecimal("25.00"));

        assertEquals("Filtro de óleo", item.descricao());
    }

    @Test
    void deveRejeitarReferenciaAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        null,
                        TipoItemOrcamento.PECA,
                        "Filtro",
                        1,
                        BigDecimal.TEN));
    }

    @Test
    void deveRejeitarTipoAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        UUID.randomUUID(),
                        null,
                        "Filtro",
                        1,
                        BigDecimal.TEN));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveRejeitarDescricaoInvalida(String descricao) {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        UUID.randomUUID(),
                        TipoItemOrcamento.PECA,
                        descricao,
                        1,
                        BigDecimal.TEN));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarQuantidadeInvalida(int quantidade) {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        UUID.randomUUID(),
                        TipoItemOrcamento.PECA,
                        "Filtro",
                        quantidade,
                        BigDecimal.TEN));
    }

    @Test
    void deveRejeitarValorUnitarioAusente() {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        UUID.randomUUID(),
                        TipoItemOrcamento.PECA,
                        "Filtro",
                        1,
                        null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.01"})
    void deveRejeitarValorUnitarioNaoPositivo(String valor) {
        assertThrows(
                RegraNegocioException.class,
                () -> new ItemOrcamento(
                        UUID.randomUUID(),
                        TipoItemOrcamento.PECA,
                        "Filtro",
                        1,
                        new BigDecimal(valor)));
    }
}
