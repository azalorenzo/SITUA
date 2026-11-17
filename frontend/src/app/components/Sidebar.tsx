import { Home, UserCheck, FileText, Bell, BookOpen } from 'lucide-react';

interface PropiedadesMenuCoordinacion {
  activeSection: string;
  onSectionChange: (seccion: string) => void;
}

export function Sidebar({activeSection,onSectionChange}:PropiedadesMenuCoordinacion) {
  const menuItems = [
    { id: 'inicio', label: 'Inicio', icon: Home },
    { id: 'tutores', label: 'Tutores', icon: UserCheck },
    { id: 'estudiantes', label: 'Estudiantes', icon: UserCheck },
    { id: 'asignaciones', label: 'Asignaciones', icon: UserCheck },
    { id: 'reportes', label: 'Reportes', icon: FileText },
    { id: 'avisos', label: 'Avisos', icon: Bell },
  ];
  

  return (
    <aside className="w-64 shrink-0 bg-[#1C4543] text-white h-screen flex flex-col">
      <div className="p-6 border-b border-[#1F6662]">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-white rounded-lg flex items-center justify-center">
            <BookOpen className="w-6 h-6 text-[#1C4543]" />
          </div>
          <div>
            <h1 className="font-semibold">Tutoría</h1>
          </div>
        </div>
      </div>

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
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

    </aside>
  );
}
