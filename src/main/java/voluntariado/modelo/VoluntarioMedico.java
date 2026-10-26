package voluntariado.modelo;

public class VoluntarioMedico extends Voluntario {

    public VoluntarioMedico(String dni, String nombre, String apellido, String telefono, String mail) {
        super(dni, nombre, apellido, telefono, mail);
    }

    @Override
    public String getRol() {
        return "MEDICO";
    }
}
