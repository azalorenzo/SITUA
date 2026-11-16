import { solicitarApi } from './autenticacion';
import type { FiltrosAvisos,PaginaAvisos,PaginaDestinatarios,DetalleAviso,DatosAviso,AvisoCoordinacion } from '../tipos/avisos';
export function consultarAvisos(comprobacion:AbortSignal,filtros:FiltrosAvisos){
  const parametros=new URLSearchParams({pagina:String(filtros.pagina),tamano:String(filtros.tamano)});
  for(const [campo,valor] of Object.entries({busqueda:filtros.busqueda.trim(),categoria:filtros.categoria.trim(),prioridad:filtros.prioridad==='todas'?'':filtros.prioridad,estado:filtros.estado==='todos'?'':filtros.estado}))if(valor)parametros.set(campo,valor);
  return solicitarApi<PaginaAvisos>(`/coordinacion/avisos?${parametros}`,{signal:comprobacion});
}
export function consultarDestinatarios(comprobacion:AbortSignal,pagina:number,busqueda:string,rol:string){
  const parametros=new URLSearchParams({pagina:String(pagina),tamano:'10'});
  if(busqueda.trim())parametros.set('busqueda',busqueda.trim());if(rol)parametros.set('rol',rol);
  return solicitarApi<PaginaDestinatarios>(`/coordinacion/avisos/destinatarios?${parametros}`,{signal:comprobacion});
}
export function consultarDetalleAviso(id:number,comprobacion:AbortSignal){return solicitarApi<DetalleAviso>(`/coordinacion/avisos/${id}`,{signal:comprobacion});}
export function crearAviso(datos:DatosAviso){return solicitarApi<DetalleAviso>('/coordinacion/avisos',{method:'POST',body:JSON.stringify(datos)});}
export function editarAviso(id:number,datos:DatosAviso){return solicitarApi<DetalleAviso>(`/coordinacion/avisos/${id}`,{method:'PUT',body:JSON.stringify(datos)});}
export function retirarAviso(id:number){return solicitarApi<AvisoCoordinacion>(`/coordinacion/avisos/${id}/retirar`,{method:'PATCH'});}
