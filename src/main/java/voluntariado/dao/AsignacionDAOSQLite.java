package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.RepositorioEnMemoria;

public class AsignacionDAOSQLite implements AsignacionDAO {

    // Si el QR ya existe solo cambia el estado: el voluntario y el punto de una asignacion no se editan.
    private static final String UPSERT_ASIGNACION = """
            INSERT INTO asignacion (codigo_qr, dni, codigo_punto, estado) VALUES (?, ?, ?, ?)
            ON CONFLICT (codigo_qr) DO UPDATE SET estado = excluded.estado""";

    private final ConexionBD conexionBD;

    public AsignacionDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public void guardar(Asignacion asignacion) {
        if (asignacion == null) {
            throw new DatosInvalidosException("La asignacion es obligatoria");
        }
        try (Connection c = conexionBD.abrir(); PreparedStatement ps = c.prepareStatement(UPSERT_ASIGNACION)) {
            ps.setString(1, asignacion.getCodigoQr());
            ps.setString(2, asignacion.getVoluntario().getDni());
            ps.setString(3, asignacion.getPunto().getCodigo());
            ps.setString(4, asignacion.getEstado().name());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (Errores.esViolacionDeRestriccion(e)) {
                throw new PersistenciaException("No se pudo guardar la asignacion: el voluntario ya tiene una "
                        + "asignacion confirmada, o el voluntario o el punto no estan guardados", e);
            }
            throw new PersistenciaException("No se pudo guardar la asignacion", e);
        }
    }

    @Override
    public List<Asignacion> listarTodas(RepositorioEnMemoria repositorio) {
        if (repositorio == null) {
            throw new DatosInvalidosException("El repositorio es obligatorio");
        }
        List<Asignacion> asignaciones = new ArrayList<>();
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT codigo_qr, dni, codigo_punto, estado FROM asignacion ORDER BY codigo_punto, dni");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                asignaciones.add(new Asignacion(
                        repositorio.buscarVoluntario(rs.getString("dni")),
                        repositorio.buscarPunto(rs.getString("codigo_punto")),
                        rs.getString("codigo_qr"),
                        EstadoAsignacion.valueOf(rs.getString("estado"))));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer las asignaciones", e);
        }
        return asignaciones;
    }

    @Override
    public void eliminar(String codigoQr) {
        Errores.validarClave(codigoQr, "codigo QR");
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement("DELETE FROM asignacion WHERE codigo_qr = ?")) {
            ps.setString(1, codigoQr);
            if (ps.executeUpdate() == 0) {
                throw new DatosInvalidosException("La asignacion no esta registrada");
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo borrar la asignacion " + codigoQr, e);
        }
    }
}
