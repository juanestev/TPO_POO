package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.PuntoDeControl;

class PuntoDeControlDAOTest extends BaseTemporal {

    @Test
    void guardaYLeeElPuntoConSusZonasCercanas() {
        PuntoDeControlDAO dao = new PuntoDeControlDAOSQLite(conexion);
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 3);
        punto.agregarZonaCercana("Norte");
        punto.agregarZonaCercana("Sur");
        dao.guardar(punto);

        PuntoDeControl leido = dao.buscarPorCodigo("P1");
        assertEquals("Centro", leido.getZona());
        assertEquals(3, leido.getDotacionRequerida());
        assertEquals(List.of("Norte", "Sur"), leido.getZonasCercanas());
    }

    @Test
    void guardarDosVecesActualizaElPunto() {
        PuntoDeControlDAO dao = new PuntoDeControlDAOSQLite(conexion);
        PuntoDeControl punto = new PuntoDeControl("P1", "Centro", 3);
        punto.agregarZonaCercana("Norte");
        dao.guardar(punto);

        punto.setDotacionRequerida(5);
        punto.quitarZonaCercana("Norte");
        punto.agregarZonaCercana("Oeste");
        dao.guardar(punto);

        PuntoDeControl leido = dao.buscarPorCodigo("P1");
        assertEquals(5, leido.getDotacionRequerida());
        assertEquals(List.of("Oeste"), leido.getZonasCercanas());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void eliminarYBuscarInexistente() {
        PuntoDeControlDAO dao = new PuntoDeControlDAOSQLite(conexion);
        dao.guardar(new PuntoDeControl("P1", "Centro", 3));
        dao.eliminar("P1");

        assertTrue(dao.listarTodos().isEmpty());
        assertThrows(DatosInvalidosException.class, () -> dao.buscarPorCodigo("P1"));
        assertThrows(DatosInvalidosException.class, () -> dao.buscarPorCodigo(" "));
    }
}
