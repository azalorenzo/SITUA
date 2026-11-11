import { mockReports, showMockAction } from '../mock/data';
import { useState } from 'react';
import { Users, CheckCircle, AlertTriangle, Download, FileText, TrendingUp, Loader2 } from 'lucide-react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';


interface Reporte {
  id_reporte: string;
  estudiante: string;
  tutor: string;
  fecha: string;
  estatus: string;
  motivo: string;
}

export function TutorReportsView() {
  const [reportes, setReportes] = useState<Reporte[]>(mockReports);
  const [cargando, setCargando] = useState(false);
  const [exportando, setExportando] = useState<string | null>(null);
  const [error, setError] = useState('');

  const performanceData = [
    { mes: 'Ene', promedio: 8.2 },
    { mes: 'Feb', promedio: 8.5 },
    { mes: 'Mar', promedio: 8.3 },
    { mes: 'Abr', promedio: 8.7 },
    { mes: 'May', promedio: 8.9 }
  ];

  const progressData = [
    { estudiante: 'Ana Laura', inicial: 7.2, actual: 7.8 },
    { estudiante: 'Carlos', inicial: 8.0, actual: 8.5 },
    { estudiante: 'María', inicial: 8.3, actual: 8.7 },
    { estudiante: 'Roberto', inicial: 9.0, actual: 9.3 },
    { estudiante: 'Laura', inicial: 8.8, actual: 9.1 }
  ];

  const exportarPDF = (_idReporte: string) => showMockAction();
  const exportarTodos = () => showMockAction();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Completado': return 'bg-[#E4F3ED] text-[#227F78]';
      case 'Pendiente':  return 'bg-[#FFF3D6] text-[#986314]';
      default:           return 'bg-[#DCE3EA] text-[#0B2828]';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Reportes y análisis</h2>
          <p className="text-sm text-[#53716E]">Dashboard analítico de desempeño</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={exportarTodos}
            disabled={cargando || reportes.length === 0}
            className="flex items-center gap-2 px-4 py-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <FileText className="w-4 h-4" />
            Generar reporte
          </button>
          <button
            onClick={exportarTodos}
            disabled={cargando || reportes.length === 0}
            className="flex items-center gap-2 px-4 py-2 border border-[#DCE3EA] text-[#0B2828] rounded-lg hover:bg-[#F5F7FA] disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Download className="w-4 h-4" />
            Exportar PDF
          </button>
        </div>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-lg p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="bg-white bg-opacity-20 p-3 rounded-lg">
              <CheckCircle className="w-6 h-6" />
            </div>
            <TrendingUp className="w-5 h-5" />
          </div>
          <p className="text-[#D3F4EE] text-sm mb-1">Total de reportes</p>
          <p className="font-semibold mb-2">{cargando ? '...' : `${reportes.length} reportes`}</p>
          <p className="text-xs text-[#D3F4EE]">
            {reportes.filter(r => r.estatus === 'Completado').length} completados
          </p>
        </div>

        <div className="bg-gradient-to-br from-[#227F78] to-[#227F78] rounded-lg p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="bg-white bg-opacity-20 p-3 rounded-lg">
              <Users className="w-6 h-6" />
            </div>
            <TrendingUp className="w-5 h-5" />
          </div>
          <p className="text-[#E4F3ED] text-sm mb-1">Estudiantes atendidos</p>
          <p className="font-semibold mb-2">
            {cargando ? '...' : `${new Set(reportes.map(r => r.estudiante)).size} estudiantes`}
          </p>
          <p className="text-xs text-[#E4F3ED]">100% de asistencia</p>
        </div>

        <div className="bg-gradient-to-br from-[#986314] to-[#986314] rounded-lg p-6 text-white shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="bg-white bg-opacity-20 p-3 rounded-lg">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <span className="text-xs bg-white bg-opacity-20 px-2 py-1 rounded-full">Atención</span>
          </div>
          <p className="text-[#FFF3D6] text-sm mb-1">Reportes pendientes</p>
          <p className="font-semibold mb-2">
            {cargando ? '...' : `${reportes.filter(r => r.estatus === 'Pendiente').length} pendientes`}
          </p>
          <p className="text-xs text-[#FFF3D6]">Requieren seguimiento</p>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <h3 className="font-semibold mb-4">Rendimiento académico promedio</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={performanceData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA" />
              <XAxis dataKey="mes" stroke="#53716E" />
              <YAxis domain={[7, 10]} stroke="#53716E" />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="promedio"
                name="Promedio"
                stroke="#1F6662"
                strokeWidth={3}
                dot={{ fill: '#1F6662', r: 5 }}
                activeDot={{ r: 7 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <h3 className="font-semibold mb-4">Progreso de estudiantes</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={progressData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA" />
              <XAxis dataKey="estudiante" stroke="#53716E" />
              <YAxis domain={[6, 10]} stroke="#53716E" />
              <Tooltip />
              <Legend />
              <Bar dataKey="inicial" name="Inicial" fill="#53716E" />
              <Bar dataKey="actual" name="Actual" fill="#1F6662" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Tabla de reportes de ejemplo */}
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-center">
          <h3 className="font-semibold">Reportes registrados</h3>
          {error && <p className="text-sm text-[#B83F4B]">{error}</p>}
        </div>

        {cargando ? (
          <div className="flex items-center justify-center py-16 gap-3 text-[#53716E]">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span className="text-sm">Cargando reportes...</span>
          </div>
        ) : reportes.length === 0 ? (
          <div className="text-center py-16 text-[#53716E] text-sm">
            No hay reportes registrados aún.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]">
                <tr>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Estudiante</th>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Tutor</th>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Fecha</th>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Motivo</th>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Estatus</th>
                  <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Reporte</th>
                </tr>
              </thead>
              <tbody>
                {reportes.map((reporte) => (
                  <tr key={reporte.id_reporte} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white text-xs font-medium flex-shrink-0">
                          {reporte.estudiante
                            .split(' ')
                            .map((n) => n[0])
                            .join('')
                            .slice(0, 2)
                            .toUpperCase()}
                        </div>
                        <span className="font-medium text-sm">{reporte.estudiante}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-sm text-[#0B2828]">{reporte.tutor}</td>
                    <td className="py-4 px-6 text-sm text-[#0B2828]">{reporte.fecha}</td>
                    <td className="py-4 px-6 text-sm text-[#53716E] max-w-[200px] truncate" title={reporte.motivo}>
                      {reporte.motivo}
                    </td>
                    <td className="py-4 px-6">
                      <span className={`text-xs px-3 py-1 rounded-full font-medium ${getStatusColor(reporte.estatus)}`}>
                        {reporte.estatus}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      <button
                        onClick={() => exportarPDF(reporte.id_reporte)}
                        disabled={exportando === reporte.id_reporte}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-[#1F6662] text-white text-xs rounded-lg hover:bg-[#1C4543] disabled:opacity-60 disabled:cursor-not-allowed transition-colors"
                      >
                        {exportando === reporte.id_reporte ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        ) : (
                          <Download className="w-3.5 h-3.5" />
                        )}
                        PDF
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm border-l-4 border-[#1F6662]">
          <p className="text-sm text-[#53716E] mb-1">Promedio general</p>
          <p className="font-semibold">8.66</p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm border-l-4 border-[#227F78]">
          <p className="text-sm text-[#53716E] mb-1">Tasa de aprobación</p>
          <p className="font-semibold">94%</p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm border-l-4 border-[#6F548F]">
          <p className="text-sm text-[#53716E] mb-1">Horas de tutoría</p>
          <p className="font-semibold">42h</p>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm border-l-4 border-[#986314]">
          <p className="text-sm text-[#53716E] mb-1">Satisfacción</p>
          <p className="font-semibold">4.8/5.0</p>
        </div>
      </div>
    </div>
  );
}
