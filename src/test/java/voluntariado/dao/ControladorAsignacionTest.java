package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorAsignacion;
import voluntariado.controlador.ControladorAsignacion.Resultado;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.CalculadorCobertura;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.EstrategiaPorHabilidadYDisponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.seguridad.PermisoDenegadoException;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.Sesion;
import voluntariado.seguridad.Usuario;
import voluntariado.servicios.NotificadorMailConsola;
import voluntariado.servicios.ServicioNotificaciones;

class ControladorAsignacionTest extends BaseTemporal {

    private final Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));
    private RepositorioEnMemoria repositorio;
    private ControladorAsistencia asistencia;
    private NotificadorMailConsola correo;
    private Sesion sesion;
    private ControladorAsignacion controlador;
    private PuntoDeControl origen;
    private PuntoDeControl destino;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
        PuntoDeControlDAOSQLite puntos = new PuntoDeControlDAOSQLite(conexion);
        VoluntarioDAOSQLite voluntarios = new VoluntarioDAOSQLite(conexion);
        origen = new PuntoDeControl("P1", "Plaza", 1);
        origen.agregarZonaCercana("Catedral");
        destino = new PuntoDeControl("P2", "Catedral", 1);
        destino.agregarZonaCercana("Plaza");
        for (PuntoDeControl p : List.of(origen, destino)) {
            puntos.guardar(p);
            repositorio.agregarPunto(p);
        }
        for (int i = 0; i < 3; i++) {
            VoluntarioMedico m = new VoluntarioMedico("1234567" + i, "Nombre" + i, "Apellido" + i, "112345678" + i,
                    "v" + i + "@ejemplo.com");
            m.agregarHabilidad(Habilidad.RCP);
            m.agregarDisponibilidad(turno);
            voluntarios.guardar(m);
            repositorio.agregarVoluntario(m);
        }
        asistencia = new ControladorAsistencia(repositorio, new AsistenciaDAOSQLite(conexion));
        correo = new NotificadorMailConsola();
        sesion = new Sesion();
        sesion.iniciar(new Usuario("coord", "h", "s", Rol.COORDINADOR));
        controlador = new ControladorAsignacion(repositorio, new AsignacionDAOSQLite(conexion), asistencia,
                new ServicioNotificaciones(correo), new EstrategiaPorHabilidadYDisponibilidad(), sesion);
    }

    @Test
    void sugerirYConfirmarGuardaYAvisa() {
        List<Asignacion> sugerencias = controlador.sugerir("P1", Habilidad.RCP, turno);
        assertEquals(1, sugerencias.size());

        Resultado resultado = controlador.confirmar(sugerencias, turno, "Plaza central");

        assertEquals(1, resultado.confirmadas());
        assertTrue(resultado.errores().isEmpty());
        assertEquals(1, correo.getEnviados().size());
        assertEquals(1, repositorio.getAsignacionesPorPunto("P1").size());
        assertEquals(1, new AsignacionDAOSQLite(conexion).listarTodas(repositorio).size());
    }

    @Test
    void sinPermisoDeCoordinadorNoPuedeConfirmar() {
        List<Asignacion> sugerencias = controlador.sugerir("P1", Habilidad.RCP, turno);
        sesion.iniciar(new Usuario("oper", "h", "s", Rol.OPERADOR));
        assertThrows(PermisoDenegadoException.class, () -> controlador.confirmar(sugerencias, turno, "Plaza"));
        assertTrue(repositorio.getAsignacionesPorPunto("P1").isEmpty());
    }

    @Test
    void reasignarMueveAlVoluntarioConAvisoDeCambioYActualizaLaCobertura() {
        // Dos voluntarios en P1 (dotacion 1) -> sobrante; P2 queda sin nadie -> deficit.
        origen.setDotacionRequerida(2);
        Asignacion a = new Asignacion(repositorio.buscarVoluntario("12345670"), origen);
        Asignacion b = new Asignacion(repositorio.buscarVoluntario("12345671"), origen);
        controlador.confirmar(List.of(a, b), turno, "Plaza");
        origen.setDotacionRequerida(1);
        asistencia.registrarEntradaManual(a.getCodigoQr(), LocalDateTime.of(2026, 10, 31, 8, 0));
        asistencia.registrarEntradaManual(b.getCodigoQr(), LocalDateTime.of(2026, 10, 31, 8, 0));
        CalculadorCobertura calculador = new CalculadorCobertura();
        assertEquals(EstadoCobertura.SOBRANTE, calculador.calcular(origen, asistencia.getRegistros()));
        assertEquals(EstadoCobertura.DEFICIT, calculador.calcular(destino, asistencia.getRegistros()));

        List<Asignacion> sugerencias = controlador.sugerirReasignacion("P2", Habilidad.RCP, turno);
        assertEquals(1, sugerencias.size());
        Resultado resultado = controlador.confirmar(sugerencias, turno, "Catedral");

        assertEquals(1, resultado.reasignadas());
        assertTrue(resultado.errores().isEmpty());
        assertEquals(1, repositorio.getAsignacionesPorPunto("P1").size());
        assertEquals(1, repositorio.getAsignacionesPorPunto("P2").size());
        assertEquals(EstadoCobertura.CUBIERTO, calculador.calcular(origen, asistencia.getRegistros()));
        assertEquals(3, correo.getEnviados().size()); // 2 avisos de asignacion + 1 de cambio
        assertEquals("Cambio de asignacion", correo.getEnviados().get(2).asunto());
        // Lo que esta en la base coincide con la memoria: una sola asignacion confirmada por voluntario.
        assertEquals(2, new AsignacionDAOSQLite(conexion).listarTodas(repositorio).size());
    }
}
