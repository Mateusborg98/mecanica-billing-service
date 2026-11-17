package br.com.techchallenge.mecanica.billing.application.gateway;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.domain.cobranca.Cobranca;
public interface ProvedorCheckout { Checkout criar(Cobranca cobranca); }
