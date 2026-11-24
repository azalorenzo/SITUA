import { Home,Calendar,BookOpen,MessageSquare,Bell } from 'lucide-react';
import { NavegacionLateral } from './NavegacionLateral';
const opciones=[{id:'inicio',nombre:'Inicio',icono:Home},{id:'mi-agenda',nombre:'Mi agenda',icono:Calendar},{id:'mis-tutorias',nombre:'Mis tutorías',icono:BookOpen},{id:'mensajes',nombre:'Mensajes',icono:MessageSquare},{id:'avisos',nombre:'Avisos',icono:Bell}];
export function StudentSidebar({activeSection,onSectionChange,abierto=false,alCerrar=()=>{}}:{activeSection:string;onSectionChange:(seccion:string)=>void;abierto?:boolean;alCerrar?:()=>void}){return <NavegacionLateral opciones={opciones} seccion={activeSection} alCambiar={onSectionChange} abierto={abierto} alCerrar={alCerrar}/>;}
