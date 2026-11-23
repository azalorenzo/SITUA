export interface SesionAgenda {
  id:number;disponibilidadId:number|null;tutor:string;estudiante:string;tema:string;
  inicioEn:string;finEn:string;modalidad:'presencial'|'virtual';ubicacion:string|null;enlaceVirtual:string|null;
  estado:'solicitada'|'confirmada'|'rechazada'|'cancelada'|'completada'|'no_asistio';periodoId:number;periodo:string;
  mensajeSolicitud:string|null;puedeCancelar:boolean;puedeResponder:boolean;puedeRegistrar:boolean;observaciones:string|null;duracionMinutos:number;
}
export interface PaginaSesiones {contenido:SesionAgenda[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;}
export interface OpcionSesion {periodoId:number;periodo:string;tutorId:number;tutor:string;}
export interface ResumenSesiones {proximas:SesionAgenda[];actividades:{id:number;tipo:string;tema:string;estado:SesionAgenda['estado'];ocurridoEn:string}[];}
export interface PaginaHorariosDisponibles {contenido:import('./horarios').Horario[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;}
