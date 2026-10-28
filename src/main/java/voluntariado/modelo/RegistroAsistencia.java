package voluntariado.modelo;

import java.time.LocalDateTime;

public class RegistroAsistencia {

    private final Asignacion asignacion;
    private LocalDateTime entrada;
    private LocalDateTime salida;

    public RegistroAsistencia(Asignacion asignacion) {
        this(asignacion, null, null);
    }

    public RegistroAsistencia(Asignacion asignacion, LocalDateTime entrada, LocalDateTime salida) {
        if (asignacion == null || asignacion.getEstado() != EstadoAsignacion.CONFIRMADA) {
            throw new DatosInvalidosException("La asistencia necesita una asignacion confirmada");
        }
        if (salida != null && (entrada == null || salida.isBefore(entrada))) {
            throw new DatosInvalidosException("La salida requiere una entrada anterior");
        }
        this.asignacion = asignacion;
        this.entrada = entrada;
        this.salida = salida;
    }

    public Asignacion getAsignacion() {
        return asignacion;
    }

    public LocalDateTime getEntrada() {
        return entrada;
    }

    public LocalDateTime getSalida() {
        return salida;
    }

    public boolean estaPresente() {
        return entrada != null && salida == null;
    }

    public void registrarEntrada(String codigoQr, LocalDateTime momento) {
        validarCodigoYMomento(codigoQr, momento);
        if (entrada != null) {
            throw new DatosInvalidosException("La entrada ya fue registrada");
        }
        entrada = momento;
    }

    public void registrarSalida(String codigoQr, LocalDateTime momento) {
        validarCodigoYMomento(codigoQr, momento);
        if (!estaPresente() || momento.isBefore(entrada)) {
            throw new DatosInvalidosException("La salida requiere una entrada anterior");
        }
        salida = momento;
    }

    private void validarCodigoYMomento(String codigoQr, LocalDateTime momento) {
        if (!asignacion.getCodigoQr().equals(codigoQr) || momento == null) {
            throw new DatosInvalidosException("Codigo o momento de asistencia invalido");
        }
    }
}
