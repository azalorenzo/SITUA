import { useState } from 'react';
import { Search, Filter, Eye, Calendar, X, Clock, FileText, AlertCircle, TrendingUp, CheckCircle, Loader2 } from 'lucide-react';


// Detecta el periodo según el mes actual
function getPeriodo(): string {
  const mes = new Date().getMonth() + 1; // 1-12
  if (mes >= 1 && mes <= 6) return 'Enero – Junio ' + new Date().getFullYear();
  return 'Agosto – Diciembre ' + new Date().getFullYear();
}

interface Student {
  id: number;
  name: string;
  career: string;
  semester: string;
  average: number;
  riesgo: 'Alto' | 'Medio' | 'Bajo';
  condicion: string;
  email: string;
  phone: string;
  tutoringSessions: number;
  lastSession: string;
}

export function MyStudentsView() {
  const [searchQuery, setSearchQuery]       = useState('');
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [filterStatus, setFilterStatus]     = useState<string>('Todos');

  // Modal Registrar
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [registerStudent, setRegisterStudent]     = useState<Student | null>(null);
  const [regTema, setRegTema]       = useState('');
  const [regFecha, setRegFecha]     = useState(new Date().toISOString().split('T')[0]);
  const [regGuardando, setRegGuardando] = useState(false);
  const [regExito, setRegExito]     = useState(false);
  const [regError, setRegError]     = useState('');

  const periodo = getPeriodo();

  const students: Student[] = [
    { id: 1, name: 'Ana Laura Gómez',       career: 'Ingeniería en Software', semester: '4to Semestre', average: 7.2, riesgo: 'Bajo',  condicion: 'Materias en curso normal',                                              email: 'ana.gomez@universidad.edu',      phone: '555-0101', tutoringSessions: 8,  lastSession: '15 mayo 2026' },
    { id: 2, name: 'Carlos Méndez Ruiz',    career: 'Ingeniería Industrial',  semester: '5to Semestre', average: 8.1, riesgo: 'Medio', condicion: 'Materias en repetición',                                                email: 'carlos.mendez@universidad.edu',  phone: '555-0102', tutoringSessions: 12, lastSession: '18 mayo 2026' },
    { id: 3, name: 'María Fernández López', career: 'Arquitectura',           semester: '3er Semestre', average: 8.5, riesgo: 'Medio', condicion: '2 materias reprobadas',                                                 email: 'maria.fernandez@universidad.edu',phone: '555-0103', tutoringSessions: 6,  lastSession: '20 mayo 2026' },
    { id: 4, name: 'Roberto Silva García',  career: 'Diseño Gráfico',         semester: '6to Semestre', average: 9.2, riesgo: 'Alto',  condicion: 'Materia en segundo recurso / especial',                                 email: 'roberto.silva@universidad.edu',  phone: '555-0104', tutoringSessions: 15, lastSession: '22 mayo 2026' },
    { id: 5, name: 'Laura Hernández Cruz',  career: 'Ingeniería en Software', semester: '4to Semestre', average: 9.0, riesgo: 'Alto',  condicion: 'Más de dos materias reprobadas',                                        email: 'laura.hernandez@universidad.edu',phone: '555-0105', tutoringSessions: 10, lastSession: '21 mayo 2026' },
    { id: 6, name: 'Pedro Sánchez Ruiz',    career: 'Ingeniería Civil',       semester: '5to Semestre', average: 7.5, riesgo: 'Bajo',  condicion: 'Materias en curso normal',                                              email: 'pedro.sanchez@universidad.edu',  phone: '555-0106', tutoringSessions: 9,  lastSession: '19 mayo 2026' },
    { id: 7, name: 'Diana Morales Torres',  career: 'Administración',         semester: '3er Semestre', average: 8.3, riesgo: 'Medio', condicion: 'Materias en repetición y 1 materia reprobada',                          email: 'diana.morales@universidad.edu',  phone: '555-0107', tutoringSessions: 7,  lastSession: '17 mayo 2026' },
    { id: 8, name: 'Miguel Ángel Vargas',   career: 'Ingeniería Mecánica',    semester: '6to Semestre', average: 8.8, riesgo: 'Alto',  condicion: 'Materia en segundo recurso / especial y más de dos materias reprobadas', email: 'miguel.vargas@universidad.edu',  phone: '555-0108', tutoringSessions: 14, lastSession: '23 mayo 2026' },
  ];

  const tutoringHistory = [
    { date: '15 mayo 2026', topic: 'Cálculo diferencial', duration: '1 hora',   notes: 'Estudiante muestra dificultad en derivadas. Se recomienda práctica adicional.', status: 'Completada' },
    { date: '8 mayo 2026',  topic: 'Álgebra lineal',      duration: '1.5 horas', notes: 'Buena comprensión de matrices. Necesita refuerzo en determinantes.',             status: 'Completada' },
    { date: '1 mayo 2026',  topic: 'Programación básica', duration: '2 horas',   notes: 'Excelente progreso en estructuras de control. Se avanzó a funciones.',           status: 'Completada' },
    { date: '24 abril 2026',topic: 'Física I',             duration: '1 hora',   notes: 'Revisión de cinemática. Estudiante logró resolver problemas complejos.',          status: 'Completada' },
  ];

  const observations = [
    { date: '18 mayo 2026', type: 'Progreso', text: 'El estudiante ha mostrado mejora significativa en las últimas 2 semanas.', priority: 'normal' },
    { date: '10 mayo 2026', type: 'Alerta',   text: 'Requiere atención especial en matemáticas. Promedio bajo en últimos exámenes.', priority: 'high' },
    { date: '3 mayo 2026',  type: 'Nota',     text: 'Estudiante interesado en proyectos extracurriculares de programación.', priority: 'normal' },
  ];

  const getRiesgoColor = (riesgo: string) => {
    switch (riesgo) {
      case 'Bajo':  return 'bg-[#E4F3ED] text-[#227F78]';
      case 'Medio': return 'bg-[#FFF3D6] text-[#986314]';
      case 'Alto':  return 'bg-[#FCEBED] text-[#B83F4B]';
      default:      return 'bg-[#DCE3EA] text-[#0B2828]';
    }
  };

  const getAverageColor = (avg: number) => {
    if (avg >= 9) return 'text-[#227F78]';
    if (avg >= 8) return 'text-[#986314]';
    return 'text-[#B83F4B]';
  };

  const filteredStudents = students.filter(s => {
    const matchSearch = s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                        s.career.toLowerCase().includes(searchQuery.toLowerCase());
    const matchFilter = filterStatus === 'Todos' || s.riesgo === filterStatus;
    return matchSearch && matchFilter;
  });

  // ── Abrir modal registrar ───────────────────────────────────────────────────
  const openRegister = (student: Student) => {
    setRegisterStudent(student);
    setRegTema('');
    setRegFecha(new Date().toISOString().split('T')[0]);
    setRegGuardando(false);
    setRegExito(false);
    setRegError('');
    setShowRegisterModal(true);
  };

  const handleRegistrar = () => {
    if (!regTema.trim()) { setRegError('El tema es obligatorio.'); return; }
    if (!regFecha) { setRegError('La fecha es obligatoria.'); return; }
    setRegError(''); setRegExito(true);
    setTimeout(() => { setShowRegisterModal(false); setRegExito(false); }, 1800);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Mis tutorados</h2>
          <p className="text-sm text-[#53716E]">Gestiona y da seguimiento a tus estudiantes</p>
        </div>
        <span className="text-sm text-[#53716E]">
          Total: <span className="font-semibold">{students.length}</span> estudiantes
        </span>
      </div>

      {/* Search and Filters */}
      <div className="bg-white rounded-lg p-6 shadow-sm">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por nombre o carrera..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5 text-[#53716E]" />
            <select
              value={filterStatus}
              onChange={e => setFilterStatus(e.target.value)}
              className="border border-[#DCE3EA] rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            >
              <option>Todos</option>
              <option>Alto</option>
              <option>Medio</option>
              <option>Bajo</option>
            </select>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]">
              <tr>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Nombre</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Carrera</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Semestre</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Promedio</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Riesgo académico</th>
                <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {filteredStudents.map(student => (
                <tr key={student.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium text-sm">
                        {student.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                      </div>
                      <div>
                        <p className="font-medium text-sm">{student.name}</p>
                        <p className="text-xs text-[#53716E]">{student.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-4 px-6 text-sm text-[#0B2828]">{student.career}</td>
                  <td className="py-4 px-6 text-sm text-[#0B2828]">{student.semester}</td>
                  <td className="py-4 px-6">
                    <span className={`font-semibold ${getAverageColor(student.average)}`}>
                      {student.average.toFixed(1)}
                    </span>
                  </td>
                  <td className="py-4 px-6">
                    <div className="flex flex-col gap-1">
                      <span className={`text-xs px-3 py-1 rounded-full font-medium w-fit ${getRiesgoColor(student.riesgo)}`}>
                        {student.riesgo}
                      </span>
                      <span className="text-xs text-[#53716E]">{student.condicion}</span>
                    </div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="flex gap-2">
                      <button
                        onClick={() => setSelectedStudent(student)}
                        className="flex items-center gap-1 px-3 py-1.5 bg-[#F2FBF9] text-[#1F6662] rounded-lg hover:bg-[#D3F4EE] text-sm"
                      >
                        <Eye className="w-4 h-4" />
                        Ver detalle
                      </button>
                      <button
                        onClick={() => openRegister(student)}
                        className="flex items-center gap-1 px-3 py-1.5 bg-[#E4F3ED] text-[#227F78] rounded-lg hover:bg-[#E4F3ED] text-sm"
                      >
                        <Calendar className="w-4 h-4" />
                        Registrar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredStudents.length === 0 && (
          <div className="text-center py-12">
            <p className="text-[#53716E]">No se encontraron estudiantes con estos criterios.</p>
          </div>
        )}
      </div>

      {/* ── Modal Ver Detalle ─────────────────────────────────────────────────── */}
      {selectedStudent && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            {/* Header */}
            <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-start">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-semibold text-xl">
                  {selectedStudent.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
                </div>
                <div>
                  <h3 className="font-semibold mb-1">{selectedStudent.name}</h3>
                  <p className="text-sm text-[#53716E]">{selectedStudent.career}</p>
                  <div className="flex items-center gap-2 mt-2 flex-wrap">
                    <span className={`text-xs px-3 py-1 rounded-full font-medium ${getRiesgoColor(selectedStudent.riesgo)}`}>
                        Riesgo {selectedStudent.riesgo}
                      </span>
                      <span className="text-xs text-[#53716E] italic">{selectedStudent.condicion}</span>
                    <span className="text-sm text-[#53716E]">
                      Promedio: <span className={`font-semibold ${getAverageColor(selectedStudent.average)}`}>
                        {selectedStudent.average.toFixed(1)}
                      </span>
                    </span>
                    {/* PERIODO */}
                    <span className="text-xs px-3 py-1 rounded-full font-medium bg-[#D3F4EE] text-[#1C4543] flex items-center gap-1">
                      <FileText className="w-3 h-3" />
                      Periodo: {periodo}
                    </span>
                  </div>
                </div>
              </div>
              <button onClick={() => setSelectedStudent(null)} className="text-[#53716E] hover:text-[#53716E] p-1">
                <X className="w-6 h-6" />
              </button>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Stats row */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div className="bg-[#F2FBF9] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Clock className="w-4 h-4 text-[#1F6662]" />
                    <p className="text-sm font-medium text-[#0B2828]">Tutorías</p>
                  </div>
                  <p className="font-semibold text-[#1F6662]">{selectedStudent.tutoringSessions}</p>
                </div>
                <div className="bg-[#E4F3ED] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Calendar className="w-4 h-4 text-[#227F78]" />
                    <p className="text-sm font-medium text-[#227F78]">Última sesión</p>
                  </div>
                  <p className="font-semibold text-[#227F78] text-sm">{selectedStudent.lastSession}</p>
                </div>
                <div className="bg-[#F2EDF8] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <TrendingUp className="w-4 h-4 text-[#6F548F]" />
                    <p className="text-sm font-medium text-[#6F548F]">Semestre</p>
                  </div>
                  <p className="font-semibold text-[#6F548F]">{selectedStudent.semester}</p>
                </div>
                {/* Periodo destacado */}
                <div className="bg-[#F2FBF9] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <FileText className="w-4 h-4 text-[#1F6662]" />
                    <p className="text-sm font-medium text-[#0B2828]">Periodo activo</p>
                  </div>
                  <p className="font-semibold text-[#1F6662] text-sm">{periodo}</p>
                </div>
              </div>

              {/* Contacto */}
              <div className="bg-[#F5F7FA] rounded-lg p-4">
                <h4 className="font-semibold mb-3">Información de contacto</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <p className="text-sm text-[#53716E] mb-1">Correo electrónico</p>
                    <p className="text-sm font-medium">{selectedStudent.email}</p>
                  </div>
                  <div>
                    <p className="text-sm text-[#53716E] mb-1">Teléfono</p>
                    <p className="text-sm font-medium">{selectedStudent.phone}</p>
                  </div>
                </div>
              </div>

              {/* Historial tutorías */}
              <div>
                <h4 className="font-semibold mb-3">Historial de tutorías — {periodo}</h4>
                <div className="space-y-3">
                  {tutoringHistory.map((session, i) => (
                    <div key={i} className="border border-[#DCE3EA] rounded-lg p-4">
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h5 className="font-medium text-sm">{session.topic}</h5>
                          <div className="flex items-center gap-3 mt-1">
                            <span className="text-xs text-[#53716E]">{session.date}</span>
                            <span className="text-xs text-[#53716E]">• {session.duration}</span>
                          </div>
                        </div>
                        <span className="text-xs px-2 py-1 bg-[#E4F3ED] text-[#227F78] rounded-full">
                          {session.status}
                        </span>
                      </div>
                      <p className="text-sm text-[#53716E] mt-2">{session.notes}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* Observaciones */}
              <div>
                <h4 className="font-semibold mb-3">Observaciones</h4>
                <div className="space-y-3">
                  {observations.map((obs, i) => (
                    <div key={i} className={`border-l-4 rounded-lg p-4 ${obs.priority === 'high' ? 'border-[#B83F4B] bg-[#FCEBED]' : 'border-[#1F6662] bg-[#F2FBF9]'}`}>
                      <div className="flex items-start gap-3">
                        <AlertCircle className={`w-5 h-5 mt-0.5 ${obs.priority === 'high' ? 'text-[#B83F4B]' : 'text-[#1F6662]'}`} />
                        <div className="flex-1">
                          <div className="flex justify-between items-start mb-1">
                            <span className="font-medium text-sm">{obs.type}</span>
                            <span className="text-xs text-[#53716E]">{obs.date}</span>
                          </div>
                          <p className="text-sm text-[#0B2828]">{obs.text}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Footer */}
            <div className="p-6 border-t border-[#DCE3EA] flex justify-end gap-3">
              <button
                onClick={() => setSelectedStudent(null)}
                className="px-6 py-2 border border-[#DCE3EA] text-[#0B2828] rounded-lg hover:bg-[#F5F7FA]"
              >
                Cerrar
              </button>
              <button
                onClick={() => { setSelectedStudent(null); openRegister(selectedStudent); }}
                className="px-6 py-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543] flex items-center gap-2"
              >
                <Calendar className="w-4 h-4" />
                Registrar tutoría
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Modal Registrar Sesión ────────────────────────────────────────────── */}
      {showRegisterModal && registerStudent && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <div className="flex justify-between items-start mb-5">
              <div>
                <h3 className="font-semibold">Registrar tutoría</h3>
                <p className="text-sm text-[#53716E] mt-0.5">{registerStudent.name}</p>
                <span className="text-xs px-2 py-0.5 rounded-full bg-[#D3F4EE] text-[#1C4543] inline-block mt-1">
                  Periodo: {periodo}
                </span>
              </div>
              <button onClick={() => setShowRegisterModal(false)} className="text-[#53716E] hover:text-[#53716E]">
                <X className="w-5 h-5" />
              </button>
            </div>

            {regExito ? (
              <div className="flex flex-col items-center py-8 gap-3 text-[#227F78]">
                <CheckCircle className="w-12 h-12" />
                <p className="font-semibold">¡Sesión registrada correctamente!</p>
              </div>
            ) : (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-[#0B2828] mb-1">Tema de la sesión</label>
                  <input
                    type="text"
                    value={regTema}
                    onChange={e => setRegTema(e.target.value)}
                    placeholder="Ej: Cálculo diferencial, Álgebra lineal..."
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#0B2828] mb-1">Fecha</label>
                  <input
                    type="date"
                    value={regFecha}
                    onChange={e => setRegFecha(e.target.value)}
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  />
                </div>

                {regError && (
                  <p className="text-sm text-[#B83F4B] bg-[#FCEBED] rounded-lg px-3 py-2">{regError}</p>
                )}

                <div className="flex gap-3 pt-2">
                  <button
                    onClick={() => setShowRegisterModal(false)}
                    className="flex-1 px-4 py-2 border border-[#DCE3EA] text-[#0B2828] rounded-lg hover:bg-[#F5F7FA]"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleRegistrar}
                    disabled={regGuardando}
                    className="flex-1 px-4 py-2 bg-[#227F78] text-white rounded-lg hover:bg-[#227F78] disabled:opacity-60 flex items-center justify-center gap-2"
                  >
                    {regGuardando
                      ? <><Loader2 className="w-4 h-4 animate-spin" /> Guardando...</>
                      : <><Calendar className="w-4 h-4" /> Registrar</>
                    }
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
