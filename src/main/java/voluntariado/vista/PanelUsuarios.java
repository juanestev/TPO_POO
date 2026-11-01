package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import voluntariado.controlador.ControladorUsuarios;
import voluntariado.seguridad.Rol;
import voluntariado.seguridad.Usuario;

// Pantalla 5.9: gestion de usuarios y roles (solo administrador). Las claves nunca se muestran.
public class PanelUsuarios extends JPanel implements Refrescable {

    private final ControladorUsuarios controlador;
    private final ModeloTablaLista<Usuario> modelo = new ModeloTablaLista<>(
            new String[] {"Usuario", "Rol", "Estado"},
            (u, columna) -> switch (columna) {
                case 0 -> u.getNombreUsuario();
                case 1 -> u.getRol().name().charAt(0) + u.getRol().name().substring(1).toLowerCase();
                default -> u.isActivo() ? "Activo" : "Inactivo";
            });
    private final JTable tabla = new JTable(modelo);
    private final JTextField nombre = new JTextField(14);
    private final JPasswordField clave = new JPasswordField(14);
    private final JComboBox<Rol> rol = new JComboBox<>(Rol.values());

    public PanelUsuarios(ControladorUsuarios controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                Usuario u = modelo.getFila(tabla.getSelectedRow());
                nombre.setText(u.getNombreUsuario());
                rol.setSelectedItem(u.getRol());
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Usuario"));
        int fila = 0;
        Utilidades.fila(form, fila++, "Nombre de usuario", nombre);
        Utilidades.fila(form, fila++, "Clave", clave);
        Utilidades.fila(form, fila++, "Rol", rol);
        Utilidades.filaCompleta(form, fila++, new JLabel("<html><i>Clave: minimo 8 caracteres con letras y numeros.<br>"
                + "Sirve para crear el usuario o para restablecer la clave.</i></html>"));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        botones.add(boton("Crear", this::crear));
        botones.add(boton("Cambiar rol", this::cambiarRol));
        Utilidades.filaCompleta(form, fila++, botones);
        JPanel botones2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        botones2.add(boton("Activar / desactivar", this::alternarEstado));
        botones2.add(boton("Restablecer clave", this::restablecerClave));
        botones2.add(boton("Eliminar", this::eliminar));
        Utilidades.filaCompleta(form, fila, botones2);
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(form, BorderLayout.NORTH);
        add(contenedor, BorderLayout.EAST);
        refrescar();
    }

    private JButton boton(String texto, Runnable accion) {
        JButton boton = new JButton(texto);
        boton.addActionListener(e -> {
            try {
                accion.run();
            } catch (RuntimeException ex) {
                Utilidades.error(this, ex);
            }
        });
        return boton;
    }

    @Override
    public void refrescar() {
        modelo.setFilas(controlador.listar());
    }

    private void crear() {
        controlador.crear(nombre.getText(), new String(clave.getPassword()), (Rol) rol.getSelectedItem());
        clave.setText("");
        refrescar();
    }

    private void cambiarRol() {
        controlador.cambiarRol(elegido(), (Rol) rol.getSelectedItem());
        refrescar();
    }

    private void alternarEstado() {
        String usuario = elegido();
        boolean activo = modelo.getFila(tabla.getSelectedRow()).isActivo();
        controlador.cambiarEstado(usuario, !activo);
        refrescar();
    }

    private void restablecerClave() {
        controlador.restablecerClave(elegido(), new String(clave.getPassword()));
        clave.setText("");
        Utilidades.info(this, "Clave restablecida");
    }

    private void eliminar() {
        String usuario = elegido();
        if (Utilidades.confirmar(this, "Eliminar al usuario " + usuario + "?")) {
            controlador.eliminar(usuario);
            refrescar();
        }
    }

    private String elegido() {
        if (tabla.getSelectedRow() < 0) {
            throw new IllegalArgumentException("Elegi un usuario de la tabla");
        }
        return modelo.getFila(tabla.getSelectedRow()).getNombreUsuario();
    }
}
