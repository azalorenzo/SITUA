import { solicitarApi } from './autenticacion';
import type { TutorCoordinacion, EstudianteCoordinacion } from '../tipos/coordinacion';

export function consultarTutores(comprobacion: AbortSignal) {
  return solicitarApi<TutorCoordinacion[]>('/coordinacion/tutores/opciones', { signal: comprobacion });
}

export function consultarEstudiantes(comprobacion: AbortSignal) {
  return solicitarApi<EstudianteCoordinacion[]>('/coordinacion/estudiantes', { signal: comprobacion });
}
