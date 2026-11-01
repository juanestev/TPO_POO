# CLAUDE.md – Contexto del proyecto

Leé este archivo completo antes de responder. Después leé `PLAN_TPO.md` (plan de tareas y decisiones) y `README.md`.

## 1. Qué es esto
TPO de **Programación Orientada a Objetos** (UADE, Ingeniería en Informática, Grupo 3, profesor Moises Evaristo Bueno).
Tema: **Sistema de Gestión de Voluntariado** para la visita del Papa León XIV a la Argentina. Aplicación de escritorio en Java que registra voluntarios, los asigna a puntos de control, les avisa por mail, controla su presencia con QR y monitorea la cobertura.
Equipo de 5 personas; cada una trabaja en tarjetas distintas del Trello (ver `PLAN_TPO.md`, IDs tipo `3.6`).

Documentos de origen (entregados a la cátedra, fuera del repo): Actividad 01 (propuesta en Java) y Actividad 02 (arquitectura). Lo esencial está resumido acá y en `PLAN_TPO.md`.

## 2. Cómo tenés que trabajar con el usuario (MUY IMPORTANTE)
El usuario es estudiante y **quiere aprender y participar**, no recibir todo hecho.
- **Explicá el porqué** de cada decisión de diseño, con lenguaje simple y breve.
- **Hacé una tarjeta del plan por vez.** No adelantes tareas de otras fases ni de otras personas.
- Ante una decisión de diseño con alternativas reales, **proponé una recomendación y preguntá** antes de implementar.
- Dejá partes sencillas para que las escriba el usuario cuando tenga sentido (ej: validaciones, un test), indicando qué falta.
- Al terminar una tarea: resumí qué se hizo, qué concepto de POO se usó y cuál es la siguiente tarjeta.
- Respondé en **español rioplatense**, tono cercano pero claro.
- No borres ni sobrescribas archivos sin mirar antes qué hay; no hagas commits ni push salvo que se pida.
- Si el usuario pregunta por una tarea de otra persona del grupo, ayudá igual, pero avisá que es de otra tarjeta.

## 3. Decisiones técnicas ya tomadas (no re-discutir)
- **Java 21** (`release 21` en el pom), **Maven con wrapper** (`./mvnw`, en Windows `mvnw.cmd`).
- **UI: Swing**, Look & Feel Nimbus (como en la materia).
- **Base de datos: SQLite** (`org.xerial:sqlite-jdbc`), archivo `voluntariado.db` (ignorado por git).
- **Tests: JUnit 5.** Mail: Angus Mail (Jakarta Mail). QR: ZXing (`core` + `javase`).
- Comandos: `./mvnw compile`, `./mvnw test`, `./mvnw exec:java` (clase principal `voluntariado.Main`).

## 4. Arquitectura (resumen de la Actividad 02)
Arquitectura **en capas con MVC**; cada capa solo habla con la inmediata inferior. La Vista nunca accede a la BD.

```
vista → controlador → modelo → servicios/dao
```
Paquetes en `src/main/java/voluntariado/`: `vista`, `controlador`, `modelo`, `servicios`, `dao`, `seguridad` (la seguridad es transversal a todas las capas).

**Patrones:** Strategy (criterios de asignación intercambiables; F1 y F6 comparten motor), Observer (el panel de cobertura se actualiza solo), Adapter (mail detrás de una interfaz propia), DAO (el modelo no conoce la base).

**Modelo:** `Persona` (abstracta) → `VoluntarioMedico`, `VoluntarioSeguridad`, `Coordinador`; `PuntoDeControl` (zona, dotación requerida, lista de zonas cercanas); `Asignacion` (voluntario + punto, estado SUGERIDA → CONFIRMADA); `RegistroAsistencia` (check-in/out por asignación); enum de estado de cobertura (CUBIERTO / DEFICIT / SOBRANTE).
Datos en memoria: `HashMap<DNI, Voluntario>` para búsqueda instantánea + `ArrayList` de voluntarios asignados por punto. Los datos sensibles (DNI, teléfono, disponibilidad) son privados y se acceden con métodos que validan.

**Funcionalidades:**
| F | Qué hace | Componente |
|---|---|---|
| F1 | Asignación automática sugerida (el coordinador confirma) | `MotorAsignacion`, `ControladorAsignacion` |
| F2 | Mail con puesto, horario, punto de encuentro y QR; avisa cambios | `ServicioNotificaciones` (adaptador) |
| F3 | Check-in / check-out con QR | `RegistroAsistencia`, `ControladorAsistencia` |
| F4 | Panel de cobertura en tiempo real | `CalculadorCobertura`, panel (Vista) |
| F5 | Roles y autenticación | módulo `seguridad` |
| F6 | Reasignación por emergencias (zonas cercanas) | `MotorReasignacion` (reutiliza F1) |

**Roles (F5):** Administrador (gestiona usuarios; carga voluntarios y puntos; ve panel), Coordinador (confirma asignaciones/reasignaciones; check-in/out; ve panel), Operador (carga voluntarios y puntos; check-in/out; ve panel).

**Alcance acordado:** avisos solo por mail (sin SMS); cercanía por lista de zonas vecinas, sin mapa; sin cliente-servidor; si falla la red, los avisos quedan en cola. Orden de desarrollo: primero F5, F1, F2, F3; después F4 y F6. Cumplir la Ley 25.326 (datos personales).

## 5. Convenciones de código (heredadas de las clases UVA 4, 5 y 6 de la materia)
- Código y comentarios **en español**, **sin tildes ni ñ en identificadores** (`ano`, no `año`).
- Atributos privados + getters; validar en constructor/setters y lanzar **excepciones propias** (`DatosInvalidosException`; agregar `PersistenciaException`) en vez de devolver `null` o códigos.
- El **modelo no imprime ni abre ventanas**: devuelve resultados (como `Biblioteca` en UVA 4).
- Una única instancia del "sistema" compartida entre ventanas (como `Biblioteca` en UVA 6).
- `Persona` abstracta con método polimórfico (`getRol()`), `equals`/`hashCode` por clave (DNI), `Comparator` como clases `ComparadorPor...`, `Iterator` para borrar mientras se recorre.
- Swing: `JFrame` + `AbstractTableModel` para tablas; cierre con `WindowListener` que guarda los datos.
- Comentarios breves que expliquen el **porqué**, estilo de la cátedra (simple, didáctico).
- Sin sobre-ingeniería: es un TPO de nivel universitario inicial; preferir claridad a sofisticación.
- Git: una rama por tarjeta (`feature/1.1-persona`), Pull Request revisado por otra persona, `main` siempre compila.

## 6. Estado actual
- Las seis funcionalidades (F1 a F6) están implementadas con interfaz Swing, SQLite y tests (JUnit 5, todos pasando). `Sistema` arma todas las piezas y `Main` abre `VentanaLogin`.
- Seguridad (F5): `Usuario`, `UsuarioDAO`, hash PBKDF2 con sal (`ServicioClaves`), `ServicioAutenticacion`, `Sesion`, matriz `Permisos`. Los permisos se exigen en la ventana (qué pestañas ve cada rol) **y** en los controladores. Usuarios de prueba (admin, coordinador, operador) en el README.
- Pantallas en `vista/`: login, ventana principal por rol, voluntarios, puntos, asignación (F1 + reasignación F6), check-in/out con QR, cobertura (Observer) y usuarios.
- La cola de mails pendientes se guarda en SQLite (`mail_pendiente`). Por defecto los mails se simulan; el SMTP real se activa con variables de entorno y **no se probó contra un servidor**.
- Empaquetado: `./mvnw package` genera `target/voluntariado.jar` ejecutable. Documentación para la cátedra en `docs/` (arquitectura y patrones, seguridad, guion de demo, esquema de BD, UML).
- Pendiente de personas, no de código: revisión cruzada de las tarjetas ("pendiente de revisión" en `PLAN_TPO.md`), ensayo de la demo, verificar que corre en otra PC y el informe final para la cátedra. Límites conocidos en `docs/seguridad.md`.
- Si cambia el JDK: el proyecto exige Java 21 o superior para compilar (en la PC de Juan, `JAVA_HOME` apunta al 17; el JDK 25 está en `C:\Program Files\Java\jdk-25.0.2`).

## 7. Material de referencia de la materia
Ejemplos de clase del usuario (UVA 4, 5, 6 – biblioteca con Swing, excepciones y archivos de texto) están en `C:\Users\jmestevez\Documents\POO\` (`uva_4.zip`, `uva_5.zip`, `uva6.zip`). Consultalos para mantener el mismo estilo.
