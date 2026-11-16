import { useEffect,useState } from 'react';
import { consultarDestinatarios } from '../servicios/avisos';
import type { DestinatarioAviso,PaginaDestinatarios } from '../tipos/avisos';
import { EstadoConsulta } from './ConsultaCoordinacion';

export function SelectorDestinatariosAviso({seleccionados,alCambiar,desactivado}:{
  seleccionados:DestinatarioAviso[];alCambiar:(usuarios:DestinatarioAviso[])=>void;desactivado:boolean;
}){
  const [busqueda,cambiarBusqueda]=useState('');
  const [termino,cambiarTermino]=useState('');
  const [rol,cambiarRol]=useState('');
  const [pagina,cambiarPagina]=useState(1);
  const [datos,cambiarDatos]=useState<PaginaDestinatarios|null>(null);
  const [cargando,cambiarCargando]=useState(true);
  const [error,cambiarError]=useState('');
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{if(busqueda===termino)return;const espera=window.setTimeout(()=>{cambiarTermino(busqueda);cambiarPagina(1);},300);return ()=>window.clearTimeout(espera);},[busqueda,termino]);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');
    consultarDestinatarios(cancelacion.signal,pagina,termino,rol).then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron cargar los destinatarios.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});return ()=>cancelacion.abort();
  },[pagina,termino,rol,intento]);
  return <fieldset className="selector-destinatarios-aviso" disabled={desactivado}><legend>Usuarios específicos</legend>
    <div className="filtros-destinatarios-aviso"><input type="search" aria-label="Buscar destinatario" placeholder="Buscar nombre o correo" maxLength={160} value={busqueda} onChange={evento=>cambiarBusqueda(evento.target.value)}/><select aria-label="Tipo de destinatario" value={rol} onChange={evento=>{cambiarRol(evento.target.value);cambiarPagina(1);}}><option value="">Estudiantes y tutores</option><option value="estudiante">Estudiantes</option><option value="tutor">Tutores</option></select></div>
    <p className="nota-formulario-aviso">Seleccionados: {seleccionados.length} de 500</p>
    {seleccionados.length>0&&<ul className="seleccionados-aviso">{seleccionados.map(usuario=><li key={usuario.usuarioId}><span>{usuario.nombre}</span><button type="button" aria-label={`Quitar ${usuario.nombre}`} onClick={()=>alCambiar(seleccionados.filter(actual=>actual.usuarioId!==usuario.usuarioId))}>×</button></li>)}</ul>}
    <EstadoConsulta cargando={cargando} error={error} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
    {!cargando&&!error&&<>
      {datos?.contenido.length?<ul className="opciones-destinatarios-aviso">{datos.contenido.map(usuario=>{
        const elegido=seleccionados.some(actual=>actual.usuarioId===usuario.usuarioId);
        return <li key={usuario.usuarioId}><label><input type="checkbox" checked={elegido} disabled={desactivado||(!elegido&&seleccionados.length>=500)} onChange={()=>alCambiar(elegido?seleccionados.filter(actual=>actual.usuarioId!==usuario.usuarioId):[...seleccionados,usuario])}/><span>{usuario.nombre}<small>{usuario.correo}</small></span></label></li>;
      })}</ul>:<p className="nota-formulario-aviso">No se encontraron usuarios activos.</p>}
      <nav aria-label="Paginación de destinatarios" className="paginacion-destinatarios-aviso"><button type="button" disabled={!datos||datos.pagina<=1} onClick={()=>cambiarPagina((datos?.pagina||1)-1)}>Anterior</button><span>{datos?.totalRegistros||0} usuarios · página {datos?.pagina||1} de {datos?.totalPaginas||0}</span><button type="button" disabled={!datos?.totalPaginas||datos.pagina>=datos.totalPaginas} onClick={()=>cambiarPagina((datos?.pagina||1)+1)}>Siguiente</button></nav>
    </>}
  </fieldset>;
}
