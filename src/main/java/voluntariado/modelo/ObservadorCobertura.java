package voluntariado.modelo;

@FunctionalInterface
public interface ObservadorCobertura {
    void alCambiar(PuntoDeControl punto);
}
