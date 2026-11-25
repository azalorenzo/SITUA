import { useEffect,useState } from 'react';
import { Calendar } from 'lucide-react';
import { EleccionTutorEstudiante } from './EleccionTutorEstudiante';
import { EstadoConsulta } from './ConsultaCoordinacion';
import { AccionesSesion } from './AccionesSesion';
import { PaginasSesiones } from './PaginasSesiones';
import { consultarSesiones,consultarResumenSesiones,diaSesion,horaSesion,nombresEstado } from '../servicios/sesiones';
import type { PaginaSesiones,ResumenSesiones } from '../tipos/sesiones';
export function StudentTutoriasView() {
  const [datos,cambiarDatos]=useState<PaginaSesiones|null>(null),[resumen,cambiarResumen]=useState<ResumenSesiones|null>(null);
  const [pagina,cambiarPagina]=useState(1),[estado,cambiarEstado]=useState(''),[cargando,cambiarCarga]=useState(true),[error,cambiarError]=useState(''),[intento,cambiarIntento]=useState(0);
  function actualizar(){cambiarIntento(anterior=>anterior+1);}
  useEffect(()=>{const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');Promise.all([consultarSesiones(false,pagina,estado,cancelacion.signal),consultarResumenSesiones(cancelacion.signal)])
    .then(([pagina,resumen])=>{if(!cancelacion.signal.aborted){cambiarDatos(pagina);cambiarResumen(resumen);}})
    .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar tus tutorías.');})
    .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});return ()=>cancelacion.abort();},[pagina,estado,intento]);
  return <div className="space-y-6"><div className="flex flex-wrap justify-between items-center gap-3"><div><h2 className="font-semibold mb-1">Mis tutorías</h2><p className="text-sm text-[#53716E]">Seguimiento detallado de tus sesiones y actividades</p></div><button type="button" disabled={cargando} className="px-4 py-2 border rounded-lg" onClick={actualizar}>Actualizar tutorías</button></div>
    <EleccionTutorEstudiante/>
    <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar}/>
    {!cargando&&!error&&datos&&resumen&&<div className="grid grid-cols-1 lg:grid-cols-3 gap-6"><div className="lg:col-span-2 space-y-6">
      <div className="bg-white rounded-lg shadow-sm p-6"><h3 className="font-semibold mb-4">Sesiones programadas</h3><div className="space-y-3">{resumen.proximas.length?resumen.proximas.map(sesion=><div key={sesion.id} className="border rounded-lg p-4"><div className="flex flex-wrap justify-between gap-2"><div><h4 className="font-medium text-sm">{sesion.tema}</h4><p className="text-xs text-[#53716E]">{sesion.tutor} · {diaSesion(sesion.inicioEn)}, {horaSesion(sesion.inicioEn)}</p></div><span className="text-xs bg-[#E4F3ED] text-[#227F78] px-3 py-1 rounded-full self-start">{nombresEstado[sesion.estado]}</span></div><AccionesSesion sesion={sesion} alCambiar={actualizar}/></div>):<p className="text-sm text-[#53716E]">No hay sesiones programadas.</p>}</div></div>
      <div className="bg-white rounded-lg shadow-sm"><div className="p-6 border-b flex flex-wrap justify-between items-center gap-3"><h3 className="font-semibold">Estado de mis tutorías</h3><select aria-label="Estado de mis tutorías" value={estado} className="px-3 py-2 border rounded-lg" onChange={evento=>{cambiarEstado(evento.target.value);cambiarPagina(1);}}><option value="">Todas</option>{Object.entries(nombresEstado).map(([codigo,nombre])=><option key={codigo} value={codigo}>{nombre}</option>)}</select></div>
        {datos.contenido.length?<div className="overflow-x-auto"><table className="w-full text-sm" aria-label="Estado de mis tutorías"><thead className="bg-[#F5F7FA]"><tr>{['Tutor','Tema','Estado'].map(titulo=><th key={titulo} className="text-left py-3 px-6">{titulo}</th>)}</tr></thead><tbody>{datos.contenido.map(sesion=><tr key={sesion.id} className="border-b"><td className="py-3 px-6">{sesion.tutor}</td><td className="py-3 px-6">{sesion.tema}<p className="text-xs text-[#53716E]">{diaSesion(sesion.inicioEn)} · {horaSesion(sesion.inicioEn)}{sesion.estado==='completada'?` · ${sesion.duracionMinutos} min`:''}</p>{sesion.observaciones&&<p className="text-xs text-[#53716E] whitespace-pre-wrap break-words mt-1">{sesion.observaciones}</p>}</td><td className="py-3 px-6 text-[#227F78]">{nombresEstado[sesion.estado]}</td></tr>)}</tbody></table></div>:<p className="p-6 text-sm text-[#53716E]">No hay tutorías.</p>}<div className="p-6"><PaginasSesiones pagina={datos.pagina} totalPaginas={datos.totalPaginas} alCambiar={cambiarPagina}/></div>
      </div></div>
      <div className="space-y-6"><div className="bg-white rounded-lg shadow-sm p-6"><h3 className="font-semibold mb-4">Actividades recientes</h3><div className="space-y-4">{resumen.actividades.length?resumen.actividades.map(actividad=><div key={actividad.id} className="flex gap-3"><div className="bg-[#F5F7FA] p-2 rounded-lg h-fit"><Calendar className="w-4 h-4 text-[#1F6662]"/></div><div><p className="text-sm font-medium">{nombresEstado[actividad.estado]}</p><p className="text-xs text-[#53716E]">{actividad.tema}</p><p className="text-xs text-[#53716E]">{diaSesion(actividad.ocurridoEn)} · {horaSesion(actividad.ocurridoEn)}</p></div></div>):<p className="text-sm text-[#53716E]">Sin actividades recientes.</p>}</div></div></div>
    </div>}
  </div>;
}
