package voluntariado.seguridad;

// Login fallido. El mensaje es siempre el mismo, sea por usuario inexistente, clave mala o usuario inactivo,
// para que nadie pueda descubrir que usuarios existen.
public class AutenticacionException extends RuntimeException {

    public AutenticacionException() {
        super("Usuario o clave incorrectos");
    }
}
