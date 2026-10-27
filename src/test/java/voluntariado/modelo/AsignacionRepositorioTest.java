package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AsignacionRepositorioTest {

    private Voluntario voluntario(String dni) {
        return new VoluntarioMedico(dni, "Ana", "Perez", "1123456789", "ana@ejemplo.com");
    }

    @Test
    void asignacionPasaDeSugeridaAConfirmada() {
        Asignacion asignacion = new Asignacion(voluntario("12345678"), new PuntoDeControl("P1", "Centro", 2));
        assertEquals(EstadoAsignacion.SUGERIDA, asignacion.getEstado());
        asignacion.confirmar();
        assertEquals(EstadoAsignacion.CONFIRMADA, asignacion.getEstado());
        assertThrows(DatosInvalidosException.class, () -> new Asignacion(null, asignacion.getPunto()));
    }

    @Test
    void guardaVoluntariosPorDniYFiltraPorHabilidad() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        Voluntario medico = voluntario("12345678");
        medico.agregarHabilidad(Habilidad.RCP);
        repositorio.agregarVoluntario(medico);
        repositorio.agregarVoluntario(voluntario("23456789"));

        assertSame(medico, repositorio.buscarVoluntario("12345678"));
        assertEquals(1, repositorio.buscarVoluntariosPorHabilidad(Habilidad.RCP).size());
        assertThrows(DatosInvalidosException.class, () -> repositorio.agregarVoluntario(voluntario("12345678")));
        assertThrows(DatosInvalidosException.class, () -> repositorio.buscarVoluntario("99999999"));
    }

    @Test
    void guardaSoloAsignacionesConfirmadasSinDuplicarVoluntarios() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        Voluntario medico = voluntario("12345678");
        PuntoDeControl centro = new PuntoDeControl("P1", "Centro", 2);
        PuntoDeControl norte = new PuntoDeControl("P2", "Norte", 2);
        repositorio.agregarVoluntario(medico);
        repositorio.agregarPunto(centro);
        repositorio.agregarPunto(norte);
        Asignacion primera = new Asignacion(medico, centro);

        assertThrows(DatosInvalidosException.class, () -> repositorio.registrarAsignacion(primera));
        primera.confirmar();
        repositorio.registrarAsignacion(primera);
        assertSame(primera, repositorio.getAsignacionesPorPunto("P1").get(0));
        assertThrows(UnsupportedOperationException.class, () -> repositorio.getAsignacionesPorPunto("P1").clear());

        Asignacion duplicada = new Asignacion(medico, norte);
        duplicada.confirmar();
        assertThrows(DatosInvalidosException.class, () -> repositorio.registrarAsignacion(duplicada));
        assertEquals(0, repositorio.getAsignacionesPorPunto("P2").size());
    }

    @Test
    void requierePuntoYVoluntarioRegistrados() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        Voluntario medico = voluntario("12345678");
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 2);
        Asignacion asignacion = new Asignacion(medico, punto);
        asignacion.confirmar();

        assertThrows(DatosInvalidosException.class, () -> repositorio.registrarAsignacion(asignacion));
        repositorio.agregarVoluntario(medico);
        repositorio.agregarPunto(punto);
        assertSame(punto, repositorio.buscarPunto("P1"));
        assertThrows(DatosInvalidosException.class, () -> repositorio.buscarPunto("P2"));
        assertThrows(DatosInvalidosException.class, () -> repositorio.agregarPunto(new PuntoDeControl("P1", "Otra", 2)));
        repositorio.registrarAsignacion(asignacion);
        assertEquals(1, repositorio.getAsignacionesPorPunto("P1").size());
    }
}
