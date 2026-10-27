package voluntariado.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PermisosTest {

    @Test
    void coincideConLaMatrizDeLaActividadDos() {
        boolean[][] esperado = {
                {true, true, false, false, true},
                {false, false, true, true, true},
                {false, true, false, true, true}
        };
        Rol[] roles = Rol.values();
        Accion[] acciones = Accion.values();
        for (int r = 0; r < roles.length; r++) {
            for (int a = 0; a < acciones.length; a++) {
                assertEquals(esperado[r][a], Permisos.tienePermiso(roles[r], acciones[a]),
                        roles[r] + " / " + acciones[a]);
            }
        }
    }

    @Test
    void rechazaRolYAccionNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> Permisos.tienePermiso(null, Accion.VER_COBERTURA));
        assertThrows(IllegalArgumentException.class,
                () -> Permisos.tienePermiso(Rol.ADMINISTRADOR, null));
    }
}
