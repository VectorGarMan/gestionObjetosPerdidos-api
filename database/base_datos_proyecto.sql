-- =====================================================================
--  UNIVERSIDAD CATÓLICA UNIVA — Calidad de Software
--  Proyecto: Software para la gestión de objetos perdidos
--  Fase 1: Persistencia de datos (script DDL + DML)
--  Equipo: Ernesto Gabriel Roque Ramos, Victor Garza Maldonado,
--          Diego Miguel Choza Suarez
--  Docente: Mtro. Adriel Palestino
--  Gestor: PostgreSQL 14 o superior
-- =====================================================================
--
--  PASO 0 — CREACIÓN DE LA BASE DE DATOS
--  PostgreSQL no permite crear una base de datos dentro del mismo bloque
--  que el resto del script. Ejecuta SOLO la siguiente instrucción
--  conectado a la base "postgres"; después conéctate a "objetos_perdidos"
--  y ejecuta el resto del archivo (desde la sección 1 hasta el final).
--
--  CREATE DATABASE objetos_perdidos
--      WITH ENCODING = 'UTF8'
--      TEMPLATE = template0;
--
-- =====================================================================


-- =====================================================================
-- 1. LIMPIEZA (permite volver a ejecutar el script desde cero)
-- =====================================================================
DROP TABLE IF EXISTS peticion          CASCADE;
DROP TABLE IF EXISTS objeto_etiqueta   CASCADE;
DROP TABLE IF EXISTS objeto            CASCADE;
DROP TABLE IF EXISTS usuario           CASCADE;
DROP TABLE IF EXISTS modulo            CASCADE;
DROP TABLE IF EXISTS etiqueta          CASCADE;
DROP TABLE IF EXISTS color             CASCADE;
DROP TABLE IF EXISTS tipo_objeto       CASCADE;
DROP FUNCTION IF EXISTS fn_actualizar_fecha_actualizacion() CASCADE;


-- =====================================================================
-- 2. FUNCIÓN DE AUDITORÍA
--    PostgreSQL no tiene "ON UPDATE CURRENT_TIMESTAMP" (exclusivo de
--    MySQL). Esta función se asocia a cada tabla mediante un trigger
--    para que fecha_actualizacion registre la hora de cada UPDATE.
-- =====================================================================
CREATE OR REPLACE FUNCTION fn_actualizar_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================================
-- 3. CATÁLOGOS (compartidos por el alta del administrador y los filtros
--    del estudiante; alimentan GET /api/catalogos/{tipo})
-- =====================================================================

-- 3.1 Tipo de objeto (filtro principal; obligatorio en el alta)
CREATE TABLE tipo_objeto (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(60)   NOT NULL,
    descripcion         TEXT,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tipo_objeto_nombre UNIQUE (nombre)
);

-- 3.2 Color (obligatorio en el alta)
CREATE TABLE color (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(40)   NOT NULL,
    codigo_hex          VARCHAR(7),
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_color_nombre     UNIQUE (nombre),
    CONSTRAINT ck_color_codigo_hex CHECK (codigo_hex IS NULL OR codigo_hex ~ '^#[0-9A-Fa-f]{6}$')
);

-- 3.3 Otras etiquetas (selección múltiple)
CREATE TABLE etiqueta (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(40)   NOT NULL,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_etiqueta_nombre UNIQUE (nombre)
);

-- 3.4 Módulo de resguardo (los módulos de la universidad)
CREATE TABLE modulo (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(60)   NOT NULL,
    ubicacion           VARCHAR(150)  NOT NULL,
    horario_atencion    VARCHAR(80),
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_modulo_nombre UNIQUE (nombre)
);


-- =====================================================================
-- 4. USUARIOS
--    Simula la base de datos de la universidad: la identificación
--    (POST /api/usuarios/identificacion) busca por correo y devuelve
--    nombre, correo, matrícula y rol.
-- =====================================================================
CREATE TABLE usuario (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(120)  NOT NULL,
    correo              VARCHAR(100)  NOT NULL,
    matricula           VARCHAR(15),
    telefono            VARCHAR(15),
    rol                 VARCHAR(20)   NOT NULL,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuario_correo     UNIQUE (correo),
    CONSTRAINT uq_usuario_matricula  UNIQUE (matricula),
    CONSTRAINT ck_usuario_rol        CHECK (rol IN ('ESTUDIANTE', 'ADMINISTRADOR')),
    CONSTRAINT ck_usuario_correo     CHECK (correo ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    CONSTRAINT ck_usuario_matricula  CHECK (matricula IS NULL OR matricula ~ '^[A-Za-z0-9]{1,15}$'),
    -- Todo estudiante debe tener matrícula (se usa para buscar peticiones)
    CONSTRAINT ck_usuario_estudiante_matricula
        CHECK (rol <> 'ESTUDIANTE' OR matricula IS NOT NULL)
);


-- =====================================================================
-- 5. OBJETO PERDIDO
--    "estado" es el estado de negocio (PERDIDO / RECLAMADO).
--    "estatus" es el borrado lógico (TRUE = activo, FALSE = eliminado).
--    La fotografía NO se guarda en la base: solo su ruta (ruta_imagen).
-- =====================================================================
CREATE TABLE objeto (
    id                  BIGSERIAL     PRIMARY KEY,
    nombre              VARCHAR(60)   NOT NULL,
    descripcion         VARCHAR(250)  NOT NULL,
    marca               VARCHAR(40),
    lugar_perdida       VARCHAR(80)   NOT NULL,
    ruta_imagen         VARCHAR(255)  NOT NULL,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PERDIDO',
    tipo_objeto_id      BIGINT        NOT NULL,
    color_id            BIGINT        NOT NULL,
    modulo_id           BIGINT        NOT NULL,
    registrado_por_id   BIGINT        NOT NULL,
    reclamado_por_id    BIGINT,
    fecha_reclamo       TIMESTAMP,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_objeto_tipo_objeto    FOREIGN KEY (tipo_objeto_id)    REFERENCES tipo_objeto (id),
    CONSTRAINT fk_objeto_color          FOREIGN KEY (color_id)          REFERENCES color (id),
    CONSTRAINT fk_objeto_modulo         FOREIGN KEY (modulo_id)         REFERENCES modulo (id),
    CONSTRAINT fk_objeto_registrado_por FOREIGN KEY (registrado_por_id) REFERENCES usuario (id),
    CONSTRAINT fk_objeto_reclamado_por  FOREIGN KEY (reclamado_por_id)  REFERENCES usuario (id),
    CONSTRAINT ck_objeto_estado        CHECK (estado IN ('PERDIDO', 'RECLAMADO')),
    CONSTRAINT ck_objeto_nombre_largo  CHECK (char_length(nombre) BETWEEN 3 AND 60 AND nombre = btrim(nombre)),
    CONSTRAINT ck_objeto_descripcion   CHECK (char_length(descripcion) BETWEEN 10 AND 250),
    CONSTRAINT ck_objeto_lugar_perdida CHECK (char_length(lugar_perdida) BETWEEN 3 AND 80),
    CONSTRAINT ck_objeto_ruta_imagen   CHECK (ruta_imagen ~* '^/uploads/objetos/[A-Za-z0-9_\-]+\.(jpg|jpeg|png)$'),
    -- Un objeto reclamado debe indicar quién lo reclamó y cuándo
    CONSTRAINT ck_objeto_reclamo_completo CHECK (
        (estado = 'PERDIDO'   AND reclamado_por_id IS NULL     AND fecha_reclamo IS NULL) OR
        (estado = 'RECLAMADO' AND reclamado_por_id IS NOT NULL AND fecha_reclamo IS NOT NULL)
    )
);


-- =====================================================================
-- 6. RELACIÓN N:M OBJETO — ETIQUETA ("Otras etiquetas")
-- =====================================================================
CREATE TABLE objeto_etiqueta (
    id                  BIGSERIAL     PRIMARY KEY,
    objeto_id           BIGINT        NOT NULL,
    etiqueta_id         BIGINT        NOT NULL,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_objeto_etiqueta_objeto   FOREIGN KEY (objeto_id)   REFERENCES objeto (id),
    CONSTRAINT fk_objeto_etiqueta_etiqueta FOREIGN KEY (etiqueta_id) REFERENCES etiqueta (id),
    CONSTRAINT uq_objeto_etiqueta UNIQUE (objeto_id, etiqueta_id)
);


-- =====================================================================
-- 7. PETICIÓN (reclamo o reclamo de error)
-- =====================================================================
CREATE TABLE peticion (
    id                  BIGSERIAL     PRIMARY KEY,
    objeto_id           BIGINT        NOT NULL,
    solicitante_id      BIGINT        NOT NULL,
    tipo                VARCHAR(20)   NOT NULL,
    estado              VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    liberada_por_id     BIGINT,
    fecha_liberacion    TIMESTAMP,
    estatus             BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_peticion_objeto       FOREIGN KEY (objeto_id)       REFERENCES objeto (id),
    CONSTRAINT fk_peticion_solicitante  FOREIGN KEY (solicitante_id)  REFERENCES usuario (id),
    CONSTRAINT fk_peticion_liberada_por FOREIGN KEY (liberada_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_peticion_tipo   CHECK (tipo   IN ('RECLAMO', 'RECLAMO_ERROR')),
    CONSTRAINT ck_peticion_estado CHECK (estado IN ('PENDIENTE', 'LIBERADA')),
    -- Una petición liberada debe indicar quién la liberó y cuándo
    CONSTRAINT ck_peticion_liberacion_completa CHECK (
        (estado = 'PENDIENTE' AND liberada_por_id IS NULL     AND fecha_liberacion IS NULL) OR
        (estado = 'LIBERADA'  AND liberada_por_id IS NOT NULL AND fecha_liberacion IS NOT NULL)
    )
);

-- Mensaje F-03 «Ya tienes una petición pendiente para este objeto.»:
-- un estudiante no puede tener dos peticiones pendientes del mismo objeto.
CREATE UNIQUE INDEX uq_peticion_pendiente
    ON peticion (objeto_id, solicitante_id)
    WHERE estado = 'PENDIENTE' AND estatus = TRUE;


-- =====================================================================
-- 8. ÍNDICES DE APOYO A LAS CONSULTAS DE LA API
-- =====================================================================
CREATE INDEX ix_objeto_estado          ON objeto (estado, estatus);
CREATE INDEX ix_objeto_tipo_objeto     ON objeto (tipo_objeto_id);
CREATE INDEX ix_objeto_color           ON objeto (color_id);
CREATE INDEX ix_objeto_modulo          ON objeto (modulo_id);
CREATE INDEX ix_objeto_etiqueta_etq    ON objeto_etiqueta (etiqueta_id);
CREATE INDEX ix_peticion_solicitante   ON peticion (solicitante_id);
CREATE INDEX ix_peticion_objeto        ON peticion (objeto_id);


-- =====================================================================
-- 9. TRIGGERS DE fecha_actualizacion (uno por tabla)
-- =====================================================================
CREATE TRIGGER trg_tipo_objeto_fecha_actualizacion
    BEFORE UPDATE ON tipo_objeto
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_color_fecha_actualizacion
    BEFORE UPDATE ON color
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_etiqueta_fecha_actualizacion
    BEFORE UPDATE ON etiqueta
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_modulo_fecha_actualizacion
    BEFORE UPDATE ON modulo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_usuario_fecha_actualizacion
    BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_objeto_fecha_actualizacion
    BEFORE UPDATE ON objeto
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_objeto_etiqueta_fecha_actualizacion
    BEFORE UPDATE ON objeto_etiqueta
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();

CREATE TRIGGER trg_peticion_fecha_actualizacion
    BEFORE UPDATE ON peticion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha_actualizacion();


-- =====================================================================
-- 10. DATOS DE PRUEBA (DML)
--     estatus, fecha_registro y fecha_actualizacion NO se envían:
--     se asignan solos por sus valores DEFAULT.
-- =====================================================================

-- 10.1 Tipos de objeto
INSERT INTO tipo_objeto (nombre, descripcion) VALUES
    ('Mochila',                'Mochilas, bolsas y estuches de transporte'),
    ('Credencial',             'Credenciales escolares o identificaciones'),
    ('Dispositivo electrónico','Celulares, audífonos, cargadores, calculadoras'),
    ('Útiles escolares',       'Cuadernos, libros, estuches y material de clase'),
    ('Ropa',                   'Chamarras, sudaderas, gorras y similares');

-- 10.2 Colores
INSERT INTO color (nombre, codigo_hex) VALUES
    ('Negro',  '#000000'),
    ('Blanco', '#FFFFFF'),
    ('Azul',   '#1E40AF'),
    ('Rojo',   '#DC2626'),
    ('Gris',   '#6B7280'),
    ('Verde',  '#15803D');

-- 10.3 Otras etiquetas
INSERT INTO etiqueta (nombre) VALUES
    ('Con funda'),
    ('Con llavero'),
    ('Con nombre visible'),
    ('Con estampas'),
    ('Dañado');

-- 10.4 Módulos de resguardo
INSERT INTO modulo (nombre, ubicacion, horario_atencion) VALUES
    ('Módulo A', 'Edificio A, planta baja, junto a Servicios Escolares', 'Lun a Vie 8:00–20:00'),
    ('Módulo B', 'Edificio de Ingenierías, vestíbulo principal',          'Lun a Vie 8:00–18:00'),
    ('Módulo C', 'Biblioteca, mostrador de préstamo',                     'Lun a Sáb 9:00–19:00');

-- 10.5 Usuarios (2 administradores, 4 estudiantes)
INSERT INTO usuario (nombre, correo, matricula, telefono, rol) VALUES
    ('Laura Méndez Ruiz',      'laura.mendez@univa.mx',      NULL,       '3312345601', 'ADMINISTRADOR'),
    ('Jorge Salas Pérez',      'jorge.salas@univa.mx',       NULL,       '3312345602', 'ADMINISTRADOR'),
    ('Ana Torres Gómez',       'ana.torres@alumnos.univa.mx','A2023001', '3398765401', 'ESTUDIANTE'),
    ('Luis Ramírez Ortega',    'luis.ramirez@alumnos.univa.mx','A2023002','3398765402', 'ESTUDIANTE'),
    ('María Fernanda Ibarra',  'maria.ibarra@alumnos.univa.mx','A2024015','3398765403', 'ESTUDIANTE'),
    ('Carlos Núñez Vega',      'carlos.nunez@alumnos.univa.mx','A2022087','3398765404', 'ESTUDIANTE');

-- 10.6 Objetos (4 perdidos y 2 ya reclamados)
INSERT INTO objeto (nombre, descripcion, marca, lugar_perdida, ruta_imagen, estado,
                    tipo_objeto_id, color_id, modulo_id, registrado_por_id,
                    reclamado_por_id, fecha_reclamo) VALUES
    ('Mochila negra con bolsillo lateral',
     'Mochila de dos compartimentos con bolsillo lateral para botella.',
     'JanSport', 'Cafetería central', '/uploads/objetos/objeto_01.jpg', 'PERDIDO',
     1, 1, 1, 1, NULL, NULL),
    ('Audífonos inalámbricos',
     'Audífonos tipo diadema, almohadillas desgastadas.',
     'Sony', 'Aula 204, Edificio de Ingenierías', '/uploads/objetos/objeto_02.png', 'PERDIDO',
     3, 2, 2, 1, NULL, NULL),
    ('Cuaderno profesional de cuadros',
     'Cuaderno de 100 hojas con apuntes de cálculo en la portada.',
     NULL, 'Biblioteca, segundo piso', '/uploads/objetos/objeto_03.jpg', 'PERDIDO',
     4, 3, 3, 2, NULL, NULL),
    ('Sudadera gris con capucha',
     'Sudadera de algodón con capucha y cierre frontal.',
     'Nike', 'Canchas deportivas', '/uploads/objetos/objeto_04.jpg', 'PERDIDO',
     5, 5, 1, 2, NULL, NULL),
    ('Celular con funda roja',
     'Teléfono con funda de silicón y estrellado en la esquina.',
     'Samsung', 'Explanada principal', '/uploads/objetos/objeto_05.jpg', 'RECLAMADO',
     3, 4, 2, 1, 3, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('Credencial escolar',
     'Credencial con porta gafete y cordón verde.',
     NULL, 'Estacionamiento norte', '/uploads/objetos/objeto_06.png', 'RECLAMADO',
     2, 6, 3, 2, 4, CURRENT_TIMESTAMP - INTERVAL '1 day');

-- 10.7 Etiquetas de cada objeto (N:M)
INSERT INTO objeto_etiqueta (objeto_id, etiqueta_id) VALUES
    (1, 2),   -- Mochila: con llavero
    (1, 4),   -- Mochila: con estampas
    (2, 5),   -- Audífonos: dañado
    (3, 3),   -- Cuaderno: con nombre visible
    (5, 1),   -- Celular: con funda
    (5, 5),   -- Celular: dañado
    (6, 2);   -- Credencial: con llavero

-- 10.8 Peticiones
INSERT INTO peticion (objeto_id, solicitante_id, tipo, estado, liberada_por_id, fecha_liberacion) VALUES
    -- Reclamos ya entregados (dieron origen a los objetos reclamados)
    (5, 3, 'RECLAMO',       'LIBERADA',  1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (6, 4, 'RECLAMO',       'LIBERADA',  2, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    -- Reclamo pendiente de entrega
    (1, 5, 'RECLAMO',       'PENDIENTE', NULL, NULL),
    -- Reclamo de error: Carlos reporta que el celular era suyo
    (5, 6, 'RECLAMO_ERROR', 'PENDIENTE', NULL, NULL);


-- 10.9 Fechas de registro coherentes con la historia de los datos
--      Los INSERT anteriores dejan todo con la fecha actual (DEFAULT), lo que
--      haría que los objetos 5 y 6 aparezcan reclamados ANTES de registrarse.
--      Se escalonan las fechas para que cada objeto se registre antes de su
--      reclamo y cada petición antes de liberarse.
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '2 days'  WHERE id = 1;
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '4 days'  WHERE id = 2;
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '1 day'   WHERE id = 3;
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '6 days'  WHERE id = 4;
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '7 days'  WHERE id = 5;  -- reclamado hace 3 días
UPDATE objeto SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '5 days'  WHERE id = 6;  -- reclamado hace 1 día

UPDATE peticion SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '4 days'   WHERE id = 1;  -- liberada hace 3 días
UPDATE peticion SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '2 days'   WHERE id = 2;  -- liberada hace 1 día
UPDATE peticion SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '20 hours' WHERE id = 3;
UPDATE peticion SET fecha_registro = CURRENT_TIMESTAMP - INTERVAL '12 hours' WHERE id = 4;


-- =====================================================================
-- 11. CONSULTAS DE VERIFICACIÓN (evidencia para la entrega)
-- =====================================================================

-- 11.1 Columnas de control presentes en TODAS las tablas
SELECT table_name, column_name, data_type, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND column_name IN ('estatus', 'fecha_registro', 'fecha_actualizacion')
ORDER BY table_name, column_name;

-- 11.2 Las imágenes solo se guardan como ruta (VARCHAR), no como binario
SELECT table_name, column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (column_name LIKE '%imagen%' OR data_type = 'bytea');

-- 11.3 estatus y fecha_registro asignados automáticamente
SELECT id, nombre, ruta_imagen, estado, estatus, fecha_registro, fecha_actualizacion
FROM objeto
ORDER BY id;

-- 11.4 Vista de la pantalla del estudiante (PNT-03): objetos PERDIDOS activos
SELECT o.id, o.nombre, o.marca, t.nombre AS tipo, c.nombre AS color,
       m.nombre AS modulo, o.lugar_perdida, o.fecha_registro AS perdido_el
FROM objeto o
JOIN tipo_objeto t ON t.id = o.tipo_objeto_id
JOIN color       c ON c.id = o.color_id
JOIN modulo      m ON m.id = o.modulo_id
WHERE o.estado = 'PERDIDO' AND o.estatus = TRUE
ORDER BY o.fecha_registro DESC;

-- 11.5 Peticiones por matrícula (PNT-08)
SELECT p.id, p.tipo, p.estado, o.nombre AS objeto, u.matricula, u.nombre AS solicitante
FROM peticion p
JOIN objeto  o ON o.id = p.objeto_id
JOIN usuario u ON u.id = p.solicitante_id
WHERE u.matricula = 'A2022087' AND p.estatus = TRUE;


-- 11.6 Coherencia de fechas: ambas consultas deben devolver 0 filas
SELECT id, fecha_registro, fecha_reclamo FROM objeto
WHERE fecha_reclamo IS NOT NULL AND fecha_reclamo < fecha_registro;

SELECT p.id, p.fecha_registro, o.fecha_registro AS objeto_registrado, p.fecha_liberacion
FROM peticion p JOIN objeto o ON o.id = p.objeto_id
WHERE p.fecha_registro < o.fecha_registro
   OR (p.fecha_liberacion IS NOT NULL AND p.fecha_liberacion < p.fecha_registro);


-- =====================================================================
-- 12. PRUEBAS OPCIONALES (descomenta para generar más evidencia)
-- =====================================================================

-- 12.1 Borrado lógico: "eliminar" el objeto 4 sin usar DELETE
-- UPDATE objeto SET estatus = FALSE WHERE id = 4;
-- SELECT id, nombre, estatus, fecha_registro, fecha_actualizacion FROM objeto WHERE id = 4;
-- (fecha_actualizacion cambia gracias al trigger; el registro sigue existiendo)

-- 12.2 Restricción de petición duplicada (debe fallar con uq_peticion_pendiente)
-- INSERT INTO peticion (objeto_id, solicitante_id, tipo) VALUES (1, 5, 'RECLAMO');

-- 12.3 Restricción de imagen (debe fallar con ck_objeto_ruta_imagen)
-- INSERT INTO objeto (nombre, descripcion, lugar_perdida, ruta_imagen,
--                     tipo_objeto_id, color_id, modulo_id, registrado_por_id)
-- VALUES ('Prueba', 'Descripción de prueba', 'Pasillo', 'data:image/png;base64,AAAA', 1, 1, 1, 1);
