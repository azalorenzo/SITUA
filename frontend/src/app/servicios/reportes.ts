import { solicitarApi,solicitarArchivoApi } from './autenticacion';
import type { CriteriosReportes,PaginaReportes,MotivoReporte } from '../tipos/reportes';
function parametrosReportes(criterios:CriteriosReportes){
  const parametros=new URLSearchParams();
  for(const [campo,valor] of Object.entries(criterios))if(valor.trim())parametros.set(campo,valor.trim());
  return parametros;
}
export function consultarReportes(comprobacion:AbortSignal,criterios:CriteriosReportes,pagina:number){
  const parametros=parametrosReportes(criterios);parametros.set('pagina',String(pagina));parametros.set('tamano','5');
  return solicitarApi<PaginaReportes>(`/coordinacion/reportes?${parametros}`,{signal:comprobacion});
}
export function consultarMotivos(comprobacion:AbortSignal){return solicitarApi<MotivoReporte[]>('/coordinacion/reportes/motivos',{signal:comprobacion});}
export function descargarReportes(criterios:CriteriosReportes){return solicitarArchivoApi(`/coordinacion/reportes/exportar/pdf?${parametrosReportes(criterios)}`);}
export function descargarReporte(id:number){return solicitarArchivoApi(`/coordinacion/reportes/${id}/pdf`);}
