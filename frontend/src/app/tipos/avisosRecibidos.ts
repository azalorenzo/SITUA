import type { Pagina } from './paginacion';
// Aviso visible para la persona conectada, tal como lo devuelve el backend. Las fechas están en UTC sin zona.
export interface AvisoRecibido {
  id: number;
  titulo: string;
  descripcion: string;
  categoria: string;
  prioridad: 'normal' | 'media' | 'alta';
  sesionId: number | null;
  puedeResponderSesion: boolean;
  publicadoEn: string;
  leidoEn: string | null;
}

export interface AvisosMarcados {
  marcados: number;
}

export interface PaginaAvisosTutor extends Pagina<AvisoRecibido>{noLeidos:number;importantes:number;}
