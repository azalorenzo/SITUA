import { useEffect,useRef,useState } from 'react';
import { X } from 'lucide-react';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { consultarDetalleAviso,crearAviso,editarAviso,retirarAviso } from '../servicios/avisos';
import type { DatosAviso,DetalleAviso,DestinatarioAviso } from '../tipos/avisos';
import type { ErroresFormulario } from '../tipos/autenticacion';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { SelectorDestinatariosAviso } from './SelectorDestinatariosAviso';

export type OperacionAviso={tipo:'crear'}|{tipo:'editar'|'detalle'|'retirar';id:number};
export function fechaAviso(fecha:string|null){return fecha?new Intl.DateTimeFormat('es-MX',{dateStyle:'medium',timeStyle:'short'}).format(new Date(fecha+'Z')):'Sin registro';}
const categorias=['general','academico','recordatorio','sesiones'];
export function FormularioAviso({operacion,alCerrar,alGuardar}:{operacion:OperacionAviso;alCerrar:()=>void;alGuardar:(mensaje:string)=>void}){
  const dialogo=useRef<HTMLDialogElement>(null);
  const [datos,cambiarDatos]=useState<DatosAviso>({titulo:'',descripcion:'',categoria:'general',prioridad:'normal',destinatarios:operacion.tipo==='crear'?'':'conservar'});
  const [seleccionados,cambiarSeleccionados]=useState<DestinatarioAviso[]>([]);
  const [detalle,cambiarDetalle]=useState<DetalleAviso|null>(null);
  const [cargando,cambiarCargando]=useState(operacion.tipo!=='crear');
  const [errorCarga,cambiarErrorCarga]=useState('');
  const [intento,cambiarIntento]=useState(0);
  const [guardando,cambiarGuardando]=useState(false);
  const [error,cambiarError]=useState('');
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const titulo={crear:'Crear aviso',editar:'Editar aviso',detalle:'Detalle del aviso',retirar:'Retirar aviso'}[operacion.tipo];
  const id=operacion.tipo==='crear'?null:operacion.id;
  useEffect(()=>{dialogo.current?.showModal();return ()=>dialogo.current?.close();},[]);
  useEffect(()=>{
    if(id===null)return;
    const cancelacion=new AbortController();cambiarCargando(true);cambiarErrorCarga('');
    consultarDetalleAviso(id,cancelacion.signal).then(respuesta=>{
      if(cancelacion.signal.aborted)return;cambiarDetalle(respuesta);
      cambiarDatos({titulo:respuesta.aviso.titulo,descripcion:respuesta.aviso.descripcion,categoria:respuesta.aviso.categoria,prioridad:respuesta.aviso.prioridad,destinatarios:'conservar'});
    }).catch(fallo=>{if(!cancelacion.signal.aborted)cambiarErrorCarga(fallo instanceof Error?fallo.message:'No se pudo consultar el aviso.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});return ()=>cancelacion.abort();
  },[id,intento]);
  function validar(actuales:DatosAviso){
    const revisados:ErroresFormulario={};
    if(!actuales.titulo.trim()||actuales.titulo.length>200)revisados.titulo=['Escribe un título de hasta 200 caracteres.'];
    if(!actuales.descripcion.trim()||actuales.descripcion.length>10000)revisados.descripcion=['Escribe una descripción de hasta 10000 caracteres.'];
    if(!actuales.categoria.trim()||actuales.categoria.length>40)revisados.categoria=['Escribe una categoría de hasta 40 caracteres.'];
    if(!actuales.destinatarios)revisados.destinatarios=['Selecciona los destinatarios.'];
    if(actuales.destinatarios==='seleccion'&&!seleccionados.length)revisados.usuarioIds=['Selecciona al menos un usuario.'];
    return revisados;
  }
  function cambiarCampo(campo:keyof DatosAviso,valor:string){
    const nuevos={...datos,[campo]:valor};cambiarDatos(nuevos);cambiarError('');
    const revisados=validar(nuevos);cambiarErrores(anteriores=>{const siguientes={...anteriores};if(revisados[campo])siguientes[campo]=revisados[campo];else delete siguientes[campo];if(campo==='destinatarios')delete siguientes.usuarioIds;return siguientes;});
  }
  async function enviar(evento:React.FormEvent){
    evento.preventDefault();if(guardando)return;cambiarError('');
    if(operacion.tipo==='crear'||operacion.tipo==='editar'){
      const revisados=validar(datos);cambiarErrores(revisados);if(Object.keys(revisados).length){document.getElementById(`aviso-${Object.keys(revisados)[0]}`)?.focus();return;}
    }
    cambiarGuardando(true);
    try{
      const solicitud={...datos,titulo:datos.titulo.trim(),descripcion:datos.descripcion.trim(),categoria:datos.categoria.trim(),usuarioIds:datos.destinatarios==='seleccion'?seleccionados.map(usuario=>usuario.usuarioId):undefined};
      if(operacion.tipo==='crear')await crearAviso(solicitud);
      else if(operacion.tipo==='editar')await editarAviso(operacion.id,solicitud);
      else if(operacion.tipo==='retirar')await retirarAviso(operacion.id);
      alGuardar(operacion.tipo==='crear'?'Aviso guardado.':operacion.tipo==='editar'?'Aviso actualizado.':'Aviso retirado.');
    }catch(fallo){cambiarError(fallo instanceof Error?fallo.message:'No se pudo guardar el aviso.');if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);}
    finally{cambiarGuardando(false);}
  }
  function mensajes(campo:string){return errores[campo]&&<ul id={`error-aviso-${campo}`} className="errores-formulario-aviso">{errores[campo].map(texto=><li key={texto}>{texto}</li>)}</ul>;}
  function accesibilidad(campo:string){return {'aria-invalid':Boolean(errores[campo]),'aria-describedby':errores[campo]?`error-aviso-${campo}`:undefined};}
  const sinEdicion=operacion.tipo==='detalle';
  return <dialog ref={dialogo} className="dialogo-aviso" aria-labelledby="titulo-dialogo-aviso" onCancel={evento=>{evento.preventDefault();if(!guardando)alCerrar();}}>
    <header><h3 id="titulo-dialogo-aviso">{titulo}</h3><button type="button" aria-label="Cerrar aviso" disabled={guardando} onClick={alCerrar}><X aria-hidden="true"/></button></header>
    <EstadoConsulta cargando={cargando} error={errorCarga} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
    {!cargando&&!errorCarga&&<>
      {sinEdicion&&detalle?<div className="detalle-aviso"><h4>{detalle.aviso.titulo}</h4><p className="descripcion-completa-aviso">{detalle.aviso.descripcion}</p><dl><div><dt>Categoría</dt><dd>{detalle.aviso.categoria}</dd></div><div><dt>Prioridad</dt><dd>{detalle.aviso.prioridad}</dd></div><div><dt>Autor</dt><dd>{detalle.aviso.autor}</dd></div><div><dt>Publicación</dt><dd>{fechaAviso(detalle.aviso.publicadoEn)}</dd></div><div><dt>Estado</dt><dd>{detalle.aviso.retiradoEn?'Retirado':'Vigente'}</dd></div><div><dt>Retiro</dt><dd>{fechaAviso(detalle.aviso.retiradoEn)}</dd></div></dl>
        <h4>Destinatarios ({detalle.aviso.destinatarios}) · Lecturas: {detalle.aviso.lecturas}</h4><ul className="lista-detalle-destinatarios">{detalle.destinatarios.map(usuario=><li key={usuario.usuarioId}><strong>{usuario.nombre}</strong><span>{usuario.correo}</span><small>{usuario.leidoEn?`Leído: ${fechaAviso(usuario.leidoEn)}`:'Sin lectura registrada'}{usuario.ocultadoEn?' · Ocultado por el usuario':''}</small></li>)}</ul>
        <footer><button type="button" className="boton-secundario-aviso" onClick={alCerrar}>Cerrar</button></footer></div>:<form noValidate onSubmit={enviar} aria-busy={guardando} className="formulario-aviso">
        {operacion.tipo==='retirar'?<div className="confirmacion-retiro-aviso"><p>¿Deseas retirar este aviso?</p><strong>{detalle?.aviso.titulo}</strong><p>Dejará de aparecer entre los avisos vigentes. El registro y sus lecturas se conservarán.</p></div>:<>
          <div className="campo-formulario-aviso"><label htmlFor="aviso-titulo">Título</label><input id="aviso-titulo" value={datos.titulo} maxLength={200} disabled={guardando} placeholder="Escribe el título" {...accesibilidad('titulo')} onChange={evento=>cambiarCampo('titulo',evento.target.value)}/>{mensajes('titulo')}</div>
          <div className="campo-formulario-aviso"><label htmlFor="aviso-descripcion">Descripción</label><textarea id="aviso-descripcion" value={datos.descripcion} maxLength={10000} rows={4} disabled={guardando} placeholder="Escribe el contenido del aviso" {...accesibilidad('descripcion')} onChange={evento=>cambiarCampo('descripcion',evento.target.value)}/>{mensajes('descripcion')}</div>
          <div className="fila-formulario-aviso"><div className="campo-formulario-aviso"><label htmlFor="aviso-categoria">Categoría</label><input id="aviso-categoria" list="categorias-aviso" maxLength={40} value={datos.categoria} disabled={guardando} {...accesibilidad('categoria')} onChange={evento=>cambiarCampo('categoria',evento.target.value)}/><datalist id="categorias-aviso">{categorias.map(categoria=><option key={categoria} value={categoria}/>)}</datalist>{mensajes('categoria')}</div><div className="campo-formulario-aviso"><label htmlFor="aviso-prioridad">Prioridad</label><select id="aviso-prioridad" value={datos.prioridad} disabled={guardando} onChange={evento=>cambiarCampo('prioridad',evento.target.value)}><option value="normal">Normal</option><option value="media">Media</option><option value="alta">Alta</option></select>{mensajes('prioridad')}</div></div>
          <div className="campo-formulario-aviso"><label htmlFor="aviso-destinatarios">Destinatarios</label><select id="aviso-destinatarios" value={datos.destinatarios} disabled={guardando} {...accesibilidad('destinatarios')} onChange={evento=>cambiarCampo('destinatarios',evento.target.value)}><option value="">Seleccionar destinatarios</option>{operacion.tipo==='editar'&&<option value="conservar">Conservar los {detalle?.aviso.destinatarios||0} destinatarios actuales</option>}<option value="todos">Todos los estudiantes y tutores activos</option><option value="estudiantes">Estudiantes activos</option><option value="tutores">Tutores activos</option><option value="seleccion">Seleccionar usuarios específicos</option></select>{mensajes('destinatarios')}</div>
          {datos.destinatarios==='seleccion'&&<SelectorDestinatariosAviso seleccionados={seleccionados} desactivado={guardando} alCambiar={usuarios=>{cambiarSeleccionados(usuarios);cambiarErrores(anteriores=>{const siguientes={...anteriores};delete siguientes.usuarioIds;return siguientes;});}}/>}{mensajes('usuarioIds')}
          <p className="nota-formulario-aviso">Los grupos incluyen a los usuarios activos al guardar. Los usuarios nuevos no se agregan automáticamente. Al editar puedes conservar los destinatarios y sus lecturas.</p>
        </>}
        {error&&<p role="alert" className="error-general-aviso">{error}</p>}
        <footer><button type="button" className="boton-secundario-aviso" disabled={guardando} onClick={alCerrar}>Cancelar</button><button type="submit" className={operacion.tipo==='retirar'?'boton-retirar-aviso':'boton-principal-aviso'} disabled={guardando}>{guardando?'Guardando…':operacion.tipo==='retirar'?'Confirmar retiro':'Guardar aviso'}</button></footer>
      </form>}
    </>}
  </dialog>;
}
