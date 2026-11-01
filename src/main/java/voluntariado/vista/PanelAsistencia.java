package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.RegistroAsistencia;

// Pantalla 5.8: check-in / check-out. El QR se lee desde una imagen o, como plan B, se escribe el codigo a mano.
public class PanelAsistencia extends JPanel implements Refrescable {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final ControladorAsistencia controlador;
    private final JTextField codigo = new JTextField(34);
    private final JLabel mensaje = new JLabel(" ");
    private final ModeloTablaLista<Asignacion> modelo = new ModeloTablaLista<>(
            new String[] {"Voluntario", "Punto", "Estado", "Entrada", "Salida", "Codigo QR"},
            (a, columna) -> {
                RegistroAsistencia r = buscarRegistro(a);
                return switch (columna) {
                    case 0 -> a.getVoluntario().getApellido() + ", " + a.getVoluntario().getNombre();
                    case 1 -> a.getPunto().getCodigo() + " - " + a.getPunto().getZona();
                    case 2 -> r == null ? "Sin entrada" : r.estaPresente() ? "Presente" : "Se retiro";
                    case 3 -> r == null || r.getEntrada() == null ? "" : r.getEntrada().format(HORA);
                    case 4 -> r == null || r.getSalida() == null ? "" : r.getSalida().format(HORA);
                    default -> a.getCodigoQr();
                };
            });
    private final JTable tabla = new JTable(modelo);

    public PanelAsistencia(ControladorAsistencia controlador) {
        this.controlador = controlador;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        arriba.setBorder(BorderFactory.createTitledBorder("Registrar con codigo (manual) o con imagen del QR"));
        JButton entrada = new JButton("Registrar entrada");
        JButton salida = new JButton("Registrar salida");
        JButton leerEntrada = new JButton("Entrada con imagen QR...");
        JButton leerSalida = new JButton("Salida con imagen QR...");
        entrada.addActionListener(e -> registrar(true));
        salida.addActionListener(e -> registrar(false));
        leerEntrada.addActionListener(e -> leerImagen(true));
        leerSalida.addActionListener(e -> leerImagen(false));
        arriba.add(new JLabel("Codigo"));
        arriba.add(codigo);
        arriba.add(entrada);
        arriba.add(salida);
        arriba.add(leerEntrada);
        arriba.add(leerSalida);
        JPanel norte = new JPanel(new BorderLayout());
        norte.add(arriba, BorderLayout.CENTER);
        norte.add(mensaje, BorderLayout.SOUTH);
        add(norte, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                codigo.setText(modelo.getFila(tabla.convertRowIndexToModel(tabla.getSelectedRow())).getCodigoQr());
            }
        });
        tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton guardarQr = new JButton("Guardar QR de la fila elegida como imagen...");
        guardarQr.addActionListener(e -> guardarQr());
        abajo.add(new JLabel("Al elegir una fila se completa su codigo."));
        abajo.add(guardarQr);
        add(abajo, BorderLayout.SOUTH);
        refrescar();
    }

    private RegistroAsistencia buscarRegistro(Asignacion a) {
        for (RegistroAsistencia r : controlador.getRegistros()) {
            if (r.getAsignacion().getCodigoQr().equals(a.getCodigoQr())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public void refrescar() {
        modelo.setFilas(controlador.getAsignaciones());
    }

    private void registrar(boolean esEntrada) {
        try {
            RegistroAsistencia r = esEntrada
                    ? controlador.registrarEntradaManual(codigo.getText(), LocalDateTime.now())
                    : controlador.registrarSalidaManual(codigo.getText(), LocalDateTime.now());
            avisar(r, esEntrada);
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void leerImagen(boolean esEntrada) {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter("Imagenes (png, jpg)", "png", "jpg", "jpeg"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            BufferedImage imagen = ImageIO.read(selector.getSelectedFile());
            if (imagen == null) {
                throw new IllegalArgumentException("El archivo no es una imagen valida");
            }
            RegistroAsistencia r = esEntrada
                    ? controlador.registrarEntradaDesdeImagen(imagen, LocalDateTime.now())
                    : controlador.registrarSalidaDesdeImagen(imagen, LocalDateTime.now());
            avisar(r, esEntrada);
        } catch (IOException | RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void avisar(RegistroAsistencia r, boolean esEntrada) {
        mensaje.setText(" " + (esEntrada ? "Entrada" : "Salida") + " registrada: "
                + r.getAsignacion().getVoluntario().getApellido() + ", " + r.getAsignacion().getVoluntario().getNombre()
                + " en " + r.getAsignacion().getPunto().getCodigo());
        refrescar();
    }

    private void guardarQr() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Utilidades.error(this, new IllegalArgumentException("Elegi una fila de la tabla"));
            return;
        }
        Asignacion a = modelo.getFila(tabla.convertRowIndexToModel(fila));
        JFileChooser selector = new JFileChooser();
        selector.setSelectedFile(new File("qr-" + a.getVoluntario().getDni() + ".png"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.write(selector.getSelectedFile().toPath(), controlador.generarImagenQr(a.getCodigoQr()));
            Utilidades.info(this, "QR guardado en " + selector.getSelectedFile().getName());
        } catch (IOException | RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    // Para tests y otras pantallas: lista de lo que se muestra.
    List<Asignacion> filasMostradas() {
        return java.util.stream.IntStream.range(0, modelo.getRowCount()).mapToObj(modelo::getFila).toList();
    }
}
