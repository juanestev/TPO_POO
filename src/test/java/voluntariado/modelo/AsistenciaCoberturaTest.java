package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsistenciaCoberturaTest {

    private final LocalDateTime inicio = LocalDateTime.of(2026, 10, 27, 8, 0);

    private RegistroAsistencia registro(String dni, PuntoDeControl punto) {
        Voluntario voluntario = new VoluntarioMedico(dni, "Ana", "Perez", "1123456789", "ana@ejemplo.com");
        Asignacion asignacion = new Asignacion(voluntario, punto);
        asignacion.confirmar();
        return new RegistroAsistencia(asignacion);
    }

    @Test
    void entradaYSalidaExigenCodigoYOrdenCorrectos() {
        RegistroAsistencia registro = registro("12345678", new PuntoDeControl("P1", "Centro", 1));
        String codigo = registro.getAsignacion().getCodigoQr();
        assertThrows(DatosInvalidosException.class, () -> registro.registrarSalida(codigo, inicio));
        assertThrows(DatosInvalidosException.class, () -> registro.registrarEntrada("otro", inicio));
        registro.registrarEntrada(codigo, inicio);
        assertTrue(registro.estaPresente());
        assertThrows(DatosInvalidosException.class, () -> registro.registrarEntrada(codigo, inicio));
        assertThrows(DatosInvalidosException.class, () -> registro.registrarSalida(codigo, inicio.minusMinutes(1)));
        registro.registrarSalida(codigo, inicio.plusHours(4));
        assertFalse(registro.estaPresente());
    }

    @Test
    void coberturaUsaPresentesDelPuntoSinContarDuplicados() {
        PuntoDeControl centro = new PuntoDeControl("P1", "Centro", 1);
        PuntoDeControl norte = new PuntoDeControl("P2", "Norte", 1);
        RegistroAsistencia ana = registro("12345678", centro);
        RegistroAsistencia luis = registro("23456789", centro);
        RegistroAsistencia otroPunto = registro("34567890", norte);
        CalculadorCobertura calculador = new CalculadorCobertura();

        assertEquals(EstadoCobertura.DEFICIT, calculador.calcular(centro, List.of(ana, luis)));
        ana.registrarEntrada(ana.getAsignacion().getCodigoQr(), inicio);
        otroPunto.registrarEntrada(otroPunto.getAsignacion().getCodigoQr(), inicio);
        assertEquals(EstadoCobertura.CUBIERTO, calculador.calcular(centro, List.of(ana, ana, otroPunto)));
        luis.registrarEntrada(luis.getAsignacion().getCodigoQr(), inicio);
        assertEquals(EstadoCobertura.SOBRANTE, calculador.calcular(centro, List.of(ana, luis)));
        assertEquals(2, calculador.contarPresentes(centro, List.of(ana, luis)));
    }
}
