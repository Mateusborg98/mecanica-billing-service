package br.com.techchallenge.mecanica.billing.application.usecase;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;
import br.com.techchallenge.mecanica.billing.application.gateway.NotificacaoOrcamentoGateway;
import br.com.techchallenge.mecanica.billing.domain.exception.RegraNegocioException;
import br.com.techchallenge.mecanica.billing.domain.orcamento.StatusOrcamento;
public final class EnviarOrcamentoUseCase {
    private final BuscarOrcamentoPorIdUseCase buscar;
    private final NotificacaoOrcamentoGateway gateway;
    private final String base;
    private final Clock clock;
    public EnviarOrcamentoUseCase(BuscarOrcamentoPorIdUseCase buscar, NotificacaoOrcamentoGateway gateway, String base, Clock clock) {
        var uri=URI.create(base);
        if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null
            || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && java.util.Set.of("localhost","127.0.0.1").contains(uri.getHost()))))
            throw new IllegalArgumentException("Informe URL pública HTTPS ou localhost para testes.");
        this.buscar=buscar;this.gateway=gateway;this.base=base.replaceAll("/+$","");this.clock=clock;
    }
    public NotificacaoOrcamentoGateway.Situacao executar(UUID id,String destinatario) {
        if(destinatario==null || destinatario.isBlank() || destinatario.contains("\r") || destinatario.contains("\n"))
            throw new RegraNegocioException("Destinatário inválido.");
        var o=buscar.executar(id);
        if(o.getStatus()!=StatusOrcamento.AGUARDANDO_APROVACAO) throw new RegraNegocioException("Orçamento não aguarda aprovação.");
        String texto="Seu orçamento da oficina está disponível.\nTotal: R$ "+o.calcularTotal().toPlainString()
            +"\nConfira os itens e aprove ou recuse pelo link:\n"+base+"/orcamentos/"+id+"/aprovacao"
            +"\nA aprovação autoriza o reparo. O pagamento será solicitado após sua conclusão, antes da entrega.";
        gateway.agendar(id,destinatario.trim(),texto,clock.instant());
        return gateway.buscar(id);
    }
}
