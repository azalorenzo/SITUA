import { solicitarApi } from './autenticacion';
import type { AsignacionTutor, PeriodoAcademico, EstudianteAsignable, DatosAsignacion, FiltrosAsignaciones, PaginaAsignaciones } from '../tipos/asignaciones';

function parametrosAsignaciones(filtros: FiltrosAsignaciones) {
  const parametros = new URLSearchParams();
  if (filtros.periodoId) parametros.set('periodoId', filtros.periodoId);
  if (filtros.estado !== 'todos') parametros.set('estado', filtros.estado);
  if (filtros.busqueda.trim()) parametros.set('busqueda', filtros.busqueda.trim());
  return parametros;
}
export function consultarAsignaciones(comprobacion: AbortSignal, filtros: FiltrosAsignaciones) {
  const parametros = parametrosAsignaciones(filtros);
  parametros.set('pagina', String(filtros.pagina)); parametros.set('tamano', String(filtros.tamano));
  return solicitarApi<PaginaAsignaciones>(`/coordinacion/asignaciones?${parametros}`, { signal: comprobacion });
}
export function exportarAsignaciones(filtros: FiltrosAsignaciones) {
  return solicitarApi<AsignacionTutor[]>(`/coordinacion/asignaciones/exportar?${parametrosAsignaciones(filtros)}`);
}
export function consultarPeriodos(comprobacion: AbortSignal) {
  return solicitarApi<PeriodoAcademico[]>('/coordinacion/periodos', { signal: comprobacion });
}
export function consultarEstudiantesAsignables(periodoId: number, comprobacion: AbortSignal) {
  return solicitarApi<EstudianteAsignable[]>(`/coordinacion/asignaciones/estudiantes?periodoId=${periodoId}`, { signal: comprobacion });
}
export function crearAsignacion(datos: DatosAsignacion) {
  return solicitarApi<AsignacionTutor>('/coordinacion/asignaciones', { method: 'POST', body: JSON.stringify(datos) });
}
export function cambiarTutor(id: number, tutorId: number, motivo: string) {
  return solicitarApi<AsignacionTutor>(`/coordinacion/asignaciones/${id}/cambiar-tutor`, { method: 'PATCH', body: JSON.stringify({ tutorId, motivo }) });
}
export function cerrarAsignacion(id: number, accion: 'finalizar' | 'cancelar') {
  return solicitarApi<AsignacionTutor>(`/coordinacion/asignaciones/${id}/${accion}`, { method: 'PATCH' });
}
