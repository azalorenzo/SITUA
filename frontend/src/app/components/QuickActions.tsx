import { UserPlus, MessageSquare, BarChart3, Mail } from 'lucide-react';

export function QuickActions({ onSectionChange }: any) {
  const actions = [

  {
    icon: MessageSquare,
    label: 'Enviar mensaje',
    color: 'bg-[#E4F3ED] text-[#227F78]',
    section: 'mensajes'
  },

  {
    icon: BarChart3,
    label: 'Ver estadísticas',
    color: 'bg-[#F2EDF8] text-[#6F548F]',
    section: 'reportes'
  },

  {
    icon: Mail,
    label: 'Notificaciones',
    color: 'bg-[#FFF3D6] text-[#986314]',
    section: 'avisos'
  },

];  

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm">
      <h3 className="font-semibold mb-4">Acciones rápidas</h3>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {actions.map((action, index) => {
          const Icon = action.icon;
          return (
            <button
              key={index}
              onClick={() => onSectionChange(action.section)}
              className="flex flex-col items-center gap-2 p-4 rounded-lg hover:bg-[#F5F7FA] transition-colors"
            >
              <div className={`${action.color} p-3 rounded-lg`}>
                <Icon className="w-6 h-6" />
              </div>
              <span className="text-sm text-center">{action.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
