import { solicitarApi } from './autenticacion';

export type TipoLlamada = 'audio' | 'video';
export interface Llamada {
  id: number;
  conversacionId: number;
  tipo: TipoLlamada;
  estado: 'pendiente' | 'activa';
  soyIniciador: boolean;
  nombre: string;
  creadaEn: string;
  inicioEn: string | null;
  venceEn: string;
}
export interface EstadoLlamadas { habilitado: boolean; llamada: Llamada | null }
export interface AccesoLlamada { url: string; token: string; venceEn: string; tipo: TipoLlamada }
export const consultarLlamada = () => solicitarApi<EstadoLlamadas>('/llamadas/actual');
export const iniciarLlamada = (conversacionId: number, tipo: TipoLlamada) => solicitarApi<Llamada>('/llamadas', { method: 'POST', body: JSON.stringify({ conversacionId, tipo }) });
export const aceptarLlamada = (id: number) => solicitarApi<Llamada>(`/llamadas/${id}/aceptar`, { method: 'POST' });
export const rechazarLlamada = (id: number) => solicitarApi<EstadoLlamadas>(`/llamadas/${id}/rechazar`, { method: 'POST' });
export const finalizarLlamada = (id: number) => solicitarApi<EstadoLlamadas>(`/llamadas/${id}/finalizar`, { method: 'POST' });
export const entrarLlamada = (id: number) => solicitarApi<AccesoLlamada>(`/llamadas/${id}/acceso`, { method: 'POST' });
