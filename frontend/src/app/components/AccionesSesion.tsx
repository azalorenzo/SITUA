import { FormularioCierreSesion } from './FormularioCierreSesion';
import { useState } from 'react';
import { cambiarEstadoSesion } from '../servicios/sesiones';
import type { SesionAgenda } from '../tipos/sesiones';
import { Dialog as Ventana,DialogContent as Contenido,DialogTitle as Titulo } from './ui/dialog';
export function AccionesSesion({sesion,esTutor=false,alCambiar}:{sesion:SesionAgenda;esTutor?:boolean;alCambiar:()=>void}) {
  const [guardando,cambiarGuardado]=useState(false),[error,cambiarError]=useState('');
  const [accion,cambiarAccion]=useState<'confirmar'|'rechazar'|'cancelar'|'cerrar'|null>(null);
  async function enviar(){if(!accion||accion==='cerrar'||guardando)return;cambiarGuardado(true);cambiarError('');try{await cambiarEstadoSesion(esTutor,sesion.id,accion);cambiarAccion(null);alCambiar();}catch(fallo){cambiarError(fallo instanceof Error?fallo.message:'No se pudo guardar el cambio.');}finally{cambiarGuardado(false);}}
  return <><div className="flex flex-wrap gap-2 mt-2">
    {esTutor&&sesion.puedeResponder&&<><button type="button" className="text-xs text-[#1F6662] underline" onClick={()=>{cambiarAccion('confirmar');cambiarError('');}}>Confirmar</button><button type="button" className="text-xs text-[#B83F4B] underline" onClick={()=>{cambiarAccion('rechazar');cambiarError('');}}>Rechazar</button></>}
    {esTutor&&sesion.puedeRegistrar&&<button type="button" className="text-xs text-[#1F6662] underline" onClick={()=>{cambiarAccion('cerrar');cambiarError('');}}>Registrar resultado</button>}
    {sesion.puedeCancelar&&<button type="button" className="text-xs text-[#B83F4B] underline" onClick={()=>{cambiarAccion('cancelar');cambiarError('');}}>Cancelar sesión</button>}
  </div><Ventana open={accion!==null} onOpenChange={abierto=>{if(!abierto&&!guardando)cambiarAccion(null);}}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828]" onEscapeKeyDown={evento=>{if(guardando)evento.preventDefault();}} onPointerDownOutside={evento=>{if(guardando)evento.preventDefault();}}><Titulo>{accion==='cerrar'?'Registrar resultado':accion==='confirmar'?'¿Confirmar esta tutoría?':accion==='rechazar'?'¿Rechazar esta solicitud?':'¿Cancelar esta tutoría?'}</Titulo>{accion==='cerrar'?<FormularioCierreSesion alCambiarProceso={cambiarGuardado} sesion={sesion} alGuardar={()=>{cambiarAccion(null);alCambiar();}}/>:<><p className="text-sm">{sesion.tema}</p>{error&&<p role="alert" className="text-sm text-[#B83F4B]">{error}</p>}<div className="flex justify-end gap-3"><button type="button" disabled={guardando} className="px-4 py-2 border rounded-lg" onClick={()=>cambiarAccion(null)}>Volver</button><button type="button" disabled={guardando} className="px-4 py-2 rounded-lg bg-[#1F6662] text-white disabled:opacity-50" onClick={enviar}>{guardando?'Guardando…':accion==='confirmar'?'Confirmar':accion==='rechazar'?'Rechazar':'Sí, cancelar'}</button></div></>}</Contenido></Ventana></>;
}
