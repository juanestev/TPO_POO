package voluntariado.vista;

import voluntariado.modelo.EstadoCobertura;
import voluntariado.modelo.PuntoDeControl;

@FunctionalInterface
public interface VistaCobertura {
    void actualizar(PuntoDeControl punto, int presentes, EstadoCobertura estado);
}
