export interface AvisoCoordinacion {
  id:number;autorId:number;autor:string;titulo:string;descripcion:string;categoria:string;
  prioridad:'normal'|'media'|'alta';publicadoEn:string;retiradoEn:string|null;destinatarios:number;lecturas:number;
}
export interface DestinatarioAviso {
  usuarioId:number;nombre:string;correo:string;estado:string;leidoEn:string|null;ocultadoEn:string|null;
}
export interface DetalleAviso { aviso:AvisoCoordinacion;destinatarios:DestinatarioAviso[]; }
export interface FiltrosAvisos { pagina:number;tamano:number;busqueda:string;prioridad:string;estado:string;categoria:string; }
export interface PaginaAvisos {
  contenido:AvisoCoordinacion[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;
  resumen:{total:number;vigentes:number;retirados:number;importantes:number};
}
export interface PaginaDestinatarios {
  contenido:DestinatarioAviso[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;
}
export interface DatosAviso {
  titulo:string;descripcion:string;categoria:string;prioridad:string;destinatarios:string;usuarioIds?:number[];
}
