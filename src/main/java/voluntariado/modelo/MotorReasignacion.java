package voluntariado.modelo;

import java.util.List;

public class MotorReasignacion {

    private final RepositorioEnMemoria repositorio;
    private final CalculadorCobertura calculador = new CalculadorCobertura();
    private final EstrategiaReasignacion estrategia;
    private final MotorAsignacion motorBase;

    public MotorReasignacion(RepositorioEnMemoria repositorio, EstrategiaAsignacion estrategiaBase) {
        if (repositorio == null || estrategiaBase == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y estrategia");
        }
        this.repositorio = repositorio;
        this.estrategia = new EstrategiaReasignacion(estrategiaBase);
        this.motorBase = new MotorAsignacion(repositorio, estrategiaBase);
    }

    public List<Asignacion> generarSugerencias(PuntoDeControl destino, List<RegistroAsistencia> registros,
            Habilidad habilidad, Disponibilidad turno) {
        if (destino == null || repositorio.buscarPunto(destino.getCodigo()) != destino) {
            throw new DatosInvalidosException("El destino debe estar registrado");
        }
        int deficit = destino.getDotacionRequerida() - calculador.contarPresentes(destino, registros);
        if (deficit <= 0) {
            return List.of();
        }
        List<Voluntario> candidatos = estrategia.seleccionar(destino, registros, habilidad, turno);
        return motorBase.crearSugerencias(destino, habilidad, turno, candidatos, deficit, false);
    }
}
