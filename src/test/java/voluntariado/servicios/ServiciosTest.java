package voluntariado.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ServiciosTest {
    @Test
    void adaptadorConsolaRegistraMensajes() {
        NotificadorMailConsola consola = new NotificadorMailConsola();
        MensajeMail mensaje = new MensajeMail("ana@ejemplo.com", "Turno", "Presentarse a las 8");
        consola.enviar(mensaje);
        assertEquals(1, consola.getEnviados().size());
        assertEquals(mensaje, consola.getEnviados().get(0));
        assertThrows(UnsupportedOperationException.class, () -> consola.getEnviados().clear());
    }

    @Test
    void generaYLeeQrYValidaIngresoManual() throws Exception {
        byte[] png = CodigoQr.generarImagen("DNI:12345678", 180);
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals("DNI:12345678", CodigoQr.leerImagen(imagen));
        assertEquals("DNI:12345678", CodigoQr.validarCodigoManual(" DNI:12345678 "));
        assertThrows(IllegalArgumentException.class, () -> CodigoQr.validarCodigoManual("linea\notra"));
    }

    @Test
    void validaConfiguracionYMensajes() {
        assertThrows(IllegalArgumentException.class, () -> new MensajeMail("", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> new NotificadorMailSmtp("smtp", 587, "", "clave"));
        assertThrows(IllegalArgumentException.class, () -> CodigoQr.generarImagen("x", 20));
    }
}
