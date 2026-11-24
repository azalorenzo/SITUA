import { solicitarApi } from './autenticacion';
import type { DetalleEstudianteTutor, EstudianteTutor } from '../tipos/estudiantesTutor';

// El tutor se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarEstudiantesTutor(comprobacion: AbortSignal, busqueda = '', riesgo = 'todos') {
  const parametros = new URLSearchParams({ busqueda: busqueda.trim(), riesgo });
  return solicitarApi<EstudianteTutor[]>(`/tutor/estudiantes?${parametros}`, { signal: comprobacion });
}

export function consultarDetalleEstudianteTutor(id: number, comprobacion: AbortSignal) {
  return solicitarApi<DetalleEstudianteTutor>(`/tutor/estudiantes/${id}`, { signal: comprobacion });
}

export function consultarPaginaEstudiantesTutor(comprobacion:AbortSignal,busqueda:string,riesgo:string,pagina:number){
 return solicitarApi<import('../tipos/paginacion').Pagina<EstudianteTutor>>(`/tutor/estudiantes/pagina?${new URLSearchParams({busqueda,riesgo,pagina:String(pagina),tamano:'10'})}`,{signal:comprobacion});
}
