import { PaginasSesiones } from './PaginasSesiones';
import type { Pagina } from '../tipos/paginacion';
import { RegistroSesionModal } from './RegistroSesionModal';
import { useEffect, useState } from 'react';
import { Search, Filter, Eye, Calendar, X, Clock, FileText, AlertCircle, TrendingUp, CheckCircle, Loader2 } from 'lucide-react';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { consultarDetalleEstudianteTutor, consultarPaginaEstudiantesTutor } from '../servicios/estudiantesTutor';
import type { EstudianteTutor, ObservacionEstudianteTutor, SesionEstudianteTutor } from '../tipos/estudiantesTutor';
import { EstadoConsulta } from './ConsultaCoordinacion';

// Fecha de hoy en la zona del navegador, para que el día no cambie de noche por usar UTC.
function fechaLocal(): string {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`;
}

// El backend guarda las fechas en UTC sin zona; aquí se muestran en la hora del navegador.
function formatearFecha(utc: string | null): string {
  return utc ? new Date(`${utc}Z`).toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' }) : '—';
}

const ORDINALES: Record<number, string> = { 1: '1er', 2: '2do', 3: '3er', 4: '4to', 5: '5to', 6: '6to', 7: '7mo', 8: '8vo', 9: '9no', 10: '10mo', 11: '11vo', 12: '12vo' };

function etiquetaSemestre(semestre: number | null): string {
  return semestre ? `${ORDINALES[semestre] ?? `${semestre}º`} Semestre` : '—';
}

function etiquetaRiesgo(nivel: EstudianteTutor['nivelRiesgo']): string {
  switch (nivel) {
    case 'alto': return 'Alto';
    case 'medio': return 'Medio';
    case 'bajo': return 'Bajo';
    case 'sin_riesgo': return 'Sin riesgo';
    default: return 'Sin evaluar';
  }
}

function etiquetaDuracion(minutos: number): string {
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.round((minutos / 60) * 10) / 10;
  return horas === 1 ? '1 hora' : `${horas} horas`;
}

function formatoPromedio(promedio: number | null): string {
  return promedio === null ? '—' : promedio.toFixed(1);
}

function textoDeError(fallo: unknown): string {
  if (fallo instanceof ErrorSolicitud && fallo.estado === 401) return 'Tu sesión ya no es válida. Sal y vuelve a iniciar sesión.';
  return fallo instanceof Error ? fallo.message : 'No se pudo cargar la información.';
}

interface Student {
  id: number;
  name: string;
  career: string;
  semester: string;
  average: number | null;
  riesgo: string;
  condicion: string;
  email: string;
  phone: string;
  tutoringSessions: number;
  lastSession: string;
  periodo: string;
}

interface TutoringSession { date: string; topic: string; duration: string; notes: string; status: string; }
interface Observation { date: string; type: string; text: string; priority: 'normal' | 'high'; }

function aEstudiante(e: EstudianteTutor): Student {
  return {
    id: e.id, name: e.nombre, career: e.carrera, semester: etiquetaSemestre(e.semestre), average: e.promedio,
    riesgo: etiquetaRiesgo(e.nivelRiesgo), condicion: e.condicion ?? '', email: e.correo, phone: e.telefono ?? '—',
    tutoringSessions: e.sesionesCompletadas, lastSession: formatearFecha(e.ultimaSesion), periodo: e.periodo
  };
}

function aSesion(s: SesionEstudianteTutor): TutoringSession {
  return { date: formatearFecha(s.inicioEn), topic: s.tema, duration: etiquetaDuracion(s.duracionMinutos), notes: s.observaciones ?? '', status: 'Completada' };
}

const TIPOS_OBSERVACION = { nota: 'Nota', progreso: 'Progreso', alerta: 'Alerta' } as const;

function aObservacion(o: ObservacionEstudianteTutor): Observation {
  return { date: formatearFecha(o.observadoEn), type: TIPOS_OBSERVACION[o.tipo], text: o.contenido, priority: o.prioridad === 'alta' ? 'high' : 'normal' };
}

export function MyStudentsView() {
  const [searchQuery, setSearchQuery]       = useState('');
  const [selectedStudent, setSelectedStudent] = useState<Student | null>(null);
  const [filterStatus, setFilterStatus]     = useState<string>('Todos');

  // Modal Registrar
  const [showRegisterModal, setShowRegisterModal] = useState(false);
  const [registerStudent, setRegisterStudent]     = useState<Student | null>(null);
  const [busquedaAplicada, cambiarBusquedaAplicada] = useState('');
  const [pagina,cambiarPagina]=useState(1),[datosPagina,cambiarDatosPagina]=useState<Pagina<EstudianteTutor>|null>(null);
  const [cargandoLista,cambiarCargaLista]=useState(true),[errorLista,cambiarErrorLista]=useState(''),[intentoLista,cambiarIntentoLista]=useState(0);
  useEffect(() => {
    const espera = setTimeout(() => {cambiarBusquedaAplicada(searchQuery);cambiarPagina(1);}, 300);
    return () => clearTimeout(espera);
  }, [searchQuery]);
  useEffect(()=>{const cancelacion=new AbortController();cambiarCargaLista(true);cambiarErrorLista('');cambiarDatosPagina(null);
    consultarPaginaEstudiantesTutor(cancelacion.signal,busquedaAplicada,filterStatus.toLowerCase(),pagina).then(datos=>{if(!cancelacion.signal.aborted)cambiarDatosPagina(datos);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarErrorLista(textoDeError(fallo));}).finally(()=>{if(!cancelacion.signal.aborted)cambiarCargaLista(false);});return()=>cancelacion.abort();
  },[busquedaAplicada,filterStatus,pagina,intentoLista]);
  const consulta={cargando:cargandoLista,error:errorLista,actualizar:()=>cambiarIntentoLista(actual=>actual+1)};
  const students=(datosPagina?.contenido??[]).map(aEstudiante);

  // El periodo lo trae cada estudiante desde el catálogo; los dos modales nunca están abiertos a la vez.
  const periodo = (selectedStudent ?? registerStudent)?.periodo ?? '';

  // El detalle (sesiones y observaciones) se consulta al abrir "Ver detalle".
  const [tutoringHistory, setTutoringHistory] = useState<TutoringSession[]>([]);
  const [observations, setObservations] = useState<Observation[]>([]);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);
  const [errorDetalle, setErrorDetalle] = useState('');
  const [intentoDetalle, setIntentoDetalle] = useState(0);
  const idDetalle = selectedStudent?.id ?? null;

  useEffect(() => {
    setTutoringHistory([]); setObservations([]); setErrorDetalle('');
    if (idDetalle === null) { setCargandoDetalle(false); return; }
    const cancelacion = new AbortController();
    setCargandoDetalle(true);
    consultarDetalleEstudianteTutor(idDetalle, cancelacion.signal).then(detalle => {
      if (cancelacion.signal.aborted) return;
      setTutoringHistory(detalle.sesiones.map(aSesion));
      setObservations(detalle.observaciones.map(aObservacion));
    }).catch(fallo => {
      if (!cancelacion.signal.aborted) setErrorDetalle(textoDeError(fallo));
    }).finally(() => { if (!cancelacion.signal.aborted) setCargandoDetalle(false); });
    return () => cancelacion.abort();
  }, [idDetalle, intentoDetalle]);

  const getRiesgoColor = (riesgo: string) => {
    switch (riesgo) {
      case 'Bajo':  return 'bg-[#E4F3ED] text-[#227F78]';
      case 'Medio': return 'bg-[#FFF3D6] text-[#986314]';
      case 'Alto':  return 'bg-[#FCEBED] text-[#B83F4B]';
      default:      return 'bg-[#DCE3EA] text-[#0B2828]';
    }
  };

  const getAverageColor = (avg: number | null) => {
    if (avg === null) return 'text-[#53716E]';
    if (avg >= 9) return 'text-[#227F78]';
    if (avg >= 8) return 'text-[#986314]';
    return 'text-[#B83F4B]';
  };

  // ── Abrir modal registrar ───────────────────────────────────────────────────
  const openRegister = (student: Student) => {
    setSelectedStudent(null);setRegisterStudent(student);setShowRegisterModal(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h2 className="font-semibold mb-1">Mis tutorados</h2>
          <p className="text-sm text-[#53716E]">Gestiona y da seguimiento a tus estudiantes</p>
        </div>
        <span className="text-sm text-[#53716E]">
          Total: <span className="font-semibold">{datosPagina?.totalRegistros??0}</span> estudiantes
        </span>
      </div>

      <EstadoConsulta cargando={consulta.cargando} error={consulta.error} alReintentar={consulta.actualizar} />

      {/* Search and Filters */}
      <div className="bg-white rounded-lg p-6 shadow-sm">
        <div className="flex flex-col md:flex-row gap-4">
          <div className="flex-1 relative">
            <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="search"
              aria-label="Buscar tutorado"
              maxLength={200}
              placeholder="Buscar por nombre o carrera..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="w-5 h-5 text-[#53716E]" />
            <select
              aria-label="Riesgo académico"
              value={filterStatus}
              onChange={evento => {setFilterStatus(evento.target.value);cambiarPagina(1);}}
              className="border border-[#DCE3EA] rounded-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
            >
              <option>Todos</option>
              <option>Alto</option>
              <option>Medio</option>
              <option>Bajo</option>
              <option value="sin_riesgo">Sin riesgo</option>
              <option value="sin_evaluar">Sin evaluar</option>
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
              {students.map(student => (
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
                      {formatoPromedio(student.average)}
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

        {!consulta.cargando && !consulta.error && students.length === 0 && (
          <div className="text-center py-12">
            <p className="text-[#53716E]">No se encontraron estudiantes con estos criterios.</p>
          </div>
        )}
      </div>

      {datosPagina&&!consulta.cargando&&!consulta.error&&<div className="flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-[#53716E]">{datosPagina.totalRegistros} estudiantes</p><PaginasSesiones pagina={datosPagina.pagina} totalPaginas={datosPagina.totalPaginas} alCambiar={cambiarPagina}/></div>}

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
                        {formatoPromedio(selectedStudent.average)}
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
              <EstadoConsulta cargando={cargandoDetalle} error={errorDetalle} alReintentar={() => setIntentoDetalle(anterior => anterior + 1)} />

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
                      {session.notes && <p className="text-sm text-[#53716E] mt-2">{session.notes}</p>}
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

      {showRegisterModal&&registerStudent&&<RegistroSesionModal estudianteId={registerStudent.id} nombre={registerStudent.name} alCerrar={()=>setShowRegisterModal(false)} alGuardar={()=>{setShowRegisterModal(false);consulta.actualizar();}}/>}
    </div>
  );
}
