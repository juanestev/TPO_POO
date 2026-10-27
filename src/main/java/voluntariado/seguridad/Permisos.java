package voluntariado.seguridad;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

public final class Permisos {

    private static final Map<Rol, EnumSet<Accion>> POR_ROL = new EnumMap<>(Rol.class);

    static {
        POR_ROL.put(Rol.ADMINISTRADOR, EnumSet.of(
                Accion.GESTIONAR_USUARIOS, Accion.EDITAR_VOLUNTARIOS_Y_PUNTOS, Accion.VER_COBERTURA));
        POR_ROL.put(Rol.COORDINADOR, EnumSet.of(
                Accion.CONFIRMAR_ASIGNACIONES, Accion.REGISTRAR_ASISTENCIA, Accion.VER_COBERTURA));
        POR_ROL.put(Rol.OPERADOR, EnumSet.of(
                Accion.EDITAR_VOLUNTARIOS_Y_PUNTOS, Accion.REGISTRAR_ASISTENCIA, Accion.VER_COBERTURA));
    }

    private Permisos() { }

    public static boolean tienePermiso(Rol rol, Accion accion) {
        if (rol == null || accion == null) {
            throw new IllegalArgumentException("El rol y la accion son obligatorios");
        }
        return POR_ROL.get(rol).contains(accion);
    }
}
