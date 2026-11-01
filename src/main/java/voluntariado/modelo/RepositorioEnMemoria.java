package voluntariado.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositorioEnMemoria {

    private final Map<String, Voluntario> voluntariosPorDni = new HashMap<>();
    private final Map<String, PuntoDeControl> puntosPorCodigo = new HashMap<>();
    private final Map<String, List<Asignacion>> asignacionesPorPunto = new HashMap<>();
    private final List<ObservadorCobertura> observadoresCobertura = new ArrayList<>();

    public void agregarVoluntario(Voluntario voluntario) {
        if (voluntario == null) {
            throw new DatosInvalidosException("El voluntario es obligatorio");
        }
        if (voluntariosPorDni.putIfAbsent(voluntario.getDni(), voluntario) != null) {
            throw new DatosInvalidosException("Ya existe un voluntario con ese DNI");
        }
    }

    public Voluntario buscarVoluntario(String dni) {
        Voluntario voluntario = voluntariosPorDni.get(dni);
        if (voluntario == null) {
            throw new DatosInvalidosException("El voluntario no esta registrado");
        }
        return voluntario;
    }

    public List<Voluntario> buscarVoluntariosPorHabilidad(Habilidad habilidad) {
        if (habilidad == null) {
            throw new DatosInvalidosException("La habilidad es obligatoria");
        }
        List<Voluntario> encontrados = new ArrayList<>();
        for (Voluntario voluntario : voluntariosPorDni.values()) {
            if (voluntario.getHabilidades().contains(habilidad)) {
                encontrados.add(voluntario);
            }
        }
        return Collections.unmodifiableList(encontrados);
    }

    public List<Voluntario> getVoluntarios() {
        return List.copyOf(voluntariosPorDni.values());
    }

    public void agregarPunto(PuntoDeControl punto) {
        if (punto == null) {
            throw new DatosInvalidosException("El punto es obligatorio");
        }
        if (puntosPorCodigo.putIfAbsent(punto.getCodigo(), punto) != null) {
            throw new DatosInvalidosException("Ya existe un punto con ese codigo");
        }
        asignacionesPorPunto.put(punto.getCodigo(), new ArrayList<>());
    }

    public PuntoDeControl buscarPunto(String codigo) {
        PuntoDeControl punto = puntosPorCodigo.get(codigo);
        if (punto == null) {
            throw new DatosInvalidosException("El punto no esta registrado");
        }
        return punto;
    }

    public void registrarAsignacion(Asignacion asignacion) {
        if (asignacion == null || asignacion.getEstado() != EstadoAsignacion.CONFIRMADA) {
            throw new DatosInvalidosException("La asignacion debe estar confirmada");
        }
        validarNuevaAsignacion(asignacion);
        asignacionesPorPunto.get(asignacion.getPunto().getCodigo()).add(asignacion);
        notificarCobertura(asignacion.getPunto());
    }

    public List<PuntoDeControl> getPuntos() {
        List<PuntoDeControl> puntos = new ArrayList<>(puntosPorCodigo.values());
        puntos.sort(java.util.Comparator.comparing(PuntoDeControl::getCodigo));
        return List.copyOf(puntos);
    }

    // No se puede dar de baja a un voluntario con asignacion: se perderia el historial (igual que en la base).
    public void quitarVoluntario(String dni) {
        buscarVoluntario(dni);
        if (estaAsignado(dni)) {
            throw new DatosInvalidosException("El voluntario tiene una asignacion confirmada");
        }
        voluntariosPorDni.remove(dni);
    }

    public void quitarPunto(String codigo) {
        buscarPunto(codigo);
        if (!asignacionesPorPunto.get(codigo).isEmpty()) {
            throw new DatosInvalidosException("El punto tiene asignaciones confirmadas");
        }
        puntosPorCodigo.remove(codigo);
        asignacionesPorPunto.remove(codigo);
    }

    // Usado al reasignar: el voluntario deja su punto y la cobertura de ese punto se recalcula.
    public void quitarAsignacion(Asignacion asignacion) {
        if (asignacion == null || !asignacionesPorPunto.getOrDefault(asignacion.getPunto().getCodigo(), List.of())
                .contains(asignacion)) {
            throw new DatosInvalidosException("La asignacion no esta registrada");
        }
        asignacionesPorPunto.get(asignacion.getPunto().getCodigo()).remove(asignacion);
        notificarCobertura(asignacion.getPunto());
    }

    public Asignacion buscarAsignacionDeVoluntario(String dni) {
        for (List<Asignacion> asignaciones : asignacionesPorPunto.values()) {
            for (Asignacion asignacion : asignaciones) {
                if (asignacion.getVoluntario().getDni().equals(dni)) {
                    return asignacion;
                }
            }
        }
        throw new DatosInvalidosException("El voluntario no tiene una asignacion confirmada");
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

    private void notificarCobertura(PuntoDeControl punto) {
        for (ObservadorCobertura observador : List.copyOf(observadoresCobertura)) {
            observador.alCambiar(punto);
        }
    }

    public void validarNuevaAsignacion(Asignacion asignacion) {
        if (asignacion == null) {
            throw new DatosInvalidosException("La asignacion es obligatoria");
        }
        String dni = asignacion.getVoluntario().getDni();
        String codigo = asignacion.getPunto().getCodigo();
        if (voluntariosPorDni.get(dni) != asignacion.getVoluntario()
                || puntosPorCodigo.get(codigo) != asignacion.getPunto()) {
            throw new DatosInvalidosException("El voluntario y el punto deben estar registrados");
        }
        for (List<Asignacion> asignaciones : asignacionesPorPunto.values()) {
            if (asignaciones.stream().anyMatch(a -> a.getVoluntario().getDni().equals(dni))) {
                throw new DatosInvalidosException("El voluntario ya tiene una asignacion confirmada");
            }
        }
    }

    public List<Asignacion> getAsignacionesPorPunto(String codigo) {
        List<Asignacion> asignaciones = asignacionesPorPunto.get(codigo);
        if (asignaciones == null) {
            throw new DatosInvalidosException("El punto no esta registrado");
        }
        return Collections.unmodifiableList(asignaciones);
    }

    public Asignacion buscarAsignacionPorQr(String codigoQr) {
        if (codigoQr == null || codigoQr.isBlank()) {
            throw new DatosInvalidosException("El codigo QR es obligatorio");
        }
        for (List<Asignacion> asignaciones : asignacionesPorPunto.values()) {
            for (Asignacion asignacion : asignaciones) {
                if (asignacion.getCodigoQr().equals(codigoQr)) {
                    return asignacion;
                }
            }
        }
        throw new DatosInvalidosException("La asignacion no esta registrada");
    }

    public boolean estaAsignado(String dni) {
        for (List<Asignacion> asignaciones : asignacionesPorPunto.values()) {
            if (asignaciones.stream().anyMatch(a -> a.getVoluntario().getDni().equals(dni))) {
                return true;
            }
        }
        return false;
    }
}
