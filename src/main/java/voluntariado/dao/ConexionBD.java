package voluntariado.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

// Sabe donde esta la base y como abrirla. Los DAO le piden una conexion por operacion:
// en SQLite abrir el archivo es barato y asi nunca queda una conexion colgada.
public class ConexionBD {

    public static final String ARCHIVO_POR_DEFECTO = "voluntariado.db";
    private static final String SCRIPT_ESQUEMA = "/db/schema.sql";

    private final String url;

    public ConexionBD(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            throw new PersistenciaException("La ruta de la base es obligatoria");
        }
        this.url = "jdbc:sqlite:" + rutaArchivo.trim();
    }

    public Connection abrir() {
        try {
            Connection conexion = DriverManager.getConnection(url);
            try (Statement st = conexion.createStatement()) {
                // SQLite trae las claves foraneas apagadas por defecto: hay que prenderlas en cada conexion.
                st.execute("PRAGMA foreign_keys = ON");
                // Si otra parte de la app esta escribiendo, espera hasta 5 s en vez de fallar al instante.
                st.execute("PRAGMA busy_timeout = 5000");
            }
            return conexion;
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo abrir la base " + url, e);
        }
    }

    // Crea las tablas que falten. Se puede llamar siempre al iniciar la app.
    public void inicializarEsquema() {
        String script = leerScript();
        try (Connection conexion = abrir(); Statement st = conexion.createStatement()) {
            conexion.setAutoCommit(false);
            for (String sentencia : script.split(";")) {
                if (!sentencia.isBlank()) {
                    st.execute(sentencia);
                }
            }
            conexion.commit();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo crear el esquema de la base", e);
        }
    }

    private String leerScript() {
        try (InputStream entrada = ConexionBD.class.getResourceAsStream(SCRIPT_ESQUEMA)) {
            if (entrada == null) {
                throw new PersistenciaException("No se encontro el script " + SCRIPT_ESQUEMA);
            }
            StringBuilder sinComentarios = new StringBuilder();
            for (String linea : new String(entrada.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!linea.trim().startsWith("--")) {
                    sinComentarios.append(linea).append('\n');
                }
            }
            return sinComentarios.toString();
        } catch (IOException e) {
            throw new PersistenciaException("No se pudo leer el script " + SCRIPT_ESQUEMA, e);
        }
    }
}
