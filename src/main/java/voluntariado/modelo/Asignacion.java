package voluntariado.modelo;

import java.util.UUID;

public class Asignacion {

    private final Voluntario voluntario;
    private final PuntoDeControl punto;
    private final String codigoQr = UUID.randomUUID().toString();
    private EstadoAsignacion estado = EstadoAsignacion.SUGERIDA;

    public Asignacion(Voluntario voluntario, PuntoDeControl punto) {
        if (voluntario == null || punto == null) {
            throw new DatosInvalidosException("La asignacion necesita un voluntario y un punto");
        }
        this.voluntario = voluntario;
        this.punto = punto;
    }

    public Voluntario getVoluntario() {
        return voluntario;
    }

    public PuntoDeControl getPunto() {
        return punto;
    }

    public EstadoAsignacion getEstado() {
        return estado;
    }

    public String getCodigoQr() {
        return codigoQr;
    }

    public void confirmar() {
        estado = EstadoAsignacion.CONFIRMADA;
    }
}
