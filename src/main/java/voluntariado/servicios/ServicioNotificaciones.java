package voluntariado.servicios;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstadoAsignacion;

public class ServicioNotificaciones {

    private final NotificadorMail notificador;
    private final Queue<MensajeMail> pendientes = new ArrayDeque<>();

    public ServicioNotificaciones(NotificadorMail notificador) {
        if (notificador == null) {
            throw new IllegalArgumentException("El notificador es obligatorio");
        }
        this.notificador = notificador;
    }

    public MensajeMail crearAviso(Asignacion asignacion, Disponibilidad turno, String puntoEncuentro) {
        if (asignacion == null || asignacion.getEstado() != EstadoAsignacion.CONFIRMADA
                || turno == null || puntoEncuentro == null || puntoEncuentro.isBlank()) {
            throw new IllegalArgumentException("Se necesita una asignacion confirmada, turno y punto de encuentro");
        }
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("HH:mm");
        String cuerpo = "Puesto: " + asignacion.getVoluntario().getRol()
                + "\nZona: " + asignacion.getPunto().getZona()
                + "\nHorario: " + turno.getDia().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-AR"))
                + " " + turno.getDesde().format(formato)
                + " a " + turno.getHasta().format(formato)
                + "\nPunto de encuentro: " + puntoEncuentro.trim()
                + "\nCodigo para ingreso manual: " + asignacion.getCodigoQr();
        return new MensajeMail(asignacion.getVoluntario().getMail(), "Asignacion confirmada",
                cuerpo, CodigoQr.generarImagen(asignacion.getCodigoQr(), 200));
    }

    public void enviarOEncolar(MensajeMail mensaje) {
        if (mensaje == null) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        try {
            notificador.enviar(mensaje);
        } catch (IllegalStateException falloDeRed) {
            pendientes.add(mensaje);
        }
    }

    public void reintentarPendientes() {
        int intentos = pendientes.size();
        for (int i = 0; i < intentos; i++) {
            MensajeMail mensaje = pendientes.remove();
            try {
                notificador.enviar(mensaje);
            } catch (IllegalStateException falloDeRed) {
                pendientes.add(mensaje);
            }
        }
    }

    public int getCantidadPendientes() {
        return pendientes.size();
    }
}
