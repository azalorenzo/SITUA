import { solicitarApi } from './autenticacion';
import type { PaginaSesiones,ResumenSesiones,SesionAgenda,OpcionSesion,PaginaHorariosDisponibles } from '../tipos/sesiones';
export function consultarSesiones(esTutor:boolean,pagina:number,estado:string,signal:AbortSignal) {
  const parametros=new URLSearchParams({pagina:String(pagina),tamano:'5'});if(estado)parametros.set('estado',estado);
  return solicitarApi<PaginaSesiones>(`/${esTutor?'tutor':'estudiante'}/sesiones?${parametros}`,{signal});
}
export function consultarAgenda(anio:number,mes:number,signal:AbortSignal) {
  const utc=(fecha:Date)=>fecha.toISOString().slice(0,19);
  const parametros=new URLSearchParams({desde:utc(new Date(anio,mes,1)),hasta:utc(new Date(anio,mes+1,1))});
  return solicitarApi<SesionAgenda[]>(`/estudiante/sesiones/agenda?${parametros}`,{signal});
}
export function consultarResumenSesiones(signal:AbortSignal) {return solicitarApi<ResumenSesiones>('/estudiante/sesiones/resumen',{signal});}
export function consultarOpcionesSesion(signal:AbortSignal) {return solicitarApi<OpcionSesion[]>('/estudiante/sesiones/opciones',{signal});}
export function consultarHorariosDisponibles(periodoId:number,pagina:number,signal:AbortSignal) {
  return solicitarApi<PaginaHorariosDisponibles>(`/estudiante/sesiones/disponibles?periodoId=${periodoId}&pagina=${pagina}&tamano=5`,{signal});
}
export function reservarSesion(datos:{periodoId:number;disponibilidadId:number;tema:string;mensaje:string}) {
  return solicitarApi<SesionAgenda>('/estudiante/sesiones',{method:'POST',body:JSON.stringify(datos)});
}
export function cambiarEstadoSesion(esTutor:boolean,id:number,accion:'confirmar'|'rechazar'|'cancelar') {
  return solicitarApi<SesionAgenda>(`/${esTutor?'tutor':'estudiante'}/sesiones/${id}/${accion}`,{method:'PATCH'});
}
export const nombresEstado:Record<SesionAgenda['estado'],string>={solicitada:'Solicitada',confirmada:'Confirmada',rechazada:'Rechazada',cancelada:'Cancelada',completada:'Completada',no_asistio:'No asistió'};
export const fechaSesion=(valor:string)=>new Date(valor+'Z');
export const diaSesion=(valor:string)=>new Intl.DateTimeFormat('es-MX',{day:'2-digit',month:'2-digit',year:'numeric'}).format(fechaSesion(valor));
export const horaSesion=(valor:string)=>new Intl.DateTimeFormat('es-MX',{hour:'2-digit',minute:'2-digit',hour12:false}).format(fechaSesion(valor));
export const lugarSesion=(sesion:{ubicacion:string|null;enlaceVirtual:string|null})=>sesion.ubicacion||sesion.enlaceVirtual||'';
