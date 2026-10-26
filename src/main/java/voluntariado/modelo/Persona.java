package voluntariado.modelo;

import java.util.Objects;

public abstract class Persona {

    // El DNI no cambia: se usa como identidad de la persona y como clave en colecciones.
    private final String dni;
    private String nombre;
    private String apellido;
    private String telefono;
    private String mail;

    protected Persona(String dni, String nombre, String apellido, String telefono, String mail) {
        this.dni = validarDni(dni);
        setNombre(nombre);
        setApellido(apellido);
        setTelefono(telefono);
        setMail(mail);
    }

    public String getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = validarTexto(nombre, "nombre");
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = validarTexto(apellido, "apellido");
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        if (telefono == null) {
            throw new DatosInvalidosException("El telefono es obligatorio");
        }
        String normalizado = telefono.replaceAll("[\\s()\\-]", "");
        if (!normalizado.matches("\\+?[0-9]{8,15}")) {
            throw new DatosInvalidosException("El telefono debe tener entre 8 y 15 digitos");
        }
        this.telefono = normalizado;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        String normalizado = validarTexto(mail, "mail");
        if (!normalizado.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new DatosInvalidosException("El mail no tiene un formato valido");
        }
        this.mail = normalizado;
    }

    public abstract String getRol();

    @Override
    public final boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Persona persona)) {
            return false;
        }
        return dni.equals(persona.dni);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(dni);
    }

    private static String validarDni(String dni) {
        if (dni == null) {
            throw new DatosInvalidosException("El DNI es obligatorio");
        }
        String normalizado = dni.trim();
        if (!normalizado.matches("[0-9]{7,8}")) {
            throw new DatosInvalidosException("El DNI debe tener 7 u 8 digitos, sin puntos");
        }
        return normalizado;
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
