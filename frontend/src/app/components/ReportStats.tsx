import { FileCheck, TrendingUp, AlertCircle } from 'lucide-react';

export function ReportStats({ reportes }: any) {

  const total = reportes.length;

  const completados = reportes.filter(
    (r: any) => r.estatus === "Completado"
  ).length;

  const pendientes = reportes.filter(
    (r: any) => r.estatus === "Pendiente"
  ).length;

  const stats = [
    {
      icon: FileCheck,
      label: 'Total de reportes',
      value: total,
      bgColor: 'bg-[#F2FBF9]',
      iconColor: 'text-[#1F6662]'
    },
    {
      icon: TrendingUp,
      label: 'Reportes completados',
      value: completados,
      bgColor: 'bg-[#E4F3ED]',
      iconColor: 'text-[#227F78]'
    },
    {
      icon: AlertCircle,
      label: 'Reportes pendientes',
      value: pendientes,
      bgColor: 'bg-[#FFF3D6]',
      iconColor: 'text-[#986314]'
    }
  ];

  return (
    <div>
      <h3 className="font-semibold mb-4">
        Reportes visuales
      </h3>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="bg-white rounded-lg p-4 shadow-sm">
              <div className={`${stat.bgColor} p-3 rounded-lg w-fit mb-3`}>
                <Icon className={`w-6 h-6 ${stat.iconColor}`} />
              </div>
              <p className="text-sm text-[#53716E] mb-2">{stat.label}</p>
              <p className="font-semibold text-2xl">{stat.value}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
}