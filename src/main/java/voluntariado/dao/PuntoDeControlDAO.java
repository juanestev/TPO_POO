package voluntariado.dao;

import java.util.List;
import voluntariado.modelo.PuntoDeControl;

public interface PuntoDeControlDAO {

    // Alta o modificacion: si el codigo ya existe, se actualiza junto con sus zonas cercanas.
    void guardar(PuntoDeControl punto);

    // Lanza DatosInvalidosException si no existe.
    PuntoDeControl buscarPorCodigo(String codigo);

    List<PuntoDeControl> listarTodos();

    // Lanza PersistenciaException si el punto tiene asignaciones.
    void eliminar(String codigo);
}
