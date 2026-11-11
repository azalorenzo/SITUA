import { Users, CheckCircle, FileText, Calendar, AlertCircle, Clock } from 'lucide-react';

export function TutorView() {
  const students = [
    { name: 'Ana Laura Gómez', career: 'Ingeniería en Software', status: 'Bajo', statusColor: 'bg-[#FCEBED] text-[#B83F4B]' },
    { name: 'Carlos Méndez Ruiz', career: 'Ingeniería Industrial', status: 'Medio', statusColor: 'bg-[#FFF3D6] text-[#986314]' },
    { name: 'María Fernández', career: 'Arquitectura', status: 'Medio', statusColor: 'bg-[#FFF3D6] text-[#986314]' },
    { name: 'Roberto Silva', career: 'Diseño Gráfico', status: 'Alto', statusColor: 'bg-[#E4F3ED] text-[#227F78]' }
  ];

  const upcomingSessions = [
    { student: 'Ana Laura Gómez', topic: 'Cálculo diferencial', date: '20 mayo, 2026', time: '10:00 AM', status: 'Confirmada' },
    { student: 'Carlos Méndez', topic: 'Álgebra lineal', date: '21 mayo, 2026', time: '2:00 PM', status: 'Confirmada' },
    { student: 'María Fernández', topic: 'Física I', date: '22 mayo, 2026', time: '11:00 AM', status: 'Pendiente' }
  ];

  const completedSessions = [
    { student: 'Ana Laura Gómez', topic: 'Cálculo integral', status: 'Completada' },
    { student: 'Roberto Silva', topic: 'Estadística', status: 'Completada' },
    { student: 'Carlos Méndez', topic: 'Programación', status: 'Completada' }
  ];

  const alerts = [
    { icon: AlertCircle, text: 'Ana Laura requiere seguimiento urgente en matemáticas', color: 'text-[#B83F4B] bg-[#FCEBED]' },
    { icon: Clock, text: 'Recordatorio: tienes 3 reportes pendientes por entregar', color: 'text-[#986314] bg-[#FFF3D6]' }
  ];

  const calendarEvents = [
    { day: 20, label: 'Tutoría', time: '10:00 AM', color: 'bg-[#D3F4EE]' },
    { day: 21, label: 'Tutoría', time: '2:00 PM', color: 'bg-[#E4F3ED]' },
    { day: 22, label: 'Reporte', time: '', color: 'bg-[#FFF3D6]' }
  ];

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 - h full">
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Users className="w-6 h-6 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Mis estudiantes</p>
              <p className="font-semibold">16</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#E4F3ED] p-3 rounded-lg">
              <CheckCircle className="w-6 h-6 text-[#227F78]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Tutorías realizadas</p>
              <p className="font-semibold">28</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FFF3D6] p-3 rounded-lg">
              <FileText className="w-6 h-6 text-[#986314]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Reportes pendientes</p>
              <p className="font-semibold">22</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2EDF8] p-3 rounded-lg">
              <Calendar className="w-6 h-6 text-[#6F548F]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Sesiones programadas</p>
              <p className="font-semibold">5</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Grid */}
<div className="w-full">

  <div className="bg-white rounded-lg p-6 shadow-sm">

    <div className="flex justify-between items-center mb-4">

      <h3 className="font-semibold">
        Mis estudiantes
      </h3>

      <button className="text-[#1F6662] text-sm hover:underline">
        Ver todos
      </button>

    </div>

    <table className="w-full text-sm">

      <thead className="bg-[#F5F7FA]">

        <tr>

          <th className="text-left py-3 px-4">
            Estudiante
          </th>

          <th className="text-left py-3 px-4">
            Carrera
          </th>

          <th className="text-left py-3 px-4">
            Estado
          </th>

        </tr>

      </thead>

      <tbody>

        {students.map((student, index) => (

          <tr
            key={index}
            className="border-b hover:bg-[#F5F7FA]"
          >

            <td className="py-3 px-4">

              <div className="flex items-center gap-3">

                <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium">

                  {student.name
                    .split(' ')
                    .map(n => n[0])
                    .join('')
                    .slice(0, 2)}

                </div>

                <span className="font-medium">

                  {student.name}

                </span>

              </div>

            </td>

            <td className="py-3 px-4 text-[#53716E]">

              {student.career}

            </td>

            <td className="py-3 px-4">

              <span
                className={`text-xs px-3 py-1 rounded-full ${student.statusColor}`}
              >

                {student.status}

              </span>

            </td>

          </tr>

        ))}

      </tbody>

    </table>

  </div>

</div>
    </div>
  );
}