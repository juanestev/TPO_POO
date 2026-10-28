package voluntariado;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javax.imageio.ImageIO;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.dao.AsignacionDAOSQLite;
import voluntariado.dao.AsistenciaDAOSQLite;
import voluntariado.dao.CargaInicial;
import voluntariado.dao.ConexionBD;
import voluntariado.dao.DatosDePrueba;
import voluntariado.dao.PuntoDeControlDAOSQLite;
import voluntariado.dao.VoluntarioDAOSQLite;
import voluntariado.modelo.Asignacion;
import voluntariado.modelo.CalculadorCobertura;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.EstrategiaPorHabilidadYDisponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.MotorAsignacion;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.servicios.CodigoQr;
import voluntariado.servicios.NotificadorMailConsola;
import voluntariado.servicios.ServicioAsignaciones;
import voluntariado.servicios.ServicioNotificaciones;

/** Demostracion reproducible sin mails reales ni cambios en voluntariado.db. */
public final class DemoFlujo {

    private DemoFlujo() { }

    public static void main(String[] args) throws Exception {
        Path archivo = Files.createTempFile("voluntariado-demo-", ".db");
        try {
            ConexionBD conexion = new ConexionBD(archivo.toString());
            conexion.inicializarEsquema();
            PuntoDeControlDAOSQLite puntos = new PuntoDeControlDAOSQLite(conexion);
            VoluntarioDAOSQLite voluntarios = new VoluntarioDAOSQLite(conexion);
            AsignacionDAOSQLite asignaciones = new AsignacionDAOSQLite(conexion);
            DatosDePrueba.cargarSiEstaVacia(puntos, voluntarios);
            RepositorioEnMemoria repositorio = new CargaInicial(puntos, voluntarios, asignaciones).cargar();
            PuntoDeControl punto = repositorio.buscarPunto("P2");
            Disponibilidad turno = new Disponibilidad(DayOfWeek.SATURDAY,
                    LocalTime.of(8, 0), LocalTime.of(14, 0));
            List<Asignacion> sugerencias = new MotorAsignacion(repositorio,
                    new EstrategiaPorHabilidadYDisponibilidad())
                    .generarSugerencias(punto, Habilidad.CONTROL_ACCESOS, turno);
            if (sugerencias.size() < punto.getDotacionRequerida()) {
                throw new IllegalStateException("El seed no ofrece suficientes voluntarios para la demo");
            }
            ServicioAsignaciones servicioAsignaciones = new ServicioAsignaciones(repositorio, asignaciones);
            NotificadorMailConsola notificadorFalso = new NotificadorMailConsola();
            ServicioNotificaciones avisos = new ServicioNotificaciones(notificadorFalso);
            ControladorAsistencia asistencia = new ControladorAsistencia(repositorio,
                    new AsistenciaDAOSQLite(conexion));
            CalculadorCobertura cobertura = new CalculadorCobertura();

            System.out.println("Datos ficticios cargados: " + repositorio.getVoluntarios().size()
                    + " voluntarios; punto " + punto.getCodigo() + " requiere "
                    + punto.getDotacionRequerida() + " presentes");
            System.out.println("Cobertura inicial: " + cobertura.calcular(punto, asistencia.getRegistros()));
            for (int i = 0; i < punto.getDotacionRequerida(); i++) {
                Asignacion sugerencia = sugerencias.get(i);
                servicioAsignaciones.confirmar(sugerencia);
                avisos.enviarOEncolar(avisos.crearAviso(sugerencia, turno, "Catedral"));
                LocalDateTime momento = LocalDateTime.of(2026, 10, 31, 8, 0);
                if (i == 0) {
                    var imagen = ImageIO.read(new ByteArrayInputStream(
                            CodigoQr.generarImagen(sugerencia.getCodigoQr(), 200)));
                    asistencia.registrarEntradaDesdeImagen(imagen, momento);
                } else {
                    asistencia.registrarEntradaManual(sugerencia.getCodigoQr(), momento);
                }
            }
            System.out.println("Asignaciones confirmadas y persistidas: "
                    + repositorio.getAsignacionesPorPunto(punto.getCodigo()).size());
            System.out.println("Avisos simulados: " + notificadorFalso.getEnviados().size());
            System.out.println("Presentes: " + cobertura.contarPresentes(punto, asistencia.getRegistros())
                    + "; cobertura: " + cobertura.calcular(punto, asistencia.getRegistros()));
            asistencia.registrarSalidaManual(sugerencias.get(0).getCodigoQr(),
                    LocalDateTime.of(2026, 10, 31, 12, 0));
            System.out.println("Tras una salida: " + cobertura.contarPresentes(punto, asistencia.getRegistros())
                    + " presentes; cobertura: " + cobertura.calcular(punto, asistencia.getRegistros()));
            System.out.println("Registros recuperados desde SQLite: "
                    + new AsistenciaDAOSQLite(conexion).listarTodas(repositorio).size());
        } finally {
            Files.deleteIfExists(archivo);
        }
    }
}
