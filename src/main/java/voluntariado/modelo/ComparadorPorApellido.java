package voluntariado.modelo;

import java.util.Comparator;

// Ordena personas por apellido y, si empatan, por nombre (sin distinguir mayusculas).
public class ComparadorPorApellido implements Comparator<Persona> {

    @Override
    public int compare(Persona a, Persona b) {
        int porApellido = a.getApellido().compareToIgnoreCase(b.getApellido());
        return porApellido != 0 ? porApellido : a.getNombre().compareToIgnoreCase(b.getNombre());
    }
}
