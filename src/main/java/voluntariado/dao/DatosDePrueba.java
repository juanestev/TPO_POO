package voluntariado.dao;

import java.time.DayOfWeek;
import java.time.LocalTime;
import voluntariado.modelo.Disponibilidad;
import voluntariado.modelo.Habilidad;
import voluntariado.modelo.PuntoDeControl;
import voluntariado.modelo.Voluntario;
import voluntariado.modelo.VoluntarioMedico;
import voluntariado.modelo.VoluntarioSeguridad;

// Datos ficticios para probar y hacer la demo: 5 puntos y 30 voluntarios.
// Se cargan con los DAO (y no con un .sql) para que pasen por las mismas validaciones del modelo.
// Los mails usan el dominio ejemplo.com para no escribirle a nadie real por accidente.
public final class DatosDePrueba {

    public static final int CANTIDAD_VOLUNTARIOS = 30;

    private static final String[] NOMBRES = {
        "Ana", "Bruno", "Carla", "Diego", "Elena", "Facundo", "Gabriela", "Hernan", "Ines", "Julian",
        "Karina", "Lucas", "Micaela", "Nicolas", "Olivia", "Pablo", "Quimey", "Rocio", "Santiago", "Tamara",
        "Ulises", "Valentina", "Walter", "Ximena", "Yamila", "Zoe", "Agustin", "Belen", "Camilo", "Dolores"
    };
    private static final String[] APELLIDOS = {
        "Gomez", "Fernandez", "Lopez", "Martinez", "Rodriguez", "Perez", "Garcia", "Sanchez", "Romero", "Diaz",
        "Alvarez", "Torres", "Ruiz", "Flores", "Acosta", "Benitez", "Medina", "Herrera", "Suarez", "Aguirre",
        "Gimenez", "Molina", "Silva", "Castro", "Rojas", "Ortiz", "Luna", "Juarez", "Cabrera", "Rios"
    };
    private static final Habilidad[] EXTRAS = {
        Habilidad.INGLES, Habilidad.PORTUGUES, Habilidad.ASISTENCIA_MAYORES
    };
    private static final Disponibilidad[] TURNOS = {
        new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(14, 0)),
        new Disponibilidad(DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(20, 0)),
        new Disponibilidad(DayOfWeek.SUNDAY, LocalTime.of(8, 0), LocalTime.of(14, 0))
    };

    private DatosDePrueba() { }

    // Carga los datos solo si la base no tiene ningun punto ni voluntario. Devuelve true si cargo algo.
    public static boolean cargarSiEstaVacia(PuntoDeControlDAO puntoDAO, VoluntarioDAO voluntarioDAO) {
        if (!puntoDAO.listarTodos().isEmpty() || !voluntarioDAO.listarTodos().isEmpty()) {
            return false;
        }
        for (PuntoDeControl punto : crearPuntos()) {
            puntoDAO.guardar(punto);
        }
        for (int i = 0; i < CANTIDAD_VOLUNTARIOS; i++) {
            voluntarioDAO.guardar(crearVoluntario(i));
        }
        return true;
    }

    // Recorrido de la visita por el centro porteño; cada zona es vecina de las de al lado.
    static PuntoDeControl[] crearPuntos() {
        PuntoDeControl plaza = new PuntoDeControl("P1", "Plaza de Mayo", 6);
        plaza.agregarZonaCercana("Catedral");
        plaza.agregarZonaCercana("Avenida de Mayo");
        PuntoDeControl catedral = new PuntoDeControl("P2", "Catedral", 4);
        catedral.agregarZonaCercana("Plaza de Mayo");
        PuntoDeControl avenida = new PuntoDeControl("P3", "Avenida de Mayo", 5);
        avenida.agregarZonaCercana("Plaza de Mayo");
        avenida.agregarZonaCercana("Congreso");
        avenida.agregarZonaCercana("Obelisco");
        PuntoDeControl obelisco = new PuntoDeControl("P4", "Obelisco", 6);
        obelisco.agregarZonaCercana("Avenida de Mayo");
        PuntoDeControl congreso = new PuntoDeControl("P5", "Congreso", 4);
        congreso.agregarZonaCercana("Avenida de Mayo");
        return new PuntoDeControl[] {plaza, catedral, avenida, obelisco, congreso};
    }

    // Pares: medicos (RCP o primeros auxilios). Impares: seguridad (control de accesos).
    // Ademas cada uno tiene un idioma o asistencia a mayores y uno o dos turnos, rotando.
    static Voluntario crearVoluntario(int i) {
        String dni = String.valueOf(30100100 + i);
        String telefono = String.format("11400001%02d", i);
        String mail = (NOMBRES[i] + "." + APELLIDOS[i] + "@ejemplo.com").toLowerCase();
        Voluntario voluntario;
        if (i % 2 == 0) {
            voluntario = new VoluntarioMedico(dni, NOMBRES[i], APELLIDOS[i], telefono, mail);
            voluntario.agregarHabilidad(i % 4 == 0 ? Habilidad.RCP : Habilidad.PRIMEROS_AUXILIOS);
        } else {
            voluntario = new VoluntarioSeguridad(dni, NOMBRES[i], APELLIDOS[i], telefono, mail);
            voluntario.agregarHabilidad(Habilidad.CONTROL_ACCESOS);
        }
        voluntario.agregarHabilidad(EXTRAS[i % EXTRAS.length]);
        voluntario.agregarDisponibilidad(TURNOS[i % TURNOS.length]);
        if (i % 5 == 0) {
            voluntario.agregarDisponibilidad(TURNOS[(i + 1) % TURNOS.length]);
        }
        return voluntario;
    }
}
