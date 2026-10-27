package voluntariado.servicios;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Adaptador falso para pruebas y demos sin red. */
public class NotificadorMailConsola implements NotificadorMail {
    private final List<MensajeMail> enviados = new ArrayList<>();

    @Override
    public void enviar(MensajeMail mensaje) {
        if (mensaje == null) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        enviados.add(mensaje);
        // El mensaje queda disponible para la demo sin mostrar datos personales ni el codigo QR en la consola.
        System.out.println("Mail simulado registrado");
    }

    public List<MensajeMail> getEnviados() {
        return Collections.unmodifiableList(enviados);
    }
}
