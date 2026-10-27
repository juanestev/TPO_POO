package voluntariado.dao;

import java.util.List;
import voluntariado.modelo.Voluntario;

// Patron DAO: el resto del sistema guarda y lee voluntarios sin saber que hay SQL detras.
public interface VoluntarioDAO {

    // Alta o modificacion: si el DNI ya existe, se actualiza con sus habilidades y disponibilidades.
    void guardar(Voluntario voluntario);

    // Lanza DatosInvalidosException si no existe (igual que el repositorio en memoria).
    Voluntario buscarPorDni(String dni);

    List<Voluntario> listarTodos();

    // Lanza PersistenciaException si el voluntario tiene asignaciones.
    void eliminar(String dni);
}
