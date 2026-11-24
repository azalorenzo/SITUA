import { solicitarApi } from './autenticacion';
import type { ConversacionTutor as Conversacion, MensajeTutor as Mensaje } from '../tipos/mensajesTutor';
const ruta=(esTutor:boolean)=>`/${esTutor?'tutor':'estudiante'}/mensajes/conversaciones`;
export function consultarConversaciones(esTutor:boolean,busqueda:string,signal:AbortSignal){return solicitarApi<Conversacion[]>(`${ruta(esTutor)}?${new URLSearchParams({busqueda})}`,{signal});}
export function consultarMensajes(esTutor:boolean,id:number,signal:AbortSignal){return solicitarApi<Mensaje[]>(`${ruta(esTutor)}/${id}/mensajes`,{signal});}
export function leerConversacion(esTutor:boolean,id:number,ultimoMensajeId:number,signal:AbortSignal){return solicitarApi<{id:number}>(`${ruta(esTutor)}/${id}/leer?ultimoMensajeId=${ultimoMensajeId}`,{method:'PATCH',signal});}
export function enviarMensaje(esTutor:boolean,id:number,contenido:string){return solicitarApi<Mensaje>(`${ruta(esTutor)}/${id}/mensajes`,{method:'POST',body:JSON.stringify({contenido})});}
