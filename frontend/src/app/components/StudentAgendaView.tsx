import { Calendar,ChevronLeft,ChevronRight,Plus } from 'lucide-react';
import { useEffect,useState } from 'react';
import { consultarAgenda,consultarResumenSesiones,fechaSesion,diaSesion,horaSesion,lugarSesion,nombresEstado } from '../servicios/sesiones';
import type { SesionAgenda,ResumenSesiones } from '../tipos/sesiones';
import { SolicitarSesion } from './SolicitarSesion';
import { AccionesSesion } from './AccionesSesion';
import { EstadoConsulta } from './ConsultaCoordinacion';

export function StudentAgendaView() {
  const [mes,cambiarMes]=useState(()=>new Date(new Date().getFullYear(),new Date().getMonth(),1));
  const [sesiones,cambiarSesiones]=useState<SesionAgenda[]>([]),[resumen,cambiarResumen]=useState<ResumenSesiones|null>(null);
  const [abierto,cambiarAbierto]=useState(false),[cargando,cambiarCarga]=useState(true),[error,cambiarError]=useState(''),[aviso,cambiarAviso]=useState('');
  const [intento,cambiarIntento]=useState(0);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');
    Promise.all([consultarAgenda(mes.getFullYear(),mes.getMonth(),cancelacion.signal),consultarResumenSesiones(cancelacion.signal)])
      .then(([agenda,respuesta])=>{if(!cancelacion.signal.aborted){cambiarSesiones(agenda);cambiarResumen(respuesta);}})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudo consultar tu agenda.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});return ()=>cancelacion.abort();
  },[mes,intento]);
  const dias=new Date(mes.getFullYear(),mes.getMonth()+1,0).getDate();
  const celdas=[...Array(mes.getDay()).fill(null),...Array.from({length:dias},(_,indice)=>indice+1)];
  const porDia:Record<number,SesionAgenda[]>={};for(const sesion of sesiones){const dia=fechaSesion(sesion.inicioEn).getDate();(porDia[dia]??=[]).push(sesion);}
  function actualizar(){cambiarIntento(anterior=>anterior+1);}
  return <div className="space-y-6 relative">
    <div className="flex flex-wrap justify-between items-center gap-3"><div><h2 className="font-semibold mb-1">Mi agenda</h2><p className="text-sm text-[#53716E]">{new Intl.DateTimeFormat('es-MX',{month:'long',year:'numeric'}).format(mes)}</p></div><div className="flex gap-2"><button type="button" onClick={actualizar} disabled={cargando} className="px-4 py-2 border rounded-lg">Actualizar</button><button type="button" onClick={()=>{cambiarAbierto(true);cambiarAviso('');}} className="flex items-center gap-2 bg-[#1F6662] text-white px-4 py-2 rounded-lg"><Plus className="w-4 h-4"/>Solicitar tutoría</button></div></div>
    {aviso&&<p role="status" className="text-sm text-[#1F6662]">{aviso}</p>}
    <div className="flex justify-between items-center"><button type="button" aria-label="Mes anterior" className="p-2 rounded-lg hover:bg-white" onClick={()=>cambiarMes(anterior=>new Date(anterior.getFullYear(),anterior.getMonth()-1,1))}><ChevronLeft className="w-5 h-5"/></button><button type="button" aria-label="Mes siguiente" className="p-2 rounded-lg hover:bg-white" onClick={()=>cambiarMes(anterior=>new Date(anterior.getFullYear(),anterior.getMonth()+1,1))}><ChevronRight className="w-5 h-5"/></button></div>
    <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar}/>
    {!cargando&&!error&&<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div className="lg:col-span-2 bg-white rounded-lg shadow-sm p-6"><div className="grid grid-cols-7 gap-2">{['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'].map(dia=><div key={dia} className="text-center text-sm font-medium text-[#53716E] pb-2">{dia}</div>)}
        {celdas.map((dia,indice)=>dia===null?<div key={`vacio-${indice}`}/>:<div key={dia} className="aspect-square border rounded-lg p-1 text-xs relative overflow-y-auto"><div className={`w-6 h-6 flex items-center justify-center ${new Date().getFullYear()===mes.getFullYear()&&new Date().getMonth()===mes.getMonth()&&new Date().getDate()===dia?'bg-[#1F6662] text-white rounded-full':''}`}>{dia}</div>{(porDia[dia]||[]).map(sesion=><div key={sesion.id} title={`${sesion.tema} · ${nombresEstado[sesion.estado]}`} className={`p-0.5 mt-1 rounded truncate ${sesion.estado==='solicitada'||sesion.estado==='confirmada'?'bg-[#D3F4EE] text-[#1C4543]':'bg-[#F5F7FA] text-[#53716E]'}`}>{horaSesion(sesion.inicioEn)} {sesion.tema}</div>)}</div>)}
      </div></div>
      <div className="space-y-4"><h3 className="font-semibold">Próximas sesiones</h3>{!resumen?.proximas.length&&<p className="text-sm text-[#53716E]">No hay sesiones programadas.</p>}{resumen?.proximas.map(sesion=><div key={sesion.id} className="p-4 rounded-lg border bg-[#D3F4EE] text-[#1C4543]"><h4 className="font-medium text-sm">{sesion.tema}</h4><p className="text-xs mt-1">{diaSesion(sesion.inicioEn)} · {horaSesion(sesion.inicioEn)} – {horaSesion(sesion.finEn)}</p><p className="text-xs break-all">{sesion.tutor} · {lugarSesion(sesion)}</p><p className="text-xs mt-2">{nombresEstado[sesion.estado]}</p><AccionesSesion sesion={sesion} alCambiar={actualizar}/></div>)}</div>
    </div>}
    {abierto&&<SolicitarSesion alCerrar={()=>cambiarAbierto(false)} alSolicitar={sesion=>{cambiarAbierto(false);const fecha=fechaSesion(sesion.inicioEn);cambiarMes(new Date(fecha.getFullYear(),fecha.getMonth(),1));cambiarAviso('Solicitud enviada.');actualizar();}}/>}
  </div>;
}
