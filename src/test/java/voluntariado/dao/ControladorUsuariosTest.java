package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.controlador.ControladorUsuarios;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.seguridad.PermisoDenegadoException;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.ServicioAutenticacion;
import voluntariado.seguridad.ServicioClaves;
import voluntariado.seguridad.Sesion;

class ControladorUsuariosTest extends BaseTemporal {

    private UsuarioDAOSQLite dao;
    private ServicioAutenticacion autenticacion;
    private Sesion sesion;
    private ControladorUsuarios controlador;

    @BeforeEach
    void preparar() {
        dao = new UsuarioDAOSQLite(conexion);
        autenticacion = new ServicioAutenticacion(dao, new ServicioClaves());
        sesion = new Sesion();
        controlador = new ControladorUsuarios(autenticacion, dao, sesion);
        sesion.iniciar(autenticacion.crearUsuario("admin", "Admin1234", Rol.ADMINISTRADOR));
    }

    @Test
    void soloElAdministradorGestionaUsuarios() {
        sesion.iniciar(autenticacion.crearUsuario("oper", "Oper12345", Rol.OPERADOR));
        assertThrows(PermisoDenegadoException.class, () -> controlador.listar());
        assertThrows(PermisoDenegadoException.class, () -> controlador.crear("otro", "Clave1234", Rol.OPERADOR));
    }

    @Test
    void crearCambiarRolDesactivarYRestablecer() {
        controlador.crear("marta", "Clave1234", Rol.OPERADOR);
        controlador.cambiarRol("marta", Rol.COORDINADOR);
        assertEquals(Rol.COORDINADOR, dao.buscarPorNombre("marta").getRol());

        controlador.cambiarEstado("marta", false);
        assertFalse(dao.buscarPorNombre("marta").isActivo());

        controlador.cambiarEstado("marta", true);
        controlador.restablecerClave("marta", "Nueva5678");
        assertEquals("marta", autenticacion.iniciarSesion("marta", "Nueva5678").getNombreUsuario());
    }

    @Test
    void nadieSeQuitaElAccesoASiMismo() {
        assertThrows(DatosInvalidosException.class, () -> controlador.eliminar("admin"));
        assertThrows(DatosInvalidosException.class, () -> controlador.cambiarEstado("admin", false));
        assertThrows(DatosInvalidosException.class, () -> controlador.cambiarRol("admin", Rol.OPERADOR));
    }

    @Test
    void nuncaSeQuedaSinAdministradorActivo() {
        controlador.crear("admin2", "Admin5678", Rol.ADMINISTRADOR);
        sesion.iniciar(dao.buscarPorNombre("admin2"));
        controlador.cambiarEstado("admin", false);

        // Ahora admin2 es el unico activo: no se lo puede tocar desde otro administrador inactivo, y a si mismo no.
        assertThrows(DatosInvalidosException.class, () -> controlador.eliminar("admin2"));
        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void puedeEliminarAOtroUsuarioComun() {
        controlador.crear("marta", "Clave1234", Rol.OPERADOR);
        controlador.eliminar("marta");
        assertEquals(1, dao.listarTodos().size());
    }
}
