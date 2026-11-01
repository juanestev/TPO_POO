package voluntariado;

import voluntariado.controlador.ControladorAsignacion;
import voluntariado.controlador.ControladorAsistencia;
import voluntariado.controlador.ControladorCobertura;
import voluntariado.controlador.ControladorPuntos;
import voluntariado.controlador.ControladorUsuarios;
import voluntariado.controlador.ControladorVoluntarios;
import voluntariado.dao.AsignacionDAOSQLite;
import voluntariado.dao.AsistenciaDAOSQLite;
import voluntariado.dao.CargaInicial;
import voluntariado.dao.ColaMailsDAOSQLite;
import voluntariado.dao.ConexionBD;
import voluntariado.dao.DatosDePrueba;
import voluntariado.dao.PuntoDeControlDAOSQLite;
import voluntariado.dao.UsuarioDAOSQLite;
import voluntariado.dao.VoluntarioDAOSQLite;
import voluntariado.modelo.EstrategiaPorHabilidadYDisponibilidad;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.seguridad.ServicioAutenticacion;
import voluntariado.seguridad.ServicioClaves;
import voluntariado.seguridad.Sesion;
import voluntariado.seguridad.UsuariosIniciales;
import voluntariado.servicios.NotificadorMail;
import voluntariado.servicios.ServicioNotificaciones;
import voluntariado.vista.VistaCobertura;

// El "sistema": arma y guarda todas las piezas una sola vez. Las ventanas comparten esta unica instancia
// (igual idea que Biblioteca en UVA 6), asi todas ven los mismos datos y la misma sesion.
public class Sistema {

    private final Sesion sesion = new Sesion();
    private final RepositorioEnMemoria repositorio;
    private final ServicioAutenticacion autenticacion;
    private final ServicioNotificaciones notificaciones;
    private final ControladorVoluntarios voluntarios;
    private final ControladorPuntos puntos;
    private final ControladorAsistencia asistencia;
    private final ControladorAsignacion asignacion;
    private final ControladorUsuarios usuarios;

    // Abre la base (la crea si falta), carga datos de prueba si esta vacia y pasa todo a memoria.
    public Sistema(String archivoBD, NotificadorMail notificador) {
        ConexionBD conexion = new ConexionBD(archivoBD);
        conexion.inicializarEsquema();
        PuntoDeControlDAOSQLite puntoDAO = new PuntoDeControlDAOSQLite(conexion);
        VoluntarioDAOSQLite voluntarioDAO = new VoluntarioDAOSQLite(conexion);
        AsignacionDAOSQLite asignacionDAO = new AsignacionDAOSQLite(conexion);
        UsuarioDAOSQLite usuarioDAO = new UsuarioDAOSQLite(conexion);
        DatosDePrueba.cargarSiEstaVacia(puntoDAO, voluntarioDAO);
        repositorio = new CargaInicial(puntoDAO, voluntarioDAO, asignacionDAO).cargar();

        autenticacion = new ServicioAutenticacion(usuarioDAO, new ServicioClaves());
        UsuariosIniciales.cargarSiEstaVacia(autenticacion);
        notificaciones = new ServicioNotificaciones(notificador, new ColaMailsDAOSQLite(conexion));

        voluntarios = new ControladorVoluntarios(repositorio, voluntarioDAO, sesion);
        puntos = new ControladorPuntos(repositorio, puntoDAO, sesion);
        asistencia = new ControladorAsistencia(repositorio, new AsistenciaDAOSQLite(conexion), sesion);
        asignacion = new ControladorAsignacion(repositorio, asignacionDAO, asistencia, notificaciones,
                new EstrategiaPorHabilidadYDisponibilidad(), sesion);
        usuarios = new ControladorUsuarios(autenticacion, usuarioDAO, sesion);
    }

    // El panel de cobertura se crea despues que el sistema, asi que pide su controlador con su propia vista.
    public ControladorCobertura crearControladorCobertura(VistaCobertura vista) {
        return new ControladorCobertura(repositorio, asistencia, vista);
    }

    // Cierre ordenado: lo ultimo que se hace es reintentar los avisos que quedaron sin enviar.
    public void alCerrar() {
        notificaciones.reintentarPendientes();
    }

    public Sesion getSesion() {
        return sesion;
    }

    public RepositorioEnMemoria getRepositorio() {
        return repositorio;
    }

    public ServicioAutenticacion getAutenticacion() {
        return autenticacion;
    }

    public ServicioNotificaciones getNotificaciones() {
        return notificaciones;
    }

    public ControladorVoluntarios getVoluntarios() {
        return voluntarios;
    }

    public ControladorPuntos getPuntos() {
        return puntos;
    }

    public ControladorAsistencia getAsistencia() {
        return asistencia;
    }

    public ControladorAsignacion getAsignacion() {
        return asignacion;
    }

    public ControladorUsuarios getUsuarios() {
        return usuarios;
    }
}
