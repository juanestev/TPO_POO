package voluntariado.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.VoluntarioMedico;

class ServicioNotificacionesTest {

    @Test
    void avisoIncluyeDatosDelTurnoYQrPersonal() throws Exception {
        ServicioNotificaciones servicio = new ServicioNotificaciones(new NotificadorMailConsola());
        Asignacion asignacion = new Asignacion(
                new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com"),
                new PuntoDeControl("P1", "Centro", 2));
        Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearAviso(asignacion, turno, "Plaza"));
        asignacion.confirmar();

        MensajeMail aviso = servicio.crearAviso(asignacion, turno, " Plaza ");

        assertEquals("ana@ejemplo.com", aviso.destinatario());
        assertTrue(aviso.cuerpo().contains("Puesto: MEDICO"));
        assertTrue(aviso.cuerpo().contains("Horario: lunes 08:00 a 12:00"));
        assertTrue(aviso.cuerpo().contains("Punto de encuentro: Plaza"));
        assertEquals(asignacion.getCodigoQr(),
                CodigoQr.leerImagen(ImageIO.read(new ByteArrayInputStream(aviso.qrPng()))));
    }

    @Test
    void avisoCambioIndicaOrigenDestinoYUsaElQrNuevo() throws Exception {
        ServicioNotificaciones servicio = new ServicioNotificaciones(new NotificadorMailConsola());
        VoluntarioMedico voluntaria = new VoluntarioMedico(
                "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        Asignacion anterior = new Asignacion(voluntaria, new PuntoDeControl("P1", "Centro", 2));
        Asignacion nueva = new Asignacion(voluntaria, new PuntoDeControl("P2", "Norte", 2));
        Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        anterior.confirmar();
        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearAvisoCambio(anterior, nueva, turno, "Plaza"));
        nueva.confirmar();

        MensajeMail aviso = servicio.crearAvisoCambio(anterior, nueva, turno, "Plaza");

        assertEquals("ana@ejemplo.com", aviso.destinatario());
        assertTrue(aviso.cuerpo().contains("Punto anterior: P1 (Centro)"));
        assertTrue(aviso.cuerpo().contains("Punto nuevo: P2 (Norte)"));
        assertTrue(aviso.cuerpo().contains("Horario: lunes 08:00 a 12:00"));
        assertEquals(nueva.getCodigoQr(),
                CodigoQr.leerImagen(ImageIO.read(new ByteArrayInputStream(aviso.qrPng()))));
    }

    @Test
    void conservaMensajesFallidosYLosReintenta() {
        class NotificadorInestable implements NotificadorMail {
            int intentos;
            @Override
            public void enviar(MensajeMail mensaje) {
                intentos++;
                if (intentos == 1) {
                    throw new IllegalStateException("Red caida");
                }
            }
        }
        NotificadorInestable notificador = new NotificadorInestable();
        ServicioNotificaciones servicio = new ServicioNotificaciones(notificador);
        MensajeMail mensaje = new MensajeMail("ana@ejemplo.com", "Turno", "Presentarse a las 8");

        servicio.enviarOEncolar(mensaje);
        assertEquals(1, servicio.getCantidadPendientes());
        servicio.reintentarPendientes();
        assertEquals(0, servicio.getCantidadPendientes());
        assertEquals(2, notificador.intentos);
    }
}
