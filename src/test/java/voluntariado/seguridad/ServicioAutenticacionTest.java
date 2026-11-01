package voluntariado.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import voluntariado.dao.UsuarioDAO;
import voluntariado.modelo.DatosInvalidosException;

class ServicioAutenticacionTest {

    // DAO de mentira en memoria: asi se prueba la seguridad sin tocar la base.
    private static class UsuarioDAOEnMemoria implements UsuarioDAO {
        private final List<Usuario> usuarios = new ArrayList<>();

        public void guardar(Usuario u) {
            usuarios.remove(u);
            usuarios.add(u);
        }

        public Usuario buscarPorNombre(String nombre) {
            return usuarios.stream().filter(u -> u.getNombreUsuario().equalsIgnoreCase(nombre)).findFirst()
                    .orElseThrow(() -> new DatosInvalidosException("El usuario no esta registrado"));
        }

        public boolean existe(String nombre) {
            return usuarios.stream().anyMatch(u -> u.getNombreUsuario().equalsIgnoreCase(nombre));
        }

        public List<Usuario> listarTodos() {
            return List.copyOf(usuarios);
        }

        public void eliminar(String nombre) {
            usuarios.remove(buscarPorNombre(nombre));
        }
    }

    private ServicioAutenticacion servicio;
    private UsuarioDAOEnMemoria dao;

    @BeforeEach
    void preparar() {
        dao = new UsuarioDAOEnMemoria();
        servicio = new ServicioAutenticacion(dao, new ServicioClaves());
    }

    @Test
    void nuncaGuardaLaClaveEnTextoPlano() {
        Usuario creado = servicio.crearUsuario("marta", "Clave1234", Rol.COORDINADOR);
        assertNotEquals("Clave1234", creado.getHashClave());
        assertFalse(creado.getHashClave().contains("Clave1234"));
    }

    @Test
    void conLaClaveCorrectaEntra() {
        servicio.crearUsuario("marta", "Clave1234", Rol.COORDINADOR);
        assertEquals(Rol.COORDINADOR, servicio.iniciarSesion("MARTA", "Clave1234").getRol());
    }

    @Test
    void claveMalaUsuarioInexistenteOInactivoDanElMismoError() {
        Usuario marta = servicio.crearUsuario("marta", "Clave1234", Rol.OPERADOR);
        marta.setActivo(false);
        dao.guardar(marta);
        servicio.crearUsuario("juan", "Clave1234", Rol.OPERADOR);

        String malaClave = assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("juan", "otraClave99")).getMessage();
        String inexistente = assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("nadie", "Clave1234")).getMessage();
        String inactivo = assertThrows(AutenticacionException.class,
                () -> servicio.iniciarSesion("marta", "Clave1234")).getMessage();
        assertEquals(malaClave, inexistente);
        assertEquals(malaClave, inactivo);
    }

    @Test
    void dosUsuariosConLaMismaClaveTienenHashesDistintos() {
        Usuario a = servicio.crearUsuario("usuarioa", "Clave1234", Rol.OPERADOR);
        Usuario b = servicio.crearUsuario("usuariob", "Clave1234", Rol.OPERADOR);
        assertNotEquals(a.getHashClave(), b.getHashClave());
    }

    @Test
    void rechazaClavesDebilesYNombresRepetidos() {
        assertThrows(DatosInvalidosException.class, () -> servicio.crearUsuario("marta", "corta1", Rol.OPERADOR));
        assertThrows(DatosInvalidosException.class,
                () -> servicio.crearUsuario("marta", "soloLetrasAqui", Rol.OPERADOR));
        servicio.crearUsuario("marta", "Clave1234", Rol.OPERADOR);
        assertThrows(DatosInvalidosException.class,
                () -> servicio.crearUsuario("Marta", "Clave1234", Rol.OPERADOR));
    }

    @Test
    void restablecerClaveInvalidaLaAnterior() {
        servicio.crearUsuario("marta", "Clave1234", Rol.OPERADOR);
        servicio.restablecerClave("marta", "Nueva5678");
        assertThrows(AutenticacionException.class, () -> servicio.iniciarSesion("marta", "Clave1234"));
        assertEquals("marta", servicio.iniciarSesion("marta", "Nueva5678").getNombreUsuario());
    }

    @Test
    void laSesionControlaPermisosSegunElRol() {
        Sesion sesion = new Sesion();
        assertFalse(sesion.tienePermiso(Accion.VER_COBERTURA));
        assertThrows(PermisoDenegadoException.class, () -> sesion.exigirPermiso(Accion.VER_COBERTURA));

        sesion.iniciar(servicio.crearUsuario("marta", "Clave1234", Rol.OPERADOR));
        assertTrue(sesion.tienePermiso(Accion.REGISTRAR_ASISTENCIA));
        assertFalse(sesion.tienePermiso(Accion.GESTIONAR_USUARIOS));
        assertThrows(PermisoDenegadoException.class, () -> sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS));

        sesion.cerrar();
        assertFalse(sesion.hayUsuario());
    }
}
