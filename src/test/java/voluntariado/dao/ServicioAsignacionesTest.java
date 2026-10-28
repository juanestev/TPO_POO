package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.servicios.ServicioAsignaciones;

class ServicioAsignacionesTest extends BaseTemporal {

    private RepositorioEnMemoria repositorio;
    private AsignacionDAO dao;
    private ServicioAsignaciones servicio;
    private VoluntarioMedico voluntaria;
    private PuntoDeControl centro;
    private PuntoDeControl norte;

    @BeforeEach
    void preparar() {
        voluntaria = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        centro = new PuntoDeControl("P1", "Centro", 2);
        norte = new PuntoDeControl("P2", "Norte", 2);
        new VoluntarioDAOSQLite(conexion).guardar(voluntaria);
        PuntoDeControlDAO puntos = new PuntoDeControlDAOSQLite(conexion);
        puntos.guardar(centro);
        puntos.guardar(norte);
        repositorio = new RepositorioEnMemoria();
        repositorio.agregarVoluntario(voluntaria);
        repositorio.agregarPunto(centro);
        repositorio.agregarPunto(norte);
        dao = new AsignacionDAOSQLite(conexion);
        servicio = new ServicioAsignaciones(repositorio, dao);
    }

    @Test
    void confirmaLaSugerenciaEnMemoriaYEnLaBaseConElMismoQr() {
        Asignacion sugerencia = new Asignacion(voluntaria, centro);
        dao.guardar(sugerencia);

        servicio.confirmar(sugerencia);

        assertEquals(EstadoAsignacion.CONFIRMADA, sugerencia.getEstado());
        assertSame(sugerencia, repositorio.getAsignacionesPorPunto("P1").get(0));
        Asignacion guardada = dao.listarTodas(repositorio).get(0);
        assertEquals(EstadoAsignacion.CONFIRMADA, guardada.getEstado());
        assertEquals(sugerencia.getCodigoQr(), guardada.getCodigoQr());
    }

    @Test
    void falloDeLaBaseConservaLaSugerenciaSinRegistrarlaEnMemoria() {
        Asignacion existente = new Asignacion(voluntaria, norte);
        existente.confirmar();
        dao.guardar(existente);
        Asignacion sugerencia = new Asignacion(voluntaria, centro);

        assertThrows(PersistenciaException.class, () -> servicio.confirmar(sugerencia));

        assertEquals(EstadoAsignacion.SUGERIDA, sugerencia.getEstado());
        assertEquals(0, repositorio.getAsignacionesPorPunto("P1").size());
        assertEquals(1, dao.listarTodas(repositorio).size());
    }

    @Test
    void rechazaUnaSegundaConfirmacionAntesDeEscribirEnLaBase() {
        Asignacion primera = new Asignacion(voluntaria, centro);
        servicio.confirmar(primera);
        Asignacion segunda = new Asignacion(voluntaria, norte);

        assertThrows(DatosInvalidosException.class, () -> servicio.confirmar(segunda));
        assertEquals(EstadoAsignacion.SUGERIDA, segunda.getEstado());
        assertEquals(1, dao.listarTodas(repositorio).size());
    }
}
