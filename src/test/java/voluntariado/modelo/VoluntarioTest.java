package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class VoluntarioTest {

    @Test
    void cadaTipoExponeSuRol() {
        Persona medico = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        Persona seguridad = new VoluntarioSeguridad("23456789", "Luis", "Diaz", "1123456789", "luis@ejemplo.com");
        Persona coordinador = new Coordinador("34567890", "Eva", "Lopez", "1123456789", "eva@ejemplo.com");

        assertEquals("MEDICO", medico.getRol());
        assertEquals("SEGURIDAD", seguridad.getRol());
        assertEquals("COORDINADOR", coordinador.getRol());
    }

    @Test
    void registraHabilidadesYDisponibilidadSinExponerLasColecciones() {
        Voluntario voluntario = new VoluntarioMedico("12345678", "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        Disponibilidad turno = new Disponibilidad(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));

        voluntario.agregarHabilidad(Habilidad.RCP);
        voluntario.agregarDisponibilidad(turno);
        voluntario.agregarDisponibilidad(turno);

        assertTrue(voluntario.getHabilidades().contains(Habilidad.RCP));
        assertEquals(1, voluntario.getDisponibilidades().size());
        assertThrows(UnsupportedOperationException.class, () -> voluntario.getHabilidades().clear());
        assertThrows(UnsupportedOperationException.class, () -> voluntario.getDisponibilidades().clear());

        voluntario.quitarHabilidad(Habilidad.RCP);
        assertFalse(voluntario.getHabilidades().contains(Habilidad.RCP));
    }

    @Test
    void rechazaHorariosInvalidos() {
        assertThrows(DatosInvalidosException.class,
                () -> new Disponibilidad(DayOfWeek.MONDAY, LocalTime.NOON, LocalTime.NOON));
        assertThrows(DatosInvalidosException.class,
                () -> new Disponibilidad(null, LocalTime.of(8, 0), LocalTime.of(12, 0)));
    }
}
