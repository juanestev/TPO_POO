package voluntariado.modelo;

import java.util.UUID;

public class Asignacion {

    private final Voluntario voluntario;
    private final PuntoDeControl punto;
    private final String codigoQr;
    private EstadoAsignacion estado;

    // Asignacion nueva: nace SUGERIDA y con un codigo QR propio.
    public Asignacion(Voluntario voluntario, PuntoDeControl punto) {
        this(voluntario, punto, UUID.randomUUID().toString(), EstadoAsignacion.SUGERIDA);
    }

    // Usado al leer de la base: el QR ya fue enviado por mail, asi que hay que conservarlo tal cual.
    public Asignacion(Voluntario voluntario, PuntoDeControl punto, String codigoQr, EstadoAsignacion estado) {
        if (voluntario == null || punto == null) {
            throw new DatosInvalidosException("La asignacion necesita un voluntario y un punto");
        }
        if (codigoQr == null || codigoQr.isBlank() || estado == null) {
            throw new DatosInvalidosException("La asignacion necesita un codigo QR y un estado");
        }
        this.voluntario = voluntario;
        this.punto = punto;
        this.codigoQr = codigoQr.trim();
        this.estado = estado;
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
