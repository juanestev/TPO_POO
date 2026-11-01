package voluntariado.seguridad;

import voluntariado.modelo.DatosInvalidosException;

// Quien esta logueado ahora. Hay una sola instancia compartida entre todas las ventanas
// (igual idea que Biblioteca en UVA 6): la crea Main y se la pasa a quien la necesite.
public class Sesion {

    private Usuario usuario;

    public void iniciar(Usuario usuario) {
        if (usuario == null) {
            throw new DatosInvalidosException("El usuario es obligatorio");
        }
        this.usuario = usuario;
    }

    public void cerrar() {
        usuario = null;
    }

    public boolean hayUsuario() {
        return usuario != null;
    }

    public Usuario getUsuario() {
        if (usuario == null) {
            throw new PermisoDenegadoException("No hay una sesion iniciada");
        }
        return usuario;
    }

    // Para decidir que botones mostrar: no tira excepcion.
    public boolean tienePermiso(Accion accion) {
        return usuario != null && Permisos.tienePermiso(usuario.getRol(), accion);
    }

    // Para proteger una operacion: si no tiene permiso, corta con una excepcion.
    public void exigirPermiso(Accion accion) {
        if (!tienePermiso(accion)) {
            throw new PermisoDenegadoException("Su rol no tiene permiso para esta accion");
        }
    }
}
