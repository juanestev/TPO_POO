package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import voluntariado.controlador.ControladorCobertura;
import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.PuntoDeControl;

// Pantalla 5.7: panel de cobertura. Es el OBSERVADOR (patron Observer): el ControladorCobertura le avisa
// cada vez que cambia una asignacion o una entrada/salida, y la fila del punto se actualiza sola.
public class PanelCobertura extends JPanel implements VistaCobertura, Refrescable, AutoCloseable {

    private record Fila(PuntoDeControl punto, int presentes, EstadoCobertura estado) { }

    private static final Color VERDE = new Color(0xB7E1B5);
    private static final Color ROJO = new Color(0xF4B6B6);
    private static final Color AMARILLO = new Color(0xFFF0A8);

    private final Map<String, Fila> filas = new TreeMap<>();
    private final ModeloTablaLista<Fila> modelo = new ModeloTablaLista<>(
            new String[] {"Punto", "Zona", "Requeridos", "Presentes", "Estado"},
            (f, columna) -> switch (columna) {
                case 0 -> f.punto().getCodigo();
                case 1 -> f.punto().getZona();
                case 2 -> f.punto().getDotacionRequerida();
                case 3 -> f.presentes();
                default -> texto(f.estado());
            });
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = new JLabel(" ");
    private final ControladorCobertura controlador;

    public PanelCobertura(Function<VistaCobertura, ControladorCobertura> fabrica) {
        controlador = fabrica.apply(this);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla.setRowHeight(28);
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                    boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                if (!seleccionada) {
                    c.setBackground(color(modelo.getFila(t.convertRowIndexToModel(fila)).estado()));
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        leyenda.add(cuadro("Cubierto", VERDE));
        leyenda.add(cuadro("Deficit (falta gente)", ROJO));
        leyenda.add(cuadro("Sobrante", AMARILLO));
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(leyenda, BorderLayout.WEST);
        sur.add(resumen, BorderLayout.EAST);
        add(sur, BorderLayout.SOUTH);
        refrescar();
    }

    private static JLabel cuadro(String texto, Color color) {
        JLabel etiqueta = new JLabel("  " + texto + "  ");
        etiqueta.setOpaque(true);
        etiqueta.setBackground(color);
        etiqueta.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        return etiqueta;
    }

    private static Color color(EstadoCobertura estado) {
        return switch (estado) {
            case CUBIERTO -> VERDE;
            case DEFICIT -> ROJO;
            case SOBRANTE -> AMARILLO;
        };
    }

    private static String texto(EstadoCobertura estado) {
        return switch (estado) {
            case CUBIERTO -> "Cubierto";
            case DEFICIT -> "Deficit";
            case SOBRANTE -> "Sobrante";
        };
    }

    // Lo llama el controlador (Observer) cuando cambia un punto.
    @Override
    public void actualizar(PuntoDeControl punto, int presentes, EstadoCobertura estado) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> actualizar(punto, presentes, estado));
            return;
        }
        filas.put(punto.getCodigo(), new Fila(punto, presentes, estado));
        mostrar();
    }

    @Override
    public void refrescar() {
        filas.clear();
        controlador.refrescarTodos();
        mostrar();
    }

    private void mostrar() {
        modelo.setFilas(new ArrayList<>(filas.values()));
        long deficit = filas.values().stream().filter(f -> f.estado() == EstadoCobertura.DEFICIT).count();
        resumen.setText("Puntos: " + filas.size() + "  |  En deficit: " + deficit + " ");
    }

    // Para los tests: estado que muestra hoy la fila de un punto.
    EstadoCobertura estadoDe(String codigoPunto) {
        Fila fila = filas.get(codigoPunto);
        return fila == null ? null : fila.estado();
    }

    @Override
    public void close() {
        controlador.close();
    }
}
