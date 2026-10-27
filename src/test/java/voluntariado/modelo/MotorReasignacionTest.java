package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MotorReasignacionTest {

    private final Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY,
            LocalTime.of(9, 0), LocalTime.of(12, 0));

    private RegistroAsistencia presente(RepositorioEnMemoria repositorio, PuntoDeControl punto, String dni) {
        Voluntario voluntario = new VoluntarioMedico(dni, "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        voluntario.agregarHabilidad(Habilidad.RCP);
        voluntario.agregarDisponibilidad(turno);
        repositorio.agregarVoluntario(voluntario);
        Asignacion asignacion = new Asignacion(voluntario, punto);
        asignacion.confirmar();
        repositorio.registrarAsignacion(asignacion);
        RegistroAsistencia registro = new RegistroAsistencia(asignacion);
        registro.registrarEntrada(asignacion.getCodigoQr(), LocalDateTime.of(2026, 10, 26, 9, 0));
        return registro;
    }

    @Test
    void proponeSoloElSobrantePresenteDeZonaCercana() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl destino = new PuntoDeControl("P1", "Centro", 2);
        destino.agregarZonaCercana("Norte");
        PuntoDeControl norte = new PuntoDeControl("P2", "Norte", 1);
        PuntoDeControl sur = new PuntoDeControl("P3", "Sur", 1);
        repositorio.agregarPunto(destino);
        repositorio.agregarPunto(norte);
        repositorio.agregarPunto(sur);
        List<RegistroAsistencia> registros = new ArrayList<>();
        registros.add(presente(repositorio, norte, "12345678"));
        registros.add(presente(repositorio, norte, "23456789"));
        registros.add(presente(repositorio, sur, "34567890"));

        MotorReasignacion motor = new MotorReasignacion(repositorio,
                new EstrategiaPorHabilidadYDisponibilidad());
        List<Asignacion> sugerencias = motor.generarSugerencias(destino, registros, Habilidad.RCP, turno);

        assertEquals(1, sugerencias.size());
        assertEquals("12345678", sugerencias.get(0).getVoluntario().getDni());
        assertEquals(destino, sugerencias.get(0).getPunto());
        assertEquals(EstadoAsignacion.SUGERIDA, sugerencias.get(0).getEstado());
        assertEquals(0, repositorio.getAsignacionesPorPunto("P1").size());
    }

    @Test
    void noProponeSiElOrigenNoTieneSobrante() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        PuntoDeControl destino = new PuntoDeControl("P1", "Centro", 1);
        destino.agregarZonaCercana("Norte");
        PuntoDeControl norte = new PuntoDeControl("P2", "Norte", 1);
        repositorio.agregarPunto(destino);
        repositorio.agregarPunto(norte);
        List<RegistroAsistencia> registros = List.of(presente(repositorio, norte, "12345678"));

        assertEquals(0, new MotorReasignacion(repositorio,
                new EstrategiaPorHabilidadYDisponibilidad())
                .generarSugerencias(destino, registros, Habilidad.RCP, turno).size());
    }
}
