package voluntariado.dao;

import voluntariado.modelo.Asignacion;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.Voluntario;

// Al abrir la app pasa todo lo guardado en la base al repositorio en memoria (HashMap/ArrayList).
// El orden importa: primero puntos y voluntarios, despues las asignaciones que los referencian.
public class CargaInicial {

    private final PuntoDeControlDAO puntoDAO;
    private final VoluntarioDAO voluntarioDAO;
    private final AsignacionDAO asignacionDAO;

    public CargaInicial(PuntoDeControlDAO puntoDAO, VoluntarioDAO voluntarioDAO, AsignacionDAO asignacionDAO) {
        if (puntoDAO == null || voluntarioDAO == null || asignacionDAO == null) {
            throw new PersistenciaException("La carga inicial necesita los tres DAO");
        }
        this.puntoDAO = puntoDAO;
        this.voluntarioDAO = voluntarioDAO;
        this.asignacionDAO = asignacionDAO;
    }

    public RepositorioEnMemoria cargar() {
        RepositorioEnMemoria repositorio = new RepositorioEnMemoria();
        for (PuntoDeControl punto : puntoDAO.listarTodos()) {
            repositorio.agregarPunto(punto);
        }
        for (Voluntario voluntario : voluntarioDAO.listarTodos()) {
            repositorio.agregarVoluntario(voluntario);
        }
        // El repositorio solo guarda confirmadas: las sugeridas se vuelven a generar con el motor.
        for (Asignacion asignacion : asignacionDAO.listarTodas(repositorio)) {
            if (asignacion.getEstado() == EstadoAsignacion.CONFIRMADA) {
                repositorio.registrarAsignacion(asignacion);
            }
        }
        return repositorio;
    }
}
