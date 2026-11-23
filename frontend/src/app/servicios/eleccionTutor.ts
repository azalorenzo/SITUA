import { solicitarApi } from './autenticacion';
import type { EleccionTutor } from '../tipos/eleccionTutor';
export function consultarEleccionTutor(periodoId:string,pagina:number,busqueda:string,signal:AbortSignal) {
  const parametros=new URLSearchParams({pagina:String(pagina),tamano:'5',busqueda});
  if(periodoId)parametros.set('periodoId',periodoId);
  return solicitarApi<EleccionTutor>(`/estudiante/eleccion-tutor?${parametros}`,{signal});
}
export function elegirTutor(tutorId:number,periodoId:number) {
  return solicitarApi<EleccionTutor>('/estudiante/eleccion-tutor',{method:'POST',body:JSON.stringify({tutorId,periodoId})});
}
