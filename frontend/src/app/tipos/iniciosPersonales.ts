import type { SesionAgenda } from './sesiones';
import type { AvisoRecibido } from './avisosRecibidos';
export interface ActividadPersonal {id:number;titulo:string;descripcion:string|null;tipo:'taller'|'recordatorio';fecha:string|null;inicioEn:string|null;finEn:string|null;responsable:string|null;ubicacion:string|null;estado:string;}
export interface DatosActividad {descripcion?:string|null;titulo:string;tipo:'taller'|'recordatorio';fecha:string|null;inicioEn:string|null;finEn:string|null;responsable:string|null;ubicacion:string|null;}
export interface InicioEstudiante {proxima:SesionAgenda|null;virtuales:SesionAgenda[];talleres:ActividadPersonal[];recordatorios:ActividadPersonal[];}
export interface InicioTutor {indicadores:{estudiantes:number;realizadas:number;reportesPendientes:number;programadas:number};estudiantes:{id:number;nombre:string;carrera:string;nivelRiesgo:string|null}[];proximas:SesionAgenda[];}
export interface AvisosEstudiante {contenido:AvisoRecibido[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;noLeidos:number;}
