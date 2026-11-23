import { useCallback, useEffect, useState } from 'react';
import { Search, Send, Paperclip, MoreVertical, Phone, Video } from 'lucide-react';
import { consultarConversaciones, consultarMensajesConversacion, enviarMensajeConversacion, marcarConversacionLeida } from '../servicios/mensajesTutor';
import { mensajeDeError } from '../servicios/usarConsultaTutor';
import type { ConversacionTutor, MensajeTutor } from '../tipos/mensajesTutor';
import { EstadoConsulta, usarConsultaCoordinacion } from './ConsultaCoordinacion';

interface Message {
  id: number;
  text: string;
  timestamp: string;
  isSent: boolean;
}

// online llega nulo porque el sistema no registra si la otra persona está conectada.
interface Chat {
  id: number;
  studentName: string;
  studentAvatar: string | null;
  lastMessage: string;
  timestamp: string;
  unreadCount: number;
  online: boolean | null;
}

// El backend guarda las fechas en UTC sin zona; aquí se muestran en la hora del navegador.
function formatearHoraMensaje(utc: string): string {
  return new Date(`${utc}Z`).toLocaleTimeString('es-MX', { hour: 'numeric', minute: '2-digit' });
}

function formatearMarca(utc: string | null): string {
  if (!utc) return '';
  const fecha = new Date(`${utc}Z`);
  const hoy = new Date();
  if (fecha.toDateString() === hoy.toDateString()) return formatearHoraMensaje(utc);
  const ayer = new Date(hoy);
  ayer.setDate(hoy.getDate() - 1);
  if (fecha.toDateString() === ayer.toDateString()) return 'Ayer';
  return fecha.toLocaleDateString('es-MX', { day: 'numeric', month: 'short' });
}

function aChat(c: ConversacionTutor): Chat {
  return {
    id: c.id, studentName: c.nombre, studentAvatar: c.avatarUrl, lastMessage: c.ultimoMensaje ?? '',
    timestamp: formatearMarca(c.ultimoEnviadoEn), unreadCount: c.noLeidos, online: null
  };
}

function aMensaje(m: MensajeTutor): Message {
  return { id: m.id, text: m.contenido, timestamp: formatearHoraMensaje(m.enviadoEn), isSent: m.enviado };
}

async function consultarChats(comprobacion: AbortSignal) {
  return (await consultarConversaciones(comprobacion)).map(aChat);
}

// Sin foto de perfil se muestran las iniciales, como en el resto de la aplicación.
function Avatar({ nombre, url, clases }: { nombre: string; url: string | null; clases: string }) {
  if (url) return <img src={url} alt={nombre} className={`${clases} rounded-full object-cover`} />;
  return (
    <div className={`${clases} bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium`}>
      {nombre.split(' ').map(n => n[0]).join('').slice(0, 2)}
    </div>
  );
}

export function MessagesView() {
  const consultaChats = usarConsultaCoordinacion<Chat>(consultarChats);
  const [chats, setChats] = useState<Chat[]>([]);
  useEffect(() => { setChats(consultaChats.datos); }, [consultaChats.datos]);

  const [selectedChat, setSelectedChat] = useState<number | null>(null);
  const [messageInput, setMessageInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Al cargar las conversaciones se abre la primera, como hacía la pantalla.
  useEffect(() => {
    if (selectedChat === null && chats.length > 0) setSelectedChat(chats[0].id);
  }, [chats, selectedChat]);

  const consultarMensajesChat = useCallback(async (comprobacion: AbortSignal) => {
    if (selectedChat === null) return [];
    return (await consultarMensajesConversacion(selectedChat, comprobacion)).map(aMensaje);
  }, [selectedChat]);
  const consultaMensajes = usarConsultaCoordinacion<Message>(consultarMensajesChat);
  const [currentMessages, setCurrentMessages] = useState<Message[]>([]);
  useEffect(() => { setCurrentMessages(consultaMensajes.datos); }, [consultaMensajes.datos]);

  const currentChat = chats.find(chat => chat.id === selectedChat);

  // Al abrir una conversación con mensajes sin leer se marca como leída.
  const sinLeer = currentChat?.unreadCount ?? 0;
  useEffect(() => {
    if (selectedChat === null || sinLeer === 0 || consultaMensajes.cargando || consultaMensajes.error) return;
    marcarConversacionLeida(selectedChat)
      .then(() => setChats(prev => prev.map(c => (c.id === selectedChat ? { ...c, unreadCount: 0 } : c))))
      .catch(() => { /* se vuelve a intentar al abrir de nuevo la conversación */ });
  }, [selectedChat, sinLeer, consultaMensajes.cargando, consultaMensajes.error]);

  const filteredChats = chats.filter(chat =>
    chat.studentName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSendMessage = async () => {
    const texto = messageInput.trim();
    if (!texto || selectedChat === null || enviando) return;
    setEnviando(true);
    try {
      const enviado = aMensaje(await enviarMensajeConversacion(selectedChat, texto));
      setCurrentMessages(prev => [...prev, enviado]);
      setChats(prev => {
        const actual = prev.find(c => c.id === selectedChat);
        if (!actual) return prev;
        return [{ ...actual, lastMessage: texto, timestamp: enviado.timestamp }, ...prev.filter(c => c.id !== selectedChat)];
      });
      setMessageInput('');
    } catch (fallo) {
      alert(mensajeDeError(fallo, 'No se pudo enviar el mensaje.'));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="h-[calc(100vh-180px)] bg-white rounded-lg shadow-sm overflow-hidden flex">
      {/* Left Panel - Chats List */}
      <div className="w-80 border-r border-[#DCE3EA] flex flex-col">
        {/* Search */}
        <div className="p-4 border-b border-[#DCE3EA]">
          <div className="relative">
            <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 transform -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar conversación..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            />
          </div>
        </div>

        {/* Chats List */}
        <div className="flex-1 overflow-y-auto">
          <EstadoConsulta cargando={consultaChats.cargando} error={consultaChats.error} alReintentar={consultaChats.actualizar} />
          {filteredChats.map(chat => (
            <button
              key={chat.id}
              onClick={() => setSelectedChat(chat.id)}
              className={`w-full p-4 flex items-start gap-3 hover:bg-[#F5F7FA] transition-colors border-b border-[#DCE3EA] ${
                selectedChat === chat.id ? 'bg-[#F2FBF9]' : ''
              }`}
            >
              <div className="relative">
                <Avatar nombre={chat.studentName} url={chat.studentAvatar} clases="w-12 h-12" />
                {chat.online && (
                  <span className="absolute bottom-0 right-0 w-3 h-3 bg-[#227F78] border-2 border-white rounded-full"></span>
                )}
              </div>

              <div className="flex-1 text-left min-w-0">
                <div className="flex justify-between items-start mb-1">
                  <p className="font-semibold text-sm truncate">{chat.studentName}</p>
                  <span className="text-xs text-[#53716E] ml-2">{chat.timestamp}</span>
                </div>
                <p className="text-sm text-[#53716E] truncate">{chat.lastMessage}</p>
              </div>

              {chat.unreadCount > 0 && (
                <span className="bg-[#1F6662] text-white text-xs font-semibold rounded-full w-5 h-5 flex items-center justify-center">
                  {chat.unreadCount}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Right Panel - Chat */}
      {currentChat ? (
        <div className="flex-1 flex flex-col">
          {/* Chat Header */}
          <div className="p-4 border-b border-[#DCE3EA] flex items-center justify-between bg-white">
            <div className="flex items-center gap-3">
              <div className="relative">
                <Avatar nombre={currentChat.studentName} url={currentChat.studentAvatar} clases="w-10 h-10" />
                {currentChat.online && (
                  <span className="absolute bottom-0 right-0 w-3 h-3 bg-[#227F78] border-2 border-white rounded-full"></span>
                )}
              </div>
              <div>
                <h3 className="font-semibold">{currentChat.studentName}</h3>
                {currentChat.online !== null && (
                  <p className="text-xs text-[#53716E]">
                    {currentChat.online ? 'En línea' : 'Desconectado'}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Phone className="w-5 h-5 text-[#53716E]" />
              </button>
              <button className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Video className="w-5 h-5 text-[#53716E]" />
              </button>
              <button className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <MoreVertical className="w-5 h-5 text-[#53716E]" />
              </button>
            </div>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-[#F5F7FA]">
            <EstadoConsulta cargando={consultaMensajes.cargando} error={consultaMensajes.error} alReintentar={consultaMensajes.actualizar} />
            {currentMessages.map(message => (
              <div
                key={message.id}
                className={`flex ${message.isSent ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-[70%] rounded-lg px-4 py-2 ${
                    message.isSent
                      ? 'bg-[#1F6662] text-white'
                      : 'bg-white text-[#0B2828] border border-[#DCE3EA]'
                  }`}
                >
                  <p className="text-sm">{message.text}</p>
                  <p
                    className={`text-xs mt-1 ${
                      message.isSent ? 'text-[#D3F4EE]' : 'text-[#53716E]'
                    }`}
                  >
                    {message.timestamp}
                  </p>
                </div>
              </div>
            ))}
          </div>

          {/* Message Input */}
          <div className="p-4 border-t border-[#DCE3EA] bg-white">
            <div className="flex items-center gap-2">
              <button className="p-2 hover:bg-[#DCE3EA] rounded-lg">
                <Paperclip className="w-5 h-5 text-[#53716E]" />
              </button>
              <input
                type="text"
                placeholder="Escribe un mensaje..."
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                className="flex-1 px-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
              />
              <button
                onClick={handleSendMessage}
                disabled={enviando}
                className="p-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
              >
                <Send className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex-1 flex items-center justify-center bg-[#F5F7FA]">
          <div className="text-center">
            <div className="w-20 h-20 bg-[#DCE3EA] rounded-full flex items-center justify-center mx-auto mb-4">
              <Search className="w-10 h-10 text-[#53716E]" />
            </div>
            <h3 className="font-semibold text-[#0B2828] mb-2">Selecciona una conversación</h3>
            <p className="text-[#53716E] text-sm">
              Elige un estudiante para comenzar a chatear
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
