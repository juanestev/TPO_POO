package voluntariado.controlador;

import java.util.List;
import voluntariado.dao.UsuarioDAO;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.ServicioAutenticacion;
import voluntariado.seguridad.Sesion;
import voluntariado.seguridad.Usuario;

// Gestion de usuarios (solo administrador). Cuida que el sistema nunca se quede sin un administrador activo
// y que nadie se quite el acceso a si mismo por error.
public class ControladorUsuarios {

    private final ServicioAutenticacion autenticacion;
    private final UsuarioDAO dao;
    private final Sesion sesion;

    public ControladorUsuarios(ServicioAutenticacion autenticacion, UsuarioDAO dao, Sesion sesion) {
        if (autenticacion == null || dao == null || sesion == null) {
            throw new DatosInvalidosException("Se necesitan autenticacion, DAO de usuarios y sesion");
        }
        this.autenticacion = autenticacion;
        this.dao = dao;
        this.sesion = sesion;
    }

    public List<Usuario> listar() {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        return autenticacion.listarUsuarios();
    }

    public Usuario crear(String nombreUsuario, String clave, Rol rol) {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        return autenticacion.crearUsuario(nombreUsuario, clave, rol);
    }

    public void cambiarRol(String nombreUsuario, Rol rol) {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        Usuario usuario = dao.buscarPorNombre(nombreUsuario);
        if (usuario.getRol() == Rol.ADMINISTRADOR && rol != Rol.ADMINISTRADOR) {
            exigirOtroAdministrador(usuario, "quitarle el rol de administrador");
        }
        Rol anterior = usuario.getRol();
        usuario.setRol(rol);
        try {
            dao.guardar(usuario);
        } catch (RuntimeException e) {
            usuario.setRol(anterior);
            throw e;
        }
    }

    public void cambiarEstado(String nombreUsuario, boolean activo) {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        Usuario usuario = dao.buscarPorNombre(nombreUsuario);
        if (!activo) {
            exigirOtroAdministrador(usuario, "desactivarlo");
        }
        boolean anterior = usuario.isActivo();
        usuario.setActivo(activo);
        try {
            dao.guardar(usuario);
        } catch (RuntimeException e) {
            usuario.setActivo(anterior);
            throw e;
        }
    }

    public void restablecerClave(String nombreUsuario, String claveNueva) {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        autenticacion.restablecerClave(nombreUsuario, claveNueva);
    }

    public void eliminar(String nombreUsuario) {
        sesion.exigirPermiso(Accion.GESTIONAR_USUARIOS);
        exigirOtroAdministrador(dao.buscarPorNombre(nombreUsuario), "eliminarlo");
        dao.eliminar(nombreUsuario);
    }

    // Se llama antes de una accion que le quita el acceso al usuario. Si es uno mismo, o el ultimo
    // administrador activo, se rechaza. (Para usuarios no administradores no hay problema.)
    private void exigirOtroAdministrador(Usuario usuario, String accion) {
        if (usuario.equals(sesion.getUsuario())) {
            throw new DatosInvalidosException("No podes " + accion + " a tu propio usuario");
        }
        if (usuario.getRol() == Rol.ADMINISTRADOR && usuario.isActivo()) {
            long otros = dao.listarTodos().stream()
                    .filter(u -> u.getRol() == Rol.ADMINISTRADOR && u.isActivo() && !u.equals(usuario)).count();
            if (otros == 0) {
                throw new DatosInvalidosException("No se puede " + accion + ": es el ultimo administrador activo");
            }
        }
    }
}
