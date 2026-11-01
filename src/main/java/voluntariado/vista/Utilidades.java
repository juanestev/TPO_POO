package voluntariado.vista;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;

// Cosas chicas que repiten varias pantallas: mensajes, filas de formulario y textos en espanol.
final class Utilidades {

    private static final Locale ES = Locale.forLanguageTag("es-AR");

    private Utilidades() { }

    static void error(Component padre, Throwable e) {
        String mensaje = e instanceof RuntimeException && e.getMessage() != null
                ? e.getMessage() : "Ocurrio un error inesperado: " + e;
        JOptionPane.showMessageDialog(padre, mensaje, "Atencion", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirmar(Component padre, String pregunta) {
        return JOptionPane.showConfirmDialog(padre, pregunta, "Confirmar", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    // Agrega "etiqueta: campo" en la fila indicada de un panel con GridBagLayout.
    static void fila(JPanel panel, int fila, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, c);
    }

    // Una fila que ocupa todo el ancho (botones, listas...).
    static void filaCompleta(JPanel panel, int fila, JComponent componente) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = fila;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(componente, c);
    }

    static LocalTime hora(String texto) {
        try {
            return LocalTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new DatosInvalidosException("La hora debe tener el formato HH:mm, por ejemplo 08:00");
        }
    }

    static String dia(DayOfWeek dia) {
        String nombre = dia.getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }

    static String texto(Disponibilidad d) {
        return dia(d.getDia()) + " " + d.getDesde() + " a " + d.getHasta();
    }

    static String texto(Habilidad habilidad) {
        String nombre = habilidad.name().replace('_', ' ').toLowerCase(ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1);
    }

    static JComboBox<DayOfWeek> comboDias() {
        JComboBox<DayOfWeek> combo = new JComboBox<>(DayOfWeek.values());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean foco) {
                return super.getListCellRendererComponent(lista, valor == null ? "" : dia((DayOfWeek) valor),
                        indice, seleccionado, foco);
            }
        });
        return combo;
    }

    static JComboBox<Habilidad> comboHabilidades() {
        JComboBox<Habilidad> combo = new JComboBox<>(Habilidad.values());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                    boolean seleccionado, boolean foco) {
                return super.getListCellRendererComponent(lista, valor == null ? "" : texto((Habilidad) valor),
                        indice, seleccionado, foco);
            }
        });
        return combo;
    }
}
