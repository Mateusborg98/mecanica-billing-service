package br.com.techchallenge.mecanica.billing.application.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record OrderPagamento(String orderId, UUID cobrancaId, String status, String detalhe,
        String moeda, BigDecimal total, BigDecimal pago, Instant atualizadaEm) {
    public boolean creditada() { return "processed".equals(status) && "accredited".equals(detalhe); }
}
