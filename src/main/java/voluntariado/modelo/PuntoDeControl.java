package voluntariado.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PuntoDeControl {

    private final String codigo;
    private String zona;
    private int dotacionRequerida;
    private final List<String> zonasCercanas = new ArrayList<>();

    public PuntoDeControl(String codigo, String zona, int dotacionRequerida) {
        this.codigo = validarTexto(codigo, "codigo");
        setZona(zona);
        setDotacionRequerida(dotacionRequerida);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        String zonaValidada = validarTexto(zona, "zona");
        if (zonasCercanas.stream().anyMatch(z -> z.equalsIgnoreCase(zonaValidada))) {
            throw new DatosInvalidosException("La zona del punto no puede ser una zona cercana");
        }
        this.zona = zonaValidada;
    }

    public int getDotacionRequerida() {
        return dotacionRequerida;
    }

    public void setDotacionRequerida(int dotacionRequerida) {
        if (dotacionRequerida <= 0) {
            throw new DatosInvalidosException("La dotacion requerida debe ser positiva");
        }
        this.dotacionRequerida = dotacionRequerida;
    }

    public List<String> getZonasCercanas() {
        return Collections.unmodifiableList(zonasCercanas);
    }

    public void agregarZonaCercana(String zonaCercana) {
        String zonaValidada = validarTexto(zonaCercana, "zona cercana");
        if (zonaValidada.equalsIgnoreCase(zona)) {
            throw new DatosInvalidosException("La zona del punto no puede ser una zona cercana");
        }
        if (zonasCercanas.stream().noneMatch(z -> z.equalsIgnoreCase(zonaValidada))) {
            zonasCercanas.add(zonaValidada);
        }
    }

    public void quitarZonaCercana(String zonaCercana) {
        if (zonaCercana != null) {
            zonasCercanas.removeIf(z -> z.equalsIgnoreCase(zonaCercana.trim()));
        }
    }

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof PuntoDeControl punto && codigo.equals(punto.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
