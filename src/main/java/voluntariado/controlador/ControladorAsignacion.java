package voluntariado.controlador;

import java.util.ArrayList;
import java.util.List;
import voluntariado.dao.AsignacionDAO;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstrategiaAsignacion;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.MotorAsignacion;
import voluntariado.modelo.MotorReasignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Sesion;
import voluntariado.servicios.ServicioAsignaciones;
import voluntariado.servicios.ServicioNotificaciones;

// F1 y F6 desde la pantalla: el sistema SUGIERE, el coordinador CONFIRMA.
// Confirmar guarda en la base, avisa por mail (o lo deja en cola si no hay red) y actualiza la cobertura.
public class ControladorAsignacion {

    public record Resultado(int confirmadas, int reasignadas, int avisosPendientes, List<String> errores) { }

    private final RepositorioEnMemoria repositorio;
    private final AsignacionDAO dao;
    private final ServicioAsignaciones servicioAsignaciones;
    private final ControladorAsistencia asistencia;
    private final ServicioNotificaciones notificaciones;
    private final Sesion sesion;
    private final MotorAsignacion motorAsignacion;
    private final MotorReasignacion motorReasignacion;

    public ControladorAsignacion(RepositorioEnMemoria repositorio, AsignacionDAO dao, ControladorAsistencia asistencia,
            ServicioNotificaciones notificaciones, EstrategiaAsignacion estrategia, Sesion sesion) {
        if (repositorio == null || dao == null || asistencia == null || notificaciones == null
                || estrategia == null || sesion == null) {
            throw new DatosInvalidosException("El controlador de asignacion necesita todas sus piezas");
        }
        this.repositorio = repositorio;
        this.dao = dao;
        this.asistencia = asistencia;
        this.notificaciones = notificaciones;
        this.sesion = sesion;
        this.servicioAsignaciones = new ServicioAsignaciones(repositorio, dao);
        // Los dos motores comparten la misma estrategia (Strategy): cambiar el criterio cambia ambos.
        this.motorAsignacion = new MotorAsignacion(repositorio, estrategia);
        this.motorReasignacion = new MotorReasignacion(repositorio, estrategia);
    }

    public List<Asignacion> sugerir(String codigoPunto, Habilidad habilidad, Disponibilidad turno) {
        PuntoDeControl punto = repositorio.buscarPunto(codigoPunto);
        return motorAsignacion.generarSugerencias(punto, habilidad, turno);
    }

    // Propone mover voluntarios PRESENTES de zonas cercanas con sobrante hacia un punto con deficit.
    public List<Asignacion> sugerirReasignacion(String codigoDestino, Habilidad habilidad, Disponibilidad turno) {
        PuntoDeControl destino = repositorio.buscarPunto(codigoDestino);
        return motorReasignacion.generarSugerencias(destino, asistencia.getRegistros(), habilidad, turno);
    }

    public int cantidadAsignados(String codigoPunto) {
        return repositorio.getAsignacionesPorPunto(codigoPunto).size();
    }

    // Codigo del punto donde ya esta confirmado el voluntario, o null si todavia no tiene puesto.
    public String puntoActualDe(String dni) {
        return repositorio.estaAsignado(dni) ? repositorio.buscarAsignacionDeVoluntario(dni).getPunto().getCodigo() : null;
    }

    public Resultado confirmar(List<Asignacion> sugerencias, Disponibilidad turno, String puntoEncuentro) {
        sesion.exigirPermiso(Accion.CONFIRMAR_ASIGNACIONES);
        if (sugerencias == null || sugerencias.isEmpty()) {
            throw new DatosInvalidosException("No hay sugerencias para confirmar");
        }
        if (puntoEncuentro == null || puntoEncuentro.isBlank()) {
            throw new DatosInvalidosException("El punto de encuentro es obligatorio");
        }
        int confirmadas = 0;
        int reasignadas = 0;
        List<String> errores = new ArrayList<>();
        for (Asignacion sugerencia : sugerencias) {
            String quien = sugerencia.getVoluntario().getApellido() + ", " + sugerencia.getVoluntario().getNombre();
            try {
                if (repositorio.estaAsignado(sugerencia.getVoluntario().getDni())) {
                    reasignar(sugerencia, turno, puntoEncuentro);
                    reasignadas++;
                } else {
                    servicioAsignaciones.confirmar(sugerencia);
                    notificaciones.enviarOEncolar(notificaciones.crearAviso(sugerencia, turno, puntoEncuentro));
                }
                confirmadas++;
            } catch (RuntimeException e) {
                errores.add(quien + ": " + e.getMessage());
            }
        }
        return new Resultado(confirmadas, reasignadas, notificaciones.getCantidadPendientes(), errores);
    }

    // Mueve al voluntario: se borra su asignacion anterior y se confirma la nueva (con QR nuevo).
    // Si la nueva falla, se restaura la anterior para no dejar al voluntario sin puesto.
    private void reasignar(Asignacion nueva, Disponibilidad turno, String puntoEncuentro) {
        Asignacion anterior = repositorio.buscarAsignacionDeVoluntario(nueva.getVoluntario().getDni());
        if (anterior.getPunto().equals(nueva.getPunto())) {
            throw new DatosInvalidosException("El voluntario ya esta en ese punto");
        }
        dao.eliminar(anterior.getCodigoQr());
        asistencia.olvidarRegistro(anterior.getCodigoQr());
        repositorio.quitarAsignacion(anterior);
        try {
            servicioAsignaciones.confirmar(nueva);
        } catch (RuntimeException e) {
            dao.guardar(anterior);
            repositorio.registrarAsignacion(anterior);
            throw e;
        }
        notificaciones.enviarOEncolar(notificaciones.crearAvisoCambio(anterior, nueva, turno, puntoEncuentro));
    }
}
