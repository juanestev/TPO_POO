# Sistema de Gestion de Voluntariado – Visita del Papa Leon XIV

TPO de Programacion Orientada a Objetos (UADE) – Grupo 3.
Aplicacion de escritorio en Java (Swing + SQLite) para registrar voluntarios, asignarlos a puntos de control, avisarles por mail, controlar su presencia con QR y monitorear la cobertura.

## Requisitos
- JDK 21 o superior (hace falta el JDK, no solo el JRE).
- No hace falta instalar Maven: el proyecto trae `mvnw` (la primera vez descarga Maven solo).

## Comandos
En Windows PowerShell/CMD usar `mvnw.cmd` en lugar de `./mvnw`.

```bash
./mvnw compile       # compilar
./mvnw test          # correr los tests
./mvnw exec:java     # abrir la aplicacion
```

### Demo para la catedra

En PowerShell, desde la raiz del proyecto:

```powershell
.\mvnw.cmd -q '-Dexec.mainClass=voluntariado.DemoFlujo' compile exec:java
```

La demo usa una base SQLite temporal y datos ficticios. Muestra sugerencias, confirmacion persistida,
cuatro avisos simulados, lectura de QR, check-in/out y el cambio de cobertura de DEFICIT a CUBIERTO
y de nuevo a DEFICIT. No envia mails reales ni modifica `voluntariado.db`.

## Estructura
```
src/main/java/voluntariado/
├── Main.java        punto de entrada
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
- Diagrama de clases del modelo: [docs/modelo-clases.md](docs/modelo-clases.md)
- Esquema de la base (DER y decisiones): [docs/esquema-bd.md](docs/esquema-bd.md)

## Estado del desarrollo
El modelo permite sugerir y confirmar asignaciones, registrar entrada/salida con un codigo QR y calcular cobertura. Tambien hay una matriz de permisos por rol y adaptadores de mail. Voluntarios, puntos, asignaciones y asistencia se guardan en SQLite (`voluntariado.db`, se crea solo al abrir la app; si esta vacia se cargan 30 voluntarios y 5 puntos de prueba). La ventana principal sigue siendo una pantalla de prueba: los flujos todavia no estan conectados a la interfaz. La cola de mails pendientes solo dura mientras la aplicacion esta abierta.
