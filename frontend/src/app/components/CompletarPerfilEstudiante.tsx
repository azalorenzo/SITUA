import { useEffect,useState } from 'react';
import { solicitarApi,ErrorSolicitud } from '../servicios/autenticacion';
import type { ErroresFormulario } from '../tipos/autenticacion';
import { Dialog as Ventana,DialogContent as Contenido,DialogHeader as Encabezado,DialogTitle as Titulo } from './ui/dialog';
interface Perfil { completo:boolean; carreras:{id:number;nombre:string}[]; periodos:{id:number;nombre:string}[]; }
export function CompletarPerfilEstudiante({alSalir}:{alSalir:()=>void}) {
  const [datos,cambiarDatos]=useState<Perfil|null>(null);
  const [cargando,cambiarCarga]=useState(true);
  const [guardando,cambiarGuardado]=useState(false);
  const [error,cambiarError]=useState('');
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [intento,cambiarIntento]=useState(0);
  const [carrera,cambiarCarrera]=useState('');
  const [periodo,cambiarPeriodo]=useState('');
  const [semestre,cambiarSemestre]=useState('');
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');
    solicitarApi<Perfil>('/estudiante/perfil',{signal:cancelacion.signal})
      .then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar tus datos.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});
    return ()=>cancelacion.abort();
  },[intento]);
  async function guardar(evento:React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();if(guardando)return;cambiarGuardado(true);cambiarError('');cambiarErrores({});
    try{const respuesta=await solicitarApi<Perfil>('/estudiante/perfil',{method:'PUT',body:JSON.stringify({carreraId:Number(carrera),periodoId:Number(periodo),semestre:Number(semestre)})});cambiarDatos(respuesta);}
    catch(fallo){cambiarError(fallo instanceof Error?fallo.message:'No se pudieron guardar tus datos.');if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);}
    finally{cambiarGuardado(false);}
  }
  const faltanOpciones=datos&&!datos.completo&&(!datos.carreras.length||!datos.periodos.length);
  const estilo='w-full border border-[#DCE3EA] rounded-lg px-3 py-2 mt-1 bg-white';
  return <Ventana open={!datos?.completo}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828] [&>button.absolute]:hidden max-h-[90vh] overflow-y-auto" onEscapeKeyDown={evento=>evento.preventDefault()} onPointerDownOutside={evento=>evento.preventDefault()}>
    <Encabezado><Titulo>Completa tus datos</Titulo></Encabezado>
    {cargando?<p role="status">Cargando…</p>:<>
      {datos&&!datos.completo&&<form onSubmit={guardar} className="space-y-4">
        <div><label htmlFor="perfil-carrera">Carrera</label><select id="perfil-carrera" className={estilo} required disabled={guardando} value={carrera} onChange={evento=>cambiarCarrera(evento.target.value)}><option value="">Seleccionar carrera</option>{datos.carreras.map(opcion=><option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{errores.carreraId?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        <div><label htmlFor="perfil-periodo">Periodo</label><select id="perfil-periodo" className={estilo} required disabled={guardando} value={periodo} onChange={evento=>cambiarPeriodo(evento.target.value)}><option value="">Seleccionar periodo</option>{datos.periodos.map(opcion=><option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{errores.periodoId?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        <div><label htmlFor="perfil-semestre">Semestre</label><input id="perfil-semestre" className={estilo} required type="number" min={1} max={65535} step={1} value={semestre} disabled={guardando} onChange={evento=>cambiarSemestre(evento.target.value)}/>{errores.semestre?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        {faltanOpciones&&<p role="status" className="text-sm">No hay carreras o periodos disponibles. Contacta a coordinación.</p>}
        <button type="submit" disabled={guardando||Boolean(faltanOpciones)} className="w-full bg-[#1F6662] text-white rounded-lg px-4 py-2 disabled:opacity-50">{guardando?'Guardando…':'Guardar y continuar'}</button>
      </form>}
      {error&&<p role="alert" className="text-sm text-[#B83F4B]">{error}</p>}
      {(!datos||faltanOpciones)&&<button type="button" onClick={()=>cambiarIntento(anterior=>anterior+1)} className="text-[#1F6662] underline">Reintentar</button>}
    </>}
    <button type="button" disabled={guardando} className="text-sm text-[#53716E]" onClick={alSalir}>Cerrar sesión</button>
  </Contenido></Ventana>;
}
