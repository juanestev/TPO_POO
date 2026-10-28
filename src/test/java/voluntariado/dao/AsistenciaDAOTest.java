package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;

class AsistenciaDAOTest extends BaseTemporal {

    private Asignacion asignacion;
    private RepositorioEnMemoria repositorio;
    private AsistenciaDAO asistenciaDAO;

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
        asistenciaDAO = new AsistenciaDAOSQLite(conexion);
    }

    @Test
    void conservaEntradaYSalidaAlVolverACargar() {
        RegistroAsistencia registro = new RegistroAsistencia(asignacion);
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 27, 8, 0);
        LocalDateTime salida = LocalDateTime.of(2026, 10, 27, 12, 0);
        registro.registrarEntrada(asignacion.getCodigoQr(), entrada);
        asistenciaDAO.guardar(registro);

        RegistroAsistencia cargado = asistenciaDAO.listarTodas(repositorio).get(0);
        assertSame(asignacion, cargado.getAsignacion());
        assertEquals(entrada, cargado.getEntrada());
        assertTrue(cargado.estaPresente());

        registro.registrarSalida(asignacion.getCodigoQr(), salida);
        asistenciaDAO.guardar(registro);
        cargado = asistenciaDAO.listarTodas(repositorio).get(0);
        assertEquals(salida, cargado.getSalida());
        assertFalse(cargado.estaPresente());
    }

    @Test
    void rechazaEntradaParaAsignacionNoGuardada() {
        Asignacion ajena = new Asignacion(asignacion.getVoluntario(), asignacion.getPunto());
        ajena.confirmar();
        RegistroAsistencia registro = new RegistroAsistencia(ajena);
        registro.registrarEntrada(ajena.getCodigoQr(), LocalDateTime.of(2026, 10, 27, 8, 0));

        assertThrows(PersistenciaException.class, () -> asistenciaDAO.guardar(registro));
        assertTrue(asistenciaDAO.listarTodas(repositorio).isEmpty());
    }
}
