# Diagrama de clases del modelo (fase 1)

El codigo identifica un punto de control aunque haya varios en una misma zona. El DNI identifica a cada persona. El repositorio guarda asignaciones confirmadas por punto; las sugeridas todavia no forman parte de la lista de asignados.

```mermaid
classDiagram
    class Persona {
        <<abstract>>
        -String dni
        -String nombre
        -String apellido
        -String telefono
        -String mail
        +getRol() String
    }
    class Voluntario {
        <<abstract>>
        -Set~Habilidad~ habilidades
        -List~Disponibilidad~ disponibilidades
        +agregarHabilidad(Habilidad)
        +agregarDisponibilidad(Disponibilidad)
    }
    class VoluntarioMedico
    class VoluntarioSeguridad
    class Coordinador
    class Habilidad {
        <<enumeration>>
        RCP
        PRIMEROS_AUXILIOS
        INGLES
        PORTUGUES
        ASISTENCIA_MAYORES
        CONTROL_ACCESOS
    }
    class Disponibilidad {
        -DayOfWeek dia
        -LocalTime desde
        -LocalTime hasta
    }
    class PuntoDeControl {
        -String codigo
        -String zona
        -int dotacionRequerida
        -List~String~ zonasCercanas
        +agregarZonaCercana(String)
    }
    class Asignacion {
        -Voluntario voluntario
        -PuntoDeControl punto
        -EstadoAsignacion estado
        +confirmar()
    }
    class EstadoAsignacion {
        <<enumeration>>
        SUGERIDA
        CONFIRMADA
    }
    class RepositorioEnMemoria {
        -Map voluntariosPorDni
        -Map puntosPorCodigo
        -Map asignacionesPorPunto
        +agregarVoluntario(Voluntario)
        +agregarPunto(PuntoDeControl)
        +registrarAsignacion(Asignacion)
        +buscarVoluntariosPorHabilidad(Habilidad) List
    }
    class RegistroAsistencia {
        -Asignacion asignacion
        -LocalDateTime entrada
        -LocalDateTime salida
        +registrarEntrada(String, LocalDateTime)
        +registrarSalida(String, LocalDateTime)
        +estaPresente() boolean
    }
    class EstadoCobertura {
        <<enumeration>>
        CUBIERTO
        DEFICIT
        SOBRANTE
    }
    class CalculadorCobertura {
        +contarPresentes(PuntoDeControl, List) int
        +calcular(PuntoDeControl, List) EstadoCobertura
    }
    class EstrategiaAsignacion {
        <<interface>>
        +filtrar(List, Habilidad, Disponibilidad) List
    }
    class EstrategiaPorHabilidadYDisponibilidad
    class MotorAsignacion {
        +generarSugerencias(PuntoDeControl, Habilidad, Disponibilidad) List
    }
    class EstrategiaReasignacion {
        +seleccionar(PuntoDeControl, List, Habilidad, Disponibilidad) List
    }
    class MotorReasignacion {
        +generarSugerencias(PuntoDeControl, List, Habilidad, Disponibilidad) List
    }

    Persona <|-- Voluntario
    Persona <|-- Coordinador
    Voluntario <|-- VoluntarioMedico
    Voluntario <|-- VoluntarioSeguridad
    Voluntario o-- Habilidad
    Voluntario o-- Disponibilidad
    Asignacion --> Voluntario
    Asignacion --> PuntoDeControl
    Asignacion --> EstadoAsignacion
    RepositorioEnMemoria o-- Voluntario
    RepositorioEnMemoria o-- PuntoDeControl
    RepositorioEnMemoria o-- Asignacion
    RegistroAsistencia --> Asignacion
    CalculadorCobertura --> RegistroAsistencia
    CalculadorCobertura --> EstadoCobertura
    EstrategiaAsignacion <|.. EstrategiaPorHabilidadYDisponibilidad
    MotorAsignacion --> EstrategiaAsignacion
    MotorAsignacion --> RepositorioEnMemoria
    EstrategiaReasignacion --> EstrategiaAsignacion
    EstrategiaReasignacion --> CalculadorCobertura
    MotorReasignacion --> MotorAsignacion
    MotorReasignacion --> EstrategiaReasignacion
```
