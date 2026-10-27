package voluntariado.modelo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MotorAsignacion {

    private final RepositorioEnMemoria repositorio;
    private final EstrategiaAsignacion estrategia;

    public MotorAsignacion(RepositorioEnMemoria repositorio, EstrategiaAsignacion estrategia) {
        if (repositorio == null || estrategia == null) {
            throw new DatosInvalidosException("El motor necesita repositorio y estrategia");
        }
        this.repositorio = repositorio;
        this.estrategia = estrategia;
    }

    public List<Asignacion> generarSugerencias(PuntoDeControl punto, Habilidad habilidad, Disponibilidad turno) {
        if (punto == null || repositorio.buscarPunto(punto.getCodigo()) != punto) {
            throw new DatosInvalidosException("El punto debe estar registrado");
        }
        int vacantes = punto.getDotacionRequerida() - repositorio.getAsignacionesPorPunto(punto.getCodigo()).size();
        return crearSugerencias(punto, habilidad, turno, repositorio.getVoluntarios(), vacantes, true);
    }

    // Reutilizado por el motor de reasignacion, que puede proponer voluntarios ya asignados.
    List<Asignacion> crearSugerencias(PuntoDeControl punto, Habilidad habilidad, Disponibilidad turno,
            List<Voluntario> candidatos, int cupos, boolean excluirAsignados) {
        if (punto == null || repositorio.buscarPunto(punto.getCodigo()) != punto || candidatos == null) {
            throw new DatosInvalidosException("Se necesita un punto registrado y candidatos");
        }
        if (cupos <= 0) {
            return List.of();
        }
        List<Asignacion> sugerencias = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Voluntario voluntario : estrategia.filtrar(candidatos, habilidad, turno)) {
            if (repositorio.buscarVoluntario(voluntario.getDni()) != voluntario) {
                throw new DatosInvalidosException("El voluntario debe estar registrado");
            }
            if ((!excluirAsignados || !repositorio.estaAsignado(voluntario.getDni()))
                    && vistos.add(voluntario.getDni())) {
                sugerencias.add(new Asignacion(voluntario, punto));
                if (sugerencias.size() == cupos) {
                    break;
                }
            }
        }
        return List.copyOf(sugerencias);
    }
}
