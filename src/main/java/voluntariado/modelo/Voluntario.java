package voluntariado.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public abstract class Voluntario extends Persona {

    private final Set<Habilidad> habilidades = EnumSet.noneOf(Habilidad.class);
    private final List<Disponibilidad> disponibilidades = new ArrayList<>();

    protected Voluntario(String dni, String nombre, String apellido, String telefono, String mail) {
        super(dni, nombre, apellido, telefono, mail);
    }

    public Set<Habilidad> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public void agregarHabilidad(Habilidad habilidad) {
        if (habilidad == null) {
            throw new DatosInvalidosException("La habilidad es obligatoria");
        }
        habilidades.add(habilidad);
    }

    public void quitarHabilidad(Habilidad habilidad) {
        habilidades.remove(habilidad);
    }

    public List<Disponibilidad> getDisponibilidades() {
        return Collections.unmodifiableList(disponibilidades);
    }

    public void agregarDisponibilidad(Disponibilidad disponibilidad) {
        if (disponibilidad == null) {
            throw new DatosInvalidosException("La disponibilidad es obligatoria");
        }
        if (!disponibilidades.contains(disponibilidad)) {
            disponibilidades.add(disponibilidad);
        }
    }

    public void quitarDisponibilidad(Disponibilidad disponibilidad) {
        disponibilidades.remove(disponibilidad);
    }
}
