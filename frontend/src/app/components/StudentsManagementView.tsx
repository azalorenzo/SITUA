import { mockStudents } from '../mock/data';
import { Users, CheckCircle, TrendingUp, Clock, AlertTriangle, Search, Filter, Eye, Edit, Trash2, Plus } from 'lucide-react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { useState } from "react";

export function StudentsManagementView() {
  const [alumnos, setAlumnos] = useState(mockStudents);
  const riskData = [
    { id: '1', name: 'Sin riesgo', value: 203, color: '#1F6662' },
    { id: '2', name: 'Riesgo bajo', value: 187, color: '#227F78' },
    { id: '3', name: 'Riesgo medio', value: 95, color: '#986314' },
    { id: '4', name: 'Riesgo alto', value: 38, color: '#B83F4B' }
  ];

  const semesterData = [
    { semestre: '1er', estudiantes: 120 },
    { semestre: '2do', estudiantes: 98 },
    { semestre: '3er', estudiantes: 145 },
    { semestre: '4to', estudiantes: 87 },
    { semestre: '5to', estudiantes: 42 },
    { semestre: '6to', estudiantes: 31 }
  ];



  const atRiskStudents = [
    { name: 'Pedro Sánchez Ruiz', reason: 'Promedio bajo (7.2)', level: 'Alto' },
    { name: 'Diana Morales Torres', reason: 'Ausentismo frecuente', level: 'Alto' },
    { name: 'Jorge Ramírez Luna', reason: 'Bajo rendimiento matemáticas', level: 'Medio' }
  ];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Sin riesgo':
        return 'bg-[#D3F4EE] text-[#1C4543]';
      case 'Riesgo bajo':
        return 'bg-[#E4F3ED] text-[#227F78]';
      case 'Riesgo medio':
        return 'bg-[#FFF3D6] text-[#986314]';
      case 'Riesgo alto':
        return 'bg-[#FCEBED] text-[#B83F4B]';
      default:
        return 'bg-[#DCE3EA] text-[#0B2828]';
    }
  };

  const getAverageColor = (average: number) => {
    if (average >= 9) return 'text-[#227F78]';
    if (average >= 8) return 'text-[#1F6662]';
    if (average >= 7) return 'text-[#986314]';
    return 'text-[#B83F4B]';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Gestión de Estudiantes</h2>
          <p className="text-sm text-[#53716E]">Administra y monitorea el desempeño de estudiantes</p>
        </div>
        <button className="flex items-center gap-2 bg-[#1F6662] text-white px-4 py-2 rounded-lg hover:bg-[#1C4543]">
          <Plus className="w-4 h-4" />
          Agregar estudiante
        </button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Users className="w-5 h-5 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Total estudiantes</p>
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
              <p className="text-sm text-[#53716E]">Estudiantes activos</p>
              <p className="font-semibold">38</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FFF3D6] p-3 rounded-lg">
              <TrendingUp className="w-5 h-5 text-[#986314]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Promedio académico</p>
              <p className="font-semibold">8.4</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2EDF8] p-3 rounded-lg">
              <Clock className="w-5 h-5 text-[#6F548F]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Total de horas</p>
              <p className="font-semibold">1,248</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FCEBED] p-3 rounded-lg">
              <AlertTriangle className="w-5 h-5 text-[#B83F4B]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Estudiantes en riesgo</p>
              <p className="font-semibold">12</p>
            </div>
          </div>
        </div>
      </div>

      {/* Charts and At Risk Students */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Risk Distribution */}
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <h3 className="font-semibold mb-4">Estudiantes por nivel de riesgo</h3>
          <ResponsiveContainer width="100%" height={200}>
            <PieChart>
              <Pie
                data={riskData}
                cx="50%"
                cy="50%"
                innerRadius={50}
                outerRadius={80}
                paddingAngle={3}
                dataKey="value"
                nameKey="name"
              >
                {riskData.map((entry) => (
                  <Cell key={entry.id} fill={entry.color} />
                ))}
              </Pie>
              <Legend
                verticalAlign="bottom"
                height={36}
                formatter={(value) => value}
                iconSize={10}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Semester Distribution */}
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <h3 className="font-semibold mb-4">Distribución por semestre</h3>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={semesterData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA" />
              <XAxis dataKey="semestre" stroke="#53716E" />
              <YAxis stroke="#53716E" />
              <Tooltip />
              <Bar dataKey="estudiantes" fill="#1F6662" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* At Risk Students */}
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold">Estudiantes en riesgo</h3>
            <button className="text-[#1F6662] text-sm hover:underline">Ver todos</button>
          </div>
          <div className="space-y-3">
            {atRiskStudents.map((student, index) => (
              <div key={index} className={`p-3 rounded-lg border-l-4 ${
                student.level === 'Alto' ? 'border-[#B83F4B] bg-[#FCEBED]' : 'border-[#986314] bg-[#FFF3D6]'
              }`}>
                <div className="flex items-start justify-between mb-1">
                  <p className="text-sm font-medium">{student.name}</p>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${
                    student.level === 'Alto' ? 'bg-[#FCEBED] text-[#B83F4B]' : 'bg-[#FFF3D6] text-[#986314]'
                  }`}>
                    {student.level}
                  </span>
                </div>
                <p className="text-xs text-[#53716E]">{student.reason}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Students Table */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-center">
          <h3 className="font-semibold">Lista de estudiantes</h3>
          <div className="flex gap-3">
            <div className="relative">
              <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 transform -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar estudiante..."
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
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Estudiante</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Matrícula</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Carrera</th>
                // <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Semestre</th>
                //<th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Promedio</th>
                //<th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Tutor</th>
                //<th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Estado</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {alumnos.map((alumno: any) => (
                <tr key={alumno.nombre} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium">
                        {alumno.nombre
                        .split(' ')
                        .map((n: string) => n[0])
                        .join('')
                        .slice(0, 2)}
                      </div>
                      <span className="font-medium text-sm">{alumno.nombre}</span>
                    </div>
                  </td>
                  <td className="py-4 px-6 text-sm text-[#53716E]">{alumno.matricula}</td>
                  <td className="py-4 px-6 text-sm text-[#0B2828]">{alumno.carrera}</td>
                  

                  <td className="py-4 px-6">
                    <div className="flex gap-2">
                      <button className="p-1 hover:bg-[#DCE3EA] rounded">
                        <Eye className="w-4 h-4 text-[#53716E]" />
                      </button>
                      <button className="p-1 hover:bg-[#DCE3EA] rounded">
                        <Edit className="w-4 h-4 text-[#53716E]" />
                      </button>
                      <button className="p-1 hover:bg-[#DCE3EA] rounded">
                        <Trash2 className="w-4 h-4 text-[#B83F4B]" />
                      </button>
                    </div>
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
