package voluntariado;

import java.awt.Font;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import voluntariado.dao.ConexionBD;
import voluntariado.dao.PersistenciaException;
import voluntariado.servicios.NotificadorMail;
import voluntariado.servicios.NotificadorMailConsola;
import voluntariado.servicios.NotificadorMailSmtp;
import voluntariado.vista.VentanaLogin;

// Punto de entrada. Arma el sistema (base + datos en memoria) y abre la pantalla de login.
public class Main {

    public static void main(String[] args) {
        // Look and Feel Nimbus con fuente un poco mas grande (igual que en UVA 6).
        try {
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(info.getClassName());
                }
            }
        } catch (Exception e) {
            // no pasa nada: queda el aspecto estandar
        }
        Sistema sistema;
        try {
            sistema = new Sistema(ConexionBD.ARCHIVO_POR_DEFECTO, crearNotificador());
        } catch (PersistenciaException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Sistema abierto = sistema;
        SwingUtilities.invokeLater(() -> new VentanaLogin(abierto).setVisible(true));
    }

    // Si estan definidas las variables de entorno del SMTP se mandan mails reales; si no, se simulan.
    // Las credenciales nunca se escriben en el codigo.
    private static NotificadorMail crearNotificador() {
        String host = System.getenv("VOLUNTARIADO_SMTP_HOST");
        String puerto = System.getenv("VOLUNTARIADO_SMTP_PUERTO");
        String usuario = System.getenv("VOLUNTARIADO_SMTP_USUARIO");
        String clave = System.getenv("VOLUNTARIADO_SMTP_CLAVE");
        if (host != null && puerto != null && usuario != null && clave != null) {
            try {
                return new NotificadorMailSmtp(host, Integer.parseInt(puerto), usuario, clave);
            } catch (IllegalArgumentException e) {
                System.err.println("Configuracion SMTP invalida; se usan mails simulados.");
            }
        }
        return new NotificadorMailConsola();
    }
}
