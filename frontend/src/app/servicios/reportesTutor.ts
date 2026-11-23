import { solicitarApi,solicitarArchivoApi } from './autenticacion';
import type { PaginaReportes,MotivoReporte,ReporteCoordinacion } from '../tipos/reportes';
import type { AnalisisTutor,DatosReporteTutor } from '../tipos/reportesTutor';
import type { SesionRealizada } from '../tipos/registroSesiones';
function parametros(pagina:number,busqueda:string,estado:string,fecha:string){const valores=new URLSearchParams({pagina:String(pagina),tamano:'5',busqueda});if(estado)valores.set('estado',estado);if(fecha){valores.set('desde',fecha);valores.set('hasta',fecha);}return valores;}
export function consultarReportesTutor(pagina:number,busqueda:string,estado:string,fecha:string,signal:AbortSignal){return solicitarApi<PaginaReportes>(`/tutor/reportes?${parametros(pagina,busqueda,estado,fecha)}`,{signal});}
export function consultarAnalisisTutor(signal:AbortSignal){return solicitarApi<AnalisisTutor>('/tutor/reportes/analisis',{signal});}
export function consultarMotivosTutor(signal:AbortSignal){return solicitarApi<MotivoReporte[]>('/tutor/reportes/motivos',{signal});}
export function consultarSesionesReporte(estudianteId:number,periodoId:number,signal:AbortSignal){return solicitarApi<SesionRealizada[]>(`/tutor/reportes/sesiones?estudianteId=${estudianteId}&periodoId=${periodoId}`,{signal});}
export function crearReporteTutor(datos:DatosReporteTutor){return solicitarApi<ReporteCoordinacion>('/tutor/reportes',{method:'POST',body:JSON.stringify(datos)});}
export function completarReporteTutor(id:number,contenido:string){return solicitarApi<ReporteCoordinacion>(`/tutor/reportes/${id}/completar`,{method:'PATCH',body:JSON.stringify({contenido})});}
export function descargarReportesTutor(busqueda:string,estado:string,fecha:string,id?:number){return solicitarArchivoApi(id?`/tutor/reportes/${id}/pdf`:`/tutor/reportes/exportar/pdf?${parametros(1,busqueda,estado,fecha)}`);}
