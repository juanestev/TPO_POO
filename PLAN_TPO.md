# Plan de trabajo – Sistema de Gestión de Voluntariado (Visita Papa León XIV)

Base: Actividad 01 (Java + POO + MVC + HashMap/ArrayList + BD local) y Actividad 02 (arquitectura en capas, F1–F6, patrones Strategy/Observer/Adapter/DAO).

Tamaños: **S** = ~1-2 h, **M** = ~3-5 h, **L** = ~6-10 h.
Personas: **P1 a P5** (se reemplazan por nombres reales al asignar).
Cada tarea tiene un ID para usarlo como título de tarjeta en Trello (ej: `[2.3] Clase Persona`).

---

## Estructura del tablero Trello

**Listas (columnas):**
`Backlog` → `Listo para tomar` → `En progreso` → `En revisión` → `Hecho` (+ `Bloqueado`)

**Etiquetas (labels):**
- Por fase: `F0-Setup`, `F1-Modelo`, `F2-Persistencia`, `F3-Etapa1`, `F4-Etapa2`, `F5-Vista`, `F6-Cierre`
- Por funcionalidad: `F1-Asignación`, `F2-Notif`, `F3-QR`, `F4-Panel`, `F5-Seguridad`, `F6-Reasignación`
- Por tipo: `Código`, `Test`, `Docs`, `Decisión`

**Reglas del equipo (poner en una tarjeta fijada):**
1. Una tarjeta = una persona responsable.
2. Nada pasa a `Hecho` sin que *otra* persona lo revise (`En revisión`).
3. Máximo 2 tarjetas en `En progreso` por persona.
4. Si estás trabado, mover a `Bloqueado` y avisar en el grupo.

---

## FASE 0 – Setup y acuerdos (todos, antes de escribir código)

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 0.1 | Crear repositorio Git (GitHub) y dar acceso a los 5 | P1 | – | S |
| 0.2 | Acordar estrategia de ramas (main + una rama por tarjeta + Pull Request) | Todos | 0.1 | S |
| 0.3 | ✅ **Decidido:** Java 21 + Maven (con wrapper) + librerías (ver sección *Decisiones técnicas*) | Todos | – | S |
| 0.4 | ✅ **Decidido:** UI con Swing | Todos | – | S |
| 0.5 | ✅ **Decidido:** base de datos SQLite | Todos | – | S |
| 0.6 | Crear proyecto base con estructura de paquetes (`vista`, `controlador`, `modelo`, `servicios`, `dao`, `seguridad`) | P1 | 0.3 | S |
| 0.7 | Armar el tablero Trello con listas, labels y reglas | P2 | – | S |
| 0.8 | Revisar y aprobar las convenciones de la sección *Decisiones técnicas* | Todos | – | S |

**Hito 0:** proyecto compila vacío en la máquina de los 5.

---

## FASE 1 – Modelo base (Actividad 1)

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 1.1 | Clase abstracta `Persona` (atributos privados, getters/setters con validación: DNI, teléfono, mail) | P1 | 0.6 | M |
| 1.2 | Subclases `VoluntarioMedico`, `VoluntarioSeguridad`, `Coordinador` (+ otras si se acuerda: traductor, asistencia a mayores) | P1 | 1.1 | M |
| 1.3 | Enum/clase `Habilidad` (RCP, idiomas, etc.) y modelo de `Disponibilidad` horaria | P1 | 1.1 | M |
| 1.4 | Clase `PuntoDeControl` (zona, dotación requerida, lista de zonas cercanas) | P3 | 0.6 | M |
| 1.5 | Clase `Asignacion` (voluntario + punto + estado SUGERIDA/CONFIRMADA) | P3 | 1.2, 1.4 | S |
| 1.6 | Repositorio en memoria: `HashMap<DNI, Voluntario>` + `ArrayList` de asignados por punto | P1 | 1.2, 1.4 | M |
| 1.7 | Tests unitarios del modelo y validaciones | P5 | 1.1–1.6 | M |
| 1.8 | Diagrama de clases UML actualizado (Persona, subclases, PuntoDeControl, Asignacion, etc.) | P5 | 1.5 | M |

**Hito 1:** se pueden crear voluntarios y puntos en memoria y filtrarlos por habilidad desde un `main` de prueba.

---

## FASE 2 – Persistencia (DAO + base local)

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 2.1 | Diseño del esquema de BD (tablas: voluntarios, puntos, asignaciones, asistencia, usuarios, roles) + diagrama ER | P2 | 1.5 | M |
| 2.2 | Clase de conexión y script de creación de tablas | P2 | 2.1 | M |
| 2.3 | Interfaz `VoluntarioDAO` + implementación | P2 | 2.2, 1.2 | M |
| 2.4 | `PuntoDeControlDAO` + `AsignacionDAO` | P2 | 2.2, 1.4, 1.5 | M |
| 2.5 | Carga inicial: al abrir la app, cargar BD → HashMap/ArrayList | P2 | 2.3, 1.6 | S |
| 2.6 | Datos de prueba (seed): ~30 voluntarios, 5 puntos con zonas cercanas | P5 | 2.3, 2.4 | S |
| 2.7 | Tests de los DAO | P5 | 2.3, 2.4 | M |

**Hito 2:** cerrar y abrir la app y los datos siguen ahí.

---

## FASE 3 – Etapa 1 de funcionalidades (F5, F1, F2, F3)

Orden definido en la Actividad 02: primero F5 y F1 (base), luego F2 y F3.

### F5 – Roles y autenticación
| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 3.1 | Modelo `Usuario` + enum `Rol` (ADMINISTRADOR, COORDINADOR, OPERADOR) + `UsuarioDAO` | P2 | 2.2 | M |
| 3.2 | Módulo de seguridad: login, hash de contraseñas, sesión actual | P2 | 3.1 | M |
| 3.3 | Matriz de permisos (tabla de la Act. 02 §5.5) como código: `tienePermiso(rol, accion)` | P2 | 3.2 | M |
| 3.4 | Tests de permisos por rol | P5 | 3.3 | S |

### F1 – Asignación automática sugerida
| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 3.5 | Interfaz `EstrategiaAsignacion` (patrón **Strategy**) | P3 | 1.5 | S |
| 3.6 | Estrategia por habilidad + disponibilidad (filtra el HashMap) | P3 | 3.5, 1.6 | L |
| 3.7 | `MotorAsignacion`: genera sugerencias para un punto, sin duplicar voluntarios | P3 | 3.6 | M |
| 3.8 | Confirmación de asignación (SUGERIDA → CONFIRMADA) + guardado vía DAO | P3 | 3.7, 2.4 | M |
| 3.9 | Tests del motor (casos: faltan voluntarios, sobran, ninguno disponible) | P5 | 3.7 | M |

### F2 – Notificaciones por mail
| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 3.10 | Interfaz `NotificadorMail` + adaptador SMTP (**Adapter**) | P4 | 0.3 | M |
| 3.11 | `ServicioNotificaciones`: arma el mail (puesto, horario, punto de encuentro, QR) | P4 | 3.10, 3.8 | M |
| 3.12 | Cola de reintentos si falla la red (los avisos no se pierden) | P4 | 3.11 | M |
| 3.13 | Adaptador "falso" (consola/log) para tests y demo sin internet | P4 | 3.10 | S |

### F3 – Check-in / check-out con QR
| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 3.14 | Generación del QR personal (ZXing) para incluir en el mail | P4 | 3.11 | M |
| 3.15 | `RegistroAsistencia` (check-in/out por asignación) + `AsistenciaDAO` | P3 | 3.8, 2.2 | M |
| 3.16 | Lectura de QR (webcam o, como plan B, ingreso manual del código) | P4 | 3.14 | L |
| 3.17 | `ControladorAsistencia` | P1 | 3.15 | S |

**Hito 3:** desde consola/tests: login → sugerir → confirmar → mail (o log) → check-in.

---

## FASE 4 – Etapa 2 de funcionalidades (F4, F6)

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 4.1 | Enum `EstadoCobertura` (CUBIERTO, DEFICIT, SOBRANTE) | P3 | 1.4 | S |
| 4.2 | `CalculadorCobertura`: dotación requerida vs. presentes (usa asistencia) | P3 | 4.1, 3.15 | M |
| 4.3 | Patrón **Observer**: el modelo notifica al panel ante cambio de asignación/check-in | P1 | 4.2 | M |
| 4.4 | `ControladorCobertura` | P1 | 4.2, 4.3 | S |
| 4.5 | `EstrategiaReasignacion` (prioriza presentes y de zonas cercanas con sobrante) | P3 | 3.5, 4.2 | L |
| 4.6 | `MotorReasignacion` (reutiliza el motor de F1) | P3 | 4.5 | M |
| 4.7 | Aviso de cambios a voluntarios afectados (reusa F2) | P4 | 4.6, 3.11 | S |
| 4.8 | Tests de cobertura, Observer y reasignación | P5 | 4.2–4.6 | M |

**Hito 4:** simulación en tests: un punto queda con déficit → se sugiere mover gente de zona cercana.

---

## FASE 5 – Vista (puede arrancar en paralelo desde la Fase 2 con datos falsos)

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 5.1 | Mockups/bocetos en papel o Figma de todas las pantallas (acordar entre todos) | P4 + P1 | 0.4 | M |
| 5.2 | Ventana principal + navegación por rol (cada rol ve solo lo suyo) | P1 | 3.3, 5.1 | M |
| 5.3 | Pantalla de Login | P1 | 3.2 | S |
| 5.4 | Formularios de alta/baja/modificación de voluntarios + `ControladorVoluntarios` | P1 | 1.6, 2.3 | L |
| 5.5 | Pantalla de alta/edición de puntos de control (con zonas cercanas) | P5 | 1.4, 2.4 | M |
| 5.6 | Pantalla de asignación (sugerencias F1 + F6, botón confirmar) + `ControladorAsignacion` | P3 | 3.8, 4.6 | L |
| 5.7 | Panel de cobertura (colores verde/rojo/amarillo, se actualiza solo) | P5 | 4.4 | L |
| 5.8 | Pantalla del lector de QR / check-in | P4 | 3.16 | M |
| 5.9 | Pantalla de gestión de usuarios y roles (admin) | P2 | 3.3 | M |

**Hito 5:** la app completa se usa desde la interfaz.

---

## FASE 6 – Integración, pruebas y entrega

| ID | Tarea | Dueño | Depende | Tam |
|----|-------|-------|---------|-----|
| 6.1 | Prueba integral de **Flujo A** (asignar y avisar) según Act. 02 §5.6 | Todos | 5.* | M |
| 6.2 | Prueba integral de **Flujo B** (día del evento y emergencia) | Todos | 5.* | M |
| 6.3 | Revisión de seguridad: permisos por rol, datos sensibles privados, Ley 25.326 | P2 | 6.1 | S |
| 6.4 | Corrección de bugs encontrados (tarjetas nuevas por cada bug) | Todos | 6.1, 6.2 | L |
| 6.5 | Verificar que el código respete los 4 pilares y los patrones (Strategy, Observer, Adapter, DAO) | P5 | 6.4 | M |
| 6.6 | Manual de usuario / README (cómo instalar y ejecutar) | P5 | 6.4 | M |
| 6.7 | Informe final / documentación para la cátedra (actualizar diagramas con lo realmente implementado) | P1 | 6.4 | M |
| 6.8 | Guion y ensayo de la demo / presentación | Todos | 6.4 | M |
| 6.9 | Entrega (empaquetar, subir, verificar que corre en otra PC) | P1 | 6.6–6.8 | S |

---

## Resumen de carga por persona (aprox.)

| Persona | Foco principal | Tarjetas |
|---------|----------------|----------|
| P1 | Modelo (personas/voluntarios) + Controladores + UI de voluntarios y login | ~14 |
| P2 | Persistencia + Seguridad (F5) + gestión de usuarios | ~14 |
| P3 | Motores de asignación/reasignación (F1, F6) + cobertura | ~14 |
| P4 | Servicios externos: mail (F2), QR (F3) + pantalla QR | ~10 |
| P5 | Tests, UML, datos de prueba, panel de cobertura, docs | ~14 |

> Es una propuesta: lo ideal es que cada uno elija el área que más quiere aprender y rotar algunas tarjetas de revisión.

---

## Camino crítico (qué bloquea a qué)

```
0 Setup → 1 Modelo → 2 Persistencia → 3 (F5, F1) → F2 + F3 → 4 (F4 → F6) → 5 Vista final → 6 Cierre
                         ↘ Vista (5.1–5.4) puede ir en paralelo con datos falsos
```

---

## Decisiones técnicas (0.3, 0.4, 0.5) – basadas en lo visto en clase (UVA 4, 5 y 6)

**Qué usamos en clase:** Java "plano" (sin Maven ni librerías), `ArrayList`, `Comparator`, `Iterator`, excepciones propias (`DatosInvalidosException`, `ArchivoException`), `Persona` abstracta con método polimórfico `getRol()`, Swing con `JFrame`/`JTable`/`AbstractTableModel`, look & feel Nimbus, y persistencia en `.txt`. Seguimos ese estilo y solo agregamos lo que el proyecto necesita.

| Tema | Decisión | Por qué |
|---|---|---|
| Java | **Java 21 (LTS)**, `release 21` en el `pom.xml` | Compila igual en cualquier PC con JDK 21 o superior (hay quien tiene el 26) |
| Build | **Maven con wrapper (`mvnw`)** | Es lo único nuevo respecto a clase, pero hace falta: SQLite, mail y QR son librerías externas. El wrapper evita instalar Maven en cada PC |
| UI | **Swing**, Nimbus como en UVA 6 | Ya lo vimos; reutilizamos `AbstractTableModel` y el cierre con `WindowListener` |
| Base de datos | **SQLite** vía `org.xerial:sqlite-jdbc`, archivo `voluntariado.db` | Un solo archivo, sin servidor. Reemplaza los `.txt` de UVA 6 pero con la misma idea: una clase que carga y guarda, aislada en el DAO |
| Tests | **JUnit 5** | Estándar, lo corre Maven |
| Mail (F2) | **Jakarta Mail (Angus)** detrás del adaptador | SMTP sin costo por mensaje (decisión de la Act. 02) |
| QR (F3) | **ZXing** (`core` + `javase`) para generar; lectura por webcam opcional | Plan B: ingresar el código a mano |

**Convenciones de código (heredadas de clase):**
- Código y comentarios en español, sin tildes ni ñ en identificadores (`ano`, no `año`).
- Atributos privados + getters; validar en el constructor/setters y lanzar `DatosInvalidosException`.
- Excepciones propias en vez de `return null`/códigos de error (agregamos `PersistenciaException`, análoga a `ArchivoException`).
- Las clases del modelo **no imprimen ni abren ventanas**: devuelven resultados (como `Biblioteca` en UVA 4).
- Una única instancia del "sistema" compartida entre ventanas (como `Biblioteca` en UVA 6).
- Comparadores como clases `ComparadorPor...` cuando haya que ordenar.
- Paquetes: `vista`, `controlador`, `modelo`, `servicios`, `dao`, `seguridad`.
- Git: una rama por tarjeta, Pull Request revisado por otra persona.

**Nota sobre Java 26:** Maven necesita un JDK, no solo el JRE. Con `release 21` no hay problema, pero si alguien no puede instalar Maven en su PC, el wrapper lo resuelve.
