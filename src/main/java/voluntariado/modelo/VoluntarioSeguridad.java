package voluntariado.modelo;

public class VoluntarioSeguridad extends Voluntario {

    public VoluntarioSeguridad(String dni, String nombre, String apellido, String telefono, String mail) {
        super(dni, nombre, apellido, telefono, mail);
    }

    @Override
    public String getRol() {
        return "SEGURIDAD";
    }
}
