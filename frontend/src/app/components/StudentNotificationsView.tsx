import { mockNotices } from '../mock/data';
import { useState } from "react";
import {
  Bell,
  CheckCircle,
  AlertCircle,
  Info,
  Calendar
} from "lucide-react";

interface Notification {
  id: number;
  title: string;
  description: string;
  category: string;
  priority: string;
  date: string;
  time: string;
  read: boolean;
}

export function StudentNotificationsView() {

  const [notifications, setNotifications] =
    useState<Notification[]>(mockNotices.map(n => ({ id: Number(n.id_aviso), title: n.titulo, description: n.descripcion, category: n.categoria, priority: n.prioridad, date: 'Junio 2026', time: '09:00', read: n.leido })));

  const getPriorityColor = (
    priority: string
  ) => {

    switch(priority){

      case "Alta":
        return "bg-[#FCEBED] text-[#B83F4B]";

      case "Media":
        return "bg-[#FFF3D6] text-[#986314]";

      default:
        return "bg-[#DCE3EA] text-[#0B2828]";

    }

  };

  const unreadCount =
    notifications.filter(
      n => !n.read
    ).length;

  return (
    <div className="space-y-6">

      <div>
        <h2 className="font-semibold mb-1">
          Avisos
        </h2>

        <p className="text-sm text-[#53716E]">
          Mantente informado sobre tus actividades académicas
        </p>
      </div>

      <div className="bg-white rounded-lg shadow-sm">

        <div className="p-6 border-b border-[#DCE3EA] flex justify-between">

          <h3 className="font-semibold">
            Lista de avisos
          </h3>

      

        </div>

        <div className="divide-y divide-[#DCE3EA]">

          {notifications.map((notification) => (

            <div
              key={notification.id}
              className={`p-4 hover:bg-[#F5F7FA] ${
                !notification.read
                  ? "bg-[#F2FBF9]/30"
                  : ""
              }`}
            >

              <div className="flex gap-4">

                <div className="bg-[#F2FBF9] p-3 rounded-lg">

                  <Calendar className="w-5 h-5 text-[#1F6662]" />

                </div>

                <div className="flex-1">

                  <div className="flex items-center gap-2 mb-2">

                    <h4 className="font-semibold text-sm">

                      {notification.title}

                    </h4>

                    {!notification.read && (

                      <span className="w-2 h-2 bg-[#1F6662] rounded-full"></span>

                    )}

                  </div>

                  <p className="text-sm text-[#53716E] mb-2">

                    {notification.description}

                  </p>

                  <div className="flex justify-between items-center">

                    <div className="flex gap-2">

                      <span className="text-xs px-2 py-1 rounded-full bg-[#D3F4EE] text-[#1C4543]">

                        {notification.category}

                      </span>

                      <span
                        className={`text-xs px-2 py-1 rounded-full ${getPriorityColor(
                          notification.priority
                        )}`}
                      >

                        {notification.priority}

                      </span>

                    </div>

                    <div className="text-xs text-[#53716E]">

                      {notification.date}
                      {" "}
                      {notification.time}

                    </div>

                  </div>

                </div>

              </div>

            </div>

          ))}

          {notifications.length === 0 && (

            <div className="p-8 text-center text-[#53716E]">

              No tienes avisos.

            </div>

          )}

        </div>

      </div>

    </div>
  );

}