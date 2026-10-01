package voluntariado;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Punto de entrada. Por ahora solo abre una ventana vacia para comprobar que el proyecto compila y corre.
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
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Sistema de Gestion de Voluntariado");
            ventana.add(new JLabel("Proyecto base OK", SwingConstants.CENTER));
            ventana.setSize(400, 200);
            ventana.setLocationRelativeTo(null);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
    }
}
