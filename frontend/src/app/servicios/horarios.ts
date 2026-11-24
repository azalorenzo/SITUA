import { solicitarApi } from './autenticacion';
import type { Horario, DatosHorario } from '../tipos/horarios';

export function consultarHorarios(comprobacion: AbortSignal,desde?:string,hasta?:string) {
  return solicitarApi<Horario[]>(`/tutor/horarios${desde&&hasta?'?'+new URLSearchParams({desde,hasta}):''}`, { signal: comprobacion });
}

export function crearHorario(datos: DatosHorario) {
  return solicitarApi<Horario>('/tutor/horarios', { method: 'POST', body: JSON.stringify(datos) });
}

export function editarHorario(id: number, datos: DatosHorario) {
  return solicitarApi<Horario>(`/tutor/horarios/${id}`, { method: 'PUT', body: JSON.stringify(datos) });
}

export function eliminarHorario(id: number) {
  return solicitarApi<{ eliminado: boolean }>(`/tutor/horarios/${id}`, { method: 'DELETE' });
}

// El backend guarda todo en UTC sin zona (igual que asignaciones). Estas dos
// funciones son la única parte que conoce esa conversión; la pantalla sigue
// trabajando en hora local, como ya lo hacía con los datos de demostración.
export function aFechaHoraUtc(fecha: string, hora: string): string {
  const local = new Date(`${fecha}T${hora}:00`);
  return local.toISOString().slice(0, 19);
}

function aFechaLocal(utcSinZona: string): Date {
  return new Date(`${utcSinZona}Z`);
}

export function formatearFecha(utcSinZona: string): string {
  const fecha = aFechaLocal(utcSinZona);
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

export function formatearHora(utcSinZona: string): string {
  const fecha = aFechaLocal(utcSinZona);
  return `${String(fecha.getHours()).padStart(2, '0')}:${String(fecha.getMinutes()).padStart(2, '0')}`;
}

export function ubicacionMostrada(horario: Horario): string {
  return horario.modalidad === 'virtual' ? (horario.enlaceVirtual || '') : (horario.ubicacion || '');
}

export interface ResumenHorarios {total:number;disponibles:number;ocupados:number;bloqueados:number;}
export function consultarResumenHorarios(comprobacion:AbortSignal,desde:string,hasta:string){return solicitarApi<ResumenHorarios>(`/tutor/horarios/resumen?${new URLSearchParams({desde,hasta})}`,{signal:comprobacion});}
