import type { UsuarioSesion } from '../tipos/autenticacion';
import { BarraUsuario } from './BarraUsuario';
export function BarraCoordinacion({usuario,alCerrarSesion,cerrando=false,alAbrirMenu=()=>{},menuAbierto=false}:{usuario:UsuarioSesion;alCerrarSesion:()=>void;cerrando?:boolean;alAbrirMenu?:()=>void;menuAbierto?:boolean}){return <BarraUsuario usuario={usuario} rol="Coordinación" alCerrarSesion={alCerrarSesion} cerrando={cerrando} alAbrirMenu={alAbrirMenu} menuAbierto={menuAbierto}/>;}
