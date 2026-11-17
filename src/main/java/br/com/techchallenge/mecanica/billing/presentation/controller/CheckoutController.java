package br.com.techchallenge.mecanica.billing.presentation.controller;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import br.com.techchallenge.mecanica.billing.application.dto.Checkout;
import br.com.techchallenge.mecanica.billing.application.usecase.CriarCheckoutUseCase;
@io.swagger.v3.oas.annotations.tags.Tag(name = "CheckoutController")
@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RestController
@RequestMapping("/cobrancas")
public class CheckoutController {
    private final CriarCheckoutUseCase useCase;
    public CheckoutController(CriarCheckoutUseCase useCase) { this.useCase=useCase; }
    @io.swagger.v3.oas.annotations.Operation(summary = "Gerar checkoutUrl do Mercado Pago para cobranca pendente")
    @PostMapping("/{orcamentoId}/checkout")
    public Checkout criar(@PathVariable UUID orcamentoId) { return useCase.executar(orcamentoId); }
}
