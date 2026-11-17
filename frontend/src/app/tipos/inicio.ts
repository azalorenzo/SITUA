export interface ResumenInicio {
  indicadores:{totalEstudiantes:number;tutoresActivos:number;estudiantesEnRiesgo:number;sinEvaluacion:number;totalReportes:number};
  motivos:{id:number;nombre:string;cantidad:number;porcentaje:number}[];
  mensual:{mes:string;etiqueta:string;cantidad:number}[];
  desde:string;hasta:string;actualizadoEn:string;
}
