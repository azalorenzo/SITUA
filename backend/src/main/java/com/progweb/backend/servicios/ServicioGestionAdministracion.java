package com.progweb.backend.servicios;
import java.time.*;
import java.util.*;
import com.progweb.backend.errores.*;
import com.progweb.backend.repositorios.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.solicitudes.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioGestionAdministracion {
 private final JdbcTemplate consultas;private final ServicioAutenticacion autenticacion;private final RepositorioReportes reportes;private final RepositorioSesionesAgenda agenda;private final Clock reloj;
 public ServicioGestionAdministracion(JdbcTemplate consultas,ServicioAutenticacion autenticacion,RepositorioReportes reportes,RepositorioSesionesAgenda agenda,Clock reloj){this.consultas=consultas;this.autenticacion=autenticacion;this.reportes=reportes;this.agenda=agenda;this.reloj=reloj;}
 private long autor(String sujeto){var cuenta=autenticacion.consultarSesion(sujeto);if(!cuenta.roles().contains("coordinador"))throw new AccesoRechazado(403,"Se requiere coordinación.");return cuenta.id();}
 private void rechazar(String mensaje,String campo){throw new SolicitudRechazada(409,mensaje,campo);}
 private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
 public ExpedienteAdministracion expediente(long id){
  var base=consultas.query("SELECT u.nombre,e.matricula FROM estudiantes e JOIN usuarios u ON u.id=e.usuario_id WHERE e.id=?",(f,p)->new String[]{f.getString(1),f.getString(2)},id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró el estudiante."));
  var periodos=consultas.query("""
   SELECT s.*,p.nombre periodo,m.promedio,m.nivel_riesgo,m.condicion FROM situaciones_academicas s JOIN periodos_academicos p ON p.id=s.periodo_id
   LEFT JOIN mediciones_academicas m ON m.id=(SELECT x.id FROM mediciones_academicas x WHERE x.situacion_id=s.id ORDER BY x.medido_en DESC,x.id DESC LIMIT 1)
   WHERE s.estudiante_id=? ORDER BY p.fecha_inicio DESC,p.id DESC
   """,(f,p)->new ExpedienteAdministracion.Periodo(f.getLong("periodo_id"),f.getString("periodo"),f.getLong("carrera_id"),f.getInt("semestre"),f.getString("estado_inscripcion"),f.getBigDecimal("promedio"),f.getString("nivel_riesgo"),f.getString("condicion")),id);
  return new ExpedienteAdministracion(id,base[0],base[1],periodos);
 }
 @Transactional public ExpedienteAdministracion guardarExpediente(long estudiante,SolicitudExpedienteAdministracion datos,String sujeto){
  long autor=autor(sujeto);if(!agenda.bloquearEstudiante(estudiante))throw new AccesoRechazado(404,"No se encontró el estudiante.");
  if(!consultas.queryForList("SELECT id FROM estudiantes WHERE matricula=? AND id<>?",Long.class,datos.matricula().strip(),estudiante).isEmpty())rechazar("La matrícula ya está registrada.","matricula");
  var existentes=consultas.queryForList("SELECT id FROM situaciones_academicas WHERE estudiante_id=? AND periodo_id=? FOR UPDATE",Long.class,estudiante,datos.periodoId());
  if(consultas.queryForObject("SELECT COUNT(*) FROM carreras WHERE id=? AND (activa=1 OR id IN (SELECT carrera_id FROM situaciones_academicas WHERE estudiante_id=? AND periodo_id=?))",Long.class,datos.carreraId(),estudiante,datos.periodoId())==0)rechazar("Selecciona una carrera disponible.","carreraId");
  if(consultas.queryForObject("SELECT COUNT(*) FROM periodos_academicos WHERE id=?"+(existentes.isEmpty()?" AND estado<>'cerrado'":""),Long.class,datos.periodoId())==0)rechazar("Selecciona un periodo disponible.","periodoId");
  if(!datos.inscripcion().equals("activo")&&consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE estudiante_id=? AND periodo_id=? AND estado IN ('solicitada','confirmada')",Long.class,estudiante,datos.periodoId())>0)rechazar("Resuelve las sesiones pendientes antes de cambiar la inscripción.","inscripcion");
  consultas.update("UPDATE estudiantes SET matricula=? WHERE id=?",datos.matricula().strip(),estudiante);
  long situacion;if(existentes.isEmpty()){consultas.update("INSERT INTO situaciones_academicas(estudiante_id,periodo_id,carrera_id,semestre,estado_inscripcion) VALUES (?,?,?,?,?)",estudiante,datos.periodoId(),datos.carreraId(),datos.semestre(),datos.inscripcion());situacion=consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}else {situacion=existentes.getFirst();consultas.update("UPDATE situaciones_academicas SET carrera_id=?,semestre=?,estado_inscripcion=? WHERE id=?",datos.carreraId(),datos.semestre(),datos.inscripcion(),situacion);}
  if(!datos.inscripcion().equals("activo"))consultas.update("UPDATE asignaciones_tutor SET estado='finalizada',fin_en=GREATEST(inicio_en,UTC_TIMESTAMP(6)) WHERE estudiante_id=? AND periodo_id=? AND estado IN ('activa','pendiente')",estudiante,datos.periodoId());
  var anteriores=consultas.query("SELECT materias_reprobadas,materias_repeticion,materias_especial FROM mediciones_academicas WHERE situacion_id=? ORDER BY medido_en DESC,id DESC LIMIT 1",(f,pos)->new Integer[]{f.getObject(1,Integer.class),f.getObject(2,Integer.class),f.getObject(3,Integer.class)},situacion).stream().findFirst().orElse(new Integer[]{null,null,null});
  consultas.update("INSERT INTO mediciones_academicas(situacion_id,medido_en,promedio,nivel_riesgo,condicion,registrado_por,materias_reprobadas,materias_repeticion,materias_especial) VALUES (?,?,?,?,?,?,?,?,?)",situacion,ahora(),datos.promedio(),datos.riesgo(),datos.condicion(),autor,anteriores[0],anteriores[1],anteriores[2]);return expediente(estudiante);
 }
 public record Contexto(long id,long estudianteId,String estudiante,long tutorId,String tutor,long periodoId,String periodo){}
 public PaginaAdministracion<Contexto> contextos(int pagina,int tamano,String busqueda){
  ServicioCatalogosAdministracion.validarPagina(pagina,tamano,busqueda);String base=" FROM asignaciones_tutor a JOIN estudiantes e ON e.id=a.estudiante_id JOIN usuarios ue ON ue.id=e.usuario_id JOIN tutores t ON t.id=a.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id JOIN periodos_academicos p ON p.id=a.periodo_id JOIN situaciones_academicas s ON s.estudiante_id=a.estudiante_id AND s.periodo_id=a.periodo_id WHERE a.estado='activa' AND p.estado='activo' AND s.estado_inscripcion='activo' AND ue.estado='activo' AND ut.estado='activo' AND (ue.nombre LIKE ? OR e.matricula LIKE ? OR ut.nombre LIKE ?)";String buscar="%"+busqueda.strip()+"%";long total=consultas.queryForObject("SELECT COUNT(*)"+base,Long.class,buscar,buscar,buscar);int paginas=(int)((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));return new PaginaAdministracion<>(consultas.query("SELECT a.id,e.id estudiante_id,ue.nombre estudiante,t.id tutor_id,ut.nombre tutor,p.id periodo_id,p.nombre periodo"+base+" ORDER BY ue.nombre,a.id LIMIT ? OFFSET ?",(f,pos)->new Contexto(f.getLong("id"),f.getLong("estudiante_id"),f.getString("estudiante"),f.getLong("tutor_id"),f.getString("tutor"),f.getLong("periodo_id"),f.getString("periodo")),buscar,buscar,buscar,tamano,(actual-1L)*tamano),actual,tamano,total,paginas);
 }
 public Contexto contextoSesion(long id){return consultas.query("SELECT s.id,s.estudiante_id,ue.nombre estudiante,s.tutor_id,ut.nombre tutor,s.periodo_id,p.nombre periodo FROM sesiones s JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios ue ON ue.id=e.usuario_id JOIN tutores t ON t.id=s.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id JOIN periodos_academicos p ON p.id=s.periodo_id WHERE s.id=?",(f,posicion)->new Contexto(f.getLong(1),f.getLong(2),f.getString(3),f.getLong(4),f.getString(5),f.getLong(6),f.getString(7)),id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró la tutoría."));}
 public RespuestaReporte reporte(long id){return reportes.buscar(id).orElseThrow(()->new AccesoRechazado(404,"No se encontró el reporte."));}
 @Transactional public RespuestaReporte guardarReporte(Long id,SolicitudReporteAdministracion datos,String sujeto){
  long autor=autor(sujeto);RespuestaReporte anterior=null;if(id!=null){consultas.queryForList("SELECT id FROM reportes WHERE id=? FOR UPDATE",Long.class,id);anterior=reporte(id);if(!Objects.equals(anterior.estudianteId(),datos.estudianteId())||!Objects.equals(anterior.tutorId(),datos.tutorId())||!Objects.equals(anterior.periodoId(),datos.periodoId()))rechazar("No se pueden cambiar los participantes de un reporte existente.","estudianteId");}
  if(!agenda.bloquearEstudiante(datos.estudianteId())||!agenda.bloquearTutor(datos.tutorId()))throw new AccesoRechazado(404,"No se encontró el estudiante o tutor.");
  if(id==null&&(!agenda.participantesActivos(datos.estudianteId(),datos.tutorId())||!agenda.inscripcionVigente(datos.estudianteId(),datos.tutorId(),datos.periodoId())))rechazar("Selecciona una asignación activa.","estudianteId");
  if(consultas.queryForObject("SELECT COUNT(*) FROM motivos_reporte WHERE id=? AND (activo=1 OR id=?)",Long.class,datos.motivoId(),anterior==null?null:anterior.motivoId())==0)rechazar("Selecciona un motivo disponible.","motivoId");
  boolean completo=datos.estado().equals("completado");var completadoEn=completo?(anterior!=null&&anterior.estado().equals("completado")?anterior.completadoEn():ahora()):null;
  if(id==null){consultas.update("INSERT INTO reportes(estudiante_id,tutor_id,periodo_id,motivo_id,fecha,contenido,estado,completado_en,completado_por) VALUES (?,?,?,?,?,?,?,?,?)",datos.estudianteId(),datos.tutorId(),datos.periodoId(),datos.motivoId(),datos.fecha(),datos.contenido().strip(),datos.estado(),completadoEn,completo?autor:null);id=consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);}
  else {Long completadoPor=completo?autor:null;if(completo&&anterior.estado().equals("completado"))completadoPor=consultas.queryForObject("SELECT completado_por FROM reportes WHERE id=?",Long.class,id);consultas.update("UPDATE reportes SET motivo_id=?,fecha=?,contenido=?,estado=?,completado_en=?,completado_por=? WHERE id=?",datos.motivoId(),datos.fecha(),datos.contenido().strip(),datos.estado(),completadoEn,completadoPor,id);}return reporte(id);
 }
 @Transactional public void eliminarReporte(long id,String sujeto){autor(sujeto);consultas.queryForList("SELECT id FROM reportes WHERE id=? FOR UPDATE",Long.class,id);reporte(id);consultas.update("DELETE FROM reportes WHERE id=?",id);}
}
