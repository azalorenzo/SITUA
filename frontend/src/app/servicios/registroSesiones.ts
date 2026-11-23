import { solicitarApi } from './autenticacion';
import type { OpcionRegistroSesion,PaginaRealizadas,SesionRealizada,DatosRegistrarSesion } from '../tipos/registroSesiones';
export function consultarOpcionesRegistro(signal:AbortSignal,estudianteId?:number){return solicitarApi<OpcionRegistroSesion[]>(`/tutor/sesiones-realizadas/opciones${estudianteId?`?estudianteId=${estudianteId}`:''}`,{signal});}
export function registrarSesion(datos:DatosRegistrarSesion){return solicitarApi<SesionRealizada>('/tutor/sesiones-realizadas',{method:'POST',body:JSON.stringify(datos)});}
export function cerrarSesion(id:number,datos:{estado:string;inicioRealEn:string|null;finRealEn:string|null;observaciones:string}){return solicitarApi<SesionRealizada>(`/tutor/sesiones-realizadas/${id}/cerrar`,{method:'PATCH',body:JSON.stringify(datos)});}
export function consultarRealizadas(pagina:number,busqueda:string,estado:string,fecha:string,signal:AbortSignal){
 const parametros=new URLSearchParams({pagina:String(pagina),tamano:'5',busqueda});if(estado)parametros.set('estado',estado);
 if(fecha){const desde=new Date(fecha+'T00:00:00'),hasta=new Date(desde);hasta.setDate(hasta.getDate()+1);parametros.set('desde',desde.toISOString().slice(0,19));parametros.set('hasta',hasta.toISOString().slice(0,19));}
 return solicitarApi<PaginaRealizadas>(`/tutor/sesiones-realizadas?${parametros}`,{signal});
}
export const fechaLocalActual=()=>{const fecha=new Date();return `${fecha.getFullYear()}-${String(fecha.getMonth()+1).padStart(2,'0')}-${String(fecha.getDate()).padStart(2,'0')}`;};
export const entradaFechaLocal=(utc:string)=>{const fecha=new Date(utc+'Z');return `${fecha.getFullYear()}-${String(fecha.getMonth()+1).padStart(2,'0')}-${String(fecha.getDate()).padStart(2,'0')}T${String(fecha.getHours()).padStart(2,'0')}:${String(fecha.getMinutes()).padStart(2,'0')}`;};
