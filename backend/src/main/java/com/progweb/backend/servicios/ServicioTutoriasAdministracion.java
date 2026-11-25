package com.progweb.backend.servicios;
import java.time.*;
import java.util.*;
import com.progweb.backend.errores.*;
import com.progweb.backend.respuestas.*;
import com.progweb.backend.repositorios.RepositorioSesionesAgenda;
import com.progweb.backend.solicitudes.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class ServicioTutoriasAdministracion {
 private final JdbcTemplate consultas;private final ServicioSesionesAgenda agenda;private final Clock reloj;private final RepositorioSesionesAgenda repositorio;private final ServicioRegistroSesiones registros;private final ServicioAutenticacion autenticacion;private final ServicioAvisosTelefonicos avisos;
 public ServicioTutoriasAdministracion(JdbcTemplate consultas,ServicioSesionesAgenda agenda,Clock reloj,com.progweb.backend.repositorios.RepositorioSesionesAgenda repositorio,ServicioRegistroSesiones registros,ServicioAutenticacion autenticacion,ServicioAvisosTelefonicos avisos){this.repositorio=repositorio;this.registros=registros;this.autenticacion=autenticacion;this.avisos=avisos;this.consultas=consultas;this.agenda=agenda;this.reloj=reloj;}
 private static final String BASE=" FROM sesiones s JOIN estudiantes e ON e.id=s.estudiante_id JOIN usuarios ue ON ue.id=e.usuario_id JOIN tutores t ON t.id=s.tutor_id JOIN usuarios ut ON ut.id=t.usuario_id JOIN periodos_academicos p ON p.id=s.periodo_id ";
 private RespuestaSesionAgenda convertir(java.sql.ResultSet f,int posicion)throws java.sql.SQLException{var ahora=LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);var inicio=f.getObject("inicio_en",LocalDateTime.class);String estado=f.getString("estado");boolean futura=inicio.isAfter(ahora);return new RespuestaSesionAgenda(f.getLong("id"),f.getObject("disponibilidad_id",Long.class),f.getString("tutor"),f.getString("estudiante"),f.getString("tema"),inicio,f.getObject("fin_en",LocalDateTime.class),f.getString("modalidad"),f.getString("ubicacion"),f.getString("enlace_virtual"),estado,f.getLong("periodo_id"),f.getString("periodo"),f.getString("mensaje_solicitud"),futura&&List.of("solicitada","confirmada").contains(estado),futura&&estado.equals("solicitada"),!futura&&estado.equals("confirmada"),f.getString("observaciones"),0);}
 public PaginaAdministracion<RespuestaSesionAgenda> listar(int pagina,int tamano,String busqueda,String estado,String modalidad,LocalDate fecha){
  ServicioCatalogosAdministracion.validarPagina(pagina,tamano,busqueda);
  if(!estado.isEmpty()&&!List.of("solicitada","confirmada","completada","rechazada","cancelada","no_asistio","vencida").contains(estado))throw new SolicitudRechazada(422,"Selecciona un estado válido.","estado");
  if(!modalidad.isEmpty()&&!List.of("presencial","virtual").contains(modalidad))throw new SolicitudRechazada(422,"Selecciona una modalidad válida.","modalidad");
  String filtro=" WHERE (ue.nombre LIKE ? OR ut.nombre LIKE ? OR s.tema LIKE ?) AND (?='' OR s.estado=?) AND (?='' OR s.modalidad=?)";var valores=new ArrayList<Object>(Arrays.asList("%"+busqueda.strip()+"%","%"+busqueda.strip()+"%","%"+busqueda.strip()+"%",estado,estado,modalidad,modalidad));
  if(fecha!=null){filtro+=" AND s.inicio_en>=? AND s.inicio_en<?";valores.add(fecha.atStartOfDay(ZoneId.of("America/Mexico_City")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());valores.add(fecha.plusDays(1).atStartOfDay(ZoneId.of("America/Mexico_City")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());}
  long total=consultas.queryForObject("SELECT COUNT(*)"+BASE+filtro,Long.class,valores.toArray());int paginas=(int)((total+tamano-1)/tamano),actual=Math.min(pagina,Math.max(1,paginas));valores.add(tamano);valores.add((actual-1L)*tamano);
  return new PaginaAdministracion<>(consultas.query("SELECT s.*,ue.nombre estudiante,ut.nombre tutor,p.nombre periodo"+BASE+filtro+" ORDER BY s.inicio_en DESC,s.id DESC LIMIT ? OFFSET ?",this::convertir,valores.toArray()),actual,tamano,total,paginas,Map.of("total",consultas.queryForObject("SELECT COUNT(*) FROM sesiones",Long.class),"pendientes",consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE estado='solicitada'",Long.class),"completadas",consultas.queryForObject("SELECT COUNT(*) FROM sesiones WHERE estado='completada'",Long.class)));
 }
 public RespuestaSesionAgenda consultar(long id){return consultas.query("SELECT s.*,ue.nombre estudiante,ut.nombre tutor,p.nombre periodo"+BASE+" WHERE s.id=?",this::convertir,id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró la tutoría."));}
 @Transactional public RespuestaSesionAgenda cambiar(long id,String accion,String sujeto){Long tutor=consultas.queryForList("SELECT tutor_id FROM sesiones WHERE id=?",Long.class,id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró la tutoría."));return agenda.cambiarCoordinacion(sujeto,id,tutor,accion);}

 private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
 private long autor(String sujeto){var cuenta=autenticacion.consultarSesion(sujeto);if(!cuenta.roles().contains("coordinador"))throw new AccesoRechazado(403,"Se requiere coordinación.");return cuenta.id();}
 @Transactional public RespuestaSesionAgenda guardar(Long id,SolicitudTutoriaAdministracion datos,String sujeto){
  long autor=autor(sujeto);
  if(!datos.finEn().isAfter(datos.inicioEn())||Duration.between(datos.inicioEn(),datos.finEn()).toMinutes()<1||Duration.between(datos.inicioEn(),datos.finEn()).compareTo(Duration.ofDays(1))>0)throw new SolicitudRechazada(422,"Indica un horario válido de entre un minuto y un día.","finEn");
  String lugar=datos.lugar().strip();if(datos.modalidad().equals("presencial")&&lugar.length()>200)throw new SolicitudRechazada(422,"La ubicación no debe superar 200 caracteres.","lugar");
  if(datos.modalidad().equals("virtual")){try{var uri=java.net.URI.create(lugar);if(!List.of("https","http").contains(uri.getScheme())||uri.getHost()==null)throw new IllegalArgumentException();}catch(IllegalArgumentException fallo){throw new SolicitudRechazada(422,"Escribe un enlace válido con https:// o http://.","lugar");}}
  if(id==null&&!datos.finEn().isAfter(ahora())){var creada=registros.registrarCoordinacion(sujeto,datos.tutorId(),new SolicitudRegistrarSesion(datos.estudianteId(),datos.periodoId(),datos.tema(),datos.inicioEn(),datos.finEn(),lugar,datos.observaciones()));return consultar(creada.id());}
  if(!datos.inicioEn().isAfter(ahora()))throw new SolicitudRechazada(422,"Selecciona un horario futuro.","inicioEn");
  if(!repositorio.bloquearEstudiante(datos.estudianteId())||!repositorio.bloquearTutor(datos.tutorId()))throw new AccesoRechazado(404,"No se encontró el estudiante o tutor.");
  RespuestaSesionAgenda anterior=null;
  if(id!=null){consultas.queryForList("SELECT id FROM sesiones WHERE id=? FOR UPDATE",Long.class,id);anterior=consultar(id);var participantes=consultas.queryForMap("SELECT estudiante_id,tutor_id,periodo_id FROM sesiones WHERE id=?",id);if(((Number)participantes.get("estudiante_id")).longValue()!=datos.estudianteId()||((Number)participantes.get("tutor_id")).longValue()!=datos.tutorId()||anterior.periodoId()!=datos.periodoId())throw new SolicitudRechazada(409,"No se pueden cambiar los participantes de una tutoría existente.","estudianteId");if(!anterior.puedeCancelar())throw new AccesoRechazado(409,"Solo se pueden editar tutorías futuras pendientes o confirmadas.");}
  if(!repositorio.participantesActivos(datos.estudianteId(),datos.tutorId())||!repositorio.inscripcionVigente(datos.estudianteId(),datos.tutorId(),datos.periodoId()))throw new SolicitudRechazada(409,"Selecciona una asignación activa.","estudianteId");
  if(repositorio.ocupado(datos.estudianteId(),datos.tutorId(),datos.inicioEn(),datos.finEn(),id))throw new SolicitudRechazada(409,"El horario se cruza con otra tutoría.","inicioEn");
  String ubicacion=datos.modalidad().equals("presencial")?lugar:null,enlace=datos.modalidad().equals("virtual")?lugar:null;
  if(id==null){consultas.update("INSERT INTO sesiones(estudiante_id,tutor_id,periodo_id,tema,inicio_en,fin_en,modalidad,ubicacion,enlace_virtual,observaciones,estado,creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,'confirmada',?)",datos.estudianteId(),datos.tutorId(),datos.periodoId(),datos.tema().strip(),datos.inicioEn(),datos.finEn(),datos.modalidad(),ubicacion,enlace,datos.observaciones(),autor);id=consultas.queryForObject("SELECT LAST_INSERT_ID()",Long.class);repositorio.registrarCambio(id,autor,"registro",null,"confirmada",datos.inicioEn(),datos.finEn());avisos.preparar(id);}
  else {consultas.update("UPDATE sesiones SET tema=?,inicio_en=?,fin_en=?,modalidad=?,ubicacion=?,enlace_virtual=?,observaciones=?,disponibilidad_id=NULL WHERE id=?",datos.tema().strip(),datos.inicioEn(),datos.finEn(),datos.modalidad(),ubicacion,enlace,datos.observaciones(),id);consultas.update("INSERT INTO sesion_cambios(sesion_id,actor_id,tipo,estado_anterior,estado_nuevo,inicio_anterior,fin_anterior,inicio_nuevo,fin_nuevo) VALUES (?,?,'reprogramacion',?,?,?,?,?,?)",id,autor,anterior.estado(),anterior.estado(),anterior.inicioEn(),anterior.finEn(),datos.inicioEn(),datos.finEn());}
  return consultar(id);
 }
 @Transactional public RespuestaSesionAgenda cerrar(long id,SolicitudCerrarSesion datos,String sujeto){Long tutor=consultas.queryForList("SELECT tutor_id FROM sesiones WHERE id=?",Long.class,id).stream().findFirst().orElseThrow(()->new AccesoRechazado(404,"No se encontró la tutoría."));registros.cerrarCoordinacion(sujeto,tutor,id,datos);return consultar(id);}
}
