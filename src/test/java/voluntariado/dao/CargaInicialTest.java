package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstrategiaPorHabilidadYDisponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.MotorAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;

// Hito 2 del plan: cerrar y abrir la app y los datos siguen ahi.
class CargaInicialTest extends BaseTemporal {

    private CargaInicial carga() {
        return new CargaInicial(new PuntoDeControlDAOSQLite(conexion), new VoluntarioDAOSQLite(conexion),
                new AsignacionDAOSQLite(conexion));
    }

    @Test
    void cargaLosDatosDePruebaUnaSolaVez() {
        PuntoDeControlDAO puntoDAO = new PuntoDeControlDAOSQLite(conexion);
        VoluntarioDAO voluntarioDAO = new VoluntarioDAOSQLite(conexion);
        assertTrue(DatosDePrueba.cargarSiEstaVacia(puntoDAO, voluntarioDAO));
        assertFalse(DatosDePrueba.cargarSiEstaVacia(puntoDAO, voluntarioDAO));

        RepositorioEnMemoria repositorio = carga().cargar();
        assertEquals(DatosDePrueba.CANTIDAD_VOLUNTARIOS, repositorio.getVoluntarios().size());
        assertEquals(5, puntoDAO.listarTodos().size());
        assertEquals(List.of("Plaza de Mayo"), repositorio.buscarPunto("P2").getZonasCercanas());
    }

    @Test
    void lasAsignacionesConfirmadasSobrevivenAReabrirLaApp() {
        DatosDePrueba.cargarSiEstaVacia(new PuntoDeControlDAOSQLite(conexion), new VoluntarioDAOSQLite(conexion));
        RepositorioEnMemoria primeraSesion = carga().cargar();
        PuntoDeControl plaza = primeraSesion.buscarPunto("P1");
        Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));

        // Flujo F1 contra datos de la base: sugerir, confirmar una y guardar ambas.
        List<Asignacion> sugeridas = new MotorAsignacion(primeraSesion, new EstrategiaPorHabilidadYDisponibilidad())
                .generarSugerencias(plaza, Habilidad.CONTROL_ACCESOS, turno);
        assertTrue(sugeridas.size() >= 2);
        AsignacionDAO asignacionDAO = new AsignacionDAOSQLite(conexion);
        Asignacion confirmada = sugeridas.get(0);
        confirmada.confirmar();
        asignacionDAO.guardar(confirmada);
        asignacionDAO.guardar(sugeridas.get(1));

        // "Reabrir": un repositorio nuevo, leido desde cero.
        RepositorioEnMemoria segundaSesion = carga().cargar();
        List<Asignacion> enPlaza = segundaSesion.getAsignacionesPorPunto("P1");
        assertEquals(1, enPlaza.size());
        assertEquals(confirmada.getCodigoQr(), enPlaza.get(0).getCodigoQr());
        assertTrue(segundaSesion.estaAsignado(confirmada.getVoluntario().getDni()));
    }

    @Test
    void inicializarElEsquemaDosVecesNoFalla() {
        conexion.inicializarEsquema();
        assertTrue(carga().cargar().getVoluntarios().isEmpty());
    }
}
