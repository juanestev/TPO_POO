package voluntariado.modelo;

public class Coordinador extends Persona {

    public Coordinador(String dni, String nombre, String apellido, String telefono, String mail) {
        super(dni, nombre, apellido, telefono, mail);
    }

    @Override
    public String getRol() {
        return "COORDINADOR";
    }
}
