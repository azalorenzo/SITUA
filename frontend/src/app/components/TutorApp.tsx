import { ProveedorLlamadas } from './ProveedorLlamadas';
import { useState } from 'react';
import type { UsuarioSesion } from '../tipos/autenticacion';
import { TutorSidebar } from './TutorSidebar';
import { BarraUsuario } from './BarraUsuario';

import { TutorView } from './TutorView';
import { MyStudentsView } from './MyStudentsView';
import { ScheduleView } from './ScheduleView';
import { CompletedSessionsView } from './CompletedSessionsView';
import { TutorReportsView } from './TutorReportsView';
import { MessagesView } from './MessagesView';
import { TutorNotificationsView } from './TutorNotificationsView';
export function TutorApp({onLogout,usuario,cerrando=false}:{onLogout:()=>void;usuario:UsuarioSesion;cerrando?:boolean}){
 const [seccion,cambiarSeccion]=useState('inicio'),[menuAbierto,cambiarMenu]=useState(false);
 return <ProveedorLlamadas><div className="flex h-dvh bg-[#F5F7FA] text-[#0B2828]"><TutorSidebar activeSection={seccion} onSectionChange={cambiarSeccion} abierto={menuAbierto} alCerrar={()=>cambiarMenu(false)}/>
  <main className="flex-1 min-w-0 overflow-auto"><BarraUsuario usuario={usuario} rol="Tutor" alCerrarSesion={onLogout} cerrando={cerrando} alAbrirMenu={()=>cambiarMenu(true)} menuAbierto={menuAbierto}/><div className="p-4 sm:p-8">{seccion==='inicio'&&<TutorView onSectionChange={cambiarSeccion}/>}
{seccion==='mis-estudiantes'&&<MyStudentsView/>}
{seccion==='horarios'&&<ScheduleView/>}
{seccion==='sesiones-realizadas'&&<CompletedSessionsView/>}
{seccion==='reportes'&&<TutorReportsView/>}
{seccion==='mensajes'&&<MessagesView/>}
{seccion==='avisos'&&<TutorNotificationsView/>}</div></main></div></ProveedorLlamadas>;
}
