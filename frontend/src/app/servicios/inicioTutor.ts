import { solicitarApi } from './autenticacion';
import type { InicioTutor } from '../tipos/inicioTutor';

// El tutor se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarInicioTutor(comprobacion: AbortSignal) {
  return solicitarApi<InicioTutor>('/tutor/inicio', { signal: comprobacion });
}
