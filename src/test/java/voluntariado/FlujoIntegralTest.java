package voluntariado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import voluntariado.controlador.ControladorAsignacion.Resultado;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.CalculadorCobertura;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.Habilidad;
import voluntariado.seguridad.PermisoDenegadoException;
import voluntariado.servicios.MensajeMail;
import voluntariado.servicios.NotificadorMailConsola;

// Pruebas integrales (tarjetas 6.1 y 6.2): pasan por todas las capas con SQLite real.
class FlujoIntegralTest {

    @TempDir
    Path carpeta;
    private String archivo;
    private NotificadorMailConsola correo;
    private Sistema sistema;
    private final Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));
    private final CalculadorCobertura calculador = new CalculadorCobertura();

    @BeforeEach
    void preparar() {
        archivo = carpeta.resolve("integral.db").toString();
        correo = new NotificadorMailConsola();
        sistema = new Sistema(archivo, correo);
    }

    private void entrarComo(String usuario, String clave) {
        sistema.getSesion().iniciar(sistema.getAutenticacion().iniciarSesion(usuario, clave));
    }

    private EstadoCobertura cobertura(String codigoPunto) {
        return calculador.calcular(sistema.getRepositorio().buscarPunto(codigoPunto),
                sistema.getAsistencia().getRegistros());
    }

    @Test
    void flujoA_asignarYAvisar() throws Exception {
        entrarComo("coordinador", "Coord2026");
        assertEquals(EstadoCobertura.DEFICIT, cobertura("P2"));

        // 1. El sistema sugiere y el coordinador confirma.
        List<Asignacion> sugerencias = sistema.getAsignacion().sugerir("P2", Habilidad.CONTROL_ACCESOS, turno);
        assertEquals(4, sugerencias.size());
        Resultado resultado = sistema.getAsignacion().confirmar(sugerencias, turno, "Puerta lateral de la Catedral");
        assertEquals(4, resultado.confirmadas());

        // 2. Cada voluntario recibe su mail con el QR.
        assertEquals(4, correo.getEnviados().size());
        MensajeMail mail = correo.getEnviados().get(0);
        assertTrue(mail.cuerpo().contains("Puerta lateral de la Catedral"));

        // 3. El dia del evento se lee el QR del mail y se registra la entrada.
        String codigoLeido = voluntariado.servicios.CodigoQr.leerImagen(ImageIO.read(new ByteArrayInputStream(mail.qrPng())));
        sistema.getAsistencia().registrarEntradaManual(codigoLeido, LocalDateTime.now());
        assertEquals(EstadoCobertura.DEFICIT, cobertura("P2"));
        for (Asignacion a : sugerencias.subList(1, 4)) {
            sistema.getAsistencia().registrarEntradaManual(a.getCodigoQr(), LocalDateTime.now());
        }
        assertEquals(EstadoCobertura.CUBIERTO, cobertura("P2"));

        // 4. Al "cerrar y abrir" la app, todo sigue ahi (persistencia).
        Sistema reabierto = new Sistema(archivo, new NotificadorMailConsola());
        assertEquals(4, reabierto.getRepositorio().getAsignacionesPorPunto("P2").size());
        assertEquals(4, reabierto.getAsistencia().getRegistros().size());
    }

    @Test
    void flujoA_unOperadorNoPuedeConfirmar() {
        entrarComo("coordinador", "Coord2026");
        List<Asignacion> sugerencias = sistema.getAsignacion().sugerir("P2", Habilidad.CONTROL_ACCESOS, turno);
        entrarComo("operador", "Oper2026x");
        assertThrows(PermisoDenegadoException.class,
                () -> sistema.getAsignacion().confirmar(sugerencias, turno, "Catedral"));
        assertTrue(sistema.getRepositorio().getAsignacionesPorPunto("P2").isEmpty());
        assertFalse(sistema.getSesion().tienePermiso(voluntariado.seguridad.Accion.CONFIRMAR_ASIGNACIONES));
    }

    @Test
    void flujoB_emergenciaMueveGenteDeUnaZonaCercanaConSobrante() {
        // P2 (Catedral) tiene dotacion 4 y esta al lado de P1 (Plaza de Mayo, dotacion 6).
        entrarComo("admin", "Admin2026");
        sistema.getPuntos().modificar("P2", "Catedral", 5, List.of("Plaza de Mayo"));
        entrarComo("coordinador", "Coord2026");
        List<Asignacion> enP2 = sistema.getAsignacion().sugerir("P2", Habilidad.CONTROL_ACCESOS, turno);
        assertEquals(5, enP2.size());
        sistema.getAsignacion().confirmar(enP2, turno, "Catedral");
        for (Asignacion a : enP2) {
            sistema.getAsistencia().registrarEntradaManual(a.getCodigoQr(), LocalDateTime.now());
        }
        entrarComo("admin", "Admin2026");
        sistema.getPuntos().modificar("P2", "Catedral", 4, List.of("Plaza de Mayo"));
        entrarComo("coordinador", "Coord2026");
        assertEquals(EstadoCobertura.SOBRANTE, cobertura("P2"));
        assertEquals(EstadoCobertura.DEFICIT, cobertura("P1"));

        // Emergencia: P1 necesita gente. El sistema propone mover al sobrante de la zona cercana.
        List<Asignacion> propuestas = sistema.getAsignacion().sugerirReasignacion("P1", Habilidad.CONTROL_ACCESOS, turno);
        assertEquals(1, propuestas.size());
        int mailsAntes = correo.getEnviados().size();
        Resultado resultado = sistema.getAsignacion().confirmar(propuestas, turno, "Plaza de Mayo");

        assertEquals(1, resultado.reasignadas());
        assertEquals(EstadoCobertura.CUBIERTO, cobertura("P2"));
        assertEquals(1, sistema.getRepositorio().getAsignacionesPorPunto("P1").size());
        assertEquals(mailsAntes + 1, correo.getEnviados().size());
        assertEquals("Cambio de asignacion", correo.getEnviados().get(mailsAntes).asunto());

        // Y queda guardado: al reabrir, el voluntario sigue en P1 y P2 mantiene 4.
        Sistema reabierto = new Sistema(archivo, new NotificadorMailConsola());
        assertEquals(1, reabierto.getRepositorio().getAsignacionesPorPunto("P1").size());
        assertEquals(4, reabierto.getRepositorio().getAsignacionesPorPunto("P2").size());
    }
}
