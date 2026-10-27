package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.PuntoDeControl;

public class PuntoDeControlDAOSQLite implements PuntoDeControlDAO {

    private static final String UPSERT_PUNTO = """
            INSERT INTO punto_control (codigo, zona, dotacion_requerida) VALUES (?, ?, ?)
            ON CONFLICT (codigo) DO UPDATE SET zona = excluded.zona,
                dotacion_requerida = excluded.dotacion_requerida""";

    private final ConexionBD conexionBD;

    public PuntoDeControlDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public void guardar(PuntoDeControl punto) {
        if (punto == null) {
            throw new DatosInvalidosException("El punto es obligatorio");
        }
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(UPSERT_PUNTO)) {
                    ps.setString(1, punto.getCodigo());
                    ps.setString(2, punto.getZona());
                    ps.setInt(3, punto.getDotacionRequerida());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM punto_zona_cercana WHERE codigo = ?")) {
                    ps.setString(1, punto.getCodigo());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO punto_zona_cercana (codigo, zona) VALUES (?, ?)")) {
                    for (String zona : punto.getZonasCercanas()) {
                        ps.setString(1, punto.getCodigo());
                        ps.setString(2, zona);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo guardar el punto " + punto.getCodigo(), e);
        }
    }

    @Override
    public PuntoDeControl buscarPorCodigo(String codigo) {
        List<PuntoDeControl> encontrados = leer(" WHERE codigo = ?", Errores.validarClave(codigo, "codigo"));
        if (encontrados.isEmpty()) {
            throw new DatosInvalidosException("El punto no esta registrado");
        }
        return encontrados.get(0);
    }

    @Override
    public List<PuntoDeControl> listarTodos() {
        return leer("", null);
    }

    @Override
    public void eliminar(String codigo) {
        Errores.validarClave(codigo, "codigo");
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement("DELETE FROM punto_control WHERE codigo = ?")) {
            ps.setString(1, codigo);
            if (ps.executeUpdate() == 0) {
                throw new DatosInvalidosException("El punto no esta registrado");
            }
        } catch (SQLException e) {
            if (Errores.esViolacionDeRestriccion(e)) {
                throw new PersistenciaException("No se puede borrar: el punto tiene asignaciones", e);
            }
            throw new PersistenciaException("No se pudo borrar el punto " + codigo, e);
        }
    }

    // Dos consultas: puntos y zonas cercanas (sin una consulta extra por cada punto).
    private List<PuntoDeControl> leer(String filtro, String codigo) {
        Map<String, PuntoDeControl> porCodigo = new LinkedHashMap<>();
        try (Connection c = conexionBD.abrir()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT codigo, zona, dotacion_requerida FROM punto_control" + filtro + " ORDER BY codigo")) {
                if (codigo != null) {
                    ps.setString(1, codigo);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PuntoDeControl p = new PuntoDeControl(rs.getString("codigo"), rs.getString("zona"),
                                rs.getInt("dotacion_requerida"));
                        porCodigo.put(p.getCodigo(), p);
                    }
                }
            }
            if (porCodigo.isEmpty()) {
                return List.of();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT codigo, zona FROM punto_zona_cercana" + filtro + " ORDER BY codigo, rowid")) {
                if (codigo != null) {
                    ps.setString(1, codigo);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        porCodigo.get(rs.getString("codigo")).agregarZonaCercana(rs.getString("zona"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer los puntos de control", e);
        }
        return new ArrayList<>(porCodigo.values());
    }
}
