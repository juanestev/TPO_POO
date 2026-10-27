package voluntariado.dao;

import java.sql.SQLException;
import voluntariado.modelo.DatosInvalidosException;

// Ayuda a traducir errores de SQLite a mensajes entendibles.
final class Errores {

    // Codigo de SQLite para "se violo una restriccion" (PK, UNIQUE, CHECK o clave foranea).
    // Los codigos extendidos (ej: 787 = clave foranea) guardan el codigo base en los 8 bits bajos.
    private static final int SQLITE_CONSTRAINT = 19;

    private Errores() { }

    static boolean esViolacionDeRestriccion(SQLException e) {
        return (e.getErrorCode() & 0xFF) == SQLITE_CONSTRAINT;
    }

    static String validarClave(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
