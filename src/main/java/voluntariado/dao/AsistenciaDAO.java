package voluntariado.dao;

import java.util.List;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;

public interface AsistenciaDAO {

    void guardar(RegistroAsistencia registro);

    List<RegistroAsistencia> listarTodas(RepositorioEnMemoria repositorio);
}
