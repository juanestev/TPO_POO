package voluntariado.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

// Cada test arranca con una base nueva en un archivo temporal, asi no se pisan entre ellos
// ni tocan el voluntariado.db de la app.
abstract class BaseTemporal {

    protected ConexionBD conexion;
    private Path archivo;

    @BeforeEach
    void crearBase() throws IOException {
        archivo = Files.createTempFile("voluntariado-test", ".db");
        conexion = new ConexionBD(archivo.toString());
        conexion.inicializarEsquema();
    }

    @AfterEach
    void borrarBase() throws IOException {
        Files.deleteIfExists(archivo);
    }
}
