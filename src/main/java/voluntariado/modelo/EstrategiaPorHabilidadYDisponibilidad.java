package voluntariado.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EstrategiaPorHabilidadYDisponibilidad implements EstrategiaAsignacion {

    @Override
    public List<Voluntario> filtrar(List<Voluntario> voluntarios, Habilidad habilidad, Disponibilidad turno) {
        if (voluntarios == null || habilidad == null || turno == null) {
            throw new DatosInvalidosException("Se necesitan voluntarios, habilidad y turno");
        }
        List<Voluntario> aptos = new ArrayList<>();
        for (Voluntario voluntario : voluntarios) {
            if (voluntario == null) {
                throw new DatosInvalidosException("La lista contiene un voluntario nulo");
            }
            if (voluntario.getHabilidades().contains(habilidad)
                    && voluntario.getDisponibilidades().stream().anyMatch(disponible -> cubre(disponible, turno))) {
                aptos.add(voluntario);
            }
        }
        aptos.sort(Comparator.comparing(Voluntario::getDni));
        return List.copyOf(aptos);
    }

    private boolean cubre(Disponibilidad disponible, Disponibilidad turno) {
        return disponible.getDia() == turno.getDia()
                && !disponible.getDesde().isAfter(turno.getDesde())
                && !disponible.getHasta().isBefore(turno.getHasta());
    }
}
