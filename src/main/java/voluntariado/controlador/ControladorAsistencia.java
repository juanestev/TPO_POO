package voluntariado.controlador;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import voluntariado.dao.AsistenciaDAO;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.servicios.CodigoQr;

public class ControladorAsistencia {

    private final RepositorioEnMemoria repositorio;
    private final AsistenciaDAO dao;
    private final Map<String, RegistroAsistencia> registrosPorQr = new HashMap<>();

    public ControladorAsistencia(RepositorioEnMemoria repositorio, AsistenciaDAO dao) {
        if (repositorio == null || dao == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y DAO de asistencia");
        }
        this.repositorio = repositorio;
        this.dao = dao;
        for (RegistroAsistencia registro : dao.listarTodas(repositorio)) {
            registrosPorQr.put(registro.getAsignacion().getCodigoQr(), registro);
        }
    }

    public RegistroAsistencia registrarEntradaManual(String codigo, LocalDateTime momento) {
        String qr = CodigoQr.validarCodigoManual(codigo);
        RegistroAsistencia nuevo = copiarRegistro(qr);
        nuevo.registrarEntrada(qr, momento);
        dao.guardar(nuevo);
        registrosPorQr.put(qr, nuevo);
        return nuevo;
    }

    public RegistroAsistencia registrarEntradaDesdeImagen(BufferedImage imagen, LocalDateTime momento) {
        return registrarEntradaManual(CodigoQr.leerImagen(imagen), momento);
    }

    public RegistroAsistencia registrarSalidaManual(String codigo, LocalDateTime momento) {
        String qr = CodigoQr.validarCodigoManual(codigo);
        RegistroAsistencia nuevo = copiarRegistro(qr);
        nuevo.registrarSalida(qr, momento);
        dao.guardar(nuevo);
        registrosPorQr.put(qr, nuevo);
        return nuevo;
    }

    public List<RegistroAsistencia> getRegistros() {
        return List.copyOf(registrosPorQr.values());
    }

    private RegistroAsistencia copiarRegistro(String qr) {
        Asignacion asignacion = repositorio.buscarAsignacionPorQr(qr);
        RegistroAsistencia anterior = registrosPorQr.get(qr);
        return anterior == null ? new RegistroAsistencia(asignacion)
                : new RegistroAsistencia(asignacion, anterior.getEntrada(), anterior.getSalida());
    }
}
