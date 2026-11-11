import { Users, CheckCircle, Clock, AlertTriangle, Search, Filter, Plus, UserPlus, FileText, Mail, BarChart3 } from 'lucide-react';

export function AssignmentsView() {
  const assignments = [
    {
      id: 1,
      student: 'Ana Laura Gómez',
      studentId: '2021-0456',
      tutor: 'Mtro. Javier López',
      assignedDate: '15/05/2026',
      status: 'Activa',
      sessions: 8,
      lastSession: '22/05/2026'
    },
    {
      id: 2,
      student: 'Carlos Méndez Ruiz',
      studentId: '2020-0892',
      tutor: 'Mtra. Elena Torres',
      assignedDate: '10/05/2026',
      status: 'Activa',
      sessions: 12,
      lastSession: '21/05/2026'
    },
    {
      id: 3,
      student: 'María Fernández López',
      studentId: '2021-0234',
      tutor: 'Mtro. Carlos Ramírez',
      assignedDate: '08/05/2026',
      status: 'Activa',
      sessions: 6,
      lastSession: '20/05/2026'
    },
    {
      id: 4,
      student: 'Roberto Silva García',
      studentId: '2019-0567',
      tutor: 'Mtra. Ana Martínez',
      assignedDate: '01/05/2026',
      status: 'Activa',
      sessions: 15,
      lastSession: '23/05/2026'
    },
    {
      id: 5,
      student: 'Laura Hernández Cruz',
      studentId: '2021-0789',
      tutor: 'Mtro. Luis Fernández',
      assignedDate: '05/05/2026',
      status: 'Pendiente',
      sessions: 0,
      lastSession: '-'
    },
    {
      id: 6,
      student: 'Pedro Sánchez Ruiz',
      studentId: '2021-0345',
      tutor: 'Mtra. Elena Torres',
      assignedDate: '12/05/2026',
      status: 'Activa',
      sessions: 9,
      lastSession: '19/05/2026'
    }
  ];

  const pendingStudents = [
    { name: 'Jorge Ramírez Luna', id: '2021-0678', reason: 'Sin tutor asignado' },
    { name: 'Diana Morales Torres', id: '2021-0891', reason: 'Cambio de tutor solicitado' },
    { name: 'Miguel Ángel Vargas', id: '2020-0234', reason: 'Nuevo ingreso' }
  ];

  const getStatusColor = (status: string) => {
    return status === 'Activa'
      ? 'bg-[#E4F3ED] text-[#227F78]'
      : 'bg-[#FFF3D6] text-[#986314]';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Asignaciones</h2>
          <p className="text-sm text-[#53716E]">Gestiona las asignaciones de tutores y estudiantes</p>
        </div>
        <button className="flex items-center gap-2 bg-[#1F6662] text-white px-4 py-2 rounded-lg hover:bg-[#1C4543]">
          <Plus className="w-4 h-4" />
          Nueva asignación
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Users className="w-5 h-5 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Total asignaciones</p>
              <p className="font-semibold">523</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#E4F3ED] p-3 rounded-lg">
              <CheckCircle className="w-5 h-5 text-[#227F78]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Asignaciones activas</p>
              <p className="font-semibold">489</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FFF3D6] p-3 rounded-lg">
              <Clock className="w-5 h-5 text-[#986314]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Pendientes</p>
              <p className="font-semibold">34</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FCEBED] p-3 rounded-lg">
              <AlertTriangle className="w-5 h-5 text-[#B83F4B]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Sin asignar</p>
              <p className="font-semibold">5</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Assignments Table */}
        <div className="lg:col-span-2 bg-white rounded-lg shadow-sm">
          <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-center">
            <h3 className="font-semibold">Asignaciones actuales</h3>
            <div className="flex gap-3">
              <div className="relative">
                <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 transform -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Buscar asignación..."
                  className="pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                />
              </div>
              <button className="flex items-center gap-2 px-4 py-2 border border-[#DCE3EA] rounded-lg hover:bg-[#F5F7FA]">
                <Filter className="w-4 h-4" />
                Filtros
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]">
                <tr>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Estudiante</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Matrícula</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Tutor asignado</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Fecha asignación</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Sesiones</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-[#0B2828]">Estado</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((assignment) => (
                  <tr key={assignment.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white text-xs font-medium">
                          {assignment.student.split(' ').map(n => n[0]).join('').slice(0, 2)}
                        </div>
                        <span className="text-sm font-medium">{assignment.student}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-sm text-[#53716E]">{assignment.studentId}</td>
                    <td className="py-3 px-4 text-sm text-[#0B2828]">{assignment.tutor}</td>
                    <td className="py-3 px-4 text-sm text-[#53716E]">{assignment.assignedDate}</td>
                    <td className="py-3 px-4 text-sm font-medium">{assignment.sessions}</td>
                    <td className="py-3 px-4">
                      <span className={`text-xs px-3 py-1 rounded-full font-medium ${getStatusColor(assignment.status)}`}>
                        {assignment.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-4 border-t border-[#DCE3EA] flex justify-between items-center">
            <span className="text-sm text-[#53716E]">Mostrando 6 de 523 asignaciones</span>
            <div className="flex gap-2">
              <button className="px-3 py-1 border border-[#DCE3EA] rounded hover:bg-[#F5F7FA] text-sm">
                Anterior
              </button>
              <button className="px-3 py-1 bg-[#1F6662] text-white rounded hover:bg-[#1C4543] text-sm">
                1
              </button>
              <button className="px-3 py-1 border border-[#DCE3EA] rounded hover:bg-[#F5F7FA] text-sm">
                2
              </button>
              <button className="px-3 py-1 border border-[#DCE3EA] rounded hover:bg-[#F5F7FA] text-sm">
                3
              </button>
              <button className="px-3 py-1 border border-[#DCE3EA] rounded hover:bg-[#F5F7FA] text-sm">
                Siguiente
              </button>
            </div>
          </div>
        </div>

        {/* Right Panel */}
        <div className="space-y-6">
          {/* Pending Students */}
          <div className="bg-white rounded-lg p-6 shadow-sm">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold">Estudiantes pendientes de asignar</h3>
              <button className="text-[#1F6662] text-sm hover:underline">Ver todos</button>
            </div>
            <div className="space-y-3">
              {pendingStudents.map((student, index) => (
                <div key={index} className="p-3 bg-[#FFF3D6] border-l-4 border-[#986314] rounded-lg">
                  <div className="flex items-start justify-between mb-1">
                    <div>
                      <p className="text-sm font-medium">{student.name}</p>
                      <p className="text-xs text-[#53716E]">{student.id}</p>
                    </div>
                  </div>
                  <p className="text-xs text-[#53716E] mt-1">{student.reason}</p>
                  <button className="text-xs text-[#1F6662] hover:text-[#1C4543] mt-2 font-medium">
                    Asignar tutor →
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="bg-white rounded-lg p-6 shadow-sm">
            <h3 className="font-semibold mb-4">Acciones rápidas</h3>
            <div className="space-y-3">
              <button className="w-full flex items-center gap-3 p-3 bg-[#F2FBF9] rounded-lg hover:bg-[#D3F4EE] transition-colors">
                <div className="bg-[#1F6662] p-2 rounded-lg">
                  <UserPlus className="w-5 h-5 text-white" />
                </div>
                <div className="text-left">
                  <p className="text-sm font-medium">Asignar tutor</p>
                  <p className="text-xs text-[#53716E]">Crear nueva asignación</p>
                </div>
              </button>

              <button className="w-full flex items-center gap-3 p-3 bg-[#E4F3ED] rounded-lg hover:bg-[#E4F3ED] transition-colors">
                <div className="bg-[#227F78] p-2 rounded-lg">
                  <FileText className="w-5 h-5 text-white" />
                </div>
                <div className="text-left">
                  <p className="text-sm font-medium">Generar reporte</p>
                  <p className="text-xs text-[#53716E]">Exportar asignaciones</p>
                </div>
              </button>

              <button className="w-full flex items-center gap-3 p-3 bg-[#F2EDF8] rounded-lg hover:bg-[#F2EDF8] transition-colors">
                <div className="bg-[#6F548F] p-2 rounded-lg">
                  <BarChart3 className="w-5 h-5 text-white" />
                </div>
                <div className="text-left">
                  <p className="text-sm font-medium">Ver estadísticas</p>
                  <p className="text-xs text-[#53716E]">Análisis detallado</p>
                </div>
              </button>

              <button className="w-full flex items-center gap-3 p-3 bg-[#FFF3D6] rounded-lg hover:bg-[#FFF3D6] transition-colors">
                <div className="bg-[#986314] p-2 rounded-lg">
                  <Mail className="w-5 h-5 text-white" />
                </div>
                <div className="text-left">
                  <p className="text-sm font-medium">Enviar notificación</p>
                  <p className="text-xs text-[#53716E]">Avisar a tutores</p>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
