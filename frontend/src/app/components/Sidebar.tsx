import { Home,UserCheck,FileText,Bell } from 'lucide-react';
import { NavegacionLateral } from './NavegacionLateral';
const opciones=[{id:'inicio',nombre:'Inicio',icono:Home},{id:'tutores',nombre:'Tutores',icono:UserCheck},{id:'estudiantes',nombre:'Estudiantes',icono:UserCheck},{id:'asignaciones',nombre:'Asignaciones',icono:UserCheck},{id:'reportes',nombre:'Reportes',icono:FileText},{id:'avisos',nombre:'Avisos',icono:Bell}];
export function Sidebar({activeSection,onSectionChange,abierto=false,alCerrar=()=>{}}:{activeSection:string;onSectionChange:(seccion:string)=>void;abierto?:boolean;alCerrar?:()=>void}){return <NavegacionLateral opciones={opciones} seccion={activeSection} alCambiar={onSectionChange} abierto={abierto} alCerrar={alCerrar}/>;}
