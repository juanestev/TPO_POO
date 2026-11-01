package voluntariado.controlador;

import java.util.List;
import voluntariado.dao.PuntoDeControlDAO;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;

// Alta, baja y modificacion de puntos de control (misma idea que ControladorVoluntarios).
public class ControladorPuntos {

    private final RepositorioEnMemoria repositorio;
    private final PuntoDeControlDAO dao;

    public ControladorPuntos(RepositorioEnMemoria repositorio, PuntoDeControlDAO dao) {
        if (repositorio == null || dao == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y DAO de puntos");
        }
        this.repositorio = repositorio;
        this.dao = dao;
    }

    public List<PuntoDeControl> listar() {
        return repositorio.getPuntos();
    }

    public PuntoDeControl alta(String codigo, String zona, int dotacion, List<String> zonasCercanas) {
        PuntoDeControl punto = new PuntoDeControl(codigo, zona, dotacion);
        for (String zonaCercana : zonasCercanas) {
            punto.agregarZonaCercana(zonaCercana);
        }
        repositorio.agregarPunto(punto);
        try {
            dao.guardar(punto);
        } catch (RuntimeException e) {
            repositorio.quitarPunto(punto.getCodigo());
            throw e;
        }
        return punto;
    }

    public void modificar(String codigo, String zona, int dotacion, List<String> zonasCercanas) {
        PuntoDeControl punto = repositorio.buscarPunto(codigo);
        String zonaVieja = punto.getZona();
        int dotacionVieja = punto.getDotacionRequerida();
        List<String> cercanasViejas = List.copyOf(punto.getZonasCercanas());
        try {
            reemplazar(punto, zona, dotacion, zonasCercanas);
            dao.guardar(punto);
        } catch (RuntimeException e) {
            reemplazar(punto, zonaVieja, dotacionVieja, cercanasViejas);
            throw e;
        }
    }

    public void baja(String codigo) {
        PuntoDeControl punto = repositorio.buscarPunto(codigo);
        if (!repositorio.getAsignacionesPorPunto(codigo).isEmpty()) {
            throw new DatosInvalidosException("No se puede borrar: el punto tiene asignaciones confirmadas");
        }
        dao.eliminar(codigo);
        repositorio.quitarPunto(punto.getCodigo());
    }

    // Se vacian las zonas cercanas primero: el punto no admite que su propia zona sea una de ellas.
    private void reemplazar(PuntoDeControl punto, String zona, int dotacion, List<String> zonasCercanas) {
        for (String vieja : List.copyOf(punto.getZonasCercanas())) {
            punto.quitarZonaCercana(vieja);
        }
        punto.setZona(zona);
        punto.setDotacionRequerida(dotacion);
        for (String zonaCercana : zonasCercanas) {
            punto.agregarZonaCercana(zonaCercana);
        }
    }
}
