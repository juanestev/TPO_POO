package voluntariado.servicios;

import voluntariado.dao.AsignacionDAO;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.RepositorioEnMemoria;

public class ServicioAsignaciones {

    private final RepositorioEnMemoria repositorio;
    private final AsignacionDAO dao;

    public ServicioAsignaciones(RepositorioEnMemoria repositorio, AsignacionDAO dao) {
        if (repositorio == null || dao == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y DAO de asignaciones");
        }
        this.repositorio = repositorio;
        this.dao = dao;
    }

    public void confirmar(Asignacion sugerencia) {
        if (sugerencia == null || sugerencia.getEstado() != EstadoAsignacion.SUGERIDA) {
            throw new DatosInvalidosException("Solo se puede confirmar una asignacion sugerida");
        }
        repositorio.validarNuevaAsignacion(sugerencia);
        // Persistir una copia conserva la sugerencia original si la base rechaza la confirmacion.
        Asignacion confirmada = new Asignacion(sugerencia.getVoluntario(), sugerencia.getPunto(),
                sugerencia.getCodigoQr(), EstadoAsignacion.CONFIRMADA);
        dao.guardar(confirmada);
        sugerencia.confirmar();
        repositorio.registrarAsignacion(sugerencia);
    }
}
