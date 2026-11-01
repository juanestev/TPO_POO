package voluntariado.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import voluntariado.servicios.MensajeMail;

public class ColaMailsDAOSQLite implements ColaMailsDAO {

    private final ConexionBD conexionBD;

    public ColaMailsDAOSQLite(ConexionBD conexionBD) {
        if (conexionBD == null) {
            throw new PersistenciaException("La conexion es obligatoria");
        }
        this.conexionBD = conexionBD;
    }

    @Override
    public List<MensajeMail> listar() {
        List<MensajeMail> mensajes = new ArrayList<>();
        try (Connection c = conexionBD.abrir();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT destinatario, asunto, cuerpo, qr_png FROM mail_pendiente ORDER BY orden");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mensajes.add(new MensajeMail(rs.getString("destinatario"), rs.getString("asunto"),
                        rs.getString("cuerpo"), rs.getBytes("qr_png")));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron leer los avisos pendientes", e);
        }
        return mensajes;
    }

    @Override
    public void reemplazarTodos(List<MensajeMail> mensajes) {
        try (Connection c = conexionBD.abrir()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM mail_pendiente")) {
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO mail_pendiente (destinatario, asunto, cuerpo, qr_png) VALUES (?, ?, ?, ?)")) {
                    for (MensajeMail mensaje : mensajes) {
                        ps.setString(1, mensaje.destinatario());
                        ps.setString(2, mensaje.asunto());
                        ps.setString(3, mensaje.cuerpo());
                        ps.setBytes(4, mensaje.qrPng());
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
            throw new PersistenciaException("No se pudieron guardar los avisos pendientes", e);
        }
    }
}
