-- Esquema de la base local (SQLite). Se ejecuta al abrir la app: IF NOT EXISTS lo hace seguro de repetir.
-- SQLite no tiene tipo fecha/hora: se guardan como texto ISO-8601 (ej: 08:00, 2026-10-26T08:00), que ordena bien.
-- Los valores de los enums de Java se controlan con CHECK para que la base no acepte basura.

-- Voluntarios: una sola tabla para Medico y Seguridad, con la columna "tipo" para saber que subclase crear.
-- Las subclases no tienen atributos propios, asi que una tabla por subclase solo agregaria JOINs.
CREATE TABLE IF NOT EXISTS voluntario (
    dni       TEXT PRIMARY KEY CHECK (dni GLOB '[0-9][0-9][0-9][0-9][0-9][0-9][0-9]' OR dni GLOB '[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]'),
    tipo      TEXT NOT NULL CHECK (tipo IN ('MEDICO', 'SEGURIDAD')),
    nombre    TEXT NOT NULL,
    apellido  TEXT NOT NULL,
    telefono  TEXT NOT NULL,
    mail      TEXT NOT NULL
);

-- Relacion N:M voluntario-habilidad. La PK compuesta evita repetir una habilidad.
CREATE TABLE IF NOT EXISTS voluntario_habilidad (
    dni        TEXT NOT NULL REFERENCES voluntario (dni) ON DELETE CASCADE,
    habilidad  TEXT NOT NULL CHECK (habilidad IN ('RCP', 'PRIMEROS_AUXILIOS', 'INGLES', 'PORTUGUES', 'ASISTENCIA_MAYORES', 'CONTROL_ACCESOS')),
    PRIMARY KEY (dni, habilidad)
);

CREATE TABLE IF NOT EXISTS voluntario_disponibilidad (
    id     INTEGER PRIMARY KEY,
    dni    TEXT NOT NULL REFERENCES voluntario (dni) ON DELETE CASCADE,
    dia    TEXT NOT NULL CHECK (dia IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    desde  TEXT NOT NULL,
    hasta  TEXT NOT NULL,
    CHECK (desde < hasta),
    UNIQUE (dni, dia, desde, hasta)
);

CREATE TABLE IF NOT EXISTS punto_control (
    codigo              TEXT PRIMARY KEY,
    zona                TEXT NOT NULL,
    dotacion_requerida  INTEGER NOT NULL CHECK (dotacion_requerida > 0)
);

-- NOCASE: igual que en el modelo, "Centro" y "centro" son la misma zona.
CREATE TABLE IF NOT EXISTS punto_zona_cercana (
    codigo  TEXT NOT NULL REFERENCES punto_control (codigo) ON DELETE CASCADE,
    zona    TEXT NOT NULL COLLATE NOCASE,
    PRIMARY KEY (codigo, zona)
);

-- RESTRICT: no se puede borrar un voluntario o un punto que tiene asignaciones (se perderia historia).
CREATE TABLE IF NOT EXISTS asignacion (
    codigo_qr     TEXT PRIMARY KEY,
    dni           TEXT NOT NULL REFERENCES voluntario (dni) ON DELETE RESTRICT,
    codigo_punto  TEXT NOT NULL REFERENCES punto_control (codigo) ON DELETE RESTRICT,
    estado        TEXT NOT NULL CHECK (estado IN ('SUGERIDA', 'CONFIRMADA'))
);

-- Misma regla que el repositorio en memoria: un voluntario tiene como mucho UNA asignacion confirmada.
-- Es un indice parcial: solo mira las filas CONFIRMADA, las sugeridas pueden repetirse.
CREATE UNIQUE INDEX IF NOT EXISTS ux_asignacion_confirmada_por_dni ON asignacion (dni) WHERE estado = 'CONFIRMADA';
CREATE INDEX IF NOT EXISTS ix_asignacion_punto ON asignacion (codigo_punto);

-- Check-in / check-out (lo usa la tarjeta 3.15). Una fila por asignacion.
CREATE TABLE IF NOT EXISTS asistencia (
    codigo_qr  TEXT PRIMARY KEY REFERENCES asignacion (codigo_qr) ON DELETE CASCADE,
    entrada    TEXT,
    salida     TEXT,
    CHECK (salida IS NULL OR (entrada IS NOT NULL AND salida >= entrada))
);

-- Usuarios del sistema (lo usan las tarjetas 3.1 y 3.2). El rol se guarda con el nombre del enum Rol.
-- Nunca se guarda la contrasena: solo su hash y la sal con la que se calculo.
CREATE TABLE IF NOT EXISTS usuario (
    nombre_usuario  TEXT PRIMARY KEY COLLATE NOCASE,
    hash_clave      TEXT NOT NULL,
    sal             TEXT NOT NULL,
    rol             TEXT NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'COORDINADOR', 'OPERADOR')),
    activo          INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0, 1))
);
