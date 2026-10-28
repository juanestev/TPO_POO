package voluntariado.controlador;

import voluntariado.modelo.CalculadorCobertura;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.ObservadorCobertura;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.vista.VistaCobertura;

public class ControladorCobertura implements ObservadorCobertura, AutoCloseable {

    private final RepositorioEnMemoria repositorio;
    private final ControladorAsistencia asistencia;
    private final VistaCobertura vista;
    private final CalculadorCobertura calculador = new CalculadorCobertura();

    public ControladorCobertura(RepositorioEnMemoria repositorio, ControladorAsistencia asistencia,
            VistaCobertura vista) {
        if (repositorio == null || asistencia == null || vista == null) {
            throw new DatosInvalidosException("Se necesitan repositorio, asistencia y vista de cobertura");
        }
        this.repositorio = repositorio;
        this.asistencia = asistencia;
        this.vista = vista;
        repositorio.agregarObservadorCobertura(this);
        asistencia.agregarObservadorCobertura(this);
    }

    @Override
    public void alCambiar(PuntoDeControl punto) {
        if (punto == null || repositorio.buscarPunto(punto.getCodigo()) != punto) {
            throw new DatosInvalidosException("El punto debe estar registrado");
        }
        var registros = asistencia.getRegistros();
        vista.actualizar(punto, calculador.contarPresentes(punto, registros),
                calculador.calcular(punto, registros));
    }

    @Override
    public void close() {
        repositorio.quitarObservadorCobertura(this);
        asistencia.quitarObservadorCobertura(this);
    }
}
