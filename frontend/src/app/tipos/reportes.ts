export interface CriteriosReportes {
  busqueda:string;estado:string;periodoId:string;motivoId:string;desde:string;hasta:string;
}
export interface ReporteCoordinacion {
  id:number;estudianteId:number;estudiante:string;matricula:string;tutorId:number;tutor:string;
  periodoId:number;periodo:string;carrera:string;semestre:number;motivoId:number;motivo:string;
  fecha:string;estado:'pendiente'|'completado';contenido:string|null;completadoEn:string|null;completadoPor:string|null;
}
export interface ResumenReportes { total:number;completados:number;pendientes:number; }
export interface MotivoDistribucion { id:number;nombre:string;cantidad:number;porcentaje:number; }
export interface MotivoReporte { id:number;nombre:string;activo:boolean; }
export interface PaginaReportes {
  contenido:ReporteCoordinacion[];pagina:number;tamano:number;totalRegistros:number;totalPaginas:number;
  resumen:ResumenReportes;motivos:MotivoDistribucion[];
}
