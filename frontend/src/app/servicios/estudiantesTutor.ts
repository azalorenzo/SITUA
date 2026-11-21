import { solicitarApi } from './autenticacion';
import type { DetalleEstudianteTutor, EstudianteTutor } from '../tipos/estudiantesTutor';

// El tutor se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarEstudiantesTutor(comprobacion: AbortSignal) {
  return solicitarApi<EstudianteTutor[]>('/tutor/estudiantes', { signal: comprobacion });
}

export function consultarDetalleEstudianteTutor(id: number, comprobacion: AbortSignal) {
  return solicitarApi<DetalleEstudianteTutor>(`/tutor/estudiantes/${id}`, { signal: comprobacion });
}
