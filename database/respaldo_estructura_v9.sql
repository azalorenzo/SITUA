-- Respaldo del esquema V9 de SITUA, exportado de MySQL en la VPS.
-- No contiene registros, datos personales ni credenciales.
-- Para instalar el proyecto, utilizar las migraciones de Flyway y el seed.
SET NAMES utf8mb4;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actividades_personales` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `propietario_id` bigint unsigned NOT NULL,
  `titulo` varchar(200) NOT NULL,
  `descripcion` text,
  `tipo` enum('taller','recordatorio') NOT NULL,
  `fecha` date DEFAULT NULL,
  `inicio_en` datetime(6) DEFAULT NULL,
  `fin_en` datetime(6) DEFAULT NULL,
  `tutor_id` bigint unsigned DEFAULT NULL,
  `responsable_texto` varchar(160) DEFAULT NULL,
  `ubicacion` varchar(200) DEFAULT NULL,
  `estado` enum('pendiente','completada','cancelada') NOT NULL DEFAULT 'pendiente',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_actividad_propietario` (`propietario_id`,`estado`,`fecha`,`inicio_en`),
  KEY `fk_actividad_tutor` (`tutor_id`),
  CONSTRAINT `fk_actividad_propietario` FOREIGN KEY (`propietario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_actividad_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `tutores` (`id`),
  CONSTRAINT `ck_actividad_responsable` CHECK (((`tutor_id` is null) or (`responsable_texto` is null))),
  CONSTRAINT `ck_actividad_tiempo` CHECK ((((`inicio_en` is null) and (`fin_en` is null)) or ((`fecha` is null) and (`inicio_en` is not null) and (`fin_en` is not null) and (`fin_en` > `inicio_en`))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `asignaciones_tutor` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `estudiante_id` bigint unsigned NOT NULL,
  `tutor_id` bigint unsigned NOT NULL,
  `periodo_id` bigint unsigned NOT NULL,
  `asignado_por` bigint unsigned NOT NULL,
  `inicio_en` datetime(6) NOT NULL,
  `fin_en` datetime(6) DEFAULT NULL,
  `estado` enum('pendiente','activa','finalizada','cancelada') NOT NULL DEFAULT 'pendiente',
  `motivo` text,
  `activa_unica` tinyint GENERATED ALWAYS AS ((case when (`estado` = _utf8mb4'activa') then 1 else NULL end)) STORED,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_asignacion_activa` (`estudiante_id`,`periodo_id`,`activa_unica`),
  KEY `ix_asignacion_tutor` (`tutor_id`,`periodo_id`,`estado`),
  KEY `fk_asignacion_autor` (`asignado_por`),
  CONSTRAINT `fk_asignacion_autor` FOREIGN KEY (`asignado_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_asignacion_situacion` FOREIGN KEY (`estudiante_id`, `periodo_id`) REFERENCES `situaciones_academicas` (`estudiante_id`, `periodo_id`),
  CONSTRAINT `fk_asignacion_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `tutores` (`id`),
  CONSTRAINT `ck_asignacion_cierre` CHECK ((((`estado` in (_utf8mb4'finalizada',_utf8mb4'cancelada')) and (`fin_en` is not null)) or ((`estado` in (_utf8mb4'pendiente',_utf8mb4'activa')) and (`fin_en` is null)))),
  CONSTRAINT `ck_asignacion_fechas` CHECK (((`fin_en` is null) or (`fin_en` >= `inicio_en`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aviso_destinatarios` (
  `aviso_id` bigint unsigned NOT NULL,
  `usuario_id` bigint unsigned NOT NULL,
  `leido_en` datetime(6) DEFAULT NULL,
  `ocultado_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`aviso_id`,`usuario_id`),
  KEY `ix_destinatario_lectura` (`usuario_id`,`leido_en`,`aviso_id`),
  CONSTRAINT `fk_destinatario_aviso` FOREIGN KEY (`aviso_id`) REFERENCES `avisos` (`id`),
  CONSTRAINT `fk_destinatario_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `avisos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `autor_id` bigint unsigned NOT NULL,
  `titulo` varchar(200) NOT NULL,
  `descripcion` text NOT NULL,
  `categoria` varchar(40) NOT NULL DEFAULT 'general',
  `prioridad` enum('normal','media','alta') NOT NULL DEFAULT 'normal',
  `sesion_id` bigint unsigned DEFAULT NULL,
  `publicado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `retirado_en` datetime(6) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_aviso_autor` (`autor_id`),
  KEY `fk_aviso_sesion` (`sesion_id`),
  CONSTRAINT `fk_aviso_autor` FOREIGN KEY (`autor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_aviso_sesion` FOREIGN KEY (`sesion_id`) REFERENCES `sesiones` (`id`),
  CONSTRAINT `ck_aviso_retiro` CHECK (((`retirado_en` is null) or (`retirado_en` >= `publicado_en`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `carreras` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `departamento_id` bigint unsigned NOT NULL,
  `clave` varchar(30) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `activa` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_carrera_clave` (`clave`),
  KEY `fk_carrera_departamento` (`departamento_id`),
  CONSTRAINT `fk_carrera_departamento` FOREIGN KEY (`departamento_id`) REFERENCES `departamentos` (`id`),
  CONSTRAINT `ck_carrera_activa` CHECK ((`activa` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversacion_participantes` (
  `conversacion_id` bigint unsigned NOT NULL,
  `usuario_id` bigint unsigned NOT NULL,
  `unido_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `leido_hasta_en` datetime(6) DEFAULT NULL,
  `leido_hasta_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`conversacion_id`,`usuario_id`),
  KEY `ix_participante_usuario` (`usuario_id`,`conversacion_id`),
  CONSTRAINT `fk_participante_conversacion` FOREIGN KEY (`conversacion_id`) REFERENCES `conversaciones` (`id`),
  CONSTRAINT `fk_participante_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversaciones` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tipo` enum('directa') NOT NULL DEFAULT 'directa',
  `creada_por` bigint unsigned NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `usuario_menor_id` bigint unsigned DEFAULT NULL,
  `usuario_mayor_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_conversacion_personas` (`usuario_menor_id`,`usuario_mayor_id`),
  KEY `fk_conversacion_autor` (`creada_por`),
  KEY `fk_conversacion_mayor` (`usuario_mayor_id`),
  CONSTRAINT `fk_conversacion_autor` FOREIGN KEY (`creada_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_conversacion_mayor` FOREIGN KEY (`usuario_mayor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_conversacion_menor` FOREIGN KEY (`usuario_menor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_conversacion_personas` CHECK ((((`usuario_menor_id` is null) and (`usuario_mayor_id` is null)) or ((`usuario_menor_id` is not null) and (`usuario_mayor_id` is not null) and (`usuario_menor_id` < `usuario_mayor_id`))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departamentos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(120) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_departamento_nombre` (`nombre`),
  CONSTRAINT `ck_departamento_activo` CHECK ((`activo` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disponibilidades_tutor` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tutor_id` bigint unsigned NOT NULL,
  `inicio_en` datetime(6) NOT NULL,
  `fin_en` datetime(6) NOT NULL,
  `modalidad` enum('presencial','virtual') NOT NULL,
  `ubicacion` varchar(200) DEFAULT NULL,
  `enlace_virtual` varchar(1024) DEFAULT NULL,
  `estado` enum('habilitada','bloqueada') NOT NULL DEFAULT 'habilitada',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_disponibilidad_contexto` (`id`,`tutor_id`,`inicio_en`,`fin_en`,`modalidad`),
  KEY `ix_disponibilidad_intervalo` (`tutor_id`,`inicio_en`,`fin_en`),
  CONSTRAINT `fk_disponibilidad_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `tutores` (`id`),
  CONSTRAINT `ck_disponibilidad_intervalo` CHECK ((`fin_en` > `inicio_en`)),
  CONSTRAINT `ck_disponibilidad_lugar` CHECK ((((`modalidad` = _utf8mb4'presencial') and (`ubicacion` is not null) and (char_length(trim(`ubicacion`)) > 0)) or ((`modalidad` = _utf8mb4'virtual') and (`enlace_virtual` is not null) and (char_length(trim(`enlace_virtual`)) > 0))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `envios_comunicacion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `sesion_id` bigint unsigned DEFAULT NULL,
  `canal` enum('correo','sms','whatsapp') NOT NULL,
  `destinatario` varchar(254) NOT NULL,
  `tipo_evento` varchar(60) NOT NULL,
  `clave_idempotencia` varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `estado` enum('pendiente','en_proceso','enviado','entregado','fallido') NOT NULL DEFAULT 'pendiente',
  `proveedor` varchar(60) DEFAULT NULL,
  `id_externo` varchar(200) DEFAULT NULL,
  `intentos` smallint unsigned NOT NULL DEFAULT '0',
  `codigo_error` varchar(80) DEFAULT NULL,
  `detalle_error` text,
  `programado_en` datetime(6) DEFAULT NULL,
  `enviado_en` datetime(6) DEFAULT NULL,
  `entregado_en` datetime(6) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_envio_idempotencia` (`clave_idempotencia`),
  KEY `ix_envio_usuario` (`usuario_id`,`creado_en`),
  KEY `ix_envio_pendientes` (`estado`,`programado_en`,`creado_en`),
  KEY `ix_envio_proveedor` (`proveedor`,`id_externo`),
  KEY `fk_envio_sesion` (`sesion_id`),
  CONSTRAINT `fk_envio_sesion` FOREIGN KEY (`sesion_id`) REFERENCES `sesiones` (`id`),
  CONSTRAINT `fk_envio_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_envio_destinatario` CHECK ((char_length(trim(`destinatario`)) > 0)),
  CONSTRAINT `ck_envio_entrega` CHECK (((`entregado_en` is null) or ((`enviado_en` is not null) and (`entregado_en` >= `enviado_en`)))),
  CONSTRAINT `ck_envio_estado` CHECK ((((`estado` not in (_utf8mb4'enviado',_utf8mb4'entregado')) or (`enviado_en` is not null)) and ((`estado` <> _utf8mb4'entregado') or (`entregado_en` is not null)))),
  CONSTRAINT `ck_envio_evento` CHECK ((char_length(trim(`tipo_evento`)) > 0)),
  CONSTRAINT `ck_envio_idempotencia` CHECK ((char_length(trim(`clave_idempotencia`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `estudiantes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `matricula` varchar(30) NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_estudiante_usuario` (`usuario_id`),
  UNIQUE KEY `uq_estudiante_matricula` (`matricula`),
  CONSTRAINT `fk_estudiante_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_estudiante_matricula` CHECK ((char_length(trim(`matricula`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `identidades_externas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `proveedor` varchar(20) NOT NULL,
  `identificador_externo` varchar(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_identidad_proveedor` (`proveedor`,`identificador_externo`),
  UNIQUE KEY `uq_usuario_proveedor` (`usuario_id`,`proveedor`),
  CONSTRAINT `fk_identidad_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_identidad_identificador` CHECK ((char_length(trim(`identificador_externo`)) > 0)),
  CONSTRAINT `ck_identidad_proveedor` CHECK ((`proveedor` = _utf8mb4'google'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `llamadas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `conversacion_id` bigint unsigned NOT NULL,
  `iniciador_id` bigint unsigned NOT NULL,
  `receptor_id` bigint unsigned NOT NULL,
  `tipo` enum('audio','video') NOT NULL,
  `estado` enum('pendiente','activa','rechazada','cancelada','finalizada','sin_respuesta') NOT NULL DEFAULT 'pendiente',
  `sala` varchar(80) DEFAULT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `creada_en` datetime(6) NOT NULL,
  `inicio_en` datetime(6) DEFAULT NULL,
  `vence_en` datetime(6) NOT NULL,
  `finalizada_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_llamadas_iniciador` (`iniciador_id`,`estado`,`vence_en`),
  KEY `ix_llamadas_receptor` (`receptor_id`,`estado`,`vence_en`),
  KEY `fk_llamada_conversacion` (`conversacion_id`),
  CONSTRAINT `fk_llamada_conversacion` FOREIGN KEY (`conversacion_id`) REFERENCES `conversaciones` (`id`),
  CONSTRAINT `fk_llamada_iniciador` FOREIGN KEY (`iniciador_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_llamada_receptor` FOREIGN KEY (`receptor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_llamada_personas` CHECK ((`iniciador_id` <> `receptor_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `materiales_sesion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sesion_id` bigint unsigned NOT NULL,
  `nombre` varchar(200) NOT NULL,
  `archivo_url` varchar(1024) DEFAULT NULL,
  `agregado_por` bigint unsigned NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `fk_material_sesion` (`sesion_id`),
  KEY `fk_material_autor` (`agregado_por`),
  CONSTRAINT `fk_material_autor` FOREIGN KEY (`agregado_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_material_sesion` FOREIGN KEY (`sesion_id`) REFERENCES `sesiones` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mediciones_academicas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `situacion_id` bigint unsigned NOT NULL,
  `medido_en` datetime(6) NOT NULL,
  `promedio` decimal(4,2) DEFAULT NULL,
  `nivel_riesgo` enum('sin_riesgo','bajo','medio','alto') DEFAULT NULL,
  `condicion` text,
  `materias_reprobadas` smallint unsigned DEFAULT NULL,
  `materias_repeticion` smallint unsigned DEFAULT NULL,
  `materias_especial` smallint unsigned DEFAULT NULL,
  `registrado_por` bigint unsigned NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_medicion_ultima` (`situacion_id`,`medido_en`,`id`),
  KEY `fk_medicion_autor` (`registrado_por`),
  CONSTRAINT `fk_medicion_autor` FOREIGN KEY (`registrado_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_medicion_situacion` FOREIGN KEY (`situacion_id`) REFERENCES `situaciones_academicas` (`id`),
  CONSTRAINT `ck_medicion_promedio` CHECK ((`promedio` between 0 and 10))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mensajes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `conversacion_id` bigint unsigned NOT NULL,
  `emisor_id` bigint unsigned NOT NULL,
  `contenido` text NOT NULL,
  `enviado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_mensaje_historial` (`conversacion_id`,`enviado_en`,`id`),
  KEY `fk_mensaje_participante` (`conversacion_id`,`emisor_id`),
  CONSTRAINT `fk_mensaje_participante` FOREIGN KEY (`conversacion_id`, `emisor_id`) REFERENCES `conversacion_participantes` (`conversacion_id`, `usuario_id`),
  CONSTRAINT `ck_mensaje_contenido` CHECK ((char_length(trim(`contenido`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `motivos_reporte` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(40) NOT NULL,
  `nombre` varchar(120) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_motivo_codigo` (`codigo`),
  CONSTRAINT `ck_motivo_activo` CHECK ((`activo` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificaciones_sesion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sesion_id` bigint unsigned NOT NULL,
  `canal` enum('sms','whatsapp') NOT NULL,
  `estado` enum('pendiente','procesando','aceptada','error','incierta','omitida') NOT NULL DEFAULT 'pendiente',
  `identificador_proveedor` varchar(34) DEFAULT NULL,
  `codigo_error` varchar(64) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_notificacion_sesion_canal` (`sesion_id`,`canal`),
  KEY `ix_notificacion_estado` (`estado`,`id`),
  CONSTRAINT `fk_notificacion_sesion` FOREIGN KEY (`sesion_id`) REFERENCES `sesiones` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `observaciones_estudiante` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `situacion_id` bigint unsigned NOT NULL,
  `estudiante_id` bigint unsigned NOT NULL,
  `periodo_id` bigint unsigned NOT NULL,
  `autor_id` bigint unsigned NOT NULL,
  `sesion_id` bigint unsigned DEFAULT NULL,
  `tipo` enum('nota','progreso','alerta') NOT NULL DEFAULT 'nota',
  `prioridad` enum('normal','alta') NOT NULL DEFAULT 'normal',
  `contenido` text NOT NULL,
  `observado_en` datetime(6) NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_observacion_situacion` (`situacion_id`,`observado_en`),
  KEY `fk_observacion_situacion` (`situacion_id`,`estudiante_id`,`periodo_id`),
  KEY `fk_observacion_sesion` (`sesion_id`,`estudiante_id`,`periodo_id`),
  KEY `fk_observacion_autor` (`autor_id`),
  CONSTRAINT `fk_observacion_autor` FOREIGN KEY (`autor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_observacion_sesion` FOREIGN KEY (`sesion_id`, `estudiante_id`, `periodo_id`) REFERENCES `sesiones` (`id`, `estudiante_id`, `periodo_id`),
  CONSTRAINT `fk_observacion_situacion` FOREIGN KEY (`situacion_id`, `estudiante_id`, `periodo_id`) REFERENCES `situaciones_academicas` (`id`, `estudiante_id`, `periodo_id`),
  CONSTRAINT `ck_observacion_texto` CHECK ((char_length(trim(`contenido`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `periodos_academicos` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `clave` varchar(30) NOT NULL,
  `nombre` varchar(80) NOT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_fin` date NOT NULL,
  `estado` enum('planeado','activo','cerrado') NOT NULL DEFAULT 'planeado',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_periodo_clave` (`clave`),
  CONSTRAINT `ck_periodo_fechas` CHECK ((`fecha_fin` >= `fecha_inicio`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recuperaciones_contrasena` (
  `usuario_id` bigint unsigned NOT NULL,
  `codigo_hash` varchar(255) NOT NULL,
  `solicitado_en` datetime(6) NOT NULL,
  `vence_en` datetime(6) NOT NULL,
  `intentos` tinyint unsigned NOT NULL DEFAULT '0',
  `utilizado` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`usuario_id`),
  CONSTRAINT `fk_codigo_recuperacion_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_codigo_recuperacion_intentos` CHECK ((`intentos` <= 5)),
  CONSTRAINT `ck_codigo_recuperacion_vencimiento` CHECK ((`vence_en` > `solicitado_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reportes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `estudiante_id` bigint unsigned NOT NULL,
  `tutor_id` bigint unsigned NOT NULL,
  `periodo_id` bigint unsigned NOT NULL,
  `sesion_id` bigint unsigned DEFAULT NULL,
  `motivo_id` bigint unsigned NOT NULL,
  `fecha` date NOT NULL,
  `contenido` text,
  `estado` enum('pendiente','completado') NOT NULL DEFAULT 'pendiente',
  `completado_en` datetime(6) DEFAULT NULL,
  `completado_por` bigint unsigned DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_reporte_tutor` (`tutor_id`,`periodo_id`,`estado`,`fecha`),
  KEY `ix_reporte_estudiante` (`estudiante_id`,`periodo_id`,`fecha`),
  KEY `fk_reporte_sesion` (`sesion_id`,`estudiante_id`,`tutor_id`,`periodo_id`),
  KEY `fk_reporte_motivo` (`motivo_id`),
  KEY `fk_reporte_autor_cierre` (`completado_por`),
  CONSTRAINT `fk_reporte_autor_cierre` FOREIGN KEY (`completado_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_reporte_motivo` FOREIGN KEY (`motivo_id`) REFERENCES `motivos_reporte` (`id`),
  CONSTRAINT `fk_reporte_sesion` FOREIGN KEY (`sesion_id`, `estudiante_id`, `tutor_id`, `periodo_id`) REFERENCES `sesiones` (`id`, `estudiante_id`, `tutor_id`, `periodo_id`),
  CONSTRAINT `fk_reporte_situacion` FOREIGN KEY (`estudiante_id`, `periodo_id`) REFERENCES `situaciones_academicas` (`estudiante_id`, `periodo_id`),
  CONSTRAINT `fk_reporte_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `tutores` (`id`),
  CONSTRAINT `ck_reporte_cierre` CHECK ((((`estado` = _utf8mb4'pendiente') and (`completado_en` is null) and (`completado_por` is null)) or ((`estado` = _utf8mb4'completado') and (`completado_en` is not null) and (`completado_por` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `codigo` varchar(30) NOT NULL,
  `nombre` varchar(60) NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_roles_codigo` (`codigo`),
  CONSTRAINT `ck_roles_codigo` CHECK ((`codigo` in (_utf8mb4'coordinador',_utf8mb4'tutor',_utf8mb4'estudiante')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesion_cambios` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sesion_id` bigint unsigned NOT NULL,
  `actor_id` bigint unsigned DEFAULT NULL,
  `tipo` enum('solicitud','aceptacion','rechazo','reprogramacion','cancelacion','cierre','registro','vencimiento') NOT NULL,
  `estado_anterior` enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') DEFAULT NULL,
  `estado_nuevo` enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') NOT NULL,
  `inicio_anterior` datetime(6) DEFAULT NULL,
  `fin_anterior` datetime(6) DEFAULT NULL,
  `inicio_nuevo` datetime(6) NOT NULL,
  `fin_nuevo` datetime(6) NOT NULL,
  `motivo` text,
  `ocurrido_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_cambio_sesion` (`sesion_id`,`ocurrido_en`,`id`),
  KEY `fk_cambio_actor` (`actor_id`),
  CONSTRAINT `fk_cambio_actor` FOREIGN KEY (`actor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_cambio_sesion` FOREIGN KEY (`sesion_id`) REFERENCES `sesiones` (`id`),
  CONSTRAINT `ck_cambio_anterior` CHECK ((((`inicio_anterior` is null) and (`fin_anterior` is null)) or ((`inicio_anterior` is not null) and (`fin_anterior` is not null) and (`fin_anterior` > `inicio_anterior`)))),
  CONSTRAINT `ck_cambio_intervalo` CHECK ((`fin_nuevo` > `inicio_nuevo`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sesiones` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tutor_id` bigint unsigned NOT NULL,
  `estudiante_id` bigint unsigned NOT NULL,
  `periodo_id` bigint unsigned NOT NULL,
  `disponibilidad_id` bigint unsigned DEFAULT NULL,
  `tema` varchar(200) NOT NULL,
  `inicio_en` datetime(6) NOT NULL,
  `fin_en` datetime(6) NOT NULL,
  `modalidad` enum('presencial','virtual') NOT NULL,
  `ubicacion` varchar(200) DEFAULT NULL,
  `enlace_virtual` varchar(1024) DEFAULT NULL,
  `estado` enum('solicitada','confirmada','rechazada','cancelada','completada','no_asistio','vencida') NOT NULL DEFAULT 'solicitada',
  `mensaje_solicitud` text,
  `observaciones` text,
  `inicio_real_en` datetime(6) DEFAULT NULL,
  `fin_real_en` datetime(6) DEFAULT NULL,
  `creado_por` bigint unsigned NOT NULL,
  `reserva_vigente` bigint unsigned GENERATED ALWAYS AS ((case when (`estado` in (_utf8mb4'solicitada',_utf8mb4'confirmada')) then `disponibilidad_id` else NULL end)) STORED,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sesion_contexto` (`id`,`estudiante_id`,`tutor_id`,`periodo_id`),
  UNIQUE KEY `uq_sesion_estudiante_periodo` (`id`,`estudiante_id`,`periodo_id`),
  UNIQUE KEY `uq_sesion_reserva_vigente` (`reserva_vigente`),
  KEY `ix_sesion_tutor_agenda` (`tutor_id`,`inicio_en`,`estado`),
  KEY `ix_sesion_estudiante_agenda` (`estudiante_id`,`inicio_en`,`estado`),
  KEY `fk_sesion_situacion` (`estudiante_id`,`periodo_id`),
  KEY `fk_sesion_disponibilidad` (`disponibilidad_id`,`tutor_id`,`inicio_en`,`fin_en`,`modalidad`),
  KEY `fk_sesion_autor` (`creado_por`),
  KEY `ix_sesion_vencimiento` (`estado`,`inicio_en`,`id`),
  CONSTRAINT `fk_sesion_autor` FOREIGN KEY (`creado_por`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `fk_sesion_disponibilidad` FOREIGN KEY (`disponibilidad_id`, `tutor_id`, `inicio_en`, `fin_en`, `modalidad`) REFERENCES `disponibilidades_tutor` (`id`, `tutor_id`, `inicio_en`, `fin_en`, `modalidad`),
  CONSTRAINT `fk_sesion_situacion` FOREIGN KEY (`estudiante_id`, `periodo_id`) REFERENCES `situaciones_academicas` (`estudiante_id`, `periodo_id`),
  CONSTRAINT `fk_sesion_tutor` FOREIGN KEY (`tutor_id`) REFERENCES `tutores` (`id`),
  CONSTRAINT `ck_sesion_intervalo` CHECK ((`fin_en` > `inicio_en`)),
  CONSTRAINT `ck_sesion_real` CHECK ((((`inicio_real_en` is null) and (`fin_real_en` is null)) or ((`inicio_real_en` is not null) and (`fin_real_en` is not null) and (`fin_real_en` > `inicio_real_en`)))),
  CONSTRAINT `ck_sesion_tema` CHECK ((char_length(trim(`tema`)) > 0)),
  CONSTRAINT `ck_sesion_ubicacion` CHECK ((((`modalidad` = _utf8mb4'presencial') and (`ubicacion` is not null) and (char_length(trim(`ubicacion`)) > 0)) or ((`modalidad` = _utf8mb4'virtual') and (`enlace_virtual` is not null) and (char_length(trim(`enlace_virtual`)) > 0))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `situaciones_academicas` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `estudiante_id` bigint unsigned NOT NULL,
  `periodo_id` bigint unsigned NOT NULL,
  `carrera_id` bigint unsigned NOT NULL,
  `semestre` smallint unsigned NOT NULL,
  `estado_inscripcion` enum('activo','baja','egresado') NOT NULL DEFAULT 'activo',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_situacion_estudiante_periodo` (`estudiante_id`,`periodo_id`),
  UNIQUE KEY `uq_situacion_contexto` (`id`,`estudiante_id`,`periodo_id`),
  KEY `ix_situacion_periodo_carrera` (`periodo_id`,`carrera_id`,`estado_inscripcion`),
  KEY `fk_situacion_carrera` (`carrera_id`),
  CONSTRAINT `fk_situacion_carrera` FOREIGN KEY (`carrera_id`) REFERENCES `carreras` (`id`),
  CONSTRAINT `fk_situacion_estudiante` FOREIGN KEY (`estudiante_id`) REFERENCES `estudiantes` (`id`),
  CONSTRAINT `fk_situacion_periodo` FOREIGN KEY (`periodo_id`) REFERENCES `periodos_academicos` (`id`),
  CONSTRAINT `ck_situacion_semestre` CHECK ((`semestre` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tokens_recuperacion` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `token_hash` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `vence_en` datetime(6) NOT NULL,
  `utilizado_en` datetime(6) DEFAULT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_recuperacion_hash` (`token_hash`),
  KEY `ix_recuperacion_usuario` (`usuario_id`,`vence_en`,`utilizado_en`),
  KEY `ix_recuperacion_expira` (`vence_en`),
  CONSTRAINT `fk_recuperacion_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_recuperacion_hash` CHECK (regexp_like(`token_hash`,_utf8mb4'^[0-9a-f]{64}$',_utf8mb4'c')),
  CONSTRAINT `ck_recuperacion_uso` CHECK (((`utilizado_en` is null) or ((`utilizado_en` >= `creado_en`) and (`utilizado_en` <= `vence_en`)))),
  CONSTRAINT `ck_recuperacion_vencimiento` CHECK ((`vence_en` > `creado_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tokens_revocados` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `jti` varchar(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `vence_en` datetime(6) NOT NULL,
  `revocado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `motivo` enum('logout','cambio_password','administracion') NOT NULL DEFAULT 'logout',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_revocacion_jti` (`jti`),
  KEY `ix_revocacion_expira` (`vence_en`),
  KEY `ix_revocacion_usuario` (`usuario_id`,`revocado_en`),
  CONSTRAINT `fk_revocacion_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `ck_revocacion_jti` CHECK ((char_length(trim(`jti`)) > 0)),
  CONSTRAINT `ck_revocacion_vencimiento` CHECK ((`vence_en` > `revocado_en`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tutores` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `usuario_id` bigint unsigned NOT NULL,
  `departamento_id` bigint unsigned NOT NULL,
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tutor_usuario` (`usuario_id`),
  KEY `fk_tutor_departamento` (`departamento_id`),
  CONSTRAINT `fk_tutor_departamento` FOREIGN KEY (`departamento_id`) REFERENCES `departamentos` (`id`),
  CONSTRAINT `fk_tutor_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario_roles` (
  `usuario_id` bigint unsigned NOT NULL,
  `rol_id` bigint unsigned NOT NULL,
  `asignado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`usuario_id`,`rol_id`),
  KEY `fk_ur_rol` (`rol_id`),
  CONSTRAINT `fk_ur_rol` FOREIGN KEY (`rol_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_ur_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `nombre` varchar(160) NOT NULL,
  `correo` varchar(254) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `telefono` varchar(30) DEFAULT NULL,
  `avatar_url` varchar(1024) DEFAULT NULL,
  `estado` enum('activo','inactivo') NOT NULL DEFAULT 'activo',
  `creado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `actualizado_en` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version_sesion` bigint unsigned NOT NULL DEFAULT '0',
  `acepta_sms` tinyint(1) NOT NULL DEFAULT '0',
  `acepta_whatsapp` tinyint(1) NOT NULL DEFAULT '0',
  `preferencias_contacto_en` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_usuarios_correo` (`correo`),
  CONSTRAINT `ck_usuarios_correo` CHECK (((`correo` = trim(`correo`)) and (char_length(`correo`) > 3))),
  CONSTRAINT `ck_usuarios_hash` CHECK ((char_length(`password_hash`) >= 20)),
  CONSTRAINT `ck_usuarios_nombre` CHECK ((char_length(trim(`nombre`)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_avisos_visibles` AS SELECT
 1 AS `aviso_id`,
 1 AS `usuario_id`,
 1 AS `titulo`,
 1 AS `descripcion`,
 1 AS `categoria`,
 1 AS `prioridad`,
 1 AS `sesion_id`,
 1 AS `publicado_en`,
 1 AS `leido_en`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_expediente_actual_por_periodo` AS SELECT
 1 AS `situacion_id`,
 1 AS `estudiante_id`,
 1 AS `nombre`,
 1 AS `matricula`,
 1 AS `correo`,
 1 AS `telefono`,
 1 AS `periodo_id`,
 1 AS `carrera_id`,
 1 AS `carrera`,
 1 AS `semestre`,
 1 AS `estado_inscripcion`,
 1 AS `promedio`,
 1 AS `nivel_riesgo`,
 1 AS `condicion`,
 1 AS `medido_en`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_resumen_sesiones` AS SELECT
 1 AS `estudiante_id`,
 1 AS `tutor_id`,
 1 AS `periodo_id`,
 1 AS `sesiones_completadas`,
 1 AS `ultima_sesion`,
 1 AS `horas_reales`,
 1 AS `completadas_sin_duracion_real`*/;
SET character_set_client = @saved_cs_client;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_ultima_medicion_academica` AS SELECT
 1 AS `id`,
 1 AS `situacion_id`,
 1 AS `medido_en`,
 1 AS `promedio`,
 1 AS `nivel_riesgo`,
 1 AS `condicion`,
 1 AS `materias_reprobadas`,
 1 AS `materias_repeticion`,
 1 AS `materias_especial`,
 1 AS `registrado_por`*/;
SET character_set_client = @saved_cs_client;
/*!50001 DROP VIEW IF EXISTS `v_avisos_visibles`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_avisos_visibles` AS select `a`.`id` AS `aviso_id`,`d`.`usuario_id` AS `usuario_id`,`a`.`titulo` AS `titulo`,`a`.`descripcion` AS `descripcion`,`a`.`categoria` AS `categoria`,`a`.`prioridad` AS `prioridad`,`a`.`sesion_id` AS `sesion_id`,`a`.`publicado_en` AS `publicado_en`,`d`.`leido_en` AS `leido_en` from (`avisos` `a` join `aviso_destinatarios` `d` on((`d`.`aviso_id` = `a`.`id`))) where ((`a`.`retirado_en` is null) and (`d`.`ocultado_en` is null)) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_expediente_actual_por_periodo`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_expediente_actual_por_periodo` AS select `a`.`id` AS `situacion_id`,`e`.`id` AS `estudiante_id`,`u`.`nombre` AS `nombre`,`e`.`matricula` AS `matricula`,`u`.`correo` AS `correo`,`u`.`telefono` AS `telefono`,`a`.`periodo_id` AS `periodo_id`,`a`.`carrera_id` AS `carrera_id`,`c`.`nombre` AS `carrera`,`a`.`semestre` AS `semestre`,`a`.`estado_inscripcion` AS `estado_inscripcion`,`m`.`promedio` AS `promedio`,`m`.`nivel_riesgo` AS `nivel_riesgo`,`m`.`condicion` AS `condicion`,`m`.`medido_en` AS `medido_en` from ((((`situaciones_academicas` `a` join `estudiantes` `e` on((`e`.`id` = `a`.`estudiante_id`))) join `usuarios` `u` on((`u`.`id` = `e`.`usuario_id`))) join `carreras` `c` on((`c`.`id` = `a`.`carrera_id`))) left join `v_ultima_medicion_academica` `m` on((`m`.`situacion_id` = `a`.`id`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_resumen_sesiones`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_resumen_sesiones` AS select `sesiones`.`estudiante_id` AS `estudiante_id`,`sesiones`.`tutor_id` AS `tutor_id`,`sesiones`.`periodo_id` AS `periodo_id`,sum((`sesiones`.`estado` = 'completada')) AS `sesiones_completadas`,max((case when (`sesiones`.`estado` = 'completada') then `sesiones`.`inicio_en` end)) AS `ultima_sesion`,(sum((case when ((`sesiones`.`estado` = 'completada') and (`sesiones`.`inicio_real_en` is not null)) then timestampdiff(SECOND,`sesiones`.`inicio_real_en`,`sesiones`.`fin_real_en`) else 0 end)) / 3600.0) AS `horas_reales`,sum(((`sesiones`.`estado` = 'completada') and (`sesiones`.`inicio_real_en` is null))) AS `completadas_sin_duracion_real` from `sesiones` group by `sesiones`.`estudiante_id`,`sesiones`.`tutor_id`,`sesiones`.`periodo_id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!50001 DROP VIEW IF EXISTS `v_ultima_medicion_academica`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 SQL SECURITY INVOKER */
/*!50001 VIEW `v_ultima_medicion_academica` AS select `ordenadas`.`id` AS `id`,`ordenadas`.`situacion_id` AS `situacion_id`,`ordenadas`.`medido_en` AS `medido_en`,`ordenadas`.`promedio` AS `promedio`,`ordenadas`.`nivel_riesgo` AS `nivel_riesgo`,`ordenadas`.`condicion` AS `condicion`,`ordenadas`.`materias_reprobadas` AS `materias_reprobadas`,`ordenadas`.`materias_repeticion` AS `materias_repeticion`,`ordenadas`.`materias_especial` AS `materias_especial`,`ordenadas`.`registrado_por` AS `registrado_por` from (select `m`.`id` AS `id`,`m`.`situacion_id` AS `situacion_id`,`m`.`medido_en` AS `medido_en`,`m`.`promedio` AS `promedio`,`m`.`nivel_riesgo` AS `nivel_riesgo`,`m`.`condicion` AS `condicion`,`m`.`materias_reprobadas` AS `materias_reprobadas`,`m`.`materias_repeticion` AS `materias_repeticion`,`m`.`materias_especial` AS `materias_especial`,`m`.`registrado_por` AS `registrado_por`,`m`.`creado_en` AS `creado_en`,`m`.`actualizado_en` AS `actualizado_en`,row_number() OVER (PARTITION BY `m`.`situacion_id` ORDER BY `m`.`medido_en` desc,`m`.`id` desc )  AS `posicion` from `mediciones_academicas` `m`) `ordenadas` where (`ordenadas`.`posicion` = 1) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
