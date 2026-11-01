package voluntariado.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.Usuario;

class UsuarioDAOTest extends BaseTemporal {

    private UsuarioDAO usuarioDAO;

    @BeforeEach
    void preparar() {
        usuarioDAO = new UsuarioDAOSQLite(conexion);
    }

    @Test
    void guardaYRecuperaTodosLosDatos() {
        Usuario guardado = new Usuario("marta", "hash123", "sal456", Rol.COORDINADOR);
        guardado.setActivo(false);
        usuarioDAO.guardar(guardado);

        Usuario leido = usuarioDAO.buscarPorNombre("marta");
        assertEquals("hash123", leido.getHashClave());
        assertEquals("sal456", leido.getSal());
        assertEquals(Rol.COORDINADOR, leido.getRol());
        assertFalse(leido.isActivo());
    }

    @Test
    void guardarDeNuevoActualizaSinDistinguirMayusculas() {
        usuarioDAO.guardar(new Usuario("Marta", "hashViejo", "salVieja", Rol.OPERADOR));
        usuarioDAO.guardar(new Usuario("MARTA", "hashNuevo", "salNueva", Rol.ADMINISTRADOR));

        assertEquals(1, usuarioDAO.listarTodos().size());
        Usuario leido = usuarioDAO.buscarPorNombre("marta");
        assertEquals("hashNuevo", leido.getHashClave());
        assertEquals(Rol.ADMINISTRADOR, leido.getRol());
    }

    @Test
    void buscarUnUsuarioInexistenteLanzaExcepcion() {
        assertThrows(DatosInvalidosException.class, () -> usuarioDAO.buscarPorNombre("nadie"));
    }

    @Test
    void listarTodosDevuelveLosUsuariosOrdenadosPorNombre() {
        usuarioDAO.guardar(new Usuario("zoe", "h1", "s1", Rol.OPERADOR));
        usuarioDAO.guardar(new Usuario("ana", "h2", "s2", Rol.COORDINADOR));

        assertEquals(2, usuarioDAO.listarTodos().size());
        assertEquals("ana", usuarioDAO.listarTodos().get(0).getNombreUsuario());
    }

    @Test
    void eliminarBorraAlUsuarioYFallaSiNoExiste() {
        usuarioDAO.guardar(new Usuario("marta", "h", "s", Rol.OPERADOR));
        usuarioDAO.eliminar("marta");

        assertThrows(DatosInvalidosException.class, () -> usuarioDAO.buscarPorNombre("marta"));
        assertThrows(DatosInvalidosException.class, () -> usuarioDAO.eliminar("marta"));
    }

    @Test
    void elNombreDeUsuarioInvalidoSeRechaza() {
        assertThrows(DatosInvalidosException.class, () -> new Usuario("ab", "h", "s", Rol.OPERADOR));
        assertThrows(DatosInvalidosException.class, () -> new Usuario("con espacio", "h", "s", Rol.OPERADOR));
        assertThrows(DatosInvalidosException.class, () -> new Usuario("marta", " ", "s", Rol.OPERADOR));
    }
}
