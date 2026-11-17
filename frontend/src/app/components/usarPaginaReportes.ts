import { useEffect,useState } from 'react';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { consultarReportes } from '../servicios/reportes';
import type { CriteriosReportes,PaginaReportes } from '../tipos/reportes';
import type { ErroresFormulario } from '../tipos/autenticacion';
export function usarPaginaReportes(criterios:CriteriosReportes,pagina:number){
  const [datos,cambiarDatos]=useState<PaginaReportes|null>(null);
  const [cargando,cambiarCargando]=useState(true);
  const [error,cambiarError]=useState('');
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');cambiarErrores({});
    consultarReportes(cancelacion.signal,criterios,pagina).then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted){cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar los reportes.');if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);}})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});
    return ()=>cancelacion.abort();
  },[criterios,pagina,intento]);
  return {datos,cargando,error,errores,actualizar:()=>cambiarIntento(anterior=>anterior+1)};
}
