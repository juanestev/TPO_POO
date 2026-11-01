package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import voluntariado.controlador.ControladorVoluntarios;
import voluntariado.controlador.ControladorVoluntarios.Tipo;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.Voluntario;

// Pantalla 5.4: alta, baja y modificacion de voluntarios. Solo habla con el ControladorVoluntarios.
public class PanelVoluntarios extends JPanel implements Refrescable {

    private final ControladorVoluntarios controlador;
    private final ModeloTablaLista<Voluntario> modelo = new ModeloTablaLista<>(
            new String[] {"DNI", "Apellido", "Nombre", "Tipo", "Telefono", "Habilidades"},
            (v, columna) -> switch (columna) {
                case 0 -> v.getDni();
                case 1 -> v.getApellido();
                case 2 -> v.getNombre();
                case 3 -> v.getRol().equals("MEDICO") ? "Medico" : "Seguridad";
                case 4 -> v.getTelefono();
                default -> v.getHabilidades().stream().map(Utilidades::texto).sorted()
                        .collect(Collectors.joining(", "));
            });
    private final JTable tabla = new JTable(modelo);

    private final JComboBox<Tipo> tipo = new JComboBox<>(Tipo.values());
    private final JTextField dni = new JTextField(12);
    private final JTextField nombre = new JTextField(12);
    private final JTextField apellido = new JTextField(12);
    private final JTextField telefono = new JTextField(12);
    private final JTextField mail = new JTextField(12);
    private final Map<Habilidad, JCheckBox> habilidades = new EnumMap<>(Habilidad.class);
    private final DefaultListModel<Disponibilidad> disponibilidades = new DefaultListModel<>();
    private final JList<Disponibilidad> listaDisponibilidades = new JList<>(disponibilidades);
    private final JComboBox<DayOfWeek> dia = Utilidades.comboDias();
    private final JTextField desde = new JTextField("08:00", 5);
    private final JTextField hasta = new JTextField("14:00", 5);
    private boolean editando;

    public PanelVoluntarios(ControladorVoluntarios controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                cargarEnFormulario(modelo.getFila(tabla.convertRowIndexToModel(tabla.getSelectedRow())));
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(armarFormulario(), BorderLayout.EAST);
        listaDisponibilidades.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean foco) {
                return super.getListCellRendererComponent(lista, Utilidades.texto((Disponibilidad) valor),
                        indice, seleccionado, foco);
            }
        });
        refrescar();
    }

    private JPanel armarFormulario() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Datos del voluntario"));
        int fila = 0;
        Utilidades.fila(form, fila++, "Tipo", tipo);
        Utilidades.fila(form, fila++, "DNI", dni);
        Utilidades.fila(form, fila++, "Nombre", nombre);
        Utilidades.fila(form, fila++, "Apellido", apellido);
        Utilidades.fila(form, fila++, "Telefono", telefono);
        Utilidades.fila(form, fila++, "Mail", mail);

        JPanel checks = new JPanel(new GridLayout(0, 2));
        for (Habilidad habilidad : Habilidad.values()) {
            JCheckBox check = new JCheckBox(Utilidades.texto(habilidad));
            habilidades.put(habilidad, check);
            checks.add(check);
        }
        checks.setBorder(BorderFactory.createTitledBorder("Habilidades"));
        Utilidades.filaCompleta(form, fila++, checks);

        JPanel turnos = new JPanel(new BorderLayout(4, 4));
        turnos.setBorder(BorderFactory.createTitledBorder("Disponibilidad horaria"));
        listaDisponibilidades.setVisibleRowCount(4);
        turnos.add(new JScrollPane(listaDisponibilidades), BorderLayout.CENTER);
        JPanel nuevoTurno = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton agregar = new JButton("Agregar");
        JButton quitar = new JButton("Quitar");
        agregar.addActionListener(e -> agregarDisponibilidad());
        quitar.addActionListener(e -> quitarDisponibilidad());
        nuevoTurno.add(dia);
        nuevoTurno.add(desde);
        nuevoTurno.add(hasta);
        nuevoTurno.add(agregar);
        nuevoTurno.add(quitar);
        turnos.add(nuevoTurno, BorderLayout.SOUTH);
        Utilidades.filaCompleta(form, fila++, turnos);

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
        return contenedor;
    }

    @Override
    public void refrescar() {
        modelo.setFilas(controlador.listar());
    }

    void nuevo() {
        editando = false;
        tabla.clearSelection();
        tipo.setEnabled(true);
        dni.setEnabled(true);
        for (JTextField campo : List.of(dni, nombre, apellido, telefono, mail)) {
            campo.setText("");
        }
        habilidades.values().forEach(check -> check.setSelected(false));
        disponibilidades.clear();
    }

    private void cargarEnFormulario(Voluntario v) {
        editando = true;
        tipo.setSelectedItem(v.getRol().equals("MEDICO") ? Tipo.MEDICO : Tipo.SEGURIDAD);
        tipo.setEnabled(false);
        dni.setText(v.getDni());
        dni.setEnabled(false);
        nombre.setText(v.getNombre());
        apellido.setText(v.getApellido());
        telefono.setText(v.getTelefono());
        mail.setText(v.getMail());
        habilidades.forEach((habilidad, check) -> check.setSelected(v.getHabilidades().contains(habilidad)));
        disponibilidades.clear();
        v.getDisponibilidades().forEach(disponibilidades::addElement);
    }

    void guardar() {
        try {
            Set<Habilidad> elegidas = EnumSet.noneOf(Habilidad.class);
            habilidades.forEach((habilidad, check) -> {
                if (check.isSelected()) {
                    elegidas.add(habilidad);
                }
            });
            List<Disponibilidad> turnos = Collections.list(disponibilidades.elements());
            if (editando) {
                controlador.modificar(dni.getText().trim(), nombre.getText(), apellido.getText(),
                        telefono.getText(), mail.getText(), elegidas, turnos);
            } else {
                controlador.alta((Tipo) tipo.getSelectedItem(), dni.getText(), nombre.getText(),
                        apellido.getText(), telefono.getText(), mail.getText(), elegidas, turnos);
            }
            String dniGuardado = dni.getText().trim();
            refrescar();
            nuevo();
            Utilidades.info(this, "Voluntario " + dniGuardado + " guardado");
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    void eliminar() {
        if (!editando) {
            Utilidades.error(this, new IllegalArgumentException("Elegi un voluntario de la tabla"));
            return;
        }
        if (!Utilidades.confirmar(this, "Dar de baja al voluntario " + dni.getText() + "?")) {
            return;
        }
        try {
            controlador.baja(dni.getText().trim());
            refrescar();
            nuevo();
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void agregarDisponibilidad() {
        try {
            Disponibilidad turno = new Disponibilidad((DayOfWeek) dia.getSelectedItem(),
                    Utilidades.hora(desde.getText()), Utilidades.hora(hasta.getText()));
            if (!Collections.list(disponibilidades.elements()).contains(turno)) {
                disponibilidades.addElement(turno);
            }
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void quitarDisponibilidad() {
        List<Disponibilidad> elegidas = new ArrayList<>(listaDisponibilidades.getSelectedValuesList());
        elegidas.forEach(disponibilidades::removeElement);
    }
}
