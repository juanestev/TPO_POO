package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.Usuario;

public class UsuarioDAOSQLite implements UsuarioDAO {

    private static final String UPSERT_USUARIO = """
            INSERT INTO usuario (nombre_usuario, hash_clave, sal, rol, activo) VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (nombre_usuario) DO UPDATE SET hash_clave = excluded.hash_clave,
                sal = excluded.sal, rol = excluded.rol, activo = excluded.activo""";

    private static final String SELECT_USUARIO =
            "SELECT nombre_usuario, hash_clave, sal, rol, activo FROM usuario";

    private final ConexionBD conexionBD;

    public UsuarioDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public void guardar(Usuario usuario) {
        if (usuario == null) {
            throw new DatosInvalidosException("El usuario es obligatorio");
        }
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(UPSERT_USUARIO)) {
            ps.setString(1, usuario.getNombreUsuario());
            ps.setString(2, usuario.getHashClave());
            ps.setString(3, usuario.getSal());
            ps.setString(4, usuario.getRol().name());
            ps.setInt(5, usuario.isActivo() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el usuario " + usuario.getNombreUsuario(), e);
        }
    }

    @Override
    public Usuario buscarPorNombre(String nombreUsuario) {
        String nombre = Errores.validarClave(nombreUsuario, "nombre de usuario");
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(SELECT_USUARIO + " WHERE nombre_usuario = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DatosInvalidosException("El usuario no esta registrado");
                }
                return armar(rs);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo leer el usuario " + nombre, e);
        }
    }

    @Override
    public boolean existe(String nombreUsuario) {
        String nombre = Errores.validarClave(nombreUsuario, "nombre de usuario");
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM usuario WHERE nombre_usuario = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el usuario " + nombre, e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(SELECT_USUARIO + " ORDER BY nombre_usuario");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(armar(rs));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer los usuarios", e);
        }
        return usuarios;
    }

    @Override
    public void eliminar(String nombreUsuario) {
        String nombre = Errores.validarClave(nombreUsuario, "nombre de usuario");
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement("DELETE FROM usuario WHERE nombre_usuario = ?")) {
            ps.setString(1, nombre);
            if (ps.executeUpdate() == 0) {
                throw new DatosInvalidosException("El usuario no esta registrado");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo borrar el usuario " + nombre, e);
        }
    }

    private Usuario armar(ResultSet rs) throws SQLException {
        Rol rol;
        try {
            rol = Rol.valueOf(rs.getString("rol"));
        } catch (IllegalArgumentException e) {
            throw new PersistenciaException("La base tiene un rol desconocido: " + rs.getString("rol"), e);
        }
        Usuario usuario = new Usuario(rs.getString("nombre_usuario"), rs.getString("hash_clave"),
                rs.getString("sal"), rol);
        usuario.setActivo(rs.getInt("activo") == 1);
        return usuario;
    }
}
