package voluntariado.vista;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import voluntariado.Sistema;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.Habilidad;
import voluntariado.servicios.NotificadorMailConsola;

// Las pantallas son JPanel: se pueden armar sin monitor, asi se prueban en cualquier PC.
class PanelesTest {

    @TempDir
    Path carpeta;
    private Sistema sistema;

    @BeforeEach
    void preparar() {
        sistema = new Sistema(carpeta.resolve("prueba.db").toString(), new NotificadorMailConsola());
        sistema.getSesion().iniciar(sistema.getAutenticacion().iniciarSesion("admin", "Admin2026"));
    }

    @Test
    void todasLasPantallasSeArmanConLosDatosDePrueba() {
        assertNotNull(new PanelVoluntarios(sistema.getVoluntarios()));
        assertNotNull(new PanelPuntos(sistema.getPuntos()));
        assertNotNull(new PanelUsuarios(sistema.getUsuarios()));
        assertNotNull(new PanelAsignacion(sistema.getAsignacion(), sistema.getPuntos()));
        assertNotNull(new PanelAsistencia(sistema.getAsistencia()));
        assertEquals(30, sistema.getVoluntarios().listar().size());
        assertEquals(5, sistema.getPuntos().listar().size());
        assertEquals(3, sistema.getUsuarios().listar().size());
    }

    @Test
    void elPanelDeCoberturaSeActualizaSoloAlRegistrarAsistencia() throws Exception {
        PanelCobertura panel = new PanelCobertura(sistema::crearControladorCobertura);
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(EstadoCobertura.DEFICIT, panel.estadoDe("P2"));

        sistema.getSesion().iniciar(sistema.getAutenticacion().iniciarSesion("coordinador", "Coord2026"));
        Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));
        List<Asignacion> sugerencias = sistema.getAsignacion().sugerir("P2", Habilidad.CONTROL_ACCESOS, turno);
        sistema.getAsignacion().confirmar(sugerencias, turno, "Catedral");
        for (Asignacion a : sugerencias) {
            sistema.getAsistencia().registrarEntradaManual(a.getCodigoQr(), LocalDateTime.now());
        }
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(EstadoCobertura.CUBIERTO, panel.estadoDe("P2"));
        panel.close();
    }
}
