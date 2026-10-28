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
