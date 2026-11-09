package br.com.techchallenge.mecanica.billing.infrastructure.notification;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
class SmtpEmailGatewayTest {
    @Test void enviaMensagemPeloProtocoloSmtpSemContatoExterno() throws Exception {
        try(var server=new ServerSocket();var pool=Executors.newSingleThreadExecutor()) {
            server.bind(new InetSocketAddress("127.0.0.1",0));server.setSoTimeout(5000);
            var recebido=pool.submit(()->receber(server));
            var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(server.getLocalPort());sender.setDefaultEncoding("UTF-8");
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout","5000");
            new SmtpEmailGateway(sender,"oficina@example.test").enviar("cliente@example.test","Orçamento: https://oficina.example/orcamentos/id/aprovacao");
            var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties()),
                new ByteArrayInputStream(recebido.get(5,TimeUnit.SECONDS).getBytes(StandardCharsets.US_ASCII)));
            assertEquals("Orçamento da oficina para aprovação",message.getSubject());
            assertEquals("cliente@example.test",message.getAllRecipients()[0].toString());
            assertTrue(message.getContent().toString().contains("https://oficina.example/orcamentos/id/aprovacao"));
        }
    }
    private String receber(ServerSocket server) throws Exception {
        try(var socket=server.accept()) {
            socket.setSoTimeout(5000);var input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
            var output=new PrintWriter(socket.getOutputStream(),true,StandardCharsets.US_ASCII);output.print("220 localhost test\r\n");output.flush();
            var body=new StringBuilder();boolean data=false;String line;
            while((line=input.readLine())!=null) {
                if(data){if(line.equals(".")){data=false;output.print("250 accepted\r\n");}else body.append(line).append("\r\n");}
                else if(line.startsWith("DATA")){data=true;output.print("354 send message\r\n");}
                else if(line.startsWith("QUIT")){output.print("221 bye\r\n");output.flush();break;}
                else output.print("250 OK\r\n");
                output.flush();
            }
            return body.toString();
        }
    }
    @Test void remetenteInvalidoNaoPodeSerConfigurado() {
        for(String from:java.util.Arrays.asList(null,"","x\ny","x\ry"))
            assertThrows(IllegalArgumentException.class,()->new SmtpEmailGateway(new JavaMailSenderImpl(),from));
    }
}
