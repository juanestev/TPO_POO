package voluntariado;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import voluntariado.dao.AsignacionDAOSQLite;
import voluntariado.dao.CargaInicial;
import voluntariado.dao.ConexionBD;
import voluntariado.dao.DatosDePrueba;
import voluntariado.dao.PersistenciaException;
import voluntariado.dao.PuntoDeControlDAOSQLite;
import voluntariado.dao.VoluntarioDAOSQLite;
import voluntariado.modelo.RepositorioEnMemoria;

// Punto de entrada. Abre la base, carga los datos en memoria y muestra una ventana de prueba con lo cargado.
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
        RepositorioEnMemoria repositorio;
        try {
            repositorio = cargarDatos();
        } catch (PersistenciaException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String resumen = "Voluntarios: " + repositorio.getVoluntarios().size();
        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame("Sistema de Gestion de Voluntariado");
            ventana.add(new JLabel(resumen, SwingConstants.CENTER));
            ventana.setSize(400, 200);
            ventana.setLocationRelativeTo(null);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
    }

    // Crea las tablas si faltan, carga datos de prueba si la base esta vacia y pasa todo a memoria.
    private static RepositorioEnMemoria cargarDatos() {
        ConexionBD conexion = new ConexionBD(ConexionBD.ARCHIVO_POR_DEFECTO);
        conexion.inicializarEsquema();
        PuntoDeControlDAOSQLite puntoDAO = new PuntoDeControlDAOSQLite(conexion);
        VoluntarioDAOSQLite voluntarioDAO = new VoluntarioDAOSQLite(conexion);
        DatosDePrueba.cargarSiEstaVacia(puntoDAO, voluntarioDAO);
        return new CargaInicial(puntoDAO, voluntarioDAO, new AsignacionDAOSQLite(conexion)).cargar();
    }
}
