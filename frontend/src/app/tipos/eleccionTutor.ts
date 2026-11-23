export interface TutorDisponible { id:number;nombre:string;departamento:string; }
export interface EleccionTutor {
  periodos:{id:number;nombre:string}[];periodoId:number|null;asignado:TutorDisponible|null;
  contenido:TutorDisponible[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;
}
