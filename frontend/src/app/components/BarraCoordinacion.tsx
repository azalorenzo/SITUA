import { Users, ChevronDown, LogOut } from 'lucide-react';
import type { UsuarioSesion } from '../tipos/autenticacion';
import imagenCoordinacion from '../../assets/secretary.png';
import {
  DropdownMenu as Menu, DropdownMenuTrigger as BotonMenu,
  DropdownMenuContent as ContenidoMenu, DropdownMenuItem as OpcionMenu,
  DropdownMenuLabel as EtiquetaMenu, DropdownMenuSeparator as SeparadorMenu,
} from './ui/dropdown-menu';

export function BarraCoordinacion({usuario,alCerrarSesion}:{usuario:UsuarioSesion;alCerrarSesion:()=>void}) {
  return <header className="bg-white shadow-sm sticky top-0 z-10">
    <div className="px-8 py-4 flex flex-wrap justify-between items-center gap-4">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-12 h-12 shrink-0 bg-[#1F6662] rounded-full flex items-center justify-center text-white"><Users className="w-6 h-6" aria-hidden="true"/></div>
        <div><h2 className="font-semibold">¡Hola, {usuario.nombre}!</h2><p className="text-sm text-[#53716E]">Gestiona tu programa de tutorados</p></div>
      </div>
      <Menu>
        <BotonMenu asChild><button type="button" aria-label="Menú de usuario" className="ml-auto flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-[#F5F7FA] focus-visible:outline-2 focus-visible:outline-[#1F6662]">
          <img src={usuario.avatarUrl||imagenCoordinacion} alt="" className="w-10 h-10 shrink-0 rounded-full object-cover"/>
          <span className="hidden md:block text-left max-w-52"><span className="block truncate font-semibold text-sm">{usuario.nombre}</span><span className="block text-xs text-[#53716E]">Coordinación</span></span>
          <ChevronDown className="w-4 h-4 text-[#53716E]" aria-hidden="true"/>
        </button></BotonMenu>
        <ContenidoMenu align="end" sideOffset={8} className="w-64 max-w-[calc(100vw-2rem)] bg-white text-[#0B2828] border-[#DCE3EA]">
          <EtiquetaMenu><span className="block break-words">{usuario.nombre}</span><span className="block break-all text-xs font-normal text-[#53716E]">{usuario.correo}</span></EtiquetaMenu>
          <SeparadorMenu/>
          <OpcionMenu onSelect={alCerrarSesion} className="cursor-pointer text-[#B83F4B]"><LogOut aria-hidden="true"/>Cerrar sesión</OpcionMenu>
        </ContenidoMenu>
      </Menu>
    </div>
  </header>;
}
