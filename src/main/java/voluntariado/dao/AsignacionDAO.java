package voluntariado.dao;

import java.util.List;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.AsignacionHistorica;
import voluntariado.modelo.RepositorioEnMemoria;

public interface AsignacionDAO {

    // Alta, o actualizacion del estado si el codigo QR ya existe (ej: SUGERIDA -> CONFIRMADA).
    // Lanza PersistenciaException si el voluntario ya tiene otra asignacion confirmada.
    void guardar(Asignacion asignacion);

    // El cambio de puesto debe guardar la nueva asignacion o conservar la anterior con su asistencia.
    void reasignar(Asignacion anterior, Asignacion nueva);

    // Puestos anteriores con la asistencia que tenian al momento del cambio.
    List<AsignacionHistorica> listarHistorial();

    // Arma las asignaciones usando los voluntarios y puntos YA cargados en el repositorio,
    // asi apuntan a los mismos objetos que usa el resto del sistema.
    List<Asignacion> listarTodas(RepositorioEnMemoria repositorio);

    void eliminar(String codigoQr);
}
