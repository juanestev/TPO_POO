package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorPuntos;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;

class ControladorPuntosTest extends BaseTemporal {

    private RepositorioEnMemoria repositorio;
    private PuntoDeControlDAOSQLite dao;
    private ControladorPuntos controlador;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        dao = new PuntoDeControlDAOSQLite(conexion);
        controlador = new ControladorPuntos(repositorio, dao);
    }

    @Test
    void altaGuardaConSusZonasCercanas() {
        controlador.alta("P1", "Plaza", 5, List.of("Catedral", "Avenida"));

        assertEquals(List.of("Catedral", "Avenida"), dao.buscarPorCodigo("P1").getZonasCercanas());
        assertEquals(1, controlador.listar().size());
    }

    @Test
    void altaInvalidaNoDejaNadaGuardado() {
        assertThrows(DatosInvalidosException.class, () -> controlador.alta("P1", "Plaza", 0, List.of()));
        assertThrows(DatosInvalidosException.class, () -> controlador.alta("P1", "Plaza", 3, List.of("plaza")));
        assertTrue(repositorio.getPuntos().isEmpty());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void modificarCambiaZonaDotacionYCercanas() {
        controlador.alta("P1", "Plaza", 5, List.of("Catedral"));
        controlador.modificar("P1", "Catedral", 8, List.of("Plaza"));

        PuntoDeControl guardado = dao.buscarPorCodigo("P1");
        assertEquals("Catedral", guardado.getZona());
        assertEquals(8, guardado.getDotacionRequerida());
        assertEquals(List.of("Plaza"), guardado.getZonasCercanas());
    }

    @Test
    void modificarInvalidoDejaElPuntoComoEstaba() {
        controlador.alta("P1", "Plaza", 5, List.of("Catedral"));
        assertThrows(DatosInvalidosException.class, () -> controlador.modificar("P1", "Nueva", -1, List.of("Otra")));

        PuntoDeControl enMemoria = repositorio.buscarPunto("P1");
        assertEquals("Plaza", enMemoria.getZona());
        assertEquals(5, enMemoria.getDotacionRequerida());
        assertEquals(List.of("Catedral"), enMemoria.getZonasCercanas());
    }

    @Test
    void bajaBorraPeroNoSiTieneAsignaciones() {
        controlador.alta("P1", "Plaza", 5, List.of());
        controlador.alta("P2", "Catedral", 5, List.of());
        VoluntarioMedico medico = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        new VoluntarioDAOSQLite(conexion).guardar(medico);
        repositorio.agregarVoluntario(medico);
        Asignacion asignacion = new Asignacion(medico, repositorio.buscarPunto("P2"));
        asignacion.confirmar();
        new AsignacionDAOSQLite(conexion).guardar(asignacion);
        repositorio.registrarAsignacion(asignacion);

        controlador.baja("P1");
        assertThrows(DatosInvalidosException.class, () -> dao.buscarPorCodigo("P1"));
        assertThrows(DatosInvalidosException.class, () -> controlador.baja("P2"));
        assertEquals(1, controlador.listar().size());
    }
}
