package voluntariado.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EstrategiaReasignacion {

    private final EstrategiaAsignacion estrategiaBase;
    private final CalculadorCobertura calculador = new CalculadorCobertura();

    public EstrategiaReasignacion(EstrategiaAsignacion estrategiaBase) {
        if (estrategiaBase == null) {
            throw new DatosInvalidosException("La estrategia base es obligatoria");
        }
        this.estrategiaBase = estrategiaBase;
    }

    public List<Voluntario> seleccionar(PuntoDeControl destino, List<RegistroAsistencia> registros,
            Habilidad habilidad, Disponibilidad turno) {
        if (destino == null || registros == null || habilidad == null || turno == null) {
            throw new DatosInvalidosException("Se necesitan destino, asistencias, habilidad y turno");
        }
        Map<PuntoDeControl, List<Voluntario>> porOrigen = new HashMap<>();
        for (RegistroAsistencia registro : registros) {
            if (registro == null) {
                throw new DatosInvalidosException("La lista contiene una asistencia nula");
            }
            PuntoDeControl origen = registro.getAsignacion().getPunto();
            if (registro.estaPresente() && !origen.equals(destino)
                    && destino.getZonasCercanas().stream().anyMatch(z -> z.equalsIgnoreCase(origen.getZona()))) {
                porOrigen.computeIfAbsent(origen, p -> new ArrayList<>())
                        .add(registro.getAsignacion().getVoluntario());
            }
        }
        List<PuntoDeControl> origenes = new ArrayList<>(porOrigen.keySet());
        origenes.sort(Comparator.comparing(PuntoDeControl::getCodigo));
        List<Voluntario> seleccionados = new ArrayList<>();
        for (PuntoDeControl origen : origenes) {
            int sobrante = calculador.contarPresentes(origen, registros) - origen.getDotacionRequerida();
            if (sobrante <= 0) {
                continue;
            }
            List<Voluntario> aptos = estrategiaBase.filtrar(porOrigen.get(origen), habilidad, turno);
            for (Voluntario voluntario : aptos) {
                if (seleccionados.stream().noneMatch(v -> v.getDni().equals(voluntario.getDni()))) {
                    seleccionados.add(voluntario);
                    if (--sobrante == 0) {
                        break;
                    }
                }
            }
        }
        return List.copyOf(seleccionados);
    }
}
