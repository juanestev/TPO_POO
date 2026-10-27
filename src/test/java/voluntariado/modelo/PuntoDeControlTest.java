package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PuntoDeControlTest {

    @Test
    void guardaZonasCercanasSinDuplicarlasNiExponerLaLista() {
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 3);
        punto.agregarZonaCercana(" Norte ");
        punto.agregarZonaCercana("norte");

        assertEquals(1, punto.getZonasCercanas().size());
        assertEquals("Norte", punto.getZonasCercanas().get(0));
        assertThrows(UnsupportedOperationException.class, () -> punto.getZonasCercanas().clear());
        assertThrows(DatosInvalidosException.class, () -> punto.setZona("Norte"));
        assertEquals("Centro", punto.getZona());
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(DatosInvalidosException.class, () -> new PuntoDeControl(" ", "Centro", 3));
        assertThrows(DatosInvalidosException.class, () -> new PuntoDeControl("P1", "Centro", 0));
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 3);
        assertThrows(DatosInvalidosException.class, () -> punto.agregarZonaCercana("centro"));
        assertThrows(DatosInvalidosException.class, () -> punto.setDotacionRequerida(-1));
        assertEquals(3, punto.getDotacionRequerida());
    }
}
