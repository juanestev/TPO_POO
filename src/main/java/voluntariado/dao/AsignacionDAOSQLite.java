package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.AsignacionHistorica;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.EstadoAsignacion;
import voluntariado.modelo.RepositorioEnMemoria;

public class AsignacionDAOSQLite implements AsignacionDAO {

    // Si el QR ya existe solo cambia el estado: el voluntario y el punto de una asignacion no se editan.
    private static final String UPSERT_ASIGNACION = """
            INSERT INTO asignacion (codigo_qr, dni, codigo_punto, estado) VALUES (?, ?, ?, ?)
            ON CONFLICT (codigo_qr) DO UPDATE SET estado = excluded.estado""";

    private static final String INSERT_ASIGNACION =
            "INSERT INTO asignacion (codigo_qr, dni, codigo_punto, estado) VALUES (?, ?, ?, ?)";

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
    public void reasignar(Asignacion anterior, Asignacion nueva) {
        if (anterior == null || nueva == null || anterior.getEstado() != EstadoAsignacion.CONFIRMADA
                || nueva.getEstado() != EstadoAsignacion.CONFIRMADA
                || !anterior.getVoluntario().getDni().equals(nueva.getVoluntario().getDni())
                || anterior.getCodigoQr().equals(nueva.getCodigoQr())) {
            throw new DatosInvalidosException("El cambio necesita dos asignaciones confirmadas del mismo voluntario y QR distintos");
        }
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO asignacion_historial
                            (codigo_qr, dni, codigo_punto, entrada, salida, fecha_cambio)
                        SELECT a.codigo_qr, a.dni, a.codigo_punto, r.entrada, r.salida, ?
                        FROM asignacion a LEFT JOIN asistencia r ON r.codigo_qr = a.codigo_qr
                        WHERE a.codigo_qr = ? AND a.dni = ? AND a.estado = 'CONFIRMADA'""")) {
                    ps.setString(1, LocalDateTime.now().toString());
                    ps.setString(2, anterior.getCodigoQr());
                    ps.setString(3, anterior.getVoluntario().getDni());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("La asignacion anterior no esta guardada");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM asignacion WHERE codigo_qr = ?")) {
                    ps.setString(1, anterior.getCodigoQr());
                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("La asignacion anterior no esta guardada");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(INSERT_ASIGNACION)) {
                    ps.setString(1, nueva.getCodigoQr());
                    ps.setString(2, nueva.getVoluntario().getDni());
                    ps.setString(3, nueva.getPunto().getCodigo());
                    ps.setString(4, nueva.getEstado().name());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo reasignar al voluntario", e);
        }
    }

    @Override
    public List<AsignacionHistorica> listarHistorial() {
        List<AsignacionHistorica> historial = new ArrayList<>();
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement("""
                        SELECT codigo_qr, dni, codigo_punto, entrada, salida, fecha_cambio
                        FROM asignacion_historial ORDER BY fecha_cambio DESC, codigo_qr""");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String entrada = rs.getString("entrada");
                String salida = rs.getString("salida");
                historial.add(new AsignacionHistorica(rs.getString("codigo_qr"), rs.getString("dni"),
                        rs.getString("codigo_punto"), entrada == null ? null : LocalDateTime.parse(entrada),
                        salida == null ? null : LocalDateTime.parse(salida),
                        LocalDateTime.parse(rs.getString("fecha_cambio"))));
            }
        } catch (SQLException | DateTimeParseException e) {
            throw new PersistenciaException("No se pudo leer el historial de asignaciones", e);
        }
        return historial;
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
