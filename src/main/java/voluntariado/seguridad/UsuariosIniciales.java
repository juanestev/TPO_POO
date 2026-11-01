package voluntariado.seguridad;

// Usuarios de prueba para poder entrar la primera vez: uno por rol. Solo se crean si no hay ninguno.
// Las claves son de prueba (estan en el README): en un sistema real se cambiarian al primer ingreso.
public final class UsuariosIniciales {

    private UsuariosIniciales() { }

    // Devuelve true si creo los usuarios.
    public static boolean cargarSiEstaVacia(ServicioAutenticacion servicio) {
        if (!servicio.listarUsuarios().isEmpty()) {
            return false;
        }
        servicio.crearUsuario("admin", "Admin2026", Rol.ADMINISTRADOR);
        servicio.crearUsuario("coordinador", "Coord2026", Rol.COORDINADOR);
        servicio.crearUsuario("operador", "Oper2026x", Rol.OPERADOR);
        return true;
    }
}
