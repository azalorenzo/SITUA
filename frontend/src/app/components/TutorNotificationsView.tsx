import { PaginasSesiones } from './PaginasSesiones';
import { ConfirmarAccion } from './ConfirmarAccion';
import { cambiarEstadoSesion } from '../servicios/sesiones';
import type { PaginaAvisosTutor } from '../tipos/avisosRecibidos';
import { useEffect, useState } from 'react';
import { Bell, CheckCircle, AlertCircle, Calendar, Users, FileText, MessageSquare, Clock, Info } from 'lucide-react';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { consultarPaginaAvisosTutor, marcarAvisoLeido, marcarAvisosLeidos } from '../servicios/avisosRecibidos';
import type { AvisoRecibido } from '../tipos/avisosRecibidos';
import { EstadoConsulta } from './ConsultaCoordinacion';

interface Notification {
  id: number;
  idSesion?: string;
  puedeResponder:boolean;
  type: 'success' | 'warning' | 'info' | 'alert';
  title: string;
  message: string;
  date: string;
  time: string;
  isImportant: boolean;
  read: boolean;
}

// El backend guarda las fechas en UTC sin zona; aquí se muestran en la hora del navegador.
function aNotificacion(aviso: AvisoRecibido): Notification {
  const publicado = new Date(`${aviso.publicadoEn}Z`);
  return {
    id: aviso.id,
    idSesion: aviso.sesionId ? String(aviso.sesionId) : undefined,
    puedeResponder:aviso.puedeResponderSesion,
    type: aviso.prioridad === 'alta' ? 'alert' : aviso.prioridad === 'media' ? 'warning' : 'info',
    title: aviso.titulo,
    message: aviso.descripcion,
    date: publicado.toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' }),
    time: publicado.toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit', hour12: false }),
    isImportant: aviso.prioridad === 'alta',
    read: aviso.leidoEn !== null
  };
}

function textoDeError(fallo: unknown): string {
  return fallo instanceof ErrorSolicitud || fallo instanceof Error ? fallo.message : 'No se pudo completar la acción.';
}

export function TutorNotificationsView() {
  const [datos,cambiarDatos]=useState<PaginaAvisosTutor|null>(null),[pagina,cambiarPagina]=useState(1),[texto,cambiarTexto]=useState(''),[busqueda,cambiarBusqueda]=useState('');
  const [lectura,cambiarLectura]=useState('todos'),[prioridad,cambiarPrioridad]=useState('todos'),[cargando,cambiarCarga]=useState(true),[error,cambiarError]=useState(''),[intento,cambiarIntento]=useState(0);
  const [ocupado,cambiarOcupado]=useState(false),[accion,cambiarAccion]=useState<{aviso:Notification;tipo:'confirmar'|'rechazar'}|null>(null),[errorAccion,cambiarErrorAccion]=useState('');
  const actualizar=()=>cambiarIntento(valor=>valor+1);
  useEffect(()=>{const espera=setTimeout(()=>{cambiarBusqueda(texto.trim());cambiarPagina(1);},300);return()=>clearTimeout(espera);},[texto]);
  useEffect(()=>{const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');cambiarDatos(null);consultarPaginaAvisosTutor(cancelacion.signal,pagina,busqueda,lectura,prioridad)
    .then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);}).catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(textoDeError(fallo));}).finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});return()=>cancelacion.abort();
  },[pagina,busqueda,lectura,prioridad,intento]);
  const consulta={cargando,error,actualizar},notifications=(datos?.contenido??[]).map(aNotificacion);
  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success':
        return CheckCircle;
      case 'warning':
        return Clock;
      case 'alert':
        return AlertCircle;
      case 'info':
        return Info;
      default:
        return Bell;
    }
  };
  

  const getNotificationColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'bg-[#E4F3ED] border-[#227F78] text-[#227F78]';
      case 'warning':
        return 'bg-[#FFF3D6] border-[#986314] text-[#986314]';
      case 'alert':
        return 'bg-[#FCEBED] border-[#B83F4B] text-[#B83F4B]';
      case 'info':
        return 'bg-[#F2FBF9] border-[#1F6662] text-[#1C4543]';
      default:
        return 'bg-[#F5F7FA] border-[#DCE3EA] text-[#0B2828]';
    }
  };

  const getIconColor = (type: string) => {
    switch (type) {
      case 'success':
        return 'text-[#227F78]';
      case 'warning':
        return 'text-[#986314]';
      case 'alert':
        return 'text-[#B83F4B]';
      case 'info':
        return 'text-[#1F6662]';
      default:
        return 'text-[#53716E]';
    }
  };

  const unreadCount=datos?.noLeidos??0,importantCount=datos?.importantes??0;
  const marcarComoLeida=async(id:number)=>{if(ocupado)return;cambiarOcupado(true);cambiarErrorAccion('');try{await marcarAvisoLeido(id);actualizar();}catch(fallo){cambiarErrorAccion(textoDeError(fallo));}finally{cambiarOcupado(false);}};
  const marcarTodasLeidas=async()=>{if(ocupado)return;cambiarOcupado(true);cambiarErrorAccion('');try{await marcarAvisosLeidos();actualizar();}catch(fallo){cambiarErrorAccion(textoDeError(fallo));}finally{cambiarOcupado(false);}};
  const responderSesion=async()=>{if(!accion||!accion.aviso.idSesion||ocupado)return;cambiarOcupado(true);cambiarErrorAccion('');try{
    await cambiarEstadoSesion(true,Number(accion.aviso.idSesion),accion.tipo);await marcarAvisoLeido(accion.aviso.id);cambiarAccion(null);actualizar();
   }catch(fallo){cambiarErrorAccion(textoDeError(fallo));actualizar();}finally{cambiarOcupado(false);}};
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h2 className="font-semibold mb-1">Centro de notificaciones</h2>
          <p className="text-sm text-[#53716E]">Mantente al día con tus actividades</p>
        </div>
<button
  disabled={ocupado||cargando||!unreadCount}
  onClick={marcarTodasLeidas}
  className="px-4 py-2 text-sm text-[#1F6662] hover:bg-[#F2FBF9] rounded-lg"
>
  Marcar todas como leídas
</button>
      </div>

      <EstadoConsulta cargando={consulta.cargando} error={consulta.error} alReintentar={consulta.actualizar} />

      {errorAccion&&!accion&&<p role="alert" className="text-sm text-[#B83F4B]">{errorAccion}</p>}
      <div className="bg-white rounded-lg p-4 shadow-sm flex flex-col sm:flex-row gap-3"><input type="search" aria-label="Buscar aviso" maxLength={200} placeholder="Buscar aviso…" value={texto} onChange={evento=>cambiarTexto(evento.target.value)} className="min-w-0 flex-1 px-3 py-2 border rounded-lg"/><select aria-label="Lectura de avisos" value={lectura} onChange={evento=>{cambiarLectura(evento.target.value);cambiarPagina(1);}} className="border rounded-lg px-3 py-2"><option value="todos">Todos</option><option value="no_leidos">No leídos</option><option value="leidos">Leídos</option></select><select aria-label="Prioridad de avisos" value={prioridad} onChange={evento=>{cambiarPrioridad(evento.target.value);cambiarPagina(1);}} className="border rounded-lg px-3 py-2"><option value="todos">Todas las prioridades</option><option value="normal">Normal</option><option value="media">Media</option><option value="alta">Alta</option></select></div>
      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Bell className="w-5 h-5 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Total de avisos</p>
              <p className="font-semibold">{datos?.totalRegistros??0}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#E4F3ED] p-3 rounded-lg">
              <CheckCircle className="w-5 h-5 text-[#227F78]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">No leídos</p>
              <p className="font-semibold">{unreadCount}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FCEBED] p-3 rounded-lg">
              <AlertCircle className="w-5 h-5 text-[#B83F4B]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Importantes</p>
              <p className="font-semibold">{importantCount}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Notifications List */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#DCE3EA]">
          <h3 className="font-semibold">Avisos recientes</h3>
        </div>

        <div className="divide-y divide-[#DCE3EA]">
          {notifications.map((notification) => {
            const Icon = getNotificationIcon(notification.type);
            const colorClass = getNotificationColor(notification.type);
            const iconColorClass = getIconColor(notification.type);

            return (
              <div
                key={notification.id}
                className={`p-6 hover:bg-[#F5F7FA] transition-colors ${
                  !notification.read ? 'bg-[#F2FBF9]/30' : ''
                }`}
              >
                <div className="flex gap-4">
                  <div className={`${colorClass} p-3 rounded-lg h-fit border-l-4`}>
                    <Icon className={`w-5 h-5 ${iconColorClass}`} />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-start justify-between gap-2 mb-2">
                      <div className="flex items-center gap-2">
                        <h4 className="font-semibold text-sm break-words">{notification.title}</h4>
                        {notification.isImportant && (
                          <span className="bg-[#FCEBED] text-[#B83F4B] text-xs px-2 py-0.5 rounded-full font-medium">
                            Importante
                          </span>
                        )}
                        {!notification.read && (
                          <span className="w-2 h-2 bg-[#1F6662] rounded-full"></span>
                        )}
                      </div>
                      <div className="text-right">
                        <p className="text-xs text-[#53716E]">{notification.date}</p>
                        <p className="text-xs text-[#53716E]">{notification.time}</p>
                      </div>
                    </div>

                    <p className="text-sm text-[#0B2828] mb-3 whitespace-pre-wrap break-words">{notification.message}</p>
                    {notification.idSesion&&notification.puedeResponder&&(

                      <div className="flex gap-2 mt-3">

                      <button
                        disabled={ocupado}
                        onClick={()=>{cambiarErrorAccion('');cambiarAccion({aviso:notification,tipo:'confirmar'});}}
                        className="bg-[#227F78] text-white px-3 py-1 rounded text-xs"
                      >
                        Aceptar
                      </button>

                      <button
                        disabled={ocupado}
                        onClick={()=>{cambiarErrorAccion('');cambiarAccion({aviso:notification,tipo:'rechazar'});}}
                        className="bg-[#B83F4B] text-white px-3 py-1 rounded text-xs"
                      >
                        Rechazar
                      </button>

                      </div>

                    )}

                    <div className="flex gap-2">
                      {!notification.read && (
                        <button
                          disabled={ocupado} onClick={() =>
                            marcarComoLeida(notification.id)
                          }
                          className="text-xs text-[#1F6662] hover:text-[#1C4543] font-medium"
                        >
                          Marcar como leída
                        </button>
                      )}
                    
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Empty State */}
      {!consulta.cargando && !consulta.error && notifications.length === 0 && (
        <div className="bg-white rounded-lg p-12 text-center shadow-sm">
          <div className="w-16 h-16 bg-[#DCE3EA] rounded-full flex items-center justify-center mx-auto mb-4">
            <Bell className="w-8 h-8 text-[#53716E]" />
          </div>
          <h3 className="font-semibold mb-2">No tienes notificaciones</h3>
          <p className="text-[#53716E]">Cuando recibas avisos, aparecerán aquí</p>
        </div>
      )}
      {!cargando&&!error&&datos&&<PaginasSesiones pagina={datos.pagina} totalPaginas={datos.totalPaginas} alCambiar={cambiarPagina} desactivado={ocupado}/>}
      {accion&&<ConfirmarAccion titulo={accion.tipo==='confirmar'?'Confirmar tutoría':'Rechazar solicitud'} detalle={accion.aviso.title} confirmacion={accion.tipo==='confirmar'?'Aceptar':'Rechazar'} pendiente={ocupado} error={errorAccion} alCerrar={()=>{cambiarAccion(null);cambiarErrorAccion('');}} alConfirmar={responderSesion}/>}
    </div>
  );
}
