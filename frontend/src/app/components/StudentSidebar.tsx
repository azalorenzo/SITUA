import {
  Home,
  Calendar,
  BookOpen,
  MessageSquare,
  Bell,
  LogOut
} from 'lucide-react';

import estudianteImg from '../../assets/estudiante.png';

interface StudentSidebarProps {
  onLogout: () => void;
  activeSection: string;
  onSectionChange: (section: string) => void;
  nombreEstudiante: string;
}

export function StudentSidebar({ onLogout,
  activeSection,
  onSectionChange,
  nombreEstudiante
}: StudentSidebarProps) {

  const menuItems = [
    { id: 'inicio', label: 'Inicio', icon: Home },
    { id: 'mi-agenda', label: 'Mi agenda', icon: Calendar },
    { id: 'mis-tutorias', label: 'Mis tutorías', icon: BookOpen },
    { id: 'mensajes', label: 'Mensajes', icon: MessageSquare },
    { id: 'avisos', label: 'Avisos', icon: Bell },
  ];

  return (

    <aside className="w-64 bg-[#1C4543] text-white h-screen flex flex-col">

      {/* LOGO */}
      <div className="p-6 border-b border-[#1F6662]">

        <div className="flex items-center gap-3">

          <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center">

            <BookOpen className="w-6 h-6 text-[#1C4543]" />

          </div>

          <div>

            <h1 className="font-semibold">
              Tutoría
            </h1>

          </div>

        </div>

      </div>

      {/* MENU */}
      <nav className="flex-1 py-6">

        {menuItems.map((item) => {

          const Icon = item.icon;

          return (

            <button
              key={item.id}
              onClick={() => onSectionChange(item.id)}
              className={`w-full flex items-center gap-3 px-6 py-3 transition-colors ${
                activeSection === item.id
                  ? 'bg-[#1F6662] border-l-4 border-white'
                  : 'hover:bg-[#1F6662]/50'
              }`}
            >

              <Icon className="w-5 h-5" />

              <span>
                {item.label}
              </span>

            </button>

          );
        })}
      </nav>

      {/* FOOTER */}
      <div className="p-6 border-t border-[#1F6662]">

        <div className="flex items-center gap-3 mb-4">

          <img
            src={estudianteImg}
            alt="Estudiante"
            className="w-10 h-10 rounded-full"
          />

          <div className="text-sm">

            <p className="font-medium uppercase">
              {nombreEstudiante}
            </p>

            <p className="text-[#D3F4EE] text-xs">
              Estudiante
            </p>

          </div>

        </div>

        <button

          onClick={onLogout}

          className="flex items-center gap-2 text-[#D3F4EE] hover:text-white transition"

        >

          <LogOut className="w-5 h-5" />

          <span>Salir</span>

        </button>

      </div>

    </aside>
  );
}