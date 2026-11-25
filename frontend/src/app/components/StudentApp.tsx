import { ProveedorLlamadas } from './ProveedorLlamadas';
import { useState } from 'react';
import type { UsuarioSesion } from '../tipos/autenticacion';
import { StudentSidebar } from './StudentSidebar';
import { BarraUsuario } from './BarraUsuario';
import { CompletarPerfilEstudiante } from './CompletarPerfilEstudiante';
import { StudentView } from './StudentView';
import { StudentAgendaView } from './StudentAgendaView';
import { StudentTutoriasView } from './StudentTutoriasView';
import { StudentMessagesView } from './StudentMessagesView';
import { StudentNotificationsView } from './StudentNotificationsView';
export function StudentApp({onLogout,usuario,cerrando=false}:{onLogout:()=>void;usuario:UsuarioSesion;cerrando?:boolean}){
 const [seccion,cambiarSeccion]=useState('inicio'),[menuAbierto,cambiarMenu]=useState(false),[editarContacto,cambiarContacto]=useState(false);
 return <ProveedorLlamadas><div className="flex h-dvh bg-[#F5F7FA] text-[#0B2828]"><CompletarPerfilEstudiante alSalir={onLogout} editarContacto={editarContacto} alCerrar={()=>cambiarContacto(false)}/><StudentSidebar activeSection={seccion} onSectionChange={cambiarSeccion} abierto={menuAbierto} alCerrar={()=>cambiarMenu(false)}/>
  <main className="flex-1 min-w-0 overflow-auto"><BarraUsuario usuario={usuario} rol="Estudiante" alEditarContacto={()=>cambiarContacto(true)} alCerrarSesion={onLogout} cerrando={cerrando} alAbrirMenu={()=>cambiarMenu(true)} menuAbierto={menuAbierto}/><div className="p-4 sm:p-8">{seccion==='inicio'&&<StudentView/>}
{seccion==='mi-agenda'&&<StudentAgendaView/>}
{seccion==='mis-tutorias'&&<StudentTutoriasView/>}
{seccion==='mensajes'&&<StudentMessagesView/>}
{seccion==='avisos'&&<StudentNotificationsView/>}</div></main></div></ProveedorLlamadas>;
}
