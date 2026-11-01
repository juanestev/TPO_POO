package voluntariado.seguridad;

import java.util.List;
import voluntariado.dao.UsuarioDAO;
import voluntariado.modelo.DatosInvalidosException;

// Alta de usuarios y login. Une Usuario (datos), ServicioClaves (hash) y UsuarioDAO (base).
public class ServicioAutenticacion {

    private final UsuarioDAO dao;
    private final ServicioClaves claves;
    // Sal de relleno: si el usuario no existe igual se calcula un hash, asi el login tarda parecido.
    private final String salFalsa;

    public ServicioAutenticacion(UsuarioDAO dao, ServicioClaves claves) {
        if (dao == null || claves == null) {
            throw new DatosInvalidosException("Se necesitan el DAO de usuarios y el servicio de claves");
        }
        this.dao = dao;
        this.claves = claves;
        this.salFalsa = claves.generarSal();
    }

    public Usuario crearUsuario(String nombreUsuario, String clave, Rol rol) {
        claves.validarClaveNueva(clave);
        String sal = claves.generarSal();
        Usuario usuario = new Usuario(nombreUsuario, claves.calcularHash(clave, sal), sal, rol);
        if (dao.existe(usuario.getNombreUsuario())) {
            throw new DatosInvalidosException("Ya existe un usuario con ese nombre");
        }
        dao.guardar(usuario);
        return usuario;
    }

    public Usuario iniciarSesion(String nombreUsuario, String clave) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || !dao.existe(nombreUsuario.trim())) {
            claves.calcularHash(clave == null ? "" : clave, salFalsa);
            throw new AutenticacionException();
        }
        Usuario usuario = dao.buscarPorNombre(nombreUsuario.trim());
        boolean claveOk = claves.coincide(clave, usuario.getSal(), usuario.getHashClave());
        if (!claveOk || !usuario.isActivo()) {
            throw new AutenticacionException();
        }
        return usuario;
    }

    // Lo usa el administrador para dejar una clave nueva (por ejemplo, si alguien la olvido).
    public void restablecerClave(String nombreUsuario, String claveNueva) {
        claves.validarClaveNueva(claveNueva);
        Usuario usuario = dao.buscarPorNombre(nombreUsuario);
        String sal = claves.generarSal();
        usuario.cambiarClave(claves.calcularHash(claveNueva, sal), sal);
        dao.guardar(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return dao.listarTodos();
    }
}
