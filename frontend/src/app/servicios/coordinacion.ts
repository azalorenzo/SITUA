import { solicitarApi } from './autenticacion';
import type { TutorCoordinacion, FiltrosEstudiantes, PaginaEstudiantes } from '../tipos/coordinacion';

export function consultarTutores(comprobacion: AbortSignal) {
  return solicitarApi<TutorCoordinacion[]>('/coordinacion/tutores/opciones', { signal: comprobacion });
}

export function consultarEstudiantes(filtros:FiltrosEstudiantes, comprobacion:AbortSignal) {
  const parametros=new URLSearchParams({pagina:String(filtros.pagina),tamano:String(filtros.tamano)});
  if(filtros.busqueda.trim())parametros.set('busqueda',filtros.busqueda.trim());
  if(filtros.estado!=='todos')parametros.set('estado',filtros.estado);
  return solicitarApi<PaginaEstudiantes>(`/coordinacion/estudiantes?${parametros}`, {signal:comprobacion});
}
