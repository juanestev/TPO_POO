package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import voluntariado.controlador.ControladorPuntos;
import voluntariado.modelo.PuntoDeControl;

// Pantalla 5.5: alta, edicion y baja de puntos de control, con sus zonas cercanas (para la reasignacion).
public class PanelPuntos extends JPanel implements Refrescable {

    private final ControladorPuntos controlador;
    private final ModeloTablaLista<PuntoDeControl> modelo = new ModeloTablaLista<>(
            new String[] {"Codigo", "Zona", "Dotacion requerida", "Zonas cercanas"},
            (p, columna) -> switch (columna) {
                case 0 -> p.getCodigo();
                case 1 -> p.getZona();
                case 2 -> p.getDotacionRequerida();
                default -> String.join(", ", p.getZonasCercanas());
            });
    private final JTable tabla = new JTable(modelo);
    private final JTextField codigo = new JTextField(12);
    private final JTextField zona = new JTextField(12);
    private final JSpinner dotacion = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
    private final JTextField cercanas = new JTextField(12);
    private boolean editando;

    public PanelPuntos(ControladorPuntos controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                cargarEnFormulario(modelo.getFila(tabla.getSelectedRow()));
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Punto de control"));
        int fila = 0;
        Utilidades.fila(form, fila++, "Codigo", codigo);
        Utilidades.fila(form, fila++, "Zona", zona);
        Utilidades.fila(form, fila++, "Dotacion requerida", dotacion);
        Utilidades.fila(form, fila++, "Zonas cercanas", cercanas);
        Utilidades.filaCompleta(form, fila++, new JLabel("<html><i>Separadas por coma. Son las zonas desde donde se<br>"
                + "puede traer gente en una emergencia.</i></html>"));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        JButton nuevo = new JButton("Nuevo");
        JButton guardar = new JButton("Guardar");
        JButton eliminar = new JButton("Eliminar");
        nuevo.addActionListener(e -> nuevo());
        guardar.addActionListener(e -> guardar());
        eliminar.addActionListener(e -> eliminar());
        botones.add(nuevo);
        botones.add(guardar);
        botones.add(eliminar);
        Utilidades.filaCompleta(form, fila, botones);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(form, BorderLayout.NORTH);
        add(contenedor, BorderLayout.EAST);
        refrescar();
    }

    @Override
    public void refrescar() {
        modelo.setFilas(controlador.listar());
    }

    void nuevo() {
        editando = false;
        tabla.clearSelection();
        codigo.setEnabled(true);
        codigo.setText("");
        zona.setText("");
        cercanas.setText("");
        dotacion.setValue(1);
    }

    private void cargarEnFormulario(PuntoDeControl p) {
        editando = true;
        codigo.setText(p.getCodigo());
        codigo.setEnabled(false);
        zona.setText(p.getZona());
        dotacion.setValue(p.getDotacionRequerida());
        cercanas.setText(String.join(", ", p.getZonasCercanas()));
    }

    void guardar() {
        try {
            List<String> zonas = new ArrayList<>();
            for (String parte : cercanas.getText().split(",")) {
                if (!parte.isBlank()) {
                    zonas.add(parte.trim());
                }
            }
            int dotacionElegida = (Integer) dotacion.getValue();
            String codigoGuardado = codigo.getText().trim();
            if (editando) {
                controlador.modificar(codigoGuardado, zona.getText(), dotacionElegida, zonas);
            } else {
                controlador.alta(codigoGuardado, zona.getText(), dotacionElegida, zonas);
            }
            refrescar();
            nuevo();
            Utilidades.info(this, "Punto " + codigoGuardado + " guardado");
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    void eliminar() {
        if (!editando) {
            Utilidades.error(this, new IllegalArgumentException("Elegi un punto de la tabla"));
            return;
        }
        if (!Utilidades.confirmar(this, "Borrar el punto " + codigo.getText() + "?")) {
            return;
        }
        try {
            controlador.baja(codigo.getText().trim());
            refrescar();
            nuevo();
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }
}
