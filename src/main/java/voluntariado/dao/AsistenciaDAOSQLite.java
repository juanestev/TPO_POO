package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.RegistroAsistencia;
import voluntariado.modelo.RepositorioEnMemoria;

public class AsistenciaDAOSQLite implements AsistenciaDAO {

    private static final String GUARDAR = """
            INSERT INTO asistencia (codigo_qr, entrada, salida) VALUES (?, ?, ?)
            ON CONFLICT (codigo_qr) DO UPDATE SET entrada = excluded.entrada, salida = excluded.salida""";

    private final ConexionBD conexionBD;

    public AsistenciaDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public void guardar(RegistroAsistencia registro) {
        if (registro == null || registro.getEntrada() == null) {
            throw new DatosInvalidosException("La asistencia necesita una entrada registrada");
        }
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(GUARDAR)) {
            ps.setString(1, registro.getAsignacion().getCodigoQr());
            ps.setString(2, registro.getEntrada().toString());
            ps.setString(3, registro.getSalida() == null ? null : registro.getSalida().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar la asistencia", e);
        }
    }

    @Override
    public List<RegistroAsistencia> listarTodas(RepositorioEnMemoria repositorio) {
        if (repositorio == null) {
            throw new DatosInvalidosException("El repositorio es obligatorio");
        }
        List<RegistroAsistencia> registros = new ArrayList<>();
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT codigo_qr, entrada, salida FROM asistencia ORDER BY entrada");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String entrada = rs.getString("entrada");
                String salida = rs.getString("salida");
                registros.add(new RegistroAsistencia(
                        repositorio.buscarAsignacionPorQr(rs.getString("codigo_qr")),
                        entrada == null ? null : LocalDateTime.parse(entrada),
                        salida == null ? null : LocalDateTime.parse(salida)));
            }
        } catch (SQLException | DateTimeParseException e) {
            throw new PersistenciaException("No se pudo leer la asistencia", e);
        }
        return registros;
    }
}
