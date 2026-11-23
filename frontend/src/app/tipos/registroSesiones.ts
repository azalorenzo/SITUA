export interface OpcionRegistroSesion {estudianteId:number;nombre:string;periodoId:number;periodo:string;}
export interface SesionRealizada {id:number;estudianteId:number;estudiante:string;periodoId:number;periodo:string;tema:string;inicioEn:string;finEn:string;estado:'completada'|'cancelada'|'no_asistio';ubicacion:string;observaciones:string|null;duracionMinutos:number;}
export interface PaginaRealizadas {contenido:SesionRealizada[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;resumen:{total:number;completadas:number;canceladas:number;noAsistio:number;horas:number};}
export interface DatosRegistrarSesion {estudianteId:number;periodoId:number;tema:string;inicioEn:string;finEn:string;ubicacion:string;observaciones:string;}
