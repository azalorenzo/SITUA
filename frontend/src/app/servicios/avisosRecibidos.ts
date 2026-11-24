import { solicitarApi } from './autenticacion';
import type { AvisoRecibido, AvisosMarcados } from '../tipos/avisosRecibidos';

// La persona se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarAvisosRecibidos(comprobacion: AbortSignal) {
  return solicitarApi<AvisoRecibido[]>('/tutor/avisos', { signal: comprobacion });
}

export function marcarAvisoLeido(id: number) {
  return solicitarApi<AvisoRecibido>(`/tutor/avisos/${id}/leer`, { method: 'PATCH' });
}

export function marcarAvisosLeidos() {
  return solicitarApi<AvisosMarcados>('/tutor/avisos/leer-todos', { method: 'PATCH' });
}

export function consultarPaginaAvisosTutor(comprobacion:AbortSignal,pagina:number,busqueda:string,lectura:string,prioridad:string){return solicitarApi<import('../tipos/avisosRecibidos').PaginaAvisosTutor>(`/tutor/avisos/pagina?${new URLSearchParams({pagina:String(pagina),tamano:'5',busqueda,lectura,prioridad})}`,{signal:comprobacion});}
