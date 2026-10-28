package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.servicios.CodigoQr;

class ControladorAsistenciaTest extends BaseTemporal {

    private Asignacion asignacion;
    private RepositorioEnMemoria repositorio;
    private AsistenciaDAO dao;

    @BeforeEach
    void preparar() {
        VoluntarioMedico voluntaria = new VoluntarioMedico(
                "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        new VoluntarioDAOSQLite(conexion).guardar(voluntaria);
        new PuntoDeControlDAOSQLite(conexion).guardar(punto);
        asignacion = new Asignacion(voluntaria, punto);
        asignacion.confirmar();
        new AsignacionDAOSQLite(conexion).guardar(asignacion);
        repositorio = new RepositorioEnMemoria();
        repositorio.agregarVoluntario(voluntaria);
        repositorio.agregarPunto(punto);
        repositorio.registrarAsignacion(asignacion);
        dao = new AsistenciaDAOSQLite(conexion);
    }

    @Test
    void leeQrDeImagenYConservaCheckInYCheckOutTrasReiniciar() throws Exception {
        ControladorAsistencia controlador = new ControladorAsistencia(repositorio, dao);
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 27, 8, 0);
        LocalDateTime salida = LocalDateTime.of(2026, 10, 27, 12, 0);
        var imagen = ImageIO.read(new ByteArrayInputStream(
                CodigoQr.generarImagen(asignacion.getCodigoQr(), 200)));

        assertTrue(controlador.registrarEntradaDesdeImagen(imagen, entrada).estaPresente());
        assertEquals(entrada, new ControladorAsistencia(repositorio, dao).getRegistros().get(0).getEntrada());
        assertFalse(controlador.registrarSalidaManual(asignacion.getCodigoQr(), salida).estaPresente());
        assertEquals(salida, new ControladorAsistencia(repositorio, dao).getRegistros().get(0).getSalida());
    }

    @Test
    void codigoAjenoNoCreaAsistenciaYEntradaDuplicadaNoLaReemplaza() {
        ControladorAsistencia controlador = new ControladorAsistencia(repositorio, dao);
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 27, 8, 0);

        assertThrows(DatosInvalidosException.class,
                () -> controlador.registrarEntradaManual("codigo-inexistente", entrada));
        assertTrue(controlador.getRegistros().isEmpty());
        controlador.registrarEntradaManual(asignacion.getCodigoQr(), entrada);
        assertThrows(DatosInvalidosException.class,
                () -> controlador.registrarEntradaManual(asignacion.getCodigoQr(), entrada.plusHours(1)));
        assertEquals(entrada, new ControladorAsistencia(repositorio, dao).getRegistros().get(0).getEntrada());
    }
}
