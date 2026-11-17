import { Download,ChevronLeft,ChevronRight } from 'lucide-react';
import type { PaginaReportes } from '../tipos/reportes';
import { EstadoConsulta } from './ConsultaCoordinacion';
function TablaDetalleReportes({datos,cargando,error,alReintentar,alCambiarPagina,alDescargar,descargando}:{
  datos:PaginaReportes|null;cargando:boolean;error:string;alReintentar:()=>void;alCambiarPagina:(pagina:number)=>void;
  alDescargar:(id:number)=>void;descargando:number|'todos'|null;
}){
  const actual=datos?.pagina||1,total=datos?.totalRegistros||0,paginas=datos?.totalPaginas||0;
  return <section className="tabla-detalle-reportes" aria-labelledby="titulo-tabla-reportes" aria-busy={cargando}><h3 id="titulo-tabla-reportes">Detalle de reportes</h3><EstadoConsulta cargando={cargando} error={error} alReintentar={alReintentar}/>
    {!cargando&&!error&&<>
      {datos?.contenido.length?<div className="desplazamiento-tabla-reportes"><table aria-label="Detalle de reportes"><thead><tr>{['Estudiante','Tutor','Fecha','Estado','Motivo','Acciones'].map(titulo=><th key={titulo} scope="col">{titulo}</th>)}</tr></thead><tbody>{datos.contenido.map(reporte=><tr key={reporte.id}><td><strong>{reporte.estudiante}</strong><small>{reporte.matricula} · {reporte.periodo}</small></td><td>{reporte.tutor}</td><td><time dateTime={reporte.fecha}>{new Intl.DateTimeFormat('es-MX',{day:'2-digit',month:'2-digit',year:'numeric',timeZone:'UTC'}).format(new Date(reporte.fecha+'T00:00:00Z'))}</time></td><td><span className={`estado-reporte ${reporte.estado}`}>{reporte.estado==='completado'?'Completado':'Pendiente'}</span></td><td>{reporte.motivo}</td><td><button type="button" className="boton-exportar-reporte" aria-label={`Exportar reporte ${reporte.id}`} disabled={descargando!==null} onClick={()=>alDescargar(reporte.id)}><Download aria-hidden="true"/>{descargando===reporte.id?'Generando…':'PDF'}</button></td></tr>)}</tbody></table></div>:<p className="nota-reportes lista-vacia-reportes">No hay reportes que coincidan con los filtros.</p>}
      <div className="pie-tabla-reportes"><p role="status">Mostrando {total?(actual-1)*(datos?.tamano||5)+1:0}–{Math.min(actual*(datos?.tamano||5),total)} de {total} reportes</p><nav aria-label="Paginación de reportes"><button type="button" disabled={actual<=1} onClick={()=>alCambiarPagina(actual-1)}><ChevronLeft aria-hidden="true"/>Anterior</button>{Array.from({length:paginas},(_,indice)=>indice+1).map(numero=><button type="button" key={numero} aria-label={`Página ${numero}`} aria-current={numero===actual?'page':undefined} onClick={()=>alCambiarPagina(numero)}>{numero}</button>)}<button type="button" disabled={!paginas||actual>=paginas} onClick={()=>alCambiarPagina(actual+1)}>Siguiente<ChevronRight aria-hidden="true"/></button></nav></div>
    </>}
  </section>;
}
export { TablaDetalleReportes as ReportDetailsTable };
