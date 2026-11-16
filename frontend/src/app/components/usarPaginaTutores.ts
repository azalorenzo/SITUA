import { useEffect,useState } from 'react';
import { consultarPaginaTutores } from '../servicios/tutores';
import type { FiltrosTutores,PaginaTutores } from '../tipos/coordinacion';

export function usarPaginaTutores({pagina,tamano,busqueda,estado,departamentoId}:FiltrosTutores) {
  const [datos,cambiarDatos]=useState<PaginaTutores|null>(null);
  const [cargando,cambiarCargando]=useState(true);
  const [error,cambiarError]=useState('');
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');
    consultarPaginaTutores(cancelacion.signal,{pagina,tamano,busqueda,estado,departamentoId}).then(respuesta=>{
      if(!cancelacion.signal.aborted) cambiarDatos(respuesta);
    }).catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudo consultar la lista.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});
    return ()=>cancelacion.abort();
  },[pagina,tamano,busqueda,estado,departamentoId,intento]);
  return {datos,cargando,error,actualizar:()=>cambiarIntento(anterior=>anterior+1)};
}
