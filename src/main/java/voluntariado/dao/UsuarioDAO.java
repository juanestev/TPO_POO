package voluntariado.dao;

import java.util.List;
import voluntariado.seguridad.Usuario;

// Patron DAO: el modulo de seguridad guarda y lee usuarios sin saber que hay SQL detras.
public interface UsuarioDAO {

    // Alta o modificacion: si el nombre ya existe (sin distinguir mayusculas), se actualiza.
    void guardar(Usuario usuario);

    // Lanza DatosInvalidosException si no existe (igual que los otros DAO).
    Usuario buscarPorNombre(String nombreUsuario);

    List<Usuario> listarTodos();

    // Lanza DatosInvalidosException si no existe.
    void eliminar(String nombreUsuario);
}
