package voluntariado.seguridad;

import java.util.Locale;
import voluntariado.modelo.DatosInvalidosException;

// Cuenta de acceso al sistema. Nunca guarda la contrasena: solo su hash y la sal con la que se calculo.
// El calculo del hash lo hace el modulo de seguridad (tarjeta 3.2); aca solo se guardan datos validados.
public class Usuario {

    // El nombre no cambia: es la identidad del usuario (como el DNI en Persona).
    private final String nombreUsuario;
    private String hashClave;
    private String sal;
    private Rol rol;
    private boolean activo = true;

    public Usuario(String nombreUsuario, String hashClave, String sal, Rol rol) {
        this.nombreUsuario = validarNombreUsuario(nombreUsuario);
        cambiarClave(hashClave, sal);
        setRol(rol);
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getHashClave() {
        return hashClave;
    }

    public String getSal() {
        return sal;
    }

    // Se cambian juntos: un hash solo sirve con la sal con la que se calculo.
    public void cambiarClave(String hashClave, String sal) {
        this.hashClave = validarTexto(hashClave, "hash de la clave");
        this.sal = validarTexto(sal, "sal");
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        if (rol == null) {
            throw new DatosInvalidosException("El rol es obligatorio");
        }
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    // Se desactiva en lugar de borrar para conservar el historial; un usuario inactivo no puede entrar.
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // La base compara sin distinguir mayusculas (COLLATE NOCASE), asi que el objeto hace lo mismo.
    @Override
    public final boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Usuario usuario
                && nombreUsuario.equalsIgnoreCase(usuario.nombreUsuario);
    }

    @Override
    public final int hashCode() {
        return nombreUsuario.toLowerCase(Locale.ROOT).hashCode();
    }

    // Sin espacios ni simbolos raros: se escribe en el login y se usa como clave en la base.
    private static String validarNombreUsuario(String nombreUsuario) {
        String nombre = validarTexto(nombreUsuario, "nombre de usuario");
        if (!nombre.matches("[A-Za-z0-9._-]{3,30}")) {
            throw new DatosInvalidosException(
                    "El nombre de usuario debe tener entre 3 y 30 caracteres: letras, numeros, punto, guion o guion bajo");
        }
        return nombre;
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
