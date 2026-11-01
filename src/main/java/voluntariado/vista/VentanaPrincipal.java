package voluntariado.vista;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import voluntariado.Sistema;
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Usuario;

// Pantalla 5.2: ventana principal. Cada rol ve solo las pestanas que su permiso habilita (matriz de permisos).
public class VentanaPrincipal extends JFrame {

    private final Sistema sistema;
    private final Runnable alCerrarSesion;
    private final JTabbedPane pestanas = new JTabbedPane();
    private PanelCobertura panelCobertura;

    public VentanaPrincipal(Sistema sistema, Runnable alCerrarSesion) {
        this.sistema = sistema;
        this.alCerrarSesion = alCerrarSesion;
        Usuario usuario = sistema.getSesion().getUsuario();
        setTitle("Sistema de Gestion de Voluntariado - " + usuario.getNombreUsuario()
                + " (" + usuario.getRol().name().toLowerCase() + ")");

        armarPestanas();
        pestanas.addChangeListener(e -> {
            if (pestanas.getSelectedComponent() instanceof Refrescable pantalla) {
                pantalla.refrescar();
            }
        });
        add(pestanas);
        setJMenuBar(armarMenu());

        // Igual que en UVA 6: al cerrar la ventana se hace el cierre ordenado antes de salir.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                salir();
            }
        });
        setSize(1100, 650);
        setLocationRelativeTo(null);
    }

    private void armarPestanas() {
        var sesion = sistema.getSesion();
        if (sesion.tienePermiso(Accion.EDITAR_VOLUNTARIOS_Y_PUNTOS)) {
            pestanas.addTab("Voluntarios", new PanelVoluntarios(sistema.getVoluntarios()));
            pestanas.addTab("Puntos de control", new PanelPuntos(sistema.getPuntos()));
        }
        if (sesion.tienePermiso(Accion.CONFIRMAR_ASIGNACIONES)) {
            pestanas.addTab("Asignacion", new PanelAsignacion(sistema.getAsignacion(), sistema.getPuntos()));
        }
        if (sesion.tienePermiso(Accion.REGISTRAR_ASISTENCIA)) {
            pestanas.addTab("Check-in / Check-out", new PanelAsistencia(sistema.getAsistencia()));
        }
        if (sesion.tienePermiso(Accion.VER_COBERTURA)) {
            panelCobertura = new PanelCobertura(sistema::crearControladorCobertura);
            pestanas.addTab("Cobertura", panelCobertura);
        }
        if (sesion.tienePermiso(Accion.GESTIONAR_USUARIOS)) {
            pestanas.addTab("Usuarios", new PanelUsuarios(sistema.getUsuarios()));
        }
    }

    private JMenuBar armarMenu() {
        JMenu menu = new JMenu("Sesion");
        JMenuItem cerrarSesion = new JMenuItem("Cerrar sesion");
        JMenuItem salir = new JMenuItem("Salir");
        cerrarSesion.addActionListener(e -> cerrarSesion());
        salir.addActionListener(e -> salir());
        menu.add(cerrarSesion);
        menu.add(salir);
        JMenuBar barra = new JMenuBar();
        barra.add(menu);
        return barra;
    }

    private void cerrarSesion() {
        sistema.getSesion().cerrar();
        liberar();
        dispose();
        alCerrarSesion.run();
    }

    private void salir() {
        sistema.alCerrar();
        liberar();
        dispose();
        System.exit(0);
    }

    // El panel de cobertura esta registrado como observador: hay que desregistrarlo al cerrar.
    private void liberar() {
        if (panelCobertura != null) {
            panelCobertura.close();
        }
    }
}
