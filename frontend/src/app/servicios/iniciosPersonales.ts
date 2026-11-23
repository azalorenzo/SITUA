import { solicitarApi } from './autenticacion';
import type { InicioEstudiante,InicioTutor,DatosActividad,ActividadPersonal,AvisosEstudiante } from '../tipos/iniciosPersonales';
import type { AvisoRecibido,AvisosMarcados } from '../tipos/avisosRecibidos';
export function consultarInicioEstudiante(signal:AbortSignal){return solicitarApi<InicioEstudiante>('/estudiante/inicio',{signal});}
export function consultarInicioTutor(signal:AbortSignal){return solicitarApi<InicioTutor>('/tutor/inicio',{signal});}
export function guardarActividad(datos:DatosActividad,id?:number){return solicitarApi<ActividadPersonal>(`/estudiante/actividades${id?`/${id}`:''}`,{method:id?'PUT':'POST',body:JSON.stringify(datos)});}
export function eliminarActividad(id:number){return solicitarApi<{eliminado:boolean}>(`/estudiante/actividades/${id}`,{method:'DELETE'});}
export function consultarAvisosEstudiante(pagina:number,signal:AbortSignal){return solicitarApi<AvisosEstudiante>(`/estudiante/avisos?pagina=${pagina}&tamano=5`,{signal});}
export function leerAvisoEstudiante(id:number){return solicitarApi<AvisoRecibido>(`/estudiante/avisos/${id}/leer`,{method:'PATCH'});}
export function leerTodosAvisosEstudiante(){return solicitarApi<AvisosMarcados>('/estudiante/avisos/leer-todos',{method:'PATCH'});}
