import { useEffect,useState } from 'react';
import { consultarOpcionesSesion,consultarHorariosDisponibles,reservarSesion,diaSesion,horaSesion,lugarSesion } from '../servicios/sesiones';
import type { OpcionSesion,PaginaHorariosDisponibles,SesionAgenda } from '../tipos/sesiones';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { PaginasSesiones } from './PaginasSesiones';
import { Dialog as Ventana,DialogContent as Contenido,DialogHeader as Encabezado,DialogTitle as Titulo } from './ui/dialog';
export function SolicitarSesion({alCerrar,alSolicitar}:{alCerrar:()=>void;alSolicitar:(sesion:SesionAgenda)=>void}) {
  const [opciones,cambiarOpciones]=useState<OpcionSesion[]>([]),[periodo,cambiarPeriodo]=useState('');
  const [pagina,cambiarPagina]=useState(1),[datos,cambiarDatos]=useState<PaginaHorariosDisponibles|null>(null);
  const [seleccion,cambiarSeleccion]=useState(''),[tema,cambiarTema]=useState(''),[mensaje,cambiarMensaje]=useState('');
  const [cargando,cambiarCarga]=useState(true),[cargandoOpciones,cambiarCargaOpciones]=useState(true),[guardando,cambiarGuardado]=useState(false);
  const [error,cambiarError]=useState(''),[errorOpciones,cambiarErrorOpciones]=useState(''),[errorGuardado,cambiarErrorGuardado]=useState('');
  const [intento,cambiarIntento]=useState(0),[intentoOpciones,cambiarIntentoOpciones]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargaOpciones(true);cambiarErrorOpciones('');
    consultarOpcionesSesion(cancelacion.signal).then(respuesta=>{if(!cancelacion.signal.aborted){cambiarOpciones(respuesta);cambiarPeriodo(respuesta[0]?String(respuesta[0].periodoId):'');}})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarErrorOpciones(fallo instanceof Error?fallo.message:'No se pudieron consultar tus tutores.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargaOpciones(false);});
    return ()=>cancelacion.abort();
  },[intentoOpciones]);
  useEffect(()=>{
    if(!periodo){cambiarCarga(false);cambiarDatos(null);return;}
    const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');cambiarSeleccion('');
    consultarHorariosDisponibles(Number(periodo),pagina,cancelacion.signal).then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar los horarios.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});
    return ()=>cancelacion.abort();
  },[periodo,pagina,intento]);
  async function guardar(evento:React.FormEvent<HTMLFormElement>){
    evento.preventDefault();if(guardando||!seleccion||!periodo||!tema.trim())return;cambiarGuardado(true);cambiarErrorGuardado('');
    try{const sesion=await reservarSesion({periodoId:Number(periodo),disponibilidadId:Number(seleccion),tema:tema.trim(),mensaje:mensaje.trim()});alSolicitar(sesion);}
    catch(fallo){cambiarErrorGuardado(fallo instanceof Error?fallo.message:'No se pudo solicitar la tutoría.');cambiarIntento(anterior=>anterior+1);}
    finally{cambiarGuardado(false);}
  }
  const estilo='w-full border border-[#DCE3EA] rounded-lg px-3 py-2';
  return <Ventana open onOpenChange={abierto=>{if(!abierto&&!guardando)alCerrar();}}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828] max-h-[90vh] overflow-y-auto" onEscapeKeyDown={evento=>{if(guardando)evento.preventDefault();}} onPointerDownOutside={evento=>{if(guardando)evento.preventDefault();}}>
    <Encabezado><Titulo>Solicitar tutoría</Titulo></Encabezado>
    <EstadoConsulta cargando={cargandoOpciones} error={errorOpciones} alReintentar={()=>cambiarIntentoOpciones(anterior=>anterior+1)}/>
    {!cargandoOpciones&&!errorOpciones&&(opciones.length?<form className="space-y-4" onSubmit={guardar}>
      <div><label htmlFor="periodo-solicitud" className="block text-sm mb-1">Periodo y tutor</label><select id="periodo-solicitud" value={periodo} className={estilo} disabled={guardando} onChange={evento=>{cambiarPeriodo(evento.target.value);cambiarPagina(1);}}>{opciones.map(opcion=><option key={opcion.periodoId} value={opcion.periodoId}>{opcion.periodo} · {opcion.tutor}</option>)}</select></div>
      <div><label htmlFor="tema-solicitud" className="block text-sm mb-1">Tema</label><input id="tema-solicitud" required maxLength={200} className={estilo} value={tema} disabled={guardando} onChange={evento=>cambiarTema(evento.target.value)}/></div>
      <div><p className="text-sm mb-2">Horario</p><EstadoConsulta cargando={cargando} error={error} alReintentar={()=>cambiarIntento(anterior=>anterior+1)}/>
        {!cargando&&!error&&datos&&(datos.contenido.length?<><div role="radiogroup" aria-label="Horarios disponibles" className="space-y-2">{datos.contenido.map(horario=><label key={horario.id} className="border border-[#DCE3EA] rounded-lg p-3 flex gap-3 items-center cursor-pointer"><input type="radio" required name="horario-solicitado" value={horario.id} checked={seleccion===String(horario.id)} disabled={guardando} onChange={evento=>cambiarSeleccion(evento.target.value)}/><span className="text-sm"><span className="block font-medium">{diaSesion(horario.inicioEn)} · {horaSesion(horario.inicioEn)} – {horaSesion(horario.finEn)}</span><span className="block text-xs text-[#53716E] break-all">{lugarSesion(horario)}</span></span></label>)}</div><div className="mt-3"><PaginasSesiones pagina={datos.pagina} totalPaginas={datos.totalPaginas} alCambiar={cambiarPagina} desactivado={guardando}/></div></>:<p className="text-sm text-[#53716E]">No hay horarios disponibles.</p>)}
      </div>
      <div><label htmlFor="mensaje-solicitud" className="block text-sm mb-1">Mensaje (opcional)</label><textarea id="mensaje-solicitud" maxLength={1000} rows={2} className={estilo} value={mensaje} disabled={guardando} onChange={evento=>cambiarMensaje(evento.target.value)}/></div>
      {errorGuardado&&<p role="alert" className="text-sm text-[#B83F4B]">{errorGuardado}</p>}
      <div className="flex justify-end gap-3"><button type="button" disabled={guardando} className="px-4 py-2 border rounded-lg" onClick={alCerrar}>Cancelar</button><button type="submit" disabled={guardando||cargando||Boolean(error)||!seleccion} className="px-4 py-2 bg-[#1F6662] text-white rounded-lg disabled:opacity-50">{guardando?'Enviando…':'Solicitar'}</button></div>
    </form>:<p className="text-sm text-[#53716E]">No tienes un tutor asignado.</p>)}
  </Contenido></Ventana>;
}
