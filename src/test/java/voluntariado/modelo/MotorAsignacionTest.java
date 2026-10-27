package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorAsignacionTest {

    private final Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY,
            LocalTime.of(9, 0), LocalTime.of(12, 0));

    private Voluntario medico(String dni, boolean apto) {
        Voluntario voluntario = new VoluntarioMedico(dni, "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        voluntario.agregarHabilidad(Habilidad.RCP);
        if (apto) {
            voluntario.agregarDisponibilidad(new Disponibilidad(DayOfWeek.MONDAY,
                    LocalTime.of(8, 0), LocalTime.of(13, 0)));
        } else {
            voluntario.agregarDisponibilidad(new Disponibilidad(DayOfWeek.MONDAY,
                    LocalTime.of(10, 0), LocalTime.of(13, 0)));
        }
        return voluntario;
    }

    private MotorAsignacion motor(RepositorioEnMemoria repositorio) {
        return new MotorAsignacion(repositorio, new EstrategiaPorHabilidadYDisponibilidad());
    }

    @Test
    void faltanVoluntariosYDevuelveSoloLosAptos() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 3);
        repositorio.agregarPunto(punto);
        repositorio.agregarVoluntario(medico("12345678", true));
        repositorio.agregarVoluntario(medico("23456789", false));

        List<Asignacion> sugerencias = motor(repositorio).generarSugerencias(punto, Habilidad.RCP, turno);

        assertEquals(1, sugerencias.size());
        assertEquals("12345678", sugerencias.get(0).getVoluntario().getDni());
        assertEquals(EstadoAsignacion.SUGERIDA, sugerencias.get(0).getEstado());
        assertEquals(0, repositorio.getAsignacionesPorPunto("P1").size());
    }

    @Test
    void sobranVoluntariosYLimitaLasSugerenciasALasVacantes() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        repositorio.agregarPunto(punto);
        repositorio.agregarVoluntario(medico("23456789", true));
        repositorio.agregarVoluntario(medico("12345678", true));

        List<Asignacion> sugerencias = motor(repositorio).generarSugerencias(punto, Habilidad.RCP, turno);

        assertEquals(1, sugerencias.size());
        assertEquals("12345678", sugerencias.get(0).getVoluntario().getDni());
    }

    @Test
    void noSugiereSiNadieEstaDisponible() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 2);
        repositorio.agregarPunto(punto);
        repositorio.agregarVoluntario(medico("12345678", false));

        assertEquals(0, motor(repositorio).generarSugerencias(punto, Habilidad.RCP, turno).size());
    }

    @Test
    void noSugiereVoluntariosYaAsignadosAOtroPunto() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl primero = new PuntoDeControl("P1", "Centro", 1);
        PuntoDeControl segundo = new PuntoDeControl("P2", "Norte", 1);
        Voluntario voluntario = medico("12345678", true);
        repositorio.agregarPunto(primero);
        repositorio.agregarPunto(segundo);
        repositorio.agregarVoluntario(voluntario);
        Asignacion confirmada = new Asignacion(voluntario, primero);
        confirmada.confirmar();
        repositorio.registrarAsignacion(confirmada);

        assertEquals(0, motor(repositorio).generarSugerencias(primero, Habilidad.RCP, turno).size());
        assertEquals(0, motor(repositorio).generarSugerencias(segundo, Habilidad.RCP, turno).size());
    }

    @Test
    void exigeUnPuntoRegistrado() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 1);
        assertThrows(DatosInvalidosException.class,
                () -> motor(repositorio).generarSugerencias(punto, Habilidad.RCP, turno));
    }
}
