package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.Voluntario;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.modelo.VoluntarioSeguridad;

public class VoluntarioDAOSQLite implements VoluntarioDAO {

    private static final String UPSERT_VOLUNTARIO = """
            INSERT INTO voluntario (dni, tipo, nombre, apellido, telefono, mail) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (dni) DO UPDATE SET tipo = excluded.tipo, nombre = excluded.nombre,
                apellido = excluded.apellido, telefono = excluded.telefono, mail = excluded.mail""";

    private final ConexionBD conexionBD;

    public VoluntarioDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public void guardar(Voluntario voluntario) {
        if (voluntario == null) {
            throw new DatosInvalidosException("El voluntario es obligatorio");
        }
        // Todo en una transaccion: o se guarda el voluntario completo, o no se guarda nada.
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(UPSERT_VOLUNTARIO)) {
                    ps.setString(1, voluntario.getDni());
                    ps.setString(2, voluntario.getRol());
                    ps.setString(3, voluntario.getNombre());
                    ps.setString(4, voluntario.getApellido());
                    ps.setString(5, voluntario.getTelefono());
                    ps.setString(6, voluntario.getMail());
                    ps.executeUpdate();
                }
                // Las listas hijas se reemplazan enteras: es mas simple que calcular que cambio.
                ejecutar(c, "DELETE FROM voluntario_habilidad WHERE dni = ?", voluntario.getDni());
                ejecutar(c, "DELETE FROM voluntario_disponibilidad WHERE dni = ?", voluntario.getDni());
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO voluntario_habilidad (dni, habilidad) VALUES (?, ?)")) {
                    for (Habilidad habilidad : voluntario.getHabilidades()) {
                        ps.setString(1, voluntario.getDni());
                        ps.setString(2, habilidad.name());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO voluntario_disponibilidad (dni, dia, desde, hasta) VALUES (?, ?, ?, ?)")) {
                    for (Disponibilidad d : voluntario.getDisponibilidades()) {
                        ps.setString(1, voluntario.getDni());
                        ps.setString(2, d.getDia().name());
                        ps.setString(3, d.getDesde().toString());
                        ps.setString(4, d.getHasta().toString());
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
            throw new PersistenciaException("No se pudo guardar el voluntario " + voluntario.getDni(), e);
        }
    }

    @Override
    public Voluntario buscarPorDni(String dni) {
        List<Voluntario> encontrados = leer(" WHERE dni = ?", Errores.validarClave(dni, "DNI"));
        if (encontrados.isEmpty()) {
            throw new DatosInvalidosException("El voluntario no esta registrado");
        }
        return encontrados.get(0);
    }

    @Override
    public List<Voluntario> listarTodos() {
        return leer("", null);
    }

    @Override
    public void eliminar(String dni) {
        Errores.validarClave(dni, "DNI");
        try (Connection c = conexionBD.abrir()) {
            if (ejecutar(c, "DELETE FROM voluntario WHERE dni = ?", dni) == 0) {
                throw new DatosInvalidosException("El voluntario no esta registrado");
            }
        } catch (SQLException e) {
            if (Errores.esViolacionDeRestriccion(e)) {
                throw new PersistenciaException("No se puede borrar: el voluntario tiene asignaciones", e);
            }
            throw new PersistenciaException("No se pudo borrar el voluntario " + dni, e);
        }
    }

    // Tres consultas en total (voluntarios, habilidades, disponibilidades), sin importar cuantos voluntarios haya.
    // Hacer una consulta de hijos por cada voluntario seria el clasico problema "N+1".
    private List<Voluntario> leer(String filtro, String dni) {
        Map<String, Voluntario> porDni = new LinkedHashMap<>();
        try (Connection c = conexionBD.abrir()) {
            try (PreparedStatement ps = preparar(c,
                    "SELECT dni, tipo, nombre, apellido, telefono, mail FROM voluntario" + filtro + " ORDER BY dni", dni);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Voluntario v = crear(rs.getString("tipo"), rs.getString("dni"), rs.getString("nombre"),
                            rs.getString("apellido"), rs.getString("telefono"), rs.getString("mail"));
                    porDni.put(v.getDni(), v);
                }
            }
            if (porDni.isEmpty()) {
                return List.of();
            }
            try (PreparedStatement ps = preparar(c,
                    "SELECT dni, habilidad FROM voluntario_habilidad" + filtro, dni);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porDni.get(rs.getString("dni")).agregarHabilidad(Habilidad.valueOf(rs.getString("habilidad")));
                }
            }
            try (PreparedStatement ps = preparar(c,
                    "SELECT dni, dia, desde, hasta FROM voluntario_disponibilidad" + filtro + " ORDER BY id", dni);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    porDni.get(rs.getString("dni")).agregarDisponibilidad(new Disponibilidad(
                            DayOfWeek.valueOf(rs.getString("dia")),
                            LocalTime.parse(rs.getString("desde")),
                            LocalTime.parse(rs.getString("hasta"))));
                }
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer los voluntarios", e);
        }
        return new ArrayList<>(porDni.values());
    }

    // La columna "tipo" dice que subclase crear (es el mismo texto que devuelve getRol()).
    private Voluntario crear(String tipo, String dni, String nombre, String apellido, String telefono, String mail) {
        return switch (tipo) {
            case "MEDICO" -> new VoluntarioMedico(dni, nombre, apellido, telefono, mail);
            case "SEGURIDAD" -> new VoluntarioSeguridad(dni, nombre, apellido, telefono, mail);
            default -> throw new PersistenciaException("Tipo de voluntario desconocido en la base: " + tipo);
        };
    }

    private PreparedStatement preparar(Connection c, String sql, String parametro) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        if (parametro != null) {
            ps.setString(1, parametro);
        }
        return ps;
    }

    private int ejecutar(Connection c, String sql, String parametro) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, parametro);
            return ps.executeUpdate();
        }
    }
}
