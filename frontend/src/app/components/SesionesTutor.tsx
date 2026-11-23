import { useEffect,useState } from 'react';
import { consultarSesiones,diaSesion,horaSesion,lugarSesion,nombresEstado } from '../servicios/sesiones';
import type { PaginaSesiones } from '../tipos/sesiones';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { AccionesSesion } from './AccionesSesion';
import { PaginasSesiones } from './PaginasSesiones';
export function SesionesTutor({alCambiar}:{alCambiar:()=>void}) {
  const [datos,cambiarDatos]=useState<PaginaSesiones|null>(null),[pagina,cambiarPagina]=useState(1),[estado,cambiarEstado]=useState('solicitada');
  const [cargando,cambiarCarga]=useState(true),[error,cambiarError]=useState(''),[intento,cambiarIntento]=useState(0);
  function actualizar(){cambiarIntento(anterior=>anterior+1);}
  useEffect(()=>{const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');consultarSesiones(true,pagina,estado,cancelacion.signal)
    .then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
    .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar las sesiones.');})
    .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});return ()=>cancelacion.abort();},[pagina,estado,intento]);
  return <section className="bg-white rounded-lg p-6 shadow-sm space-y-4" aria-labelledby="titulo-solicitudes-tutor"><div className="flex flex-wrap justify-between items-center gap-3"><h3 id="titulo-solicitudes-tutor" className="font-semibold">Sesiones de tutoría</h3><div className="flex gap-2"><select aria-label="Estado de las sesiones" className="px-3 py-2 border border-[#DCE3EA] rounded-lg" value={estado} onChange={evento=>{cambiarEstado(evento.target.value);cambiarPagina(1);}}><option value="">Todas</option>{Object.entries(nombresEstado).map(([codigo,nombre])=><option key={codigo} value={codigo}>{nombre}</option>)}</select><button type="button" disabled={cargando} className="px-3 py-2 border rounded-lg" onClick={()=>{actualizar();alCambiar();}}>Actualizar sesiones</button></div></div>
    <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar}/>
    {!cargando&&!error&&datos&&<>{!datos.contenido.length?<p className="text-sm text-[#53716E]">No hay sesiones.</p>:<div className="space-y-3">{datos.contenido.map(sesion=><article key={sesion.id} className="border border-[#DCE3EA] rounded-lg p-4"><div className="flex flex-wrap justify-between gap-2"><h4 className="text-sm font-medium">{sesion.tema}</h4><span className="text-xs text-[#1F6662]">{nombresEstado[sesion.estado]}</span></div><p className="text-sm mt-1">{sesion.estudiante}</p><p className="text-xs text-[#53716E]">{diaSesion(sesion.inicioEn)} · {horaSesion(sesion.inicioEn)} – {horaSesion(sesion.finEn)} · {sesion.periodo}</p><p className="text-xs text-[#53716E] break-all">{lugarSesion(sesion)}</p>{sesion.mensajeSolicitud&&<p className="text-sm mt-2 whitespace-pre-wrap break-words">{sesion.mensajeSolicitud}</p>}<AccionesSesion esTutor sesion={sesion} alCambiar={()=>{actualizar();alCambiar();}}/></article>)}</div>}<PaginasSesiones pagina={datos.pagina} totalPaginas={datos.totalPaginas} alCambiar={cambiarPagina}/></>}
  </section>;
}
