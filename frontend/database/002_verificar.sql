-- Consultas de solo lectura después de importar 001_tutoria.sql.
SELECT VERSION() AS version_servidor, DATABASE() AS base_actual;

SELECT TABLE_TYPE, COUNT(*) AS cantidad
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'tutoria'
GROUP BY TABLE_TYPE;
-- Esperado: 24 BASE TABLE y 4 VIEW.

SELECT CONSTRAINT_TYPE, COUNT(*) AS cantidad
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'tutoria'
GROUP BY CONSTRAINT_TYPE;

SELECT TABLE_NAME, ENGINE, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'tutoria' AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;

SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME,
       REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'tutoria' AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION;

SELECT codigo, nombre FROM tutoria.roles ORDER BY id;
SELECT codigo, nombre FROM tutoria.motivos_reporte ORDER BY id;
SELECT COUNT(*) AS usuarios_reales_o_prueba FROM tutoria.usuarios;
-- Esperado después de la creación: 3 roles, 4 motivos y 0 usuarios.
