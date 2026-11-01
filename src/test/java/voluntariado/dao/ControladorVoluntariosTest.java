package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorVoluntarios;
import voluntariado.controlador.ControladorVoluntarios.Tipo;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.Voluntario;

class ControladorVoluntariosTest extends BaseTemporal {

    private RepositorioEnMemoria repositorio;
    private VoluntarioDAOSQLite dao;
    private ControladorVoluntarios controlador;
    private final Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        dao = new VoluntarioDAOSQLite(conexion);
        controlador = new ControladorVoluntarios(repositorio, dao);
    }

    @Test
    void altaGuardaEnMemoriaYEnLaBase() {
        controlador.alta(Tipo.MEDICO, "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com",
                Set.of(Habilidad.RCP), List.of(turno));

        assertEquals("Ana", repositorio.buscarVoluntario("12345678").getNombre());
        Voluntario guardado = dao.buscarPorDni("12345678");
        assertTrue(guardado.getHabilidades().contains(Habilidad.RCP));
        assertEquals(1, guardado.getDisponibilidades().size());
    }

    @Test
    void altaConDatosInvalidosNoDejaNadaGuardado() {
        assertThrows(DatosInvalidosException.class, () -> controlador.alta(Tipo.SEGURIDAD, "123", "Ana", "Perez",
                "1123456789", "ana@ejemplo.com", Set.of(), List.of()));
        assertTrue(repositorio.getVoluntarios().isEmpty());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void altaConDniRepetidoSeRechaza() {
        controlador.alta(Tipo.MEDICO, "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com", Set.of(), List.of());
        assertThrows(DatosInvalidosException.class, () -> controlador.alta(Tipo.SEGURIDAD, "12345678", "Otra", "Persona",
                "1123456789", "otra@ejemplo.com", Set.of(), List.of()));
        assertEquals("Ana", dao.buscarPorDni("12345678").getNombre());
    }

    @Test
    void modificarCambiaMemoriaYBase() {
        controlador.alta(Tipo.MEDICO, "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com",
                Set.of(Habilidad.RCP), List.of(turno));
        controlador.modificar("12345678", "Anita", "Perez", "1155556666", "anita@ejemplo.com",
                Set.of(Habilidad.INGLES), List.of());

        Voluntario guardado = dao.buscarPorDni("12345678");
        assertEquals("Anita", guardado.getNombre());
        assertEquals(Set.of(Habilidad.INGLES), guardado.getHabilidades());
        assertTrue(guardado.getDisponibilidades().isEmpty());
    }

    @Test
    void modificarConDatoInvalidoDejaTodoComoEstaba() {
        controlador.alta(Tipo.MEDICO, "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com",
                Set.of(Habilidad.RCP), List.of(turno));
        assertThrows(DatosInvalidosException.class, () -> controlador.modificar("12345678", "Anita", "Perez",
                "1155556666", "mail-malo", Set.of(Habilidad.INGLES), List.of()));

        Voluntario enMemoria = repositorio.buscarVoluntario("12345678");
        assertEquals("Ana", enMemoria.getNombre());
        assertEquals(Set.of(Habilidad.RCP), enMemoria.getHabilidades());
        assertEquals(1, enMemoria.getDisponibilidades().size());
    }

    @Test
    void bajaBorraDeLosDosLugaresPeroNoSiTieneAsignacion() {
        controlador.alta(Tipo.MEDICO, "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com", Set.of(), List.of());
        controlador.alta(Tipo.MEDICO, "87654321", "Bea", "Gomez", "1123456780", "bea@ejemplo.com", Set.of(), List.of());
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        new PuntoDeControlDAOSQLite(conexion).guardar(punto);
        repositorio.agregarPunto(punto);
        Asignacion asignacion = new Asignacion(repositorio.buscarVoluntario("87654321"), punto);
        asignacion.confirmar();
        new AsignacionDAOSQLite(conexion).guardar(asignacion);
        repositorio.registrarAsignacion(asignacion);

        controlador.baja("12345678");
        assertThrows(DatosInvalidosException.class, () -> repositorio.buscarVoluntario("12345678"));
        assertThrows(DatosInvalidosException.class, () -> dao.buscarPorDni("12345678"));
        assertThrows(DatosInvalidosException.class, () -> controlador.baja("87654321"));
        assertEquals(1, controlador.listar().size());
    }

    @Test
    void listarDevuelveOrdenadoPorApellido() {
        controlador.alta(Tipo.MEDICO, "12345678", "Zoe", "Zapata", "1123456789", "z@ejemplo.com", Set.of(), List.of());
        controlador.alta(Tipo.SEGURIDAD, "87654321", "Ana", "Acosta", "1123456780", "a@ejemplo.com", Set.of(), List.of());
        assertEquals("Acosta", controlador.listar().get(0).getApellido());
    }
}
