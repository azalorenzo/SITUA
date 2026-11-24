import { useEffect,useRef,useState } from 'react';
import { ArrowLeft,Search,Send,Paperclip,MoreVertical,Phone,Video } from 'lucide-react';
import { usarMensajes } from '../servicios/usarMensajes';
import { EstadoConsulta } from './ConsultaCoordinacion';
const hora=(valor:string)=>new Date(valor+'Z').toLocaleTimeString('es-MX',{hour:'2-digit',minute:'2-digit'});
function marca(valor:string|null){if(!valor)return '';const fecha=new Date(valor+'Z');return fecha.toDateString()===new Date().toDateString()?hora(valor):fecha.toLocaleDateString('es-MX',{day:'numeric',month:'short'});}
function Avatar({nombre,url,clases}:{nombre:string;url:string|null;clases:string}){return url?<img src={url} alt={nombre} className={`${clases} rounded-full object-cover`}/>:<div className={`${clases} shrink-0 rounded-full bg-[#1F6662] text-white flex items-center justify-center font-medium`}>{nombre.trim().split(/\s+/).slice(0,2).map(parte=>parte[0]).join('')}</div>;}
export function PanelMensajes({esTutor}:{esTutor:boolean}){
 const datos=usarMensajes(esTutor),panel=useRef<HTMLDivElement>(null);
 const [mostrarLista,cambiarLista]=useState(false);
 useEffect(()=>{if(panel.current)panel.current.scrollTop=panel.current.scrollHeight;},[datos.seleccionada?.id,datos.mensajes.at(-1)?.id]);
  return (
    <div className="h-[calc(100dvh-136px)] min-h-[360px] md:h-[calc(100vh-180px)] bg-white rounded-lg shadow-sm overflow-hidden flex min-w-0">
      <div className={`w-full md:w-80 md:shrink-0 min-w-0 border-r border-[#DCE3EA] flex-col ${!datos.seleccionada||mostrarLista?'flex':'hidden md:flex'}`}>
        <div className="p-4 border-b border-[#DCE3EA]">
          {!esTutor&&<h3 className="font-semibold mb-3">Mensajes</h3>}
          <div className="relative">
            <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 transform -translate-y-1/2" />
            <input
              type="search"
              aria-label="Buscar conversación" maxLength={200}
              placeholder="Buscar conversación..."
              value={datos.busqueda}
              onChange={evento => datos.cambiarBusqueda(evento.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            />
          </div>
        </div>
        <div className="flex-1 overflow-y-auto"><EstadoConsulta cargando={datos.cargandoLista} error={datos.errorLista} alReintentar={datos.reintentar}/>
          {!datos.cargandoLista&&!datos.errorLista&&!datos.conversaciones.length&&<p className="p-4 text-sm text-[#53716E]">Sin conversaciones.</p>}
          {datos.conversaciones.map(conversacion => (
            <button
              key={conversacion.id}
              aria-label={`Abrir conversación con ${conversacion.nombre}`} onClick={() => {datos.seleccionar(conversacion);cambiarLista(false);}}
              className={`w-full p-4 flex items-start gap-3 hover:bg-[#F5F7FA] transition-colors border-b border-[#DCE3EA] ${
                datos.seleccionada?.id === conversacion.id ? 'bg-[#F2FBF9]' : ''
              }`}
            >
              <div className="relative">
                <Avatar nombre={conversacion.nombre} url={conversacion.avatarUrl} clases="w-12 h-12"/>
              </div>

              <div className="flex-1 text-left min-w-0">
                <div className="flex justify-between items-start mb-1">
                  <div>
                    <p className="font-semibold text-sm truncate">{conversacion.nombre}</p>
                    <p className="text-xs text-[#53716E]">{(esTutor?'Estudiante':'Tutor')}</p>
                  </div>
                  <span className="text-xs text-[#53716E] ml-2">{marca(conversacion.ultimoEnviadoEn)}</span>
                </div>
                <p className="text-sm text-[#53716E] truncate">{conversacion.ultimoMensaje}</p>
              </div>

              {conversacion.noLeidos > 0 && (
                <span className="bg-[#1F6662] text-white text-xs font-semibold rounded-full w-5 h-5 flex items-center justify-center">
                  {conversacion.noLeidos}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>
      {datos.seleccionada ? (
        <div className={`flex-1 min-w-0 flex-col ${mostrarLista?'hidden md:flex':'flex'}`}>
          <div className="p-4 border-b border-[#DCE3EA] flex items-center justify-between bg-white">
            <div className="flex items-center gap-3 min-w-0">
              <button type="button" aria-label="Volver a conversaciones" className="md:hidden shrink-0 text-[#1F6662]" onClick={()=>cambiarLista(true)}><ArrowLeft className="w-5 h-5"/></button><div className="relative shrink-0">
                <Avatar nombre={datos.seleccionada.nombre} url={datos.seleccionada.avatarUrl} clases="w-10 h-10"/>
              </div>
              <div className="min-w-0">
                <h3 className="font-semibold truncate">{datos.seleccionada.nombre}</h3>
                <p className="text-xs text-[#53716E]">
                  {esTutor?'Estudiante':'Tutor'}
                </p>
              </div>
            </div>

            <div className="flex shrink-0 items-center gap-1">
              <button type="button" disabled aria-label="No disponible" className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Phone className="w-5 h-5 text-[#53716E]" />
              </button>
              <button type="button" disabled aria-label="No disponible" className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Video className="w-5 h-5 text-[#53716E]" />
              </button>
              <button type="button" disabled aria-label="No disponible" className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <MoreVertical className="w-5 h-5 text-[#53716E]" />
              </button>
            </div>
          </div>
          <div ref={panel} role="log" aria-label="Mensajes de la conversación" className="flex-1 overflow-y-auto p-4 space-y-4 bg-[#F5F7FA]"><EstadoConsulta cargando={datos.cargandoMensajes} error={datos.errorMensajes} alReintentar={datos.reintentar}/>
            {datos.mensajes.map(mensaje => (
              <div
                key={mensaje.id}
                className={`flex ${mensaje.enviado ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[70%] rounded-lg px-4 py-2 ${
                    mensaje.enviado
                      ? 'bg-[#1F6662] text-white'
                      : 'bg-white text-[#0B2828] border border-[#DCE3EA]'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap break-words">{mensaje.contenido}</p>
                  <p
                    className={`text-xs mt-1 ${
                      mensaje.enviado ? 'text-[#D3F4EE]' : 'text-[#53716E]'
                    }`}
                  >
                    {hora(mensaje.enviadoEn)}
                  </p>
                </div>
              </div>
            ))}
          </div>
          <div className="p-4 border-t border-[#DCE3EA] bg-white">{datos.errorEnvio&&<p role="alert" className="text-sm text-[#B83F4B] mb-2">{datos.errorEnvio}</p>}
            <div className="flex shrink-0 items-center gap-1">
              <button type="button" disabled aria-label="No disponible" className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Paperclip className="w-5 h-5 text-[#53716E]" />
              </button>
              <input
                type="text"
                placeholder="Escribe un mensaje..."
                value={datos.borrador}
                onChange={evento => datos.cambiarBorrador(evento.target.value)}
                aria-label="Escribe un mensaje" maxLength={4000} disabled={datos.enviando||datos.cargandoMensajes} onKeyDown={evento=>{if(evento.key==='Enter'&&!evento.nativeEvent.isComposing){evento.preventDefault();datos.enviar();}}}
                className="flex-1 min-w-0 px-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
              />
              <button
                onClick={datos.enviar} aria-label="Enviar mensaje" disabled={datos.enviando||datos.cargandoMensajes||!datos.borrador.trim()}
                className="p-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
              >
                <Send className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="hidden md:flex flex-1 items-center justify-center bg-[#F5F7FA]">
          <div className="text-center">
            <div className="w-20 h-20 bg-[#DCE3EA] rounded-full flex items-center justify-center mx-auto mb-4">
              <Search className="w-10 h-10 text-[#53716E]" />
            </div>
            <h3 className="font-semibold text-[#0B2828] mb-2">Selecciona una conversación</h3>
            <p className="text-[#53716E] text-sm">
              Sin conversaciones.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
