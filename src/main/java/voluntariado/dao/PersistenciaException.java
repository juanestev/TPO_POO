package voluntariado.dao;

// Error al leer o guardar en la base. Envuelve la SQLException para que las capas de arriba
// no dependan de JDBC (igual idea que ArchivoException en UVA 6).
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
