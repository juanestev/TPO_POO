package voluntariado;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import voluntariado.controlador.ControladorVoluntarios.Tipo;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.seguridad.PermisoDenegadoException;
import voluntariado.servicios.NotificadorMailConsola;

// Revision de seguridad (6.3): cada rol solo puede hacer lo que dice la matriz de permisos,
// aunque llegue al controlador por otro camino que la pantalla.
class PermisosControladoresTest {

    @TempDir
    Path carpeta;
    private Sistema sistema;

    @BeforeEach
    void preparar() {
        sistema = new Sistema(carpeta.resolve("permisos.db").toString(), new NotificadorMailConsola());
    }

    private void entrarComo(String usuario, String clave) {
        sistema.getSesion().iniciar(sistema.getAutenticacion().iniciarSesion(usuario, clave));
    }

    @Test
    void sinSesionNoSePuedeHacerNada() {
        assertThrows(PermisoDenegadoException.class, () -> sistema.getPuntos().alta("P9", "Zona", 2, List.of()));
        assertThrows(PermisoDenegadoException.class, () -> sistema.getUsuarios().listar());
    }

    @Test
    void elCoordinadorNoEditaVoluntariosNiPuntosNiUsuarios() {
        entrarComo("coordinador", "Coord2026");
        assertThrows(PermisoDenegadoException.class, () -> sistema.getPuntos().alta("P9", "Zona", 2, List.of()));
        assertThrows(PermisoDenegadoException.class, () -> sistema.getPuntos().baja("P1"));
        assertThrows(PermisoDenegadoException.class, () -> sistema.getVoluntarios().baja("30100100"));
        assertThrows(PermisoDenegadoException.class, () -> sistema.getUsuarios().listar());
    }

    @Test
    void elOperadorNoConfirmaAsignacionesNiGestionaUsuarios() {
        entrarComo("operador", "Oper2026x");
        assertThrows(PermisoDenegadoException.class,
                () -> sistema.getAsignacion().confirmar(List.of(), null, "x"));
        assertThrows(PermisoDenegadoException.class, () -> sistema.getUsuarios().crear("otro", "Clave1234", voluntariado.seguridad.Rol.OPERADOR));
        // Lo suyo si puede: dar de alta un voluntario.
        assertDoesNotThrow(() -> sistema.getVoluntarios().alta(Tipo.MEDICO, "40111222", "Luz", "Mora",
                "1144445555", "luz@ejemplo.com", Set.of(), List.of()));
    }

    @Test
    void elAdministradorNoRegistraAsistenciaNiConfirma() {
        entrarComo("admin", "Admin2026");
        assertThrows(PermisoDenegadoException.class,
                () -> sistema.getAsistencia().registrarEntradaManual("codigo-cualquiera", java.time.LocalDateTime.now()));
        assertThrows(PermisoDenegadoException.class,
                () -> sistema.getAsignacion().confirmar(List.of(), null, "x"));
        // Lo suyo si puede: ver usuarios y editar puntos.
        assertDoesNotThrow(() -> sistema.getUsuarios().listar());
        assertThrows(DatosInvalidosException.class, () -> sistema.getPuntos().alta("P9", "Zona", 0, List.of()));
    }
}
