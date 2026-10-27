package voluntariado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.CalculadorCobertura;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.EstrategiaPorHabilidadYDisponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.MotorAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Permisos;
import voluntariado.seguridad.Rol;
import voluntariado.servicios.CodigoQr;
import voluntariado.servicios.MensajeMail;
import voluntariado.servicios.NotificadorMailConsola;
import voluntariado.servicios.ServicioNotificaciones;

class FlujoSinBaseDeDatosTest {

    @Test
    void sugiereConfirmaAvisaYRegistraEntrada() throws Exception {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        VoluntarioMedico medico = new VoluntarioMedico("12345678", "Ana", "Perez",
                "1123456789", "ana@ejemplo.com");
        Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(12, 0));
        medico.agregarHabilidad(Habilidad.RCP);
        medico.agregarDisponibilidad(turno);
        repositorio.agregarPunto(punto);
        repositorio.agregarVoluntario(medico);

        Asignacion sugerida = new MotorAsignacion(repositorio,
                new EstrategiaPorHabilidadYDisponibilidad())
                .generarSugerencias(punto, Habilidad.RCP, turno).get(0);
        assertTrue(Permisos.tienePermiso(Rol.COORDINADOR, Accion.CONFIRMAR_ASIGNACIONES));
        sugerida.confirmar();
        repositorio.registrarAsignacion(sugerida);

        NotificadorMailConsola correoFalso = new NotificadorMailConsola();
        ServicioNotificaciones avisos = new ServicioNotificaciones(correoFalso);
        MensajeMail mensaje = avisos.crearAviso(sugerida, turno, "Plaza central");
        avisos.enviarOEncolar(mensaje);
        assertEquals(1, correoFalso.getEnviados().size());
        String codigoLeido = CodigoQr.leerImagen(ImageIO.read(new ByteArrayInputStream(mensaje.qrPng())));

        RegistroAsistencia asistencia = new RegistroAsistencia(sugerida);
        assertEquals(EstadoCobertura.DEFICIT,
                new CalculadorCobertura().calcular(punto, List.of(asistencia)));
        asistencia.registrarEntrada(codigoLeido, LocalDateTime.of(2026, 10, 26, 8, 0));
        assertEquals(EstadoCobertura.CUBIERTO,
                new CalculadorCobertura().calcular(punto, List.of(asistencia)));
    }
}
