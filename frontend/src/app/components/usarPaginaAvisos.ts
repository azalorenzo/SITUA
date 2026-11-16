import { useEffect,useState } from 'react';
import { consultarAvisos } from '../servicios/avisos';
import type { FiltrosAvisos,PaginaAvisos } from '../tipos/avisos';
export function usarPaginaAvisos({pagina,tamano,busqueda,prioridad,estado,categoria}:FiltrosAvisos){
  const [datos,cambiarDatos]=useState<PaginaAvisos|null>(null);
  const [cargando,cambiarCargando]=useState(true);
  const [error,cambiarError]=useState('');
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');
    consultarAvisos(cancelacion.signal,{pagina,tamano,busqueda,prioridad,estado,categoria}).then(respuesta=>{
      if(!cancelacion.signal.aborted)cambiarDatos(respuesta);
    }).catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron cargar los avisos.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});
    return ()=>cancelacion.abort();
  },[pagina,tamano,busqueda,prioridad,estado,categoria,intento]);
  return {datos,cargando,error,actualizar:()=>cambiarIntento(anterior=>anterior+1)};
}
