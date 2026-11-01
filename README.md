# Sistema de Gestion de Voluntariado – Visita del Papa Leon XIV

TPO de Programacion Orientada a Objetos (UADE) – Grupo 3.
Aplicacion de escritorio en Java (Swing + SQLite) para registrar voluntarios, asignarlos a puntos de control, avisarles por mail, controlar su presencia con QR y monitorear la cobertura.

## Requisitos
- JDK 21 o superior (hace falta el JDK, no solo el JRE).
- No hace falta instalar Maven: el proyecto trae `mvnw` (la primera vez descarga Maven solo).

## Como ejecutarlo

En Windows PowerShell/CMD usar `mvnw.cmd` en lugar de `./mvnw`.

```bash
./mvnw exec:java     # abrir la aplicacion
./mvnw test          # correr los tests
./mvnw compile       # solo compilar
```

### Ejecutar desde un unico archivo (para entregar o probar en otra PC)

```bash
./mvnw package -DskipTests
java -jar target/voluntariado.jar
```

`target/voluntariado.jar` lleva adentro todas las librerias: solo hace falta Java 21 o superior. La base de datos
`voluntariado.db` se crea sola, junto a donde se ejecute, y se cargan 30 voluntarios y 5 puntos de prueba si esta vacia.

> Si Windows usa un JDK viejo (por ejemplo la variable `JAVA_HOME` apunta al 17), el error es
> `release version 21 not supported`. Se arregla apuntando `JAVA_HOME` a un JDK 21 o superior.

## Usuarios de prueba

Al abrir la app por primera vez se crean tres usuarios, uno por rol. **Son claves de prueba: cambiarlas en un uso real.**

| Usuario | Clave | Rol | Puede |
|---|---|---|---|
| `admin` | `Admin2026` | Administrador | gestionar usuarios; cargar voluntarios y puntos; ver cobertura |
| `coordinador` | `Coord2026` | Coordinador | confirmar asignaciones y reasignaciones; check-in/out; ver cobertura |
| `operador` | `Oper2026x` | Operador | cargar voluntarios y puntos; check-in/out; ver cobertura |

Cada rol ve solo las pestañas que le corresponden. Tras 5 intentos fallidos, el login se bloquea 30 segundos.

## Manual de uso

1. **Ingreso.** Escribir usuario y clave. Menu *Sesion* → *Cerrar sesion* para cambiar de usuario.
2. **Voluntarios** (admin y operador). Elegir uno de la tabla para editarlo, o *Nuevo*. Se completan datos, habilidades y disponibilidad horaria (dia + desde/hasta, boton *Agregar*). *Eliminar* da de baja (no se puede si ya tiene asignacion confirmada).
3. **Puntos de control** (admin y operador). Codigo, zona, dotacion requerida y *zonas cercanas* separadas por coma: son las zonas desde donde se puede traer gente en una emergencia.
4. **Asignacion** (coordinador).
   - *Sugerir asignacion*: elegir punto, habilidad y turno; el sistema propone voluntarios libres y disponibles. Escribir el punto de encuentro y *Confirmar*: se guarda, se arma el mail con el QR y se envia (o queda en cola si no hay red).
   - *Sugerir reasignacion (emergencia)*: para un punto con deficit, propone mover voluntarios **presentes** de zonas cercanas con sobrante. Al confirmar, el voluntario cambia de punto, recibe un QR nuevo y un aviso de cambio.
5. **Check-in / Check-out** (coordinador y operador). Escribir o elegir el codigo QR del voluntario y *Registrar entrada/salida*, o leer una imagen del QR con *Entrada/Salida con imagen QR...*. Con *Guardar QR de la fila elegida* se obtiene una imagen para probar el lector.
6. **Cobertura** (todos). Tabla por punto: **verde** cubierto, **rojo** deficit, **amarillo** sobrante. Se actualiza sola cuando cambian asignaciones o entradas/salidas.
7. **Usuarios** (admin). Crear usuarios, cambiar rol, activar/desactivar, restablecer clave y eliminar. No se puede quitar el acceso al propio usuario ni dejar al sistema sin administrador activo.

### Mails

Por defecto los mails se **simulan** (no se envia nada real). Para enviar con un servidor SMTP real, definir estas variables de entorno antes de abrir la app:
`VOLUNTARIADO_SMTP_HOST`, `VOLUNTARIADO_SMTP_PUERTO`, `VOLUNTARIADO_SMTP_USUARIO` y `VOLUNTARIADO_SMTP_CLAVE`.
Si no hay conexion, los avisos quedan guardados en la base y se reintentan al cerrar la app.

## Demo de consola para la catedra

Recorre el flujo completo sin interfaz, con una base temporal y datos ficticios (no modifica `voluntariado.db`):

```powershell
.\mvnw.cmd -q '-Dexec.mainClass=voluntariado.DemoFlujo' compile exec:java
```

El guion sugerido para la presentacion esta en [docs/guion-demo.md](docs/guion-demo.md).

## Estructura
```
src/main/java/voluntariado/
├── Main.java        punto de entrada
├── Sistema.java     arma las piezas una sola vez (instancia compartida por las ventanas)
├── vista/           pantallas Swing
├── controlador/     traducen acciones de la vista en operaciones del sistema
├── modelo/          entidades y reglas (Persona, PuntoDeControl, motores...)
├── servicios/       mail, QR y otros servicios externos (adaptadores)
├── dao/             acceso a la base SQLite
└── seguridad/       login, roles y permisos
src/test/java/       tests JUnit 5 (mismos paquetes)
```

## Documentacion
- Plan de trabajo y decisiones tecnicas: [PLAN_TPO.md](PLAN_TPO.md)
- Arquitectura, pilares de POO y patrones: [docs/arquitectura-y-patrones.md](docs/arquitectura-y-patrones.md)
- Diagrama de clases del modelo: [docs/modelo-clases.md](docs/modelo-clases.md)
- Esquema de la base (DER y decisiones): [docs/esquema-bd.md](docs/esquema-bd.md)
- Revision de seguridad y datos personales (Ley 25.326): [docs/seguridad.md](docs/seguridad.md)
- Guion de la demo: [docs/guion-demo.md](docs/guion-demo.md)

## Estado y limites
Estan implementadas las seis funcionalidades (F1 a F6) con interfaz, base SQLite y tests automaticos.
Limites conocidos: el envio por SMTP real no se probo contra un servidor; no hay lectura de QR por webcam (se lee desde una imagen o se escribe el codigo); la base no esta cifrada. El detalle esta en [docs/seguridad.md](docs/seguridad.md).
