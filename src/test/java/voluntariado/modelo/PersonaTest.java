package voluntariado.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PersonaTest {

    private static class PersonaDePrueba extends Persona {
        PersonaDePrueba(String dni, String nombre, String apellido, String telefono, String mail) {
            super(dni, nombre, apellido, telefono, mail);
        }

        @Override
        public String getRol() {
            return "PRUEBA";
        }
    }

    private PersonaDePrueba persona(String dni) {
        return new PersonaDePrueba(dni, " Ana ", " Perez ", "+54 (11) 1234-5678", "ana@ejemplo.com");
    }

    @Test
    void guardaDatosValidosNormalizados() {
        Persona persona = persona("12345678");
        assertEquals("Ana", persona.getNombre());
        assertEquals("Perez", persona.getApellido());
        assertEquals("+541112345678", persona.getTelefono());
        assertEquals("PRUEBA", persona.getRol());
    }

    @Test
    void rechazaDatosInvalidos() {
        assertThrows(DatosInvalidosException.class, () -> persona("12.345.678"));
        assertThrows(DatosInvalidosException.class,
                () -> new PersonaDePrueba("12345678", "Ana", "Perez", "123", "ana@ejemplo.com"));
        assertThrows(DatosInvalidosException.class,
                () -> new PersonaDePrueba("12345678", "Ana", "Perez", "1123456789", "correo-invalido"));
    }

    @Test
    void setterInvalidoConservaElValorAnterior() {
        Persona persona = persona("12345678");
        assertThrows(DatosInvalidosException.class, () -> persona.setMail("sin-arroba"));
        assertEquals("ana@ejemplo.com", persona.getMail());
    }

    @Test
    void dniDefineLaIdentidad() {
        Persona primera = persona("12345678");
        Persona segunda = persona("12345678");
        assertEquals(primera, segunda);
        assertEquals(primera.hashCode(), segunda.hashCode());
        assertNotEquals(primera, persona("87654321"));
    }
}
