import { GestionReporteAdministracion, EliminarReporteAdministracion } from './GestionAdministracion';
import { useState } from 'react';
import { Download,RefreshCw,Plus } from 'lucide-react';
import { ReportGenerator } from './ReportGenerator';
import { ReportStats } from './ReportStats';
import { AcademicReasonsChart } from './AcademicReasonsChart';
import { ReportDetailsTable } from './ReportDetailsTable';
import { usarPaginaReportes } from './usarPaginaReportes';
import { usarConsultaCoordinacion,EstadoConsulta } from './ConsultaCoordinacion';
import { consultarPeriodos } from '../servicios/asignaciones';
import { consultarMotivos,descargarReporte,descargarReportes } from '../servicios/reportes';
import type { CriteriosReportes, ReporteCoordinacion } from '../tipos/reportes';
import type { ErroresFormulario } from '../tipos/autenticacion';
import '../../styles/reportes.css';

const criteriosIniciales:CriteriosReportes={busqueda:'',estado:'',periodoId:'',motivoId:'',desde:'',hasta:''};
function ReportesCoordinacion(){
  const [edicion,cambiarEdicion]=useState<ReporteCoordinacion|'nuevo'|null>(null),[eliminar,cambiarEliminar]=useState<ReporteCoordinacion|null>(null);
  const [criterios,cambiarCriterios]=useState(criteriosIniciales);
  const [aplicados,cambiarAplicados]=useState(criteriosIniciales);
  const [pagina,cambiarPagina]=useState(1);
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [descargando,cambiarDescargando]=useState<number|'todos'|null>(null);
  const [errorDescarga,cambiarErrorDescarga]=useState('');
  const consulta=usarPaginaReportes(aplicados,pagina);
  const periodos=usarConsultaCoordinacion(consultarPeriodos);
  const motivos=usarConsultaCoordinacion(consultarMotivos);
  function buscar(evento:React.FormEvent){
    evento.preventDefault();const revisados:ErroresFormulario={};
    if(criterios.desde&&criterios.hasta&&criterios.desde>criterios.hasta)revisados.hasta=['La fecha final debe ser igual o posterior a la inicial.'];
    cambiarErrores(revisados);if(Object.keys(revisados).length)return;
    cambiarPagina(1);cambiarAplicados({...criterios});cambiarErrorDescarga('');
  }
  async function descargar(id?:number){
    if(descargando!==null)return;cambiarDescargando(id??'todos');cambiarErrorDescarga('');
    try{
      const archivo=id===undefined?await descargarReportes(aplicados):await descargarReporte(id);
      const direccion=URL.createObjectURL(archivo);const enlace=document.createElement('a');enlace.href=direccion;enlace.download=id===undefined?'reportes.pdf':`reporte-${id}.pdf`;enlace.click();window.setTimeout(()=>URL.revokeObjectURL(direccion),1000);
    }catch(fallo){cambiarErrorDescarga(fallo instanceof Error?fallo.message:'No se pudo descargar el reporte.');}
    finally{cambiarDescargando(null);}
  }
  function actualizar(){consulta.actualizar();periodos.actualizar();motivos.actualizar();}
  return <section className="pagina-reportes" aria-labelledby="titulo-reportes"><header className="encabezado-reportes"><div><h2 id="titulo-reportes">Reportes</h2><p>Consulta los reportes académicos y genera documentos con los criterios seleccionados</p></div><div><button type="button" className="boton-principal-reporte" onClick={()=>cambiarEdicion('nuevo')}><Plus aria-hidden="true"/>Crear reporte</button><button type="button" className="boton-secundario-reporte" disabled={consulta.cargando} onClick={actualizar}><RefreshCw aria-hidden="true"/>Actualizar</button><button type="button" className="boton-principal-reporte" disabled={consulta.cargando||Boolean(consulta.error)||!consulta.datos?.totalRegistros||descargando!==null} onClick={()=>descargar()}><Download aria-hidden="true"/>{descargando==='todos'?'Generando PDF…':'Descargar resultados PDF'}</button></div></header>
    <ReportGenerator criterios={criterios} alCambiar={(campo,valor)=>{const nuevos={...criterios,[campo]:valor};cambiarCriterios(nuevos);cambiarErrores(nuevos.desde&&nuevos.hasta&&nuevos.desde>nuevos.hasta?{hasta:['La fecha final debe ser igual o posterior a la inicial.']}:{});}} alBuscar={buscar} periodos={periodos.datos} motivos={motivos.datos} cargandoCatalogos={periodos.cargando||motivos.cargando} errores={{...consulta.errores,...errores}}/>
    {(periodos.error||motivos.error)&&<EstadoConsulta cargando={false} error={periodos.error||motivos.error} alReintentar={()=>{periodos.actualizar();motivos.actualizar();}}/>}
    {errorDescarga&&<p role="alert" className="error-descarga-reporte">{errorDescarga}</p>}
    <ReportStats resumen={consulta.error?undefined:consulta.datos?.resumen} cargando={consulta.cargando}/>
    <div className="contenido-reportes"><ReportDetailsTable datos={consulta.datos} cargando={consulta.cargando} error={consulta.error} alReintentar={consulta.actualizar} alCambiarPagina={cambiarPagina} alDescargar={descargar} descargando={descargando} alEditar={cambiarEdicion} alEliminar={cambiarEliminar}/><AcademicReasonsChart motivos={consulta.error?[]:consulta.datos?.motivos||[]} cargando={consulta.cargando}/></div>
    <p className="nota-reportes">Las tarjetas y la distribución de motivos corresponden a todos los resultados filtrados, no solo a la página visible. Descargar un documento no cambia el estado del reporte.</p>
  {edicion&&<GestionReporteAdministracion reporte={edicion==='nuevo'?undefined:edicion} alCerrar={()=>cambiarEdicion(null)} alGuardar={()=>{cambiarEdicion(null);actualizar();}}/>}{eliminar&&<EliminarReporteAdministracion reporte={eliminar} alCerrar={()=>cambiarEliminar(null)} alGuardar={()=>{cambiarEliminar(null);actualizar();}}/>}</section>;
}
export { ReportesCoordinacion as ReportsView };
