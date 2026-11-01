package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import voluntariado.controlador.ControladorAsignacion;
import voluntariado.controlador.ControladorAsignacion.Resultado;
import voluntariado.controlador.ControladorPuntos;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.PuntoDeControl;

// Pantalla 5.6: el sistema SUGIERE (F1) o propone REASIGNAR por emergencia (F6) y el coordinador CONFIRMA.
public class PanelAsignacion extends JPanel implements Refrescable {

    private final ControladorAsignacion controlador;
    private final ControladorPuntos puntos;

    private final JComboBox<PuntoDeControl> punto = new JComboBox<>();
    private final JComboBox<Habilidad> habilidad = Utilidades.comboHabilidades();
    private final JComboBox<DayOfWeek> dia = Utilidades.comboDias();
    private final JTextField desde = new JTextField("08:00", 5);
    private final JTextField hasta = new JTextField("14:00", 5);
    private final JTextField encuentro = new JTextField("Puesto de acreditacion", 18);
    private final JLabel estado = new JLabel(" ");
    private final ModeloTablaLista<Asignacion> modelo = new ModeloTablaLista<>(
            new String[] {"Voluntario", "DNI", "Tipo", "Habilidades", "Movimiento"},
            (a, columna) -> switch (columna) {
                case 0 -> a.getVoluntario().getApellido() + ", " + a.getVoluntario().getNombre();
                case 1 -> a.getVoluntario().getDni();
                case 2 -> a.getVoluntario().getRol().equals("MEDICO") ? "Medico" : "Seguridad";
                case 3 -> a.getVoluntario().getHabilidades().stream().map(Utilidades::texto).sorted()
                        .collect(Collectors.joining(", "));
                default -> {
                    String actual = controlador().puntoActualDe(a.getVoluntario().getDni());
                    yield actual == null ? "Asignacion nueva" : "Reasignar desde " + actual;
                }
            });
    private final JTable tabla = new JTable(modelo);
    private Disponibilidad turnoSugerido;

    public PanelAsignacion(ControladorAsignacion controlador, ControladorPuntos puntos) {
        this.controlador = controlador;
        this.puntos = puntos;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Dos filas para que nada quede cortado si la ventana es angosta.
        JPanel filtros = new JPanel(new GridLayout(2, 1));
        filtros.setBorder(BorderFactory.createTitledBorder("Que necesitamos"));
        punto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean foco) {
                PuntoDeControl p = (PuntoDeControl) valor;
                return super.getListCellRendererComponent(lista,
                        p == null ? "" : p.getCodigo() + " - " + p.getZona(), indice, seleccionado, foco);
            }
        });
        punto.addActionListener(e -> actualizarEstado());
        JPanel primeraFila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        primeraFila.add(new JLabel("Punto"));
        primeraFila.add(punto);
        primeraFila.add(new JLabel("Habilidad"));
        primeraFila.add(habilidad);
        primeraFila.add(new JLabel("Turno"));
        primeraFila.add(dia);
        primeraFila.add(desde);
        primeraFila.add(new JLabel("a"));
        primeraFila.add(hasta);
        JPanel segundaFila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        segundaFila.add(new JLabel("Punto de encuentro"));
        segundaFila.add(encuentro);
        JButton sugerir = new JButton("Sugerir asignacion");
        JButton reasignar = new JButton("Sugerir reasignacion (emergencia)");
        sugerir.addActionListener(e -> sugerir(false));
        reasignar.addActionListener(e -> sugerir(true));
        segundaFila.add(sugerir);
        segundaFila.add(reasignar);
        filtros.add(primeraFila);
        filtros.add(segundaFila);

        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(filtros, BorderLayout.CENTER);
        arriba.add(estado, BorderLayout.SOUTH);
        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmar = new JButton("Confirmar seleccionadas (o todas)");
        confirmar.addActionListener(e -> confirmar());
        abajo.add(new JLabel("Elegi filas con Ctrl o Shift; si no elegis ninguna se confirman todas."));
        abajo.add(confirmar);
        add(abajo, BorderLayout.SOUTH);
        refrescar();
    }

    private ControladorAsignacion controlador() {
        return controlador;
    }

    @Override
    public void refrescar() {
        Object elegido = punto.getSelectedItem();
        punto.removeAllItems();
        for (PuntoDeControl p : puntos.listar()) {
            punto.addItem(p);
        }
        if (elegido != null) {
            punto.setSelectedItem(elegido);
        }
        modelo.setFilas(List.of());
        actualizarEstado();
    }

    private void actualizarEstado() {
        PuntoDeControl p = (PuntoDeControl) punto.getSelectedItem();
        estado.setText(p == null ? " " : " Dotacion requerida: " + p.getDotacionRequerida()
                + "  |  Confirmados: " + controlador.cantidadAsignados(p.getCodigo())
                + "  |  Zonas cercanas: " + (p.getZonasCercanas().isEmpty() ? "ninguna" : String.join(", ", p.getZonasCercanas())));
    }

    private void sugerir(boolean emergencia) {
        PuntoDeControl p = (PuntoDeControl) punto.getSelectedItem();
        if (p == null) {
            Utilidades.error(this, new IllegalArgumentException("Primero carga un punto de control"));
            return;
        }
        try {
            turnoSugerido = new Disponibilidad((DayOfWeek) dia.getSelectedItem(),
                    Utilidades.hora(desde.getText()), Utilidades.hora(hasta.getText()));
            Habilidad h = (Habilidad) habilidad.getSelectedItem();
            List<Asignacion> sugerencias = emergencia
                    ? controlador.sugerirReasignacion(p.getCodigo(), h, turnoSugerido)
                    : controlador.sugerir(p.getCodigo(), h, turnoSugerido);
            modelo.setFilas(sugerencias);
            if (sugerencias.isEmpty()) {
                Utilidades.info(this, emergencia
                        ? "No hay reasignaciones para proponer: el punto no tiene deficit o no hay gente presente "
                                + "con sobrante en zonas cercanas."
                        : "No hay sugerencias: el punto ya esta completo o no hay voluntarios libres con esa "
                                + "habilidad y ese turno.");
            }
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void confirmar() {
        if (modelo.getRowCount() == 0) {
            Utilidades.error(this, new IllegalArgumentException("No hay sugerencias para confirmar"));
            return;
        }
        List<Asignacion> elegidas = new ArrayList<>();
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                elegidas.add(modelo.getFila(i));
            }
        } else {
            for (int fila : filas) {
                elegidas.add(modelo.getFila(tabla.convertRowIndexToModel(fila)));
            }
        }
        try {
            Resultado r = controlador.confirmar(elegidas, turnoSugerido, encuentro.getText());
            StringBuilder mensaje = new StringBuilder("Confirmadas: " + r.confirmadas()
                    + " (reasignadas: " + r.reasignadas() + ")\n"
                    + "Avisos por mail pendientes de envio: " + r.avisosPendientes());
            if (!r.errores().isEmpty()) {
                mensaje.append("\n\nNo se pudieron confirmar:\n- ").append(String.join("\n- ", r.errores()));
            }
            Utilidades.info(this, mensaje.toString());
            modelo.setFilas(List.of());
            actualizarEstado();
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }
}
