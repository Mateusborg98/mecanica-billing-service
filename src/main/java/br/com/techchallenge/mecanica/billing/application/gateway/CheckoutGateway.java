package br.com.techchallenge.mecanica.billing.application.gateway;
import java.util.UUID;
import java.util.Optional;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
public interface CheckoutGateway {
    Optional<Checkout> buscar(UUID cobrancaId);
    Checkout salvarSeAusente(UUID cobrancaId, Checkout checkout);
}
