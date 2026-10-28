package voluntariado.controlador;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import voluntariado.dao.AsistenciaDAO;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.ObservadorCobertura;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.servicios.CodigoQr;

public class ControladorAsistencia {

    private final RepositorioEnMemoria repositorio;
    private final AsistenciaDAO dao;
    private final Map<String, RegistroAsistencia> registrosPorQr = new HashMap<>();
    private final List<ObservadorCobertura> observadoresCobertura = new ArrayList<>();

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
        notificarCobertura(nuevo);
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
        notificarCobertura(nuevo);
        return nuevo;
    }

    public List<RegistroAsistencia> getRegistros() {
        return List.copyOf(registrosPorQr.values());
    }

    public void agregarObservadorCobertura(ObservadorCobertura observador) {
        if (observador == null) {
            throw new DatosInvalidosException("El observador es obligatorio");
        }
        observadoresCobertura.add(observador);
    }

    public void quitarObservadorCobertura(ObservadorCobertura observador) {
        observadoresCobertura.remove(observador);
    }

    private void notificarCobertura(RegistroAsistencia registro) {
        for (ObservadorCobertura observador : List.copyOf(observadoresCobertura)) {
            observador.alCambiar(registro.getAsignacion().getPunto());
        }
    }

    private RegistroAsistencia copiarRegistro(String qr) {
        Asignacion asignacion = repositorio.buscarAsignacionPorQr(qr);
        RegistroAsistencia anterior = registrosPorQr.get(qr);
        return anterior == null ? new RegistroAsistencia(asignacion)
                : new RegistroAsistencia(asignacion, anterior.getEntrada(), anterior.getSalida());
    }
}
