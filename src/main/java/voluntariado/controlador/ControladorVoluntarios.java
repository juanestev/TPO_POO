package voluntariado.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import voluntariado.dao.VoluntarioDAO;
import voluntariado.modelo.ComparadorPorApellido;
import voluntariado.modelo.DatosInvalidosException;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.RepositorioEnMemoria;
import voluntariado.modelo.Voluntario;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.modelo.VoluntarioSeguridad;
import voluntariado.seguridad.Accion;
import voluntariado.seguridad.Sesion;

// Alta, baja y modificacion de voluntarios. La vista habla solo con esta clase: nunca con el DAO.
// Cada operacion cambia la memoria y la base; si la base falla, se deshace lo de la memoria
// para que los dos lugares no queden distintos.
public class ControladorVoluntarios {

    public enum Tipo { MEDICO, SEGURIDAD }

    private final RepositorioEnMemoria repositorio;
    private final VoluntarioDAO dao;
    private final Sesion sesion;

    // Sin sesion no se controlan permisos (solo para pruebas); la app siempre usa el otro constructor.
    public ControladorVoluntarios(RepositorioEnMemoria repositorio, VoluntarioDAO dao) {
        this(repositorio, dao, null);
    }

    // Con sesion: las operaciones que modifican datos exigen el permiso del rol (ademas de ocultar la pantalla).
    public ControladorVoluntarios(RepositorioEnMemoria repositorio, VoluntarioDAO dao, Sesion sesion) {
        if (repositorio == null || dao == null) {
            throw new DatosInvalidosException("Se necesitan repositorio y DAO de voluntarios");
        }
        this.repositorio = repositorio;
        this.dao = dao;
        this.sesion = sesion;
    }

    private void exigirPermiso() {
        if (sesion != null) {
            sesion.exigirPermiso(Accion.EDITAR_VOLUNTARIOS_Y_PUNTOS);
        }
    }

    public List<Voluntario> listar() {
        List<Voluntario> voluntarios = new ArrayList<>(repositorio.getVoluntarios());
        voluntarios.sort(new ComparadorPorApellido());
        return voluntarios;
    }

    public Voluntario alta(Tipo tipo, String dni, String nombre, String apellido, String telefono, String mail,
            Set<Habilidad> habilidades, List<Disponibilidad> disponibilidades) {
        exigirPermiso();
        if (tipo == null) {
            throw new DatosInvalidosException("El tipo de voluntario es obligatorio");
        }
        Voluntario voluntario = tipo == Tipo.MEDICO
                ? new VoluntarioMedico(dni, nombre, apellido, telefono, mail)
                : new VoluntarioSeguridad(dni, nombre, apellido, telefono, mail);
        aplicarListas(voluntario, habilidades, disponibilidades);
        repositorio.agregarVoluntario(voluntario);
        try {
            dao.guardar(voluntario);
        } catch (RuntimeException e) {
            repositorio.quitarVoluntario(voluntario.getDni());
            throw e;
        }
        return voluntario;
    }

    public void modificar(String dni, String nombre, String apellido, String telefono, String mail,
            Set<Habilidad> habilidades, List<Disponibilidad> disponibilidades) {
        exigirPermiso();
        Voluntario voluntario = repositorio.buscarVoluntario(dni);
        String nombreViejo = voluntario.getNombre();
        String apellidoViejo = voluntario.getApellido();
        String telefonoViejo = voluntario.getTelefono();
        String mailViejo = voluntario.getMail();
        Set<Habilidad> habilidadesViejas = Set.copyOf(voluntario.getHabilidades());
        List<Disponibilidad> disponibilidadesViejas = List.copyOf(voluntario.getDisponibilidades());
        try {
            voluntario.setNombre(nombre);
            voluntario.setApellido(apellido);
            voluntario.setTelefono(telefono);
            voluntario.setMail(mail);
            aplicarListas(voluntario, habilidades, disponibilidades);
            dao.guardar(voluntario);
        } catch (RuntimeException e) {
            voluntario.setNombre(nombreViejo);
            voluntario.setApellido(apellidoViejo);
            voluntario.setTelefono(telefonoViejo);
            voluntario.setMail(mailViejo);
            aplicarListas(voluntario, habilidadesViejas, disponibilidadesViejas);
            throw e;
        }
    }

    public void baja(String dni) {
        exigirPermiso();
        Voluntario voluntario = repositorio.buscarVoluntario(dni);
        if (repositorio.estaAsignado(dni)) {
            throw new DatosInvalidosException("No se puede dar de baja: el voluntario tiene una asignacion confirmada");
        }
        dao.eliminar(dni);
        repositorio.quitarVoluntario(voluntario.getDni());
    }

    // Reemplaza las habilidades y disponibilidades por las nuevas. Si alguna no es valida, no queda a medias.
    private void aplicarListas(Voluntario voluntario, Set<Habilidad> habilidades, List<Disponibilidad> disponibilidades) {
        if (habilidades == null || disponibilidades == null) {
            throw new DatosInvalidosException("Las habilidades y disponibilidades no pueden ser nulas");
        }
        for (Habilidad habilidad : List.copyOf(voluntario.getHabilidades())) {
            voluntario.quitarHabilidad(habilidad);
        }
        for (Disponibilidad disponibilidad : List.copyOf(voluntario.getDisponibilidades())) {
            voluntario.quitarDisponibilidad(disponibilidad);
        }
        for (Habilidad habilidad : habilidades) {
            voluntario.agregarHabilidad(habilidad);
        }
        for (Disponibilidad disponibilidad : disponibilidades) {
            voluntario.agregarDisponibilidad(disponibilidad);
        }
    }
}
