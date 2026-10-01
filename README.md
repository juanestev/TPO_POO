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
