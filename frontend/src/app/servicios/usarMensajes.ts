import { useEffect,useRef,useState } from 'react';
import type { ConversacionTutor as Conversacion,MensajeTutor as Mensaje } from '../tipos/mensajesTutor';
import { consultarConversaciones,consultarMensajes,leerConversacion,enviarMensaje } from './mensajes';
const textoError=(fallo:unknown)=>fallo instanceof Error?fallo.message:'No se pudieron consultar los mensajes.';
export function usarMensajes(esTutor:boolean){
 const [conversaciones,cambiarConversaciones]=useState<Conversacion[]>([]),[seleccionada,cambiarSeleccionada]=useState<Conversacion|null>(null);
 const [mensajes,cambiarMensajes]=useState<Mensaje[]>([]),[busqueda,cambiarBusqueda]=useState(''),[busquedaAplicada,cambiarBusquedaAplicada]=useState('');
 const [borrador,cambiarBorrador]=useState(''),[enviando,cambiarEnvio]=useState(false),[cargandoLista,cambiarCargaLista]=useState(true),[cargandoMensajes,cambiarCargaMensajes]=useState(false);
 const [errorLista,cambiarErrorLista]=useState(''),[errorMensajes,cambiarErrorMensajes]=useState(''),[errorEnvio,cambiarErrorEnvio]=useState(''),[intento,cambiarIntento]=useState(0);
 const seleccionActual=useRef<number|null>(null),envioActual=useRef(false);
 seleccionActual.current=seleccionada?.id??null;
 useEffect(()=>{const espera=setTimeout(()=>cambiarBusquedaAplicada(busqueda.trim()),300);return()=>clearTimeout(espera);},[busqueda]);
 useEffect(()=>{
  const cancelacion=new AbortController();let espera:ReturnType<typeof setTimeout>;cambiarCargaLista(true);cambiarErrorLista('');
  async function actualizar(){try{const datos=await consultarConversaciones(esTutor,busquedaAplicada,cancelacion.signal);if(cancelacion.signal.aborted)return;cambiarConversaciones(datos);cambiarSeleccionada(actual=>actual?datos.find(dato=>dato.id===actual.id)??actual:datos[0]??null);cambiarErrorLista('');}
   catch(fallo){if(!cancelacion.signal.aborted)cambiarErrorLista(textoError(fallo));}
   finally{if(!cancelacion.signal.aborted){cambiarCargaLista(false);espera=setTimeout(actualizar,3000);}}}
  actualizar();return()=>{cancelacion.abort();clearTimeout(espera);};
 },[esTutor,busquedaAplicada,intento]);
 const id=seleccionada?.id;
 useEffect(()=>{
  cambiarMensajes([]);cambiarErrorMensajes('');cambiarErrorEnvio('');cambiarBorrador('');if(!id){cambiarCargaMensajes(false);return;}
  const cancelacion=new AbortController();let espera:ReturnType<typeof setTimeout>,ultimoLeido=0;cambiarCargaMensajes(true);
  async function actualizar(){try{const datos=await consultarMensajes(esTutor,id!,cancelacion.signal);if(cancelacion.signal.aborted)return;
    cambiarMensajes(actuales=>(actuales.at(-1)?.id??0)>(datos.at(-1)?.id??0)?actuales:datos);cambiarErrorMensajes('');
    const ultimo=datos.at(-1)?.id;if(ultimo&&ultimo!==ultimoLeido&&document.visibilityState==='visible'){
     await leerConversacion(esTutor,id!,ultimo,cancelacion.signal);if(cancelacion.signal.aborted)return;ultimoLeido=ultimo;
     cambiarConversaciones(actuales=>actuales.map(conversacion=>conversacion.id===id?{...conversacion,noLeidos:0}:conversacion));
    }}catch(fallo){if(!cancelacion.signal.aborted)cambiarErrorMensajes(textoError(fallo));}
   finally{if(!cancelacion.signal.aborted){cambiarCargaMensajes(false);espera=setTimeout(actualizar,2000);}}}
  actualizar();return()=>{cancelacion.abort();clearTimeout(espera);};
 },[id,esTutor,intento]);
 async function enviar(){const contenido=borrador.trim(),destino=seleccionActual.current;if(!contenido||!destino||envioActual.current)return;
  if(contenido.length>4000){cambiarErrorEnvio('El mensaje no debe superar 4000 caracteres.');return;}
  envioActual.current=true;cambiarEnvio(true);cambiarErrorEnvio('');try{const mensaje=await enviarMensaje(esTutor,destino,contenido);
   if(seleccionActual.current===destino){cambiarMensajes(actuales=>actuales.some(actual=>actual.id===mensaje.id)?actuales:[...actuales,mensaje]);cambiarBorrador('');}
   cambiarConversaciones(actuales=>actuales.map(conversacion=>conversacion.id===destino?{...conversacion,ultimoMensaje:mensaje.contenido,ultimoEnviadoEn:mensaje.enviadoEn}:conversacion));
  }catch(fallo){if(seleccionActual.current===destino)cambiarErrorEnvio(textoError(fallo));}finally{envioActual.current=false;cambiarEnvio(false);}}
 return {conversaciones,seleccionada,seleccionar:cambiarSeleccionada,mensajes,busqueda,cambiarBusqueda,borrador,cambiarBorrador,enviando,enviar,cargandoLista,cargandoMensajes,errorLista,errorMensajes,errorEnvio,reintentar:()=>cambiarIntento(actual=>actual+1)};
}
