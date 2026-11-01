package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import voluntariado.servicios.MensajeMail;
import voluntariado.servicios.NotificadorMail;
import voluntariado.servicios.ServicioNotificaciones;

class ColaMailsDAOTest extends BaseTemporal {

    @Test
    void guardaYRecuperaLosMensajesEnOrdenConSuQr() {
        ColaMailsDAOSQLite dao = new ColaMailsDAOSQLite(conexion);
        dao.reemplazarTodos(List.of(
                new MensajeMail("a@ejemplo.com", "Uno", "cuerpo 1", new byte[] {1, 2, 3}),
                new MensajeMail("b@ejemplo.com", "Dos", "cuerpo 2")));

        List<MensajeMail> leidos = dao.listar();
        assertEquals(2, leidos.size());
        assertEquals("Uno", leidos.get(0).asunto());
        assertArrayEquals(new byte[] {1, 2, 3}, leidos.get(0).qrPng());
        assertNull(leidos.get(1).qrPng());

        dao.reemplazarTodos(List.of());
        assertTrue(dao.listar().isEmpty());
    }

    @Test
    void losAvisosSinEnviarSobrevivenAlCierreDeLaApp() {
        ColaMailsDAOSQLite dao = new ColaMailsDAOSQLite(conexion);
        NotificadorMail sinRed = mensaje -> {
            throw new IllegalStateException("sin red");
        };
        ServicioNotificaciones primeraSesion = new ServicioNotificaciones(sinRed, dao);
        primeraSesion.enviarOEncolar(new MensajeMail("a@ejemplo.com", "Aviso", "texto", new byte[] {9}));
        assertEquals(1, primeraSesion.getCantidadPendientes());

        // "Se cierra y se abre la app": un servicio nuevo recupera el aviso y, con red, lo envia.
        java.util.List<MensajeMail> enviados = new java.util.ArrayList<>();
        ServicioNotificaciones segundaSesion = new ServicioNotificaciones(enviados::add, dao);
        assertEquals(1, segundaSesion.getCantidadPendientes());
        segundaSesion.reintentarPendientes();

        assertEquals(1, enviados.size());
        assertEquals(0, segundaSesion.getCantidadPendientes());
        assertTrue(dao.listar().isEmpty());
    }
}
