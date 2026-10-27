package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.Voluntario;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.modelo.VoluntarioSeguridad;

class VoluntarioDAOTest extends BaseTemporal {

    private final Disponibilidad sabadoManana =
            new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));

    private Voluntario medico() {
        Voluntario medico = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        medico.agregarHabilidad(Habilidad.RCP);
        medico.agregarHabilidad(Habilidad.INGLES);
        medico.agregarDisponibilidad(sabadoManana);
        return medico;
    }

    @Test
    void guardaYLeeElVoluntarioConSubclaseHabilidadesYDisponibilidad() {
        VoluntarioDAO dao = new VoluntarioDAOSQLite(conexion);
        dao.guardar(medico());

        Voluntario leido = dao.buscarPorDni("12345678");
        assertInstanceOf(VoluntarioMedico.class, leido);
        assertEquals("Perez", leido.getApellido());
        assertEquals(Set.of(Habilidad.RCP, Habilidad.INGLES), leido.getHabilidades());
        assertEquals(List.of(sabadoManana), leido.getDisponibilidades());
    }

    @Test
    void guardarDosVecesActualizaYReemplazaLasListas() {
        VoluntarioDAO dao = new VoluntarioDAOSQLite(conexion);
        Voluntario medico = medico();
        dao.guardar(medico);

        medico.setTelefono("1199998888");
        medico.quitarHabilidad(Habilidad.INGLES);
        medico.quitarDisponibilidad(sabadoManana);
        dao.guardar(medico);

        Voluntario leido = dao.buscarPorDni("12345678");
        assertEquals("1199998888", leido.getTelefono());
        assertEquals(Set.of(Habilidad.RCP), leido.getHabilidades());
        assertTrue(leido.getDisponibilidades().isEmpty());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void listaTodosOrdenadosPorDniYConSuTipo() {
        VoluntarioDAO dao = new VoluntarioDAOSQLite(conexion);
        dao.guardar(new VoluntarioSeguridad("23456789", "Luis", "Diaz", "1123450000", "luis@ejemplo.com"));
        dao.guardar(medico());

        List<Voluntario> todos = dao.listarTodos();
        assertEquals(2, todos.size());
        assertInstanceOf(VoluntarioMedico.class, todos.get(0));
        assertInstanceOf(VoluntarioSeguridad.class, todos.get(1));
    }

    @Test
    void eliminarBorraTambienSusHabilidadesYDisponibilidades() {
        VoluntarioDAO dao = new VoluntarioDAOSQLite(conexion);
        dao.guardar(medico());
        dao.eliminar("12345678");

        assertTrue(dao.listarTodos().isEmpty());
        assertThrows(DatosInvalidosException.class, () -> dao.buscarPorDni("12345678"));
        assertThrows(DatosInvalidosException.class, () -> dao.eliminar("12345678"));
        // Si lo vuelvo a dar de alta no aparecen habilidades viejas: el borrado fue en cascada.
        dao.guardar(new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com"));
        assertTrue(dao.buscarPorDni("12345678").getHabilidades().isEmpty());
    }
}
