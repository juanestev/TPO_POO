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
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Sesion;
import voluntariado.servicios.CodigoQr;

public class ControladorAsistencia {

    private final RepositorioEnMemoria repositorio;
    private final AsistenciaDAO dao;
    private final Map<String, RegistroAsistencia> registrosPorQr = new HashMap<>();
    private final List<ObservadorCobertura> observadoresCobertura = new ArrayList<>();

    private final Sesion sesion;

    // Sin sesion no se controlan permisos (solo para pruebas); la app siempre usa el otro constructor.
    public ControladorAsistencia(RepositorioEnMemoria repositorio, AsistenciaDAO dao) {
        this(repositorio, dao, null);
    }

    public ControladorAsistencia(RepositorioEnMemoria repositorio, AsistenciaDAO dao, Sesion sesion) {
        if (repositorio == null || dao == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y DAO de asistencia");
        }
        this.repositorio = repositorio;
        this.dao = dao;
        this.sesion = sesion;
        for (RegistroAsistencia registro : dao.listarTodas(repositorio)) {
            registrosPorQr.put(registro.getAsignacion().getCodigoQr(), registro);
        }
    }

    private void exigirPermiso() {
        if (sesion != null) {
            sesion.exigirPermiso(Accion.REGISTRAR_ASISTENCIA);
        }
    }

    public RegistroAsistencia registrarEntradaManual(String codigo, LocalDateTime momento) {
        exigirPermiso();
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

    public RegistroAsistencia registrarSalidaDesdeImagen(BufferedImage imagen, LocalDateTime momento) {
        return registrarSalidaManual(CodigoQr.leerImagen(imagen), momento);
    }

    // PNG del QR de una asignacion (para imprimirlo o probar el lector con una imagen).
    public byte[] generarImagenQr(String codigoQr) {
        return CodigoQr.generarImagen(repositorio.buscarAsignacionPorQr(codigoQr).getCodigoQr(), 300);
    }

    public List<Asignacion> getAsignaciones() {
        List<Asignacion> todas = new ArrayList<>();
        for (voluntariado.modelo.PuntoDeControl punto : repositorio.getPuntos()) {
            todas.addAll(repositorio.getAsignacionesPorPunto(punto.getCodigo()));
        }
        return todas;
    }

    public RegistroAsistencia registrarSalidaManual(String codigo, LocalDateTime momento) {
        exigirPermiso();
        String qr = CodigoQr.validarCodigoManual(codigo);
        RegistroAsistencia nuevo = copiarRegistro(qr);
        nuevo.registrarSalida(qr, momento);
        dao.guardar(nuevo);
        registrosPorQr.put(qr, nuevo);
        notificarCobertura(nuevo);
        return nuevo;
    }

    // Usado al reasignar: la asignacion vieja desaparece, asi que su registro de asistencia tambien.
    public void olvidarRegistro(String codigoQr) {
        registrosPorQr.remove(codigoQr);
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
