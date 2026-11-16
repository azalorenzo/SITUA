import { useEffect,useState } from 'react';
import { Bell,CheckCircle,Archive,AlertTriangle,Plus,Search,RefreshCw,Eye,Pencil,ArchiveX,ChevronLeft,ChevronRight } from 'lucide-react';
import { usarPaginaAvisos } from './usarPaginaAvisos';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { FormularioAviso,fechaAviso } from './FormularioAviso';
import type { OperacionAviso } from './FormularioAviso';
import '../../styles/avisos.css';

function AvisosCoordinacion(){
  const [busqueda,cambiarBusqueda]=useState('');
  const [categoria,cambiarCategoria]=useState('');
  const [terminos,cambiarTerminos]=useState({busqueda:'',categoria:''});
  const [prioridad,cambiarPrioridad]=useState('todas');
  const [estado,cambiarEstado]=useState('vigente');
  const [pagina,cambiarPagina]=useState(1);
  const [operacion,cambiarOperacion]=useState<OperacionAviso|null>(null);
  const [aviso,cambiarAviso]=useState('');
  const consulta=usarPaginaAvisos({pagina,tamano:5,busqueda:terminos.busqueda,categoria:terminos.categoria,prioridad,estado});
  useEffect(()=>{
    if(busqueda===terminos.busqueda&&categoria===terminos.categoria)return;
    const espera=window.setTimeout(()=>{cambiarTerminos({busqueda,categoria});cambiarPagina(1);},300);
    return ()=>window.clearTimeout(espera);
  },[busqueda,categoria,terminos]);
  const datos=consulta.datos;const filas=consulta.cargando||consulta.error?[]:datos?.contenido||[];
  const actual=datos?.pagina||1;const total=datos?.totalRegistros||0;const paginas=datos?.totalPaginas||0;
  const tarjetas=[{titulo:'Total avisos',valor:datos?.resumen.total,icono:Bell,tono:'verde'},{titulo:'Vigentes',valor:datos?.resumen.vigentes,icono:CheckCircle,tono:'verde'},{titulo:'Retirados',valor:datos?.resumen.retirados,icono:Archive,tono:'gris'},{titulo:'Prioridad alta vigente',valor:datos?.resumen.importantes,icono:AlertTriangle,tono:'rojo'}];
  return <section className="pagina-avisos" aria-labelledby="titulo-avisos">
    <header className="encabezado-avisos"><div><h2 id="titulo-avisos">Avisos</h2><p>Crea y administra avisos para estudiantes y tutores</p></div><div><button type="button" className="boton-secundario-aviso" disabled={consulta.cargando} onClick={consulta.actualizar}><RefreshCw aria-hidden="true"/>Actualizar</button><button type="button" className="boton-principal-aviso" onClick={()=>cambiarOperacion({tipo:'crear'})}><Plus aria-hidden="true"/>Crear aviso</button></div></header>
    {aviso&&<p role="status" className="confirmacion-aviso-guardado">{aviso}</p>}
    <div className="resumen-avisos">{tarjetas.map(tarjeta=>{const Icono=tarjeta.icono;return <div key={tarjeta.titulo}><span className={`icono-resumen-aviso ${tarjeta.tono}`}><Icono aria-hidden="true"/></span><div><p>{tarjeta.titulo}</p><strong>{consulta.cargando||consulta.error?'—':tarjeta.valor??'—'}</strong></div></div>;})}</div>
    <div className="filtros-avisos"><div className="busqueda-avisos"><Search aria-hidden="true"/><input type="search" aria-label="Buscar aviso" maxLength={160} placeholder="Buscar título, descripción o autor" value={busqueda} onChange={evento=>cambiarBusqueda(evento.target.value)}/></div><div><label htmlFor="filtro-categoria-aviso">Categoría</label><input id="filtro-categoria-aviso" maxLength={40} placeholder="Todas las categorías" value={categoria} onChange={evento=>cambiarCategoria(evento.target.value)}/></div><div><label htmlFor="filtro-prioridad-aviso">Prioridad</label><select id="filtro-prioridad-aviso" value={prioridad} onChange={evento=>{cambiarPrioridad(evento.target.value);cambiarPagina(1);}}><option value="todas">Todas</option><option value="normal">Normal</option><option value="media">Media</option><option value="alta">Alta</option></select></div><div><label htmlFor="filtro-estado-aviso">Estado</label><select id="filtro-estado-aviso" value={estado} onChange={evento=>{cambiarEstado(evento.target.value);cambiarPagina(1);}}><option value="vigente">Vigentes</option><option value="retirado">Retirados</option><option value="todos">Todos</option></select></div></div>
    <div className="lista-avisos-coordinacion" aria-busy={consulta.cargando}>
      <EstadoConsulta cargando={consulta.cargando} error={consulta.error} alReintentar={consulta.actualizar}/>
      {filas.length>0&&<ul aria-label="Lista de avisos">{filas.map(registro=><li key={registro.id}><article aria-labelledby={`titulo-aviso-${registro.id}`}><div className="icono-lista-aviso"><Bell aria-hidden="true"/></div><div className="contenido-lista-aviso"><div className="titulo-lista-aviso"><h3 id={`titulo-aviso-${registro.id}`}>{registro.titulo}</h3><span className={`prioridad-aviso ${registro.prioridad}`}>{registro.prioridad==='alta'?'Alta':registro.prioridad==='media'?'Media':'Normal'}</span>{registro.retiradoEn&&<span className="etiqueta-retirado-aviso">Retirado</span>}</div><p className="descripcion-lista-aviso">{registro.descripcion}</p><p className="metadatos-lista-aviso">{registro.categoria} · {fechaAviso(registro.publicadoEn)} · {registro.autor}</p><p className="lecturas-lista-aviso">{registro.destinatarios} destinatarios · {registro.lecturas} lecturas registradas</p></div><div className="acciones-lista-aviso"><button type="button" aria-label="Ver detalle" title="Ver detalle" onClick={()=>cambiarOperacion({tipo:'detalle',id:registro.id})}><Eye aria-hidden="true"/></button>{!registro.retiradoEn&&<><button type="button" aria-label="Editar aviso" title="Editar aviso" onClick={()=>cambiarOperacion({tipo:'editar',id:registro.id})}><Pencil aria-hidden="true"/></button><button type="button" aria-label="Retirar aviso" title="Retirar aviso" className="accion-retirar-aviso" onClick={()=>cambiarOperacion({tipo:'retirar',id:registro.id})}><ArchiveX aria-hidden="true"/></button></>}</div></article></li>)}</ul>}
      {!consulta.cargando&&!consulta.error&&!filas.length&&<p className="lista-vacia-avisos">No hay avisos que coincidan con los filtros.</p>}
    </div>
    {!consulta.cargando&&!consulta.error&&<div className="pie-lista-avisos"><p role="status">Mostrando {total?(actual-1)*5+1:0}–{Math.min(actual*5,total)} de {total} avisos</p><nav aria-label="Paginación de avisos"><button type="button" disabled={actual<=1} onClick={()=>cambiarPagina(actual-1)}><ChevronLeft aria-hidden="true"/>Anterior</button>{Array.from({length:paginas},(_,indice)=>indice+1).map(numero=><button type="button" key={numero} aria-label={`Página ${numero}`} aria-current={numero===actual?'page':undefined} onClick={()=>cambiarPagina(numero)}>{numero}</button>)}<button type="button" disabled={!paginas||actual>=paginas} onClick={()=>cambiarPagina(actual+1)}>Siguiente<ChevronRight aria-hidden="true"/></button></nav></div>}
    {operacion&&<FormularioAviso operacion={operacion} alCerrar={()=>cambiarOperacion(null)} alGuardar={mensaje=>{cambiarOperacion(null);cambiarAviso(mensaje);consulta.actualizar();}}/>}
  </section>;
}
export { AvisosCoordinacion as CoordinatorNotificationsView };
