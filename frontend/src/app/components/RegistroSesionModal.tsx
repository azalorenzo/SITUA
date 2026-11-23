import { useState } from 'react';
import { FormularioRegistroSesion } from './FormularioRegistroSesion';
import { Dialog as Ventana,DialogContent as Contenido,DialogTitle as Titulo } from './ui/dialog';
export function RegistroSesionModal({estudianteId,nombre,alCerrar,alGuardar}:{estudianteId:number;nombre:string;alCerrar:()=>void;alGuardar:()=>void}){
 const [guardando,cambiarGuardado]=useState(false);
 return <Ventana open onOpenChange={abierto=>{if(!abierto&&!guardando)alCerrar();}}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828] max-h-[90vh] overflow-y-auto" onEscapeKeyDown={evento=>{if(guardando)evento.preventDefault();}} onPointerDownOutside={evento=>{if(guardando)evento.preventDefault();}}><Titulo>Registrar tutoría</Titulo><p className="text-sm text-[#53716E]">{nombre}</p><FormularioRegistroSesion estudianteId={estudianteId} alGuardar={alGuardar} alCambiarProceso={cambiarGuardado}/></Contenido></Ventana>;
}
