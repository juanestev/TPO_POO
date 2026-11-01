# Arquitectura, pilares de POO y patrones (tarjetas 6.5 y 6.7)

Resumen de **lo que realmente esta implementado**. El diagrama de clases del modelo base esta en
[modelo-clases.md](modelo-clases.md) y el esquema de la base en [esquema-bd.md](esquema-bd.md).

## Capas (MVC)

Cada capa solo habla con la inmediata inferior; la vista nunca toca la base. `Sistema` arma todas las piezas una sola vez
(una unica instancia compartida entre ventanas, como `Biblioteca` en UVA 6).

```mermaid
flowchart TD
    subgraph vista
        VentanaLogin --> VentanaPrincipal
        VentanaPrincipal --> PanelVoluntarios & PanelPuntos & PanelAsignacion & PanelAsistencia & PanelCobertura & PanelUsuarios
    end
    subgraph controlador
        ControladorVoluntarios
        ControladorPuntos
        ControladorAsignacion
        ControladorAsistencia
        ControladorCobertura
        ControladorUsuarios
    end
    subgraph modelo
        RepositorioEnMemoria
        MotorAsignacion
        MotorReasignacion
        CalculadorCobertura
    end
    subgraph servicios
        ServicioAsignaciones
        ServicioNotificaciones
        CodigoQr
    end
    subgraph dao
        VoluntarioDAO
        PuntoDeControlDAO
        AsignacionDAO
        AsistenciaDAO
        UsuarioDAO
        ColaMailsDAO
    end
    subgraph seguridad
        Sesion
        ServicioAutenticacion
        Permisos
    end
    PanelVoluntarios --> ControladorVoluntarios
    PanelPuntos --> ControladorPuntos
    PanelAsignacion --> ControladorAsignacion
    PanelAsistencia --> ControladorAsistencia
    PanelCobertura --> ControladorCobertura
    PanelUsuarios --> ControladorUsuarios
    controlador --> modelo
    controlador --> servicios
    modelo --> RepositorioEnMemoria
    servicios --> dao
    controlador --> dao
```

La **seguridad** atraviesa las capas: la vista consulta `Sesion.tienePermiso` para mostrar solo lo permitido y los
controladores vuelven a exigirlo con `Sesion.exigirPermiso`.

## Los cuatro pilares

| Pilar | Donde se ve |
|---|---|
| **Encapsulamiento** | `Persona`, `Voluntario`, `PuntoDeControl`, `Usuario`: atributos privados; los datos solo cambian por setters/metodos que validan y lanzan `DatosInvalidosException`. Las colecciones se devuelven como vistas no modificables. |
| **Herencia** | `Persona` (abstracta) -> `Voluntario` (abstracta) -> `VoluntarioMedico` / `VoluntarioSeguridad`; `Persona` -> `Coordinador`. |
| **Polimorfismo** | `Persona.getRol()` (cada subclase responde distinto); `EstrategiaAsignacion`, `NotificadorMail`, los `*DAO` y `VistaCobertura` se usan por su interfaz sin saber la clase concreta. |
| **Abstraccion** | Clases abstractas (`Persona`, `Voluntario`) e interfaces que ocultan el detalle: el modelo no sabe que hay SQLite, SMTP ni Swing. |

## Los cuatro patrones

| Patron | Interfaz / participantes | Para que |
|---|---|---|
| **Strategy** | `EstrategiaAsignacion` -> `EstrategiaPorHabilidadYDisponibilidad`; la usan `MotorAsignacion` y `MotorReasignacion`. | Cambiar el criterio de asignacion sin tocar los motores. F1 y F6 comparten la misma estrategia. |
| **Observer** | `ObservadorCobertura` (interfaz). Sujetos: `RepositorioEnMemoria` (cambio de asignaciones) y `ControladorAsistencia` (check-in/out). Observador: `ControladorCobertura`, que a su vez avisa a `VistaCobertura` (`PanelCobertura`). | El panel de cobertura se actualiza solo. Probado en `PanelesTest`. |
| **Adapter** | `NotificadorMail` <- `NotificadorMailSmtp` (Jakarta/Angus Mail) y `NotificadorMailConsola` (simulado). | El sistema no depende del proveedor de mail y se prueba sin red. |
| **DAO** | `VoluntarioDAO`, `PuntoDeControlDAO`, `AsignacionDAO`, `AsistenciaDAO`, `UsuarioDAO`, `ColaMailsDAO` y sus clases `...DAOSQLite`. | El modelo no conoce la base. |

## Clases agregadas desde el modelo base

```mermaid
classDiagram
    class Usuario {
        -String nombreUsuario
        -String hashClave
        -String sal
        -Rol rol
        -boolean activo
        +cambiarClave(hash, sal)
    }
    class ServicioClaves {
        +generarSal() String
        +calcularHash(clave, sal) String
        +coincide(clave, sal, hash) boolean
    }
    class ServicioAutenticacion {
        +crearUsuario(nombre, clave, rol) Usuario
        +iniciarSesion(nombre, clave) Usuario
    }
    class Sesion {
        +iniciar(Usuario)
        +tienePermiso(Accion) boolean
        +exigirPermiso(Accion)
    }
    class Permisos {
        +tienePermiso(Rol, Accion)$ boolean
    }
    class ControladorAsignacion {
        +sugerir(punto, habilidad, turno)
        +sugerirReasignacion(punto, habilidad, turno)
        +confirmar(sugerencias, turno, encuentro) Resultado
    }
    class ControladorCobertura {
        +alCambiar(PuntoDeControl)
        +refrescarTodos()
    }
    class ObservadorCobertura {
        <<interface>>
        +alCambiar(PuntoDeControl)
    }
    class VistaCobertura {
        <<interface>>
        +actualizar(punto, presentes, estado)
    }
    ServicioAutenticacion --> ServicioClaves
    ServicioAutenticacion --> Usuario
    Sesion --> Usuario
    Sesion --> Permisos
    ControladorCobertura ..|> ObservadorCobertura
    ControladorCobertura --> VistaCobertura
```

## Observaciones honestas sobre el diseno

- `Coordinador` es una `Persona` del modelo, pero **no** se usa para iniciar sesion: el acceso al sistema se modela aparte con `Usuario` + `Rol`.
- La **reasignacion** no es una segunda interfaz Strategy: `EstrategiaReasignacion` usa por dentro la estrategia de asignacion base y agrega la regla de zonas cercanas con sobrante.
- Los datos viven en memoria (`HashMap`/`ArrayList`) y se guardan en SQLite en cada operacion; si la base falla, los controladores deshacen el cambio en memoria.
