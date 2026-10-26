package voluntariado.modelo;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public final class Disponibilidad {

    private final DayOfWeek dia;
    private final LocalTime desde;
    private final LocalTime hasta;

    public Disponibilidad(DayOfWeek dia, LocalTime desde, LocalTime hasta) {
        if (dia == null || desde == null || hasta == null || !desde.isBefore(hasta)) {
            throw new DatosInvalidosException("La disponibilidad necesita un dia y un horario valido");
        }
        this.dia = dia;
        this.desde = desde;
        this.hasta = hasta;
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public LocalTime getDesde() {
        return desde;
    }

    public LocalTime getHasta() {
        return hasta;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Disponibilidad disponibilidad)) {
            return false;
        }
        return dia == disponibilidad.dia && desde.equals(disponibilidad.desde)
                && hasta.equals(disponibilidad.hasta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dia, desde, hasta);
    }
}
