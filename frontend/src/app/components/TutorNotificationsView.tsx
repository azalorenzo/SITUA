import { mockNotices } from '../mock/data';
import { useState } from 'react';
import { Bell, CheckCircle, AlertCircle, Calendar, Users, FileText, MessageSquare, Clock, Info } from 'lucide-react';

interface Notification {
  id: number;
  idSesion?: string,
  type: 'success' | 'warning' | 'info' | 'alert';
  title: string;
  message: string;
  date: string;
  time: string;
  isImportant: boolean;
  read: boolean;
}
export function TutorNotificationsView() {
  
  const [notifications, setNotifications] = useState<Notification[]>(mockNotices.map(n => ({ id: Number(n.id_aviso), title: n.id_aviso === '1' ? 'Nueva solicitud de tutoría' : n.titulo, idSesion: n.id_aviso === '1' ? '2' : undefined, message: n.descripcion, type: n.prioridad === 'Alta' ? 'alert' : 'info', isImportant: n.prioridad === 'Alta', date: 'Junio 2026', time: '09:00', read: n.leido })));
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

  const unreadCount =
    notifications.filter(
      n => !n.read
    ).length;

  const importantCount =
    notifications.filter(
      n => n.isImportant
  ).length;
  const marcarComoLeida = (id: number) => setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
  const aceptarSesion = (_idSesion: string, idAviso: number) => setNotifications(prev => prev.filter(n => n.id !== idAviso));
  const rechazarSesion = (_idSesion: string, idAviso: number) => setNotifications(prev => prev.filter(n => n.id !== idAviso));
  const marcarTodasLeidas = () => setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Centro de notificaciones</h2>
          <p className="text-sm text-[#53716E]">Mantente al día con tus actividades</p>
        </div>
<button
  onClick={marcarTodasLeidas}
  className="px-4 py-2 text-sm text-[#1F6662] hover:bg-[#F2FBF9] rounded-lg"
>
  Marcar todas como leídas
</button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Bell className="w-5 h-5 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Total de avisos</p>
              <p className="font-semibold">{notifications.length}</p>
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

                  <div className="flex-1">
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <h4 className="font-semibold text-sm">{notification.title}</h4>
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

                    <p className="text-sm text-[#0B2828] mb-3">{notification.message}</p>
                    {notification.title ===
                      "Nueva solicitud de tutoría" &&
                      !notification.read && (

                      <div className="flex gap-2 mt-3">

                      <button
                        onClick={() => {

                          notification.idSesion &&
                          aceptarSesion(
                            notification.idSesion,
                            notification.id
                          );

                        }}
                        className="bg-[#227F78] text-white px-3 py-1 rounded text-xs"
                      >
                        Aceptar
                      </button>

                      <button
                        onClick={() => {

                          notification.idSesion &&
                          rechazarSesion(
                            notification.idSesion,
                            notification.id
                          );

                        }}
                        className="bg-[#B83F4B] text-white px-3 py-1 rounded text-xs"
                      >
                        Rechazar
                      </button>

                      </div>

                    )}

                    <div className="flex gap-2">
                      {!notification.read && (
                        <button
                          onClick={() =>
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

      {/* Empty State (commented for reference) */}
      {/* {notifications.length === 0 && (
        <div className="bg-white rounded-lg p-12 text-center shadow-sm">
          <div className="w-16 h-16 bg-[#DCE3EA] rounded-full flex items-center justify-center mx-auto mb-4">
            <Bell className="w-8 h-8 text-[#53716E]" />
          </div>
          <h3 className="font-semibold mb-2">No tienes notificaciones</h3>
          <p className="text-[#53716E]">Cuando recibas avisos, aparecerán aquí</p>
        </div>
      )} */}
    </div>
  );
}
