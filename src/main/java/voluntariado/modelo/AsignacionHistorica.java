package voluntariado.modelo;

import java.time.LocalDateTime;

// Datos conservados del puesto anterior cuando un voluntario es reasignado.
public record AsignacionHistorica(String codigoQr, String dni, String codigoPunto,
        LocalDateTime entrada, LocalDateTime salida, LocalDateTime fechaCambio) { }
