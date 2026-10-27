package voluntariado.servicios;

import java.util.Properties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.DataHandler;

/** Adaptador SMTP. La conexion solo ocurre al invocar enviar. */
public class NotificadorMailSmtp implements NotificadorMail {
    private final String usuario;
    private final String contrasena;
    private final Session sesion;

    public NotificadorMailSmtp(String host, int puerto, String usuario, String contrasena) {
        if (host == null || host.isBlank() || puerto < 1 || puerto > 65535 || usuario == null || usuario.isBlank()
                || contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La configuracion SMTP es obligatoria");
        }
        this.usuario = usuario;
        this.contrasena = contrasena;
        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.host", host);
        propiedades.put("mail.smtp.port", Integer.toString(puerto));
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.starttls.enable", "true");
        propiedades.put("mail.smtp.starttls.required", "true");
        sesion = Session.getInstance(propiedades, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(NotificadorMailSmtp.this.usuario,
                        NotificadorMailSmtp.this.contrasena);
            }
        });
    }

    @Override
    public void enviar(MensajeMail mensaje) {
        if (mensaje == null) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        try {
            MimeMessage email = new MimeMessage(sesion);
            email.setFrom(new InternetAddress(usuario));
            email.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mensaje.destinatario()));
            email.setSubject(mensaje.asunto(), "UTF-8");
            if (mensaje.qrPng() == null) {
                email.setText(mensaje.cuerpo(), "UTF-8");
            } else {
                MimeMultipart contenido = new MimeMultipart();
                MimeBodyPart texto = new MimeBodyPart();
                texto.setText(mensaje.cuerpo(), "UTF-8");
                contenido.addBodyPart(texto);
                MimeBodyPart qr = new MimeBodyPart();
                qr.setDataHandler(new DataHandler(new ByteArrayDataSource(mensaje.qrPng(), "image/png")));
                qr.setFileName("codigo-qr.png");
                contenido.addBodyPart(qr);
                email.setContent(contenido);
            }
            Transport.send(email);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el mail", e);
        }
    }
}
