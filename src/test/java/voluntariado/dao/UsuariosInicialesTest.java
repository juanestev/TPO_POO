package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.ServicioAutenticacion;
import voluntariado.seguridad.ServicioClaves;
import voluntariado.seguridad.UsuariosIniciales;

class UsuariosInicialesTest extends BaseTemporal {

    @Test
    void creaUnUsuarioPorRolSoloUnaVezYPermiteEntrar() {
        ServicioAutenticacion servicio = new ServicioAutenticacion(new UsuarioDAOSQLite(conexion), new ServicioClaves());

        assertTrue(UsuariosIniciales.cargarSiEstaVacia(servicio));
        assertFalse(UsuariosIniciales.cargarSiEstaVacia(servicio));
        assertEquals(3, servicio.listarUsuarios().size());
        assertEquals(Rol.ADMINISTRADOR, servicio.iniciarSesion("admin", "Admin2026").getRol());
    }
}
