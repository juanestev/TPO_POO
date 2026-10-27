package voluntariado.modelo;

import java.util.List;

public interface EstrategiaAsignacion {

    List<Voluntario> filtrar(List<Voluntario> voluntarios, Habilidad habilidad, Disponibilidad turno);
}
