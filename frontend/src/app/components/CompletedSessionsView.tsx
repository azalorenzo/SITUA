import { useState } from 'react';
import { Search, Filter, Eye, Calendar, X, CheckCircle, XCircle, Clock, FileText, Plus, Loader2, User } from 'lucide-react';


interface Session {
  id: number;
  date: string;
  time: string;
  student: string;
  topic: string;
  observations: string;
  status: 'Completada' | 'Cancelada';
  duration: string;
  location: string;
  materials: string[];
}

const STUDENTS = [
  'Ana Laura Gómez',
  'Carlos Méndez Ruiz',
  'María Fernández López',
  'Roberto Silva García',
  'Laura Hernández Cruz',
  'Pedro Sánchez Ruiz',
  'Diana Morales Torres',
  'Miguel Ángel Vargas',
];

export function CompletedSessionsView() {
  const [searchQuery, setSearchQuery]   = useState('');
  const [filterStatus, setFilterStatus] = useState('Todas');
  const [filterDate, setFilterDate]     = useState('');
  const [selectedSession, setSelectedSession] = useState<Session | null>(null);

  // ── Formulario inline ──────────────────────────────────────────────────────
  const [sessions, setSessions] = useState<Session[]>([
    { id: 1, date: '29 mayo 2026', time: '10:00', student: 'Ana Laura Gómez',     topic: 'Cálculo diferencial - Derivadas',        observations: 'La estudiante muestra dificultad en derivadas parciales. Se recomienda práctica adicional.', status: 'Completada', duration: '1.5 horas', location: 'Aula 301',  materials: ['Ejercicios de práctica', 'Guía de derivadas'] },
    { id: 2, date: '26 mayo 2026', time: '14:00', student: 'Carlos Méndez Ruiz',  topic: 'Álgebra lineal - Matrices',              observations: 'Excelente progreso. El estudiante comprendió operaciones con matrices.',                       status: 'Completada', duration: '2 horas',   location: 'Virtual',   materials: ['Presentación PPT', 'Ejercicios resueltos'] },
    { id: 3, date: '25 mayo 2026', time: '11:00', student: 'María Fernández López',topic: 'Física I - Cinemática',                  observations: 'Sesión cancelada por el estudiante. Se reprogramó para la próxima semana.',                    status: 'Cancelada',  duration: '1 hora',    location: 'Aula 205',  materials: [] },
    { id: 4, date: '20 mayo 2026', time: '09:00', student: 'Roberto Silva García', topic: 'Programación - Estructuras de datos',   observations: 'Se cubrieron listas enlazadas y árboles binarios con implementación en Python.',               status: 'Completada', duration: '2 horas',   location: 'Virtual',   materials: ['Código fuente', 'Documentación técnica'] },
  ]);

  const [formFecha,   setFormFecha]   = useState(new Date().toISOString().split('T')[0]);
  const [formHora,    setFormHora]    = useState('09:00');
  const [formAlumno,  setFormAlumno]  = useState('');
  const [formTema,    setFormTema]    = useState('');
  const [formObs,     setFormObs]     = useState('');
  const [formLugar,   setFormLugar]   = useState('');
  const [guardando,   setGuardando]   = useState(false);
  const [exito,       setExito]       = useState(false);
  const [formError,   setFormError]   = useState('');


  const getStatusColor = (s: string) =>
    s === 'Completada' ? 'bg-[#E4F3ED] text-[#227F78]' : 'bg-[#FCEBED] text-[#B83F4B]';
  const getStatusIcon = (s: string) =>
    s === 'Completada' ? CheckCircle : XCircle;

  const filteredSessions = sessions.filter(s => {
    const matchSearch = s.student.toLowerCase().includes(searchQuery.toLowerCase()) ||
                        s.topic.toLowerCase().includes(searchQuery.toLowerCase());
    const matchStatus = filterStatus === 'Todas' || s.status === filterStatus;
    const matchDate   = !filterDate || s.date.includes(filterDate);
    return matchSearch && matchStatus && matchDate;
  });

  // ── Guardar sesión ─────────────────────────────────────────────────────────
  const handleRegistrar = () => {
    if (!formAlumno) { setFormError('Selecciona un alumno.'); return; }
    if (!formTema.trim()) { setFormError('El tema es obligatorio.'); return; }
    if (!formFecha)   { setFormError('La fecha es obligatoria.'); return; }

    setFormError('');
    // Agregar a la lista local inmediatamente
    const nueva: Session = {
      id:           Date.now(),
      date:         new Date(formFecha + 'T00:00:00').toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' }),
      time:         formHora,
      student:      formAlumno,
      topic:        formTema,
      observations: formObs,
      status:       'Completada',
      duration:     '1 hora',
      location:     formLugar || 'Por definir',
      materials:    [],
    };
        setSessions(prev => [nueva, ...prev]);
        setExito(true);
        setFormFecha(new Date().toISOString().split('T')[0]);
        setFormHora('09:00');
        setFormAlumno('');
        setFormTema('');
        setFormObs('');
        setFormLugar('');
        setTimeout(() => setExito(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Sesiones </h2>
          <p className="text-sm text-[#53716E]">Historial y registro de tutorías</p>
        </div>
        <span className="text-sm text-[#53716E]">
          Total: <span className="font-semibold">{sessions.length}</span> sesiones
        </span>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#E4F3ED] p-3 rounded-lg"><CheckCircle className="w-5 h-5 text-[#227F78]" /></div>
            <div>
              <p className="text-sm text-[#53716E]">Completadas</p>
              <p className="font-semibold">{sessions.filter(s => s.status === 'Completada').length}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FCEBED] p-3 rounded-lg"><XCircle className="w-5 h-5 text-[#B83F4B]" /></div>
            <div>
              <p className="text-sm text-[#53716E]">Canceladas</p>
              <p className="font-semibold">{sessions.filter(s => s.status === 'Cancelada').length}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg"><Clock className="w-5 h-5 text-[#1F6662]" /></div>
            <div>
              <p className="text-sm text-[#53716E]">Horas totales</p>
              <p className="font-semibold">{sessions.reduce((acc, s) => acc + parseFloat(s.duration), 0).toFixed(1)}h</p>
            </div>
          </div>
        </div>
      </div>

      {/* ── Layout principal: lista + formulario lado a lado ────────────────── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Lista de sesiones (2/3) */}
        <div className="lg:col-span-2 space-y-4">
          {/* Filtros */}
          <div className="bg-white rounded-lg p-4 shadow-sm">
            <div className="flex flex-col md:flex-row gap-3">
              <div className="flex-1 relative">
                <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Buscar por estudiante o tema..."
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662] text-sm"
                />
              </div>
              <input
                type="date"
                value={filterDate}
                onChange={e => setFilterDate(e.target.value)}
                className="border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662] text-sm"
              />
              <select
                value={filterStatus}
                onChange={e => setFilterStatus(e.target.value)}
                className="border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662] text-sm"
              >
                <option>Todas</option>
                <option>Completada</option>
                <option>Cancelada</option>
              </select>
            </div>
          </div>

          {/* Tabla */}
          <div className="bg-white rounded-lg shadow-sm overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]">
                  <tr>
                    <th className="text-left py-3 px-4 text-xs font-semibold text-[#0B2828]">Fecha / Hora</th>
                    <th className="text-left py-3 px-4 text-xs font-semibold text-[#0B2828]">Alumno</th>
                    <th className="text-left py-3 px-4 text-xs font-semibold text-[#0B2828]">Tema</th>
                    <th className="text-left py-3 px-4 text-xs font-semibold text-[#0B2828]">Estado</th>
                    <th className="text-left py-3 px-4 text-xs font-semibold text-[#0B2828]"></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSessions.map(session => {
                    const StatusIcon = getStatusIcon(session.status);
                    return (
                      <tr key={session.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
                        <td className="py-3 px-4">
                          <p className="text-sm text-[#0B2828]">{session.date}</p>
                          <p className="text-xs text-[#53716E] flex items-center gap-1 mt-0.5">
                            <Clock className="w-3 h-3" />{session.time}
                          </p>
                        </td>
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2">
                            <div className="w-7 h-7 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white text-xs font-medium flex-shrink-0">
                              {session.student.split(' ').map(n => n[0]).join('').slice(0, 2)}
                            </div>
                            <span className="text-sm font-medium">{session.student}</span>
                          </div>
                        </td>
                        <td className="py-3 px-4 text-sm text-[#0B2828] max-w-[180px] truncate" title={session.topic}>
                          {session.topic}
                        </td>
                        <td className="py-3 px-4">
                          <span className={`text-xs px-2 py-1 rounded-full font-medium flex items-center gap-1 w-fit ${getStatusColor(session.status)}`}>
                            <StatusIcon className="w-3 h-3" />{session.status}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <button
                            onClick={() => setSelectedSession(session)}
                            className="p-1.5 bg-[#F2FBF9] text-[#1F6662] rounded-lg hover:bg-[#D3F4EE]"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {filteredSessions.length === 0 && (
              <div className="text-center py-10 text-[#53716E] text-sm">No se encontraron sesiones.</div>
            )}
          </div>
        </div>

        {/* ── Formulario de registro (1/3) ──────────────────────────────────── */}
        <div className="bg-white rounded-lg shadow-sm overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-[#DCE3EA] flex items-center gap-2">
            <div className="bg-[#E4F3ED] p-2 rounded-lg">
              <Plus className="w-4 h-4 text-[#227F78]" />
            </div>
            <h3 className="font-semibold text-sm">Registrar sesión</h3>
          </div>

          <div className="p-5 flex-1 space-y-4">
            {exito && (
              <div className="flex items-center gap-2 bg-[#E4F3ED] text-[#227F78] rounded-lg px-3 py-2 text-sm">
                <CheckCircle className="w-4 h-4 flex-shrink-0" />
                ¡Sesión registrada correctamente!
              </div>
            )}

            {/* Alumno */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Alumno</label>
              <div className="relative">
                <User className="w-4 h-4 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
                <select
                  value={formAlumno}
                  onChange={e => setFormAlumno(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm"
                >
                  <option value="">Seleccionar alumno</option>
                  {STUDENTS.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
            </div>

            {/* Fecha */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Fecha</label>
              <div className="relative">
                <Calendar className="w-4 h-4 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="date"
                  value={formFecha}
                  onChange={e => setFormFecha(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm"
                />
              </div>
            </div>

            {/* Hora */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Hora</label>
              <div className="relative">
                <Clock className="w-4 h-4 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="time"
                  value={formHora}
                  onChange={e => setFormHora(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm"
                />
              </div>
            </div>

            {/* Tema */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Tema a tratar</label>
              <input
                type="text"
                value={formTema}
                onChange={e => setFormTema(e.target.value)}
                placeholder="Ej: Cálculo diferencial..."
                className="w-full px-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm"
              />
            </div>

            {/* Lugar */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Lugar</label>
              <input
                type="text"
                value={formLugar}
                onChange={e => setFormLugar(e.target.value)}
                placeholder="Ej: Aula 301, Virtual..."
                className="w-full px-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm"
              />
            </div>

            {/* Observaciones */}
            <div>
              <label className="block text-xs font-medium text-[#0B2828] mb-1">Observaciones</label>
              <textarea
                value={formObs}
                onChange={e => setFormObs(e.target.value)}
                placeholder="Notas de la sesión..."
                rows={3}
                className="w-full px-3 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#227F78] text-sm resize-none"
              />
            </div>

            {formError && (
              <p className="text-xs text-[#B83F4B] bg-[#FCEBED] rounded-lg px-3 py-2">{formError}</p>
            )}

            <button
              onClick={handleRegistrar}
              disabled={guardando}
              className="w-full py-2.5 bg-[#227F78] text-white rounded-lg hover:bg-[#227F78] disabled:opacity-60 flex items-center justify-center gap-2 font-medium text-sm"
            >
              {guardando
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Guardando...</>
                : <><Plus className="w-4 h-4" /> Registrar sesión</>
              }
            </button>
          </div>
        </div>
      </div>

      {/* Modal Ver Detalle */}
      {selectedSession && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-lg max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
            <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-start">
              <div>
                <h3 className="font-semibold mb-2">{selectedSession.topic}</h3>
                <div className="flex items-center gap-3 flex-wrap">
                  <span className="text-sm text-[#53716E] flex items-center gap-1">
                    <Calendar className="w-3.5 h-3.5" />{selectedSession.date}
                  </span>
                  <span className="text-sm text-[#53716E] flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />{selectedSession.time}
                  </span>
                  <span className={`text-xs px-3 py-1 rounded-full font-medium ${getStatusColor(selectedSession.status)}`}>
                    {selectedSession.status}
                  </span>
                </div>
              </div>
              <button onClick={() => setSelectedSession(null)} className="text-[#53716E] hover:text-[#53716E] p-1">
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              <div className="bg-[#F2FBF9] rounded-lg p-4">
                <p className="text-sm font-medium text-[#0B2828] mb-2">Estudiante</p>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium">
                    {selectedSession.student.split(' ').map(n => n[0]).join('').slice(0, 2)}
                  </div>
                  <div>
                    <p className="font-medium">{selectedSession.student}</p>
                    <p className="text-sm text-[#53716E]">{selectedSession.location}</p>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="bg-[#F5F7FA] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Clock className="w-4 h-4 text-[#53716E]" />
                    <p className="text-sm font-medium text-[#0B2828]">Duración</p>
                  </div>
                  <p className="font-semibold">{selectedSession.duration}</p>
                </div>
                <div className="bg-[#F5F7FA] rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Calendar className="w-4 h-4 text-[#53716E]" />
                    <p className="text-sm font-medium text-[#0B2828]">Ubicación</p>
                  </div>
                  <p className="font-semibold">{selectedSession.location}</p>
                </div>
              </div>

              <div>
                <h4 className="font-semibold mb-3 flex items-center gap-2">
                  <FileText className="w-4 h-4" /> Observaciones
                </h4>
                <div className="bg-[#F5F7FA] rounded-lg p-4">
                  <p className="text-sm text-[#0B2828]">{selectedSession.observations || 'Sin observaciones.'}</p>
                </div>
              </div>

              {selectedSession.materials.length > 0 && (
                <div>
                  <h4 className="font-semibold mb-3">Material utilizado</h4>
                  <div className="space-y-2">
                    {selectedSession.materials.map((m, i) => (
                      <div key={i} className="flex items-center gap-2 p-3 bg-[#F2EDF8] rounded-lg">
                        <FileText className="w-4 h-4 text-[#6F548F]" />
                        <span className="text-sm text-[#0B2828]">{m}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="p-6 border-t border-[#DCE3EA] flex justify-end">
              <button
                onClick={() => setSelectedSession(null)}
                className="px-6 py-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
