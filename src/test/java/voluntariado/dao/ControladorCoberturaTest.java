package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.controlador.ControladorCobertura;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.servicios.ServicioAsignaciones;

class ControladorCoberturaTest extends BaseTemporal {

    @Test
    void actualizaLaVistaTrasConfirmacionEntradaYSalida() {
        VoluntarioMedico voluntaria = new VoluntarioMedico(
                "12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        new VoluntarioDAOSQLite(conexion).guardar(voluntaria);
        new PuntoDeControlDAOSQLite(conexion).guardar(punto);
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        repositorio.agregarVoluntario(voluntaria);
        repositorio.agregarPunto(punto);
        AsignacionDAOSQLite asignaciones = new AsignacionDAOSQLite(conexion);
        ControladorAsistencia asistencia = new ControladorAsistencia(repositorio,
                new AsistenciaDAOSQLite(conexion));
        List<String> cambios = new ArrayList<>();
        Asignacion sugerencia = new Asignacion(voluntaria, punto);

        try (ControladorCobertura controlador = new ControladorCobertura(repositorio, asistencia,
                (p, presentes, estado) -> cambios.add(p.getCodigo() + ":" + presentes + ":" + estado))) {
            controlador.alCambiar(punto);
            new ServicioAsignaciones(repositorio, asignaciones).confirmar(sugerencia);
            asistencia.registrarEntradaManual(sugerencia.getCodigoQr(), LocalDateTime.of(2026, 10, 27, 8, 0));
            asistencia.registrarSalidaManual(sugerencia.getCodigoQr(), LocalDateTime.of(2026, 10, 27, 12, 0));
        }

        assertEquals(List.of("P1:0:DEFICIT", "P1:0:DEFICIT", "P1:1:CUBIERTO", "P1:0:DEFICIT"), cambios);
        assertEquals(EstadoCobertura.DEFICIT,
                new voluntariado.modelo.CalculadorCobertura().calcular(punto, asistencia.getRegistros()));
    }
}
