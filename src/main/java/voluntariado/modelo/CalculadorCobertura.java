package voluntariado.modelo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalculadorCobertura {

    public int contarPresentes(PuntoDeControl punto, List<RegistroAsistencia> registros) {
        if (punto == null || registros == null) {
            throw new DatosInvalidosException("Se necesita un punto y registros de asistencia");
        }
        Set<String> presentes = new HashSet<>();
        for (RegistroAsistencia registro : registros) {
            if (registro == null) {
                throw new DatosInvalidosException("La lista contiene una asistencia nula");
            }
            if (registro.estaPresente() && registro.getAsignacion().getPunto().equals(punto)) {
                presentes.add(registro.getAsignacion().getVoluntario().getDni());
            }
        }
        return presentes.size();
    }

    public EstadoCobertura calcular(PuntoDeControl punto, List<RegistroAsistencia> registros) {
        int presentes = contarPresentes(punto, registros);
        if (presentes < punto.getDotacionRequerida()) {
            return EstadoCobertura.DEFICIT;
        }
        if (presentes > punto.getDotacionRequerida()) {
            return EstadoCobertura.SOBRANTE;
        }
        return EstadoCobertura.CUBIERTO;
    }
}
