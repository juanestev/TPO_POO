package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.Voluntario;
import voluntariado.modelo.VoluntarioMedico;

class AsignacionDAOTest extends BaseTemporal {

    private Voluntario medico;
    private PuntoDeControl centro;
    private PuntoDeControl norte;
    private AsignacionDAO dao;
    private RepositorioEnMemoria repositorio;

    @BeforeEach
    void cargarVoluntarioYPuntos() {
        medico = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        centro = new PuntoDeControl("P1", "Centro", 2);
        norte = new PuntoDeControl("P2", "Norte", 2);
        new VoluntarioDAOSQLite(conexion).guardar(medico);
        new PuntoDeControlDAOSQLite(conexion).guardar(centro);
        new PuntoDeControlDAOSQLite(conexion).guardar(norte);
        dao = new AsignacionDAOSQLite(conexion);
        repositorio = new RepositorioEnMemoria();
        repositorio.agregarVoluntario(medico);
        repositorio.agregarPunto(centro);
        repositorio.agregarPunto(norte);
    }

    @Test
    void conservaElCodigoQrYActualizaElEstadoAlConfirmar() {
        Asignacion asignacion = new Asignacion(medico, centro);
        dao.guardar(asignacion);
        asignacion.confirmar();
        dao.guardar(asignacion);

        List<Asignacion> leidas = dao.listarTodas(repositorio);
        assertEquals(1, leidas.size());
        assertEquals(asignacion.getCodigoQr(), leidas.get(0).getCodigoQr());
        assertEquals(EstadoAsignacion.CONFIRMADA, leidas.get(0).getEstado());
        // Apunta a los mismos objetos del repositorio, no a copias.
        assertSame(medico, leidas.get(0).getVoluntario());
        assertSame(centro, leidas.get(0).getPunto());
    }

    @Test
    void unVoluntarioPuedeTenerVariasSugeridasPeroUnaSolaConfirmada() {
        Asignacion enCentro = new Asignacion(medico, centro);
        Asignacion enNorte = new Asignacion(medico, norte);
        dao.guardar(enCentro);
        dao.guardar(enNorte);
        assertEquals(2, dao.listarTodas(repositorio).size());

        enCentro.confirmar();
        dao.guardar(enCentro);
        enNorte.confirmar();
        assertThrows(PersistenciaException.class, () -> dao.guardar(enNorte));
    }

    @Test
    void noSePuedeBorrarUnVoluntarioOPuntoConAsignaciones() {
        Asignacion asignacion = new Asignacion(medico, centro);
        dao.guardar(asignacion);

        assertThrows(PersistenciaException.class, () -> new VoluntarioDAOSQLite(conexion).eliminar("12345678"));
        assertThrows(PersistenciaException.class, () -> new PuntoDeControlDAOSQLite(conexion).eliminar("P1"));

        dao.eliminar(asignacion.getCodigoQr());
        new VoluntarioDAOSQLite(conexion).eliminar("12345678");
        assertTrue(dao.listarTodas(repositorio).isEmpty());
    }

    @Test
    void noSeGuardaUnaAsignacionAUnPuntoQueNoEstaEnLaBase() {
        PuntoDeControl fantasma = new PuntoDeControl("P9", "Sur", 1);
        assertThrows(PersistenciaException.class, () -> dao.guardar(new Asignacion(medico, fantasma)));
    }
}
