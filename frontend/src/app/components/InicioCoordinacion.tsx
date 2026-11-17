import { useEffect,useState } from 'react';
import { Users,UserCheck,AlertTriangle,RefreshCw } from 'lucide-react';
import { consultarInicio } from '../servicios/inicio';
import type { ResumenInicio } from '../tipos/inicio';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { RiskChart as GraficaMotivos } from './RiskChart';
import { TutorshipChart as GraficaTutorias } from './TutorshipChart';
import '../../styles/inicio-coordinacion.css';

export function InicioCoordinacion(){
  const [datos,cambiarDatos]=useState<ResumenInicio|null>(null);
  const [cargando,cambiarCargando]=useState(true);
  const [error,cambiarError]=useState('');
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');
    consultarInicio(cancelacion.signal).then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudo consultar el resumen.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});
    return ()=>cancelacion.abort();
  },[intento]);
  const indicadores=datos?.indicadores;
  const tarjetas=[{titulo:'Total estudiantes',valor:indicadores?.totalEstudiantes,icono:Users,tono:'verde'},{titulo:'Tutores activos',valor:indicadores?.tutoresActivos,icono:UserCheck,tono:'verde'},{titulo:'Estudiantes en riesgo',valor:indicadores?.estudiantesEnRiesgo,icono:AlertTriangle,tono:'rojo'}];
  return <section className="inicio-coordinacion" aria-labelledby="titulo-resumen-inicio"><header><div><h2 id="titulo-resumen-inicio">Resumen general</h2><p>Indicadores de estudiantes, tutores y seguimiento académico</p></div><button type="button" disabled={cargando} onClick={()=>cambiarIntento(anterior=>anterior+1)}><RefreshCw aria-hidden="true"/>Actualizar</button></header>
    <EstadoConsulta cargando={cargando} error={error} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
    {!cargando&&!error&&datos&&<>
      <div className="tarjetas-inicio-coordinacion">{tarjetas.map(tarjeta=>{const Icono=tarjeta.icono;return <div key={tarjeta.titulo}><span className={`icono-tarjeta-inicio ${tarjeta.tono}`}><Icono aria-hidden="true"/></span><div><p>{tarjeta.titulo}</p><strong>{tarjeta.valor}</strong></div></div>;})}</div>
      <p className="nota-inicio-coordinacion">El riesgo incluye niveles medio y alto de la última evaluación del periodo más reciente de cada estudiante. Sin evaluación registrada: {datos.indicadores.sinEvaluacion}. Total y riesgo incluyen todos los perfiles; tutores activos se refiere al acceso de su cuenta.</p>
      <div className="graficas-inicio-coordinacion"><GraficaMotivos motivos={datos.motivos} total={datos.indicadores.totalReportes}/><GraficaTutorias mensual={datos.mensual} desde={datos.desde} hasta={datos.hasta}/></div>
      <p className="nota-inicio-coordinacion">Corte del resumen: {new Intl.DateTimeFormat('es-MX',{dateStyle:'medium',timeStyle:'short',timeZone:'UTC'}).format(new Date(datos.actualizadoEn))} UTC. La distribución de motivos muestra reportes registrados; no determina el nivel de riesgo académico.</p>
    </>}
  </section>;
}
