import { solicitarApi } from './autenticacion';
import type { Departamento, FiltrosTutores, PaginaTutores, TutorCoordinacion } from '../tipos/coordinacion';

export function consultarPaginaTutores(comprobacion: AbortSignal, filtros: FiltrosTutores) {
  const parametros=new URLSearchParams({ pagina:String(filtros.pagina), tamano:String(filtros.tamano) });
  if(filtros.busqueda.trim()) parametros.set('busqueda',filtros.busqueda.trim());
  if(filtros.estado!=='todos') parametros.set('estado',filtros.estado);
  if(filtros.departamentoId) parametros.set('departamentoId',filtros.departamentoId);
  return solicitarApi<PaginaTutores>(`/coordinacion/tutores?${parametros}`,{ signal:comprobacion });
}
export function consultarDepartamentos(comprobacion: AbortSignal) {
  return solicitarApi<Departamento[]>('/coordinacion/departamentos',{ signal:comprobacion });
}
export function crearTutor(datos: { nombre:string; correo:string; contrasena:string; departamentoId:number }) {
  return solicitarApi<TutorCoordinacion>('/coordinacion/tutores',{ method:'POST',body:JSON.stringify(datos) });
}
export function editarTutor(id:number,datos:{nombre:string;correo:string;departamentoId:number}) {
  return solicitarApi<TutorCoordinacion>(`/coordinacion/tutores/${id}`,{ method:'PUT',body:JSON.stringify(datos) });
}
export function cambiarEstadoTutor(id:number,activar:boolean) {
  return solicitarApi<TutorCoordinacion>(`/coordinacion/tutores/${id}/${activar?'activar':'desactivar'}`,{ method:'PATCH' });
}
