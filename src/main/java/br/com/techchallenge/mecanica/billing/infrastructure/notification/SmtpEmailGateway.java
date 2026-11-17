package br.com.techchallenge.mecanica.billing.infrastructure.notification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import br.com.techchallenge.mecanica.billing.application.gateway.EnvioEmailGateway;
public final class SmtpEmailGateway implements EnvioEmailGateway {
    private final JavaMailSender sender;
    private final String remetente;
    public SmtpEmailGateway(JavaMailSender sender,String remetente) {
        if(remetente==null || remetente.isBlank() || remetente.contains("\r") || remetente.contains("\n"))
            throw new IllegalArgumentException("Configure MAIL_FROM.");
        this.sender=sender;this.remetente=remetente;
    }
    public void enviar(String destinatario,String texto) {
        var message=new SimpleMailMessage();message.setFrom(remetente);message.setTo(destinatario);
        message.setSubject("Orçamento da oficina para aprovação");message.setText(texto);sender.send(message);
    }
}
