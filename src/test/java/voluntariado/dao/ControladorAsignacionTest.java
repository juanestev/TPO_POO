package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.Statement;
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
import voluntariado.modelo.AsignacionHistorica;
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
import voluntariado.servicios.MensajeMail;
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
    void falloAlGuardarAvisoInformaAsignacionConfirmada() {
        ServicioNotificaciones avisosConFallo = new ServicioNotificaciones(
                mensaje -> { throw new IllegalStateException("Sin conexion"); },
                new ColaMailsDAO() {
                    @Override
                    public List<MensajeMail> listar() {
                        return List.of();
                    }

                    @Override
                    public void reemplazarTodos(List<MensajeMail> mensajes) {
                        throw new PersistenciaException("No se pudo guardar la cola");
                    }
                });
        ControladorAsignacion conFallo = new ControladorAsignacion(repositorio,
                new AsignacionDAOSQLite(conexion), asistencia, avisosConFallo,
                new EstrategiaPorHabilidadYDisponibilidad(), sesion);
        List<Asignacion> sugerencias = conFallo.sugerir("P1", Habilidad.RCP, turno);

        Resultado resultado = conFallo.confirmar(sugerencias, turno, "Plaza central");

        assertEquals(1, resultado.confirmadas());
        assertTrue(resultado.errores().isEmpty());
        assertEquals(1, resultado.erroresAviso().size());
        assertEquals(1, resultado.avisosPendientes());
        assertEquals(1, repositorio.getAsignacionesPorPunto("P1").size());
        assertEquals(1, new AsignacionDAOSQLite(conexion).listarTodas(repositorio).size());
    }

    @Test
    void sinPermisoDeCoordinadorNoPuedeConfirmar() {
        List<Asignacion> sugerencias = controlador.sugerir("P1", Habilidad.RCP, turno);
        sesion.iniciar(new Usuario("oper", "h", "s", Rol.OPERADOR));
        assertThrows(PermisoDenegadoException.class, () -> controlador.confirmar(sugerencias, turno, "Plaza"));
        assertThrows(PermisoDenegadoException.class, () -> controlador.listarHistorial());
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
        AsignacionDAOSQLite dao = new AsignacionDAOSQLite(conexion);
        assertEquals(2, dao.listarTodas(repositorio).size());
        Asignacion anterior = sugerencias.get(0).getVoluntario().getDni().equals(a.getVoluntario().getDni()) ? a : b;
        AsignacionHistorica historica = dao.listarHistorial().get(0);
        assertEquals(1, dao.listarHistorial().size());
        assertEquals(anterior.getCodigoQr(), historica.codigoQr());
        assertEquals(anterior.getVoluntario().getDni(), historica.dni());
        assertEquals("P1", historica.codigoPunto());
        assertEquals(LocalDateTime.of(2026, 10, 31, 8, 0), historica.entrada());
        assertNull(historica.salida());
        assertNotNull(historica.fechaCambio());

        // El historial sobrevive al reinicio; solo las asignaciones actuales vuelven a memoria.
        conexion.inicializarEsquema();
        RepositorioEnMemoria recargado = new CargaInicial(new PuntoDeControlDAOSQLite(conexion),
                new VoluntarioDAOSQLite(conexion), dao).cargar();
        assertEquals(1, dao.listarHistorial().size());
        assertEquals(1, recargado.getAsignacionesPorPunto("P1").size());
        assertEquals(1, recargado.getAsignacionesPorPunto("P2").size());
        assertEquals(1, new AsistenciaDAOSQLite(conexion).listarTodas(recargado).size());
    }

    @Test
    void falloAlGuardarReasignacionConservaPuestoYAsistencia() throws Exception {
        origen.setDotacionRequerida(2);
        Asignacion a = new Asignacion(repositorio.buscarVoluntario("12345670"), origen);
        Asignacion b = new Asignacion(repositorio.buscarVoluntario("12345671"), origen);
        controlador.confirmar(List.of(a, b), turno, "Plaza");
        origen.setDotacionRequerida(1);
        LocalDateTime entrada = LocalDateTime.of(2026, 10, 31, 8, 0);
        asistencia.registrarEntradaManual(a.getCodigoQr(), entrada);
        asistencia.registrarEntradaManual(b.getCodigoQr(), entrada);

        // Falla el INSERT nuevo despues del DELETE anterior: toda la transaccion debe volver atras.
        try (Connection c = conexion.abrir(); Statement st = c.createStatement()) {
            st.execute("CREATE TRIGGER fallo_demo BEFORE INSERT ON asignacion "
                    + "WHEN NEW.codigo_punto = 'P2' BEGIN SELECT RAISE(ABORT, 'fallo de prueba'); END");
        }
        List<Asignacion> sugerencias = controlador.sugerirReasignacion("P2", Habilidad.RCP, turno);
        Resultado resultado = controlador.confirmar(sugerencias, turno, "Catedral");

        assertEquals(0, resultado.confirmadas());
        assertEquals(1, resultado.errores().size());
        assertEquals(2, repositorio.getAsignacionesPorPunto("P1").size());
        assertTrue(repositorio.getAsignacionesPorPunto("P2").isEmpty());
        assertEquals(2, new AsignacionDAOSQLite(conexion).listarTodas(repositorio).size());
        assertTrue(new AsignacionDAOSQLite(conexion).listarHistorial().isEmpty());
        assertEquals(2, asistencia.getRegistros().size());
        assertEquals(2, new AsistenciaDAOSQLite(conexion).listarTodas(repositorio).size());
        assertEquals(EstadoCobertura.SOBRANTE, new CalculadorCobertura().calcular(origen, asistencia.getRegistros()));
    }
}
