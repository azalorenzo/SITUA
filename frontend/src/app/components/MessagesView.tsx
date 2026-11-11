import { useState } from 'react';
import { Search, Send, Paperclip, MoreVertical, Phone, Video } from 'lucide-react';

interface Message {
  id: number;
  senderId: string;
  text: string;
  timestamp: string;
  isSent: boolean;
}

interface Chat {
  id: number;
  studentName: string;
  studentAvatar: string;
  lastMessage: string;
  timestamp: string;
  unreadCount: number;
  online: boolean;
}

export function MessagesView() {
  const [selectedChat, setSelectedChat] = useState<number>(1);
  const [messageInput, setMessageInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const chats: Chat[] = [
    {
      id: 1,
      studentName: 'Ana Laura Gómez',
      studentAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100&h=100&fit=crop',
      lastMessage: 'Gracias por la explicación, me quedó más claro',
      timestamp: '10:30 AM',
      unreadCount: 2,
      online: true
    },
    {
      id: 2,
      studentName: 'Carlos Méndez Ruiz',
      studentAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&h=100&fit=crop',
      lastMessage: '¿Podemos reprogramar la sesión de mañana?',
      timestamp: '9:15 AM',
      unreadCount: 0,
      online: false
    },
    {
      id: 3,
      studentName: 'María Fernández López',
      studentAvatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100&h=100&fit=crop',
      lastMessage: 'Perfecto, nos vemos el viernes',
      timestamp: 'Ayer',
      unreadCount: 0,
      online: true
    },
    {
      id: 4,
      studentName: 'Roberto Silva García',
      studentAvatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&h=100&fit=crop',
      lastMessage: 'Adjunto los ejercicios resueltos',
      timestamp: 'Ayer',
      unreadCount: 1,
      online: false
    },
    {
      id: 5,
      studentName: 'Laura Hernández Cruz',
      studentAvatar: 'https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?w=100&h=100&fit=crop',
      lastMessage: 'Muchas gracias por tu ayuda',
      timestamp: '15 may',
      unreadCount: 0,
      online: false
    }
  ];

  const messages: { [key: number]: Message[] } = {
    1: [
      {
        id: 1,
        senderId: 'student',
        text: 'Hola maestro, tengo una duda sobre el tema de derivadas',
        timestamp: '10:15 AM',
        isSent: false
      },
      {
        id: 2,
        senderId: 'tutor',
        text: 'Hola Ana Laura, claro que sí. ¿Cuál es tu duda?',
        timestamp: '10:17 AM',
        isSent: true
      },
      {
        id: 3,
        senderId: 'student',
        text: 'No entiendo muy bien cómo aplicar la regla de la cadena en este ejercicio',
        timestamp: '10:18 AM',
        isSent: false
      },
      {
        id: 4,
        senderId: 'tutor',
        text: 'Perfecto. La regla de la cadena se usa cuando tienes una función compuesta. Déjame explicarte con un ejemplo.',
        timestamp: '10:20 AM',
        isSent: true
      },
      {
        id: 5,
        senderId: 'tutor',
        text: 'Si tienes f(g(x)), la derivada es f\'(g(x)) * g\'(x). ¿Tiene sentido?',
        timestamp: '10:21 AM',
        isSent: true
      },
      {
        id: 6,
        senderId: 'student',
        text: 'Gracias por la explicación, me quedó más claro',
        timestamp: '10:30 AM',
        isSent: false
      }
    ],
    2: [
      {
        id: 1,
        senderId: 'student',
        text: 'Buenos días maestro',
        timestamp: '9:10 AM',
        isSent: false
      },
      {
        id: 2,
        senderId: 'tutor',
        text: 'Buenos días Carlos, ¿cómo estás?',
        timestamp: '9:12 AM',
        isSent: true
      },
      {
        id: 3,
        senderId: 'student',
        text: '¿Podemos reprogramar la sesión de mañana?',
        timestamp: '9:15 AM',
        isSent: false
      }
    ]
  };

  const currentChat = chats.find(chat => chat.id === selectedChat);
  const currentMessages = messages[selectedChat] || [];

  const filteredChats = chats.filter(chat =>
    chat.studentName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSendMessage = () => {
    if (messageInput.trim()) {
      // Aquí iría la lógica para enviar el mensaje
      console.log('Sending message:', messageInput);
      setMessageInput('');
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
          {filteredChats.map(chat => (
            <button
              key={chat.id}
              onClick={() => setSelectedChat(chat.id)}
              className={`w-full p-4 flex items-start gap-3 hover:bg-[#F5F7FA] transition-colors border-b border-[#DCE3EA] ${
                selectedChat === chat.id ? 'bg-[#F2FBF9]' : ''
              }`}
            >
              <div className="relative">
                <img
                  src={chat.studentAvatar}
                  alt={chat.studentName}
                  className="w-12 h-12 rounded-full object-cover"
                />
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
                <img
                  src={currentChat.studentAvatar}
                  alt={currentChat.studentName}
                  className="w-10 h-10 rounded-full object-cover"
                />
                {currentChat.online && (
                  <span className="absolute bottom-0 right-0 w-3 h-3 bg-[#227F78] border-2 border-white rounded-full"></span>
                )}
              </div>
              <div>
                <h3 className="font-semibold">{currentChat.studentName}</h3>
                <p className="text-xs text-[#53716E]">
                  {currentChat.online ? 'En línea' : 'Desconectado'}
                </p>
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
