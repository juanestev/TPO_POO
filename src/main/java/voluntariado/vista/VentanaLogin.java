package voluntariado.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import voluntariado.Sistema;
import voluntariado.seguridad.AutenticacionException;
import voluntariado.seguridad.Usuario;

// Pantalla 5.3: login. Despues de varios intentos fallidos se bloquea un rato (frena el "probar claves").
public class VentanaLogin extends JFrame {

    private static final int INTENTOS_MAXIMOS = 5;
    private static final int SEGUNDOS_DE_BLOQUEO = 30;

    private final Sistema sistema;
    private final JTextField usuario = new JTextField(16);
    private final JPasswordField clave = new JPasswordField(16);
    private final JButton ingresar = new JButton("Ingresar");
    private final JLabel mensaje = new JLabel(" ", SwingConstants.CENTER);
    private int intentosFallidos;

    public VentanaLogin(Sistema sistema) {
        super("Sistema de Gestion de Voluntariado - Ingreso");
        this.sistema = sistema;
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        Utilidades.fila(form, 0, "Usuario", usuario);
        Utilidades.fila(form, 1, "Clave", clave);

        JPanel boton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        boton.add(ingresar);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(boton, BorderLayout.NORTH);
        sur.add(mensaje, BorderLayout.SOUTH);
        sur.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));

        JLabel titulo = new JLabel("Visita del Papa Leon XIV - Voluntariado", SwingConstants.CENTER);
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 8, 0, 8));
        add(titulo, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        ingresar.addActionListener(e -> ingresar());
        clave.addActionListener(e -> ingresar());
        getRootPane().setDefaultButton(ingresar);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    void ingresar() {
        try {
            Usuario encontrado = sistema.getAutenticacion().iniciarSesion(usuario.getText(),
                    new String(clave.getPassword()));
            sistema.getSesion().iniciar(encontrado);
            clave.setText("");
            dispose();
            new VentanaPrincipal(sistema, () -> new VentanaLogin(sistema).setVisible(true)).setVisible(true);
        } catch (AutenticacionException e) {
            clave.setText("");
            intentosFallidos++;
            if (intentosFallidos >= INTENTOS_MAXIMOS) {
                bloquear();
            } else {
                mensaje.setText(e.getMessage() + " (intento " + intentosFallidos + " de " + INTENTOS_MAXIMOS + ")");
            }
        } catch (RuntimeException e) {
            Utilidades.error(this, e);
        }
    }

    private void bloquear() {
        ingresar.setEnabled(false);
        int[] restantes = {SEGUNDOS_DE_BLOQUEO};
        mensaje.setText("Demasiados intentos. Espera " + restantes[0] + " segundos.");
        Timer reloj = new Timer(1000, null);
        reloj.addActionListener(e -> {
            restantes[0]--;
            if (restantes[0] <= 0) {
                reloj.stop();
                intentosFallidos = 0;
                ingresar.setEnabled(true);
                mensaje.setText(" ");
            } else {
                mensaje.setText("Demasiados intentos. Espera " + restantes[0] + " segundos.");
            }
        });
        reloj.start();
    }
}
