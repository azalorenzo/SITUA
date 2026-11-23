import { solicitarApi } from './autenticacion';
import type { ConversacionLeida, ConversacionTutor, MensajeTutor } from '../tipos/mensajesTutor';

// La persona se identifica con el JWT; nunca se envía su identificador desde React.
export function consultarConversaciones(comprobacion: AbortSignal) {
  return solicitarApi<ConversacionTutor[]>('/tutor/mensajes/conversaciones', { signal: comprobacion });
}

export function consultarMensajesConversacion(id: number, comprobacion: AbortSignal) {
  return solicitarApi<MensajeTutor[]>(`/tutor/mensajes/conversaciones/${id}/mensajes`, { signal: comprobacion });
}

export function marcarConversacionLeida(id: number) {
  return solicitarApi<ConversacionLeida>(`/tutor/mensajes/conversaciones/${id}/leer`, { method: 'PATCH' });
}

export function enviarMensajeConversacion(id: number, contenido: string) {
  return solicitarApi<MensajeTutor>(`/tutor/mensajes/conversaciones/${id}/mensajes`, { method: 'POST', body: JSON.stringify({ contenido }) });
}
