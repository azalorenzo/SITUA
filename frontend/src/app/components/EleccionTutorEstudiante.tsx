import { useEffect,useState } from 'react';
import { consultarEleccionTutor,elegirTutor } from '../servicios/eleccionTutor';
import type { EleccionTutor,TutorDisponible } from '../tipos/eleccionTutor';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { Dialog as Ventana,DialogContent as Contenido,DialogHeader as Encabezado,DialogTitle as Titulo } from './ui/dialog';

export function EleccionTutorEstudiante() {
  const [datos,cambiarDatos]=useState<EleccionTutor|null>(null);
  const [periodo,cambiarPeriodo]=useState('');
  const [pagina,cambiarPagina]=useState(1);
  const [busqueda,cambiarBusqueda]=useState('');
  const [texto,cambiarTexto]=useState('');
  const [abierto,cambiarAbierto]=useState(false);
  const [cargando,cambiarCarga]=useState(true);
  const [error,cambiarError]=useState('');
  const [errorGuardado,cambiarErrorGuardado]=useState('');
  const [guardando,cambiarGuardado]=useState(false);
  const [seleccion,cambiarSeleccion]=useState<TutorDisponible|null>(null);
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{if(texto===busqueda)return;const espera=window.setTimeout(()=>{cambiarBusqueda(texto);cambiarPagina(1);cambiarSeleccion(null);},300);return ()=>window.clearTimeout(espera);},[texto,busqueda]);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');
    consultarEleccionTutor(periodo,pagina,busqueda,cancelacion.signal)
      .then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar los tutores.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});
    return ()=>cancelacion.abort();
  },[periodo,pagina,busqueda,intento]);
  function abrir(){cambiarSeleccion(null);cambiarErrorGuardado('');cambiarAbierto(true);}
  async function inscribirse(){
    if(guardando||!seleccion||!datos?.periodoId)return;
    cambiarGuardado(true);cambiarErrorGuardado('');
    try{const respuesta=await elegirTutor(seleccion.id,datos.periodoId);cambiarDatos(respuesta);cambiarSeleccion(null);cambiarAbierto(false);}
    catch(fallo){cambiarErrorGuardado(fallo instanceof Error?fallo.message:'No se pudo completar la inscripción.');}
    finally{cambiarGuardado(false);}
  }
  const boton='px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50';
  return <section className="bg-white rounded-lg p-6 shadow-sm" aria-labelledby="titulo-mi-tutor">
    <div className="flex flex-wrap items-center justify-between gap-3"><h3 id="titulo-mi-tutor" className="font-semibold">Mi tutor</h3><button type="button" className={boton} disabled={cargando||guardando} onClick={()=>cambiarIntento(anterior=>anterior+1)}>Actualizar</button></div>
    <EstadoConsulta cargando={cargando} error={error} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
    {!cargando&&!error&&datos&&<div className="mt-3 space-y-3">
      {datos.periodos.length>1?<select aria-label="Periodo de inscripción" value={periodo||String(datos.periodoId||'')} disabled={guardando} className={boton} onChange={evento=>{cambiarPeriodo(evento.target.value);cambiarPagina(1);cambiarSeleccion(null);}}>{datos.periodos.map(opcion=><option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>:datos.periodos.map(opcion=><p key={opcion.id} className="text-sm text-[#53716E]">{opcion.nombre}</p>)}
      {datos.asignado?<><p className="font-medium">{datos.asignado.nombre}</p><p className="text-sm text-[#53716E]">{datos.asignado.departamento}</p></>:datos.periodoId?<><p className="text-sm text-[#53716E]">Sin tutor asignado</p><button type="button" className="px-4 py-2 bg-[#1F6662] text-white rounded-lg" onClick={abrir}>Elegir tutor</button></>:<p className="text-sm text-[#53716E]">No tienes una inscripción activa.</p>}
    </div>}
    <Ventana open={abierto} onOpenChange={valor=>{if(!guardando)cambiarAbierto(valor);}}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828] max-h-[90vh] overflow-y-auto" onEscapeKeyDown={evento=>{if(guardando)evento.preventDefault();}} onPointerDownOutside={evento=>{if(guardando)evento.preventDefault();}}>
      <Encabezado><Titulo>Elegir tutor</Titulo></Encabezado>
      <input type="search" aria-label="Buscar tutor" placeholder="Buscar tutor" maxLength={160} value={texto} disabled={guardando} onChange={evento=>cambiarTexto(evento.target.value)} className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2"/>
      <EstadoConsulta cargando={cargando} error={error} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
      {!cargando&&!error&&datos&&<>
        {datos.asignado?<p>Ya tienes un tutor asignado.</p>:<>
          {datos.contenido.length?<div role="radiogroup" aria-label="Tutores disponibles" className="space-y-2">{datos.contenido.map(tutor=><label key={tutor.id} className="flex items-center gap-3 border border-[#DCE3EA] rounded-lg p-3 cursor-pointer"><input type="radio" name="tutor-elegido" value={tutor.id} checked={seleccion?.id===tutor.id} disabled={guardando} onChange={()=>cambiarSeleccion(tutor)}/><span><span className="block font-medium">{tutor.nombre}</span><span className="block text-sm text-[#53716E]">{tutor.departamento}</span></span></label>)}</div>:<p className="text-sm text-[#53716E]">No se encontraron tutores.</p>}
          {datos.totalPaginas>0&&<nav aria-label="Páginas de tutores" className="flex flex-wrap gap-2"><button type="button" className={boton} disabled={guardando||datos.pagina<=1} onClick={()=>{cambiarPagina(datos.pagina-1);cambiarSeleccion(null);}}>Anterior</button>{Array.from({length:datos.totalPaginas},(_,indice)=>indice+1).map(numero=><button key={numero} type="button" className={`${boton} ${datos.pagina===numero?'bg-[#1F6662] text-white':''}`} aria-label={`Página ${numero}`} aria-current={datos.pagina===numero?'page':undefined} disabled={guardando} onClick={()=>{cambiarPagina(numero);cambiarSeleccion(null);}}>{numero}</button>)}<button type="button" className={boton} disabled={guardando||datos.pagina>=datos.totalPaginas} onClick={()=>{cambiarPagina(datos.pagina+1);cambiarSeleccion(null);}}>Siguiente</button></nav>}
        </>}
      </>}
      {errorGuardado&&<p role="alert" className="text-sm text-[#B83F4B]">{errorGuardado}</p>}
      <div className="flex justify-end gap-3"><button type="button" className={boton} disabled={guardando} onClick={()=>cambiarAbierto(false)}>Cancelar</button><button type="button" className="px-4 py-2 bg-[#1F6662] text-white rounded-lg disabled:opacity-50" disabled={guardando||cargando||Boolean(error)||!seleccion||Boolean(datos?.asignado)} onClick={inscribirse}>{guardando?'Guardando…':'Inscribirme'}</button></div>
    </Contenido></Ventana>
  </section>;
}
