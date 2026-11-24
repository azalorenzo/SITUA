import { ConfirmarAccion } from './ConfirmarAccion';
import { SesionesTutor } from './SesionesTutor';
import { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, Clock, Calendar, X, ChevronLeft, ChevronRight } from 'lucide-react';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { consultarHorarios, consultarResumenHorarios, crearHorario, editarHorario, eliminarHorario, aFechaHoraUtc, formatearFecha, formatearHora, ubicacionMostrada } from '../servicios/horarios';
import type { Horario } from '../tipos/horarios';

interface TimeSlot {
  id: number;
  date: string; // 'YYYY-MM-DD'
  startTime: string;
  endTime: string;
  location: string;
  type: 'disponible' | 'ocupado';
  studentName?: string;
}

// El backend es la única fuente real: esta función solo cambia la forma de los
// datos para que el resto de la pantalla, ya hecha, no tenga que cambiar.
function horarioATimeSlot(horario: Horario): TimeSlot {
  return {
    id: horario.id,
    date: formatearFecha(horario.inicioEn),
    startTime: formatearHora(horario.inicioEn),
    endTime: formatearHora(horario.finEn),
    location: ubicacionMostrada(horario),
    type: horario.estudianteNombre ? 'ocupado' : 'disponible',
    studentName: horario.estudianteNombre || undefined,
  };
}

const MESES = [
  'Enero','Febrero','Marzo','Abril','Mayo','Junio',
  'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
];
const DIAS_SEMANA = ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'];

function toYMD(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
}

export function ScheduleView() {

  const today = new Date();

  const [schedules, setSchedules] =
    useState<TimeSlot[]>([]);

  const [intentoHorarios,cambiarIntentoHorarios]=useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [currentYear, setCurrentYear] =
    useState(today.getFullYear());

  const [currentMonth, setCurrentMonth] =
    useState(today.getMonth());

  const [selectedDate, setSelectedDate] =
    useState(toYMD(today));

  const [resumen,cambiarResumen]=useState({total:0,disponibles:0,ocupados:0,hoy:0}),[horariosDia,cambiarHorariosDia]=useState<TimeSlot[]>([]);
  const [guardando,cambiarGuardado]=useState(false),[errorFormulario,cambiarErrorFormulario]=useState(''),[erroresFormulario,cambiarErroresFormulario]=useState<Record<string,string[]>>({});
  const [aEliminar,cambiarAEliminar]=useState<number|null>(null),[errorEliminar,cambiarErrorEliminar]=useState('');
  const hoy=toYMD(today),utc=(fecha:Date)=>fecha.toISOString().slice(0,19);
  useEffect(()=>{const cancelacion=new AbortController();setCargando(true);setError(null);setSchedules([]);
    const desde=utc(new Date(currentYear,currentMonth,1)),hasta=utc(new Date(currentYear,currentMonth+1,1));
    const inicioHoy=new Date(hoy+'T00:00:00'),finHoy=new Date(inicioHoy);finHoy.setDate(finHoy.getDate()+1);
    Promise.all([consultarHorarios(cancelacion.signal,desde,hasta),consultarResumenHorarios(cancelacion.signal,desde,hasta),consultarResumenHorarios(cancelacion.signal,utc(inicioHoy),utc(finHoy))])
      .then(([horarios,delMes,delDia])=>{if(!cancelacion.signal.aborted){setSchedules(horarios.map(horarioATimeSlot));cambiarResumen({...delMes,hoy:delDia.total});}})
      .catch(fallo=>{if(!cancelacion.signal.aborted)setError(fallo instanceof Error?fallo.message:'No se pudieron cargar tus horarios.');}).finally(()=>{if(!cancelacion.signal.aborted)setCargando(false);});return()=>cancelacion.abort();
  },[currentYear,currentMonth,intentoHorarios,hoy]);
  useEffect(()=>{const cancelacion=new AbortController();cambiarHorariosDia([]);const desde=new Date(selectedDate+'T00:00:00'),hasta=new Date(desde);hasta.setDate(hasta.getDate()+1);
    consultarHorarios(cancelacion.signal,utc(desde),utc(hasta)).then(datos=>{if(!cancelacion.signal.aborted)cambiarHorariosDia(datos.map(horarioATimeSlot));}).catch(fallo=>{if(!cancelacion.signal.aborted)setError(fallo instanceof Error?fallo.message:'No se pudieron cargar tus horarios.');});return()=>cancelacion.abort();
  },[selectedDate,intentoHorarios]);

  const [showAddModal, setShowAddModal] =
    useState(false);

  const [editingSlot, setEditingSlot] =
    useState<TimeSlot | null>(null);

  const [startTime, setStartTime] =
    useState('');

  const [endTime, setEndTime] =
    useState('');

  const [location, setLocation] =
    useState('');

  const timeSlots = [
    '07:00','08:00','09:00','10:00','11:00','12:00',
    '13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00'
  ];


  // ── Navegación de mes ──────────────────────────────────────────────────────
  const cambiarMes = (desplazamiento: number) => {
    const fecha = new Date(currentYear, currentMonth + desplazamiento, 1);
    setCurrentMonth(fecha.getMonth());
    setCurrentYear(fecha.getFullYear());
    setSelectedDate(toYMD(fecha));
  };
  const prevMonth = () => cambiarMes(-1);
  const nextMonth = () => cambiarMes(1);

  // ── Construir cuadrícula del mes ───────────────────────────────────────────
  const firstDay  = new Date(currentYear, currentMonth, 1).getDay();
  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();

  const calendarCells: (number | null)[] = [
    ...Array(firstDay).fill(null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1)
  ];
  // pad to complete last row
  while (calendarCells.length % 7 !== 0) calendarCells.push(null);

  const dateStr = (day: number) =>
    `${currentYear}-${String(currentMonth + 1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;

  const porFecha:Record<string,TimeSlot[]>={};
  for(const horario of schedules)(porFecha[horario.date]??=[]).push(horario);
  const slotsForDate=(fecha:string)=>porFecha[fecha]??[];

  // ── CRUD ──────────────────────────────────────────────────────────────────
  const mensajeError = (motivo: unknown, porDefecto: string) =>
    motivo instanceof ErrorSolicitud ? motivo.message : porDefecto;

  const validarFormulario=()=>{const errores:Record<string,string[]>={};if(!startTime)errores.inicioEn=['Selecciona la hora de inicio.'];if(!endTime)errores.finEn=['Selecciona la hora de fin.'];else if(startTime&&endTime<=startTime)errores.finEn=['La hora de fin debe ser posterior al inicio.'];if(!location.trim())errores.ubicacion=['Escribe la ubicación.'];cambiarErroresFormulario(errores);return Object.keys(errores).length===0;};
  const guardarHorario=async()=>{if(guardando||!validarFormulario())return;cambiarGuardado(true);cambiarErrorFormulario('');try{
    const datos={inicioEn:aFechaHoraUtc(editingSlot?.date??selectedDate,startTime),finEn:aFechaHoraUtc(editingSlot?.date??selectedDate,endTime),ubicacion:location.trim()};
    if(editingSlot)await editarHorario(editingSlot.id,datos);else await crearHorario(datos);closeModal();cambiarIntentoHorarios(valor=>valor+1);
   }catch(fallo){cambiarErrorFormulario(mensajeError(fallo,'No se pudo guardar el horario.'));if(fallo instanceof ErrorSolicitud)cambiarErroresFormulario(fallo.erroresPorCampo);}finally{cambiarGuardado(false);}};
  const handleAdd=guardarHorario,handleEdit=guardarHorario;
  const handleDelete=(id:number)=>{cambiarErrorEliminar('');cambiarAEliminar(id);};
  const confirmarEliminacion=async()=>{if(aEliminar===null||guardando)return;cambiarGuardado(true);cambiarErrorEliminar('');try{await eliminarHorario(aEliminar);cambiarAEliminar(null);cambiarIntentoHorarios(valor=>valor+1);}catch(fallo){cambiarErrorEliminar(mensajeError(fallo,'No se pudo eliminar el horario.'));}finally{cambiarGuardado(false);}};

  const openEdit = (slot: TimeSlot) => {
    cambiarErrorFormulario('');cambiarErroresFormulario({});setEditingSlot(slot);
    setStartTime(slot.startTime);
    setEndTime(slot.endTime);
    setLocation(slot.location);
  };

  const resetForm = () => { setStartTime(''); setEndTime(''); setLocation('');cambiarErrorFormulario('');cambiarErroresFormulario({}); };
  const closeModal = () => { setShowAddModal(false); setEditingSlot(null); resetForm(); };

  // ── Día seleccionado ───────────────────────────────────────────────────────
  const selectedSlots=horariosDia;

  const selectedDateObj = new Date(selectedDate + 'T00:00:00');
  const selectedLabel = `${selectedDateObj.getDate()} de ${MESES[selectedDateObj.getMonth()]} ${selectedDateObj.getFullYear()}`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap justify-between items-center gap-3">
        <div>
          <h2 className="font-semibold mb-1">Mis horarios</h2>
          {cargando && <p className="text-xs text-[#53716E]">Cargando horarios…</p>}
          {error && <p className="text-xs text-[#B83F4B]">{error}</p>}
        </div>
        
      </div>

      <SesionesTutor alCambiar={()=>cambiarIntentoHorarios(anterior=>anterior+1)}/>
      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Disponibles',   val: resumen.disponibles, icon: <Clock className="w-5 h-5 text-[#227F78]"/>,  bg: 'bg-[#E4F3ED]' },
          { label: 'Ocupados',      val: resumen.ocupados,    icon: <Calendar className="w-5 h-5 text-[#1F6662]"/>,  bg: 'bg-[#F2FBF9]' },
          { label: 'Este mes',      val: resumen.total, icon: <Calendar className="w-5 h-5 text-[#6F548F]"/>, bg: 'bg-[#F2EDF8]' },
          { label: 'Hoy',           val: resumen.hoy,                   icon: <Clock className="w-5 h-5 text-[#986314]"/>,   bg: 'bg-[#FFF3D6]' },
        ].map(({ label, val, icon, bg }) => (
          <div key={label} className="bg-white rounded-lg p-4 shadow-sm">
            <div className="flex items-center gap-3">
              <div className={`${bg} p-3 rounded-lg`}>{icon}</div>
              <div>
                <p className="text-sm text-[#53716E]">{label}</p>
                <p className="font-semibold">{val}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Calendar + Day detail */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Monthly calendar */}
        <div className="lg:col-span-2 bg-white rounded-lg shadow-sm overflow-hidden">
          {/* Month nav */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-[#DCE3EA]">
            <button aria-label="Mes anterior" onClick={prevMonth} className="p-2 hover:bg-[#DCE3EA] rounded-lg">
              <ChevronLeft className="w-5 h-5 text-[#53716E]" />
            </button>
            <h3 className="font-semibold text-[#0B2828]">
              {MESES[currentMonth]} {currentYear}
            </h3>
            <button aria-label="Mes siguiente" onClick={nextMonth} className="p-2 hover:bg-[#DCE3EA] rounded-lg">
              <ChevronRight className="w-5 h-5 text-[#53716E]" />
            </button>
          </div>

          {/* Day-of-week headers */}
          <div className="grid grid-cols-7 border-b border-[#DCE3EA]">
            {DIAS_SEMANA.map(d => (
              <div key={d} className="py-2 text-center text-xs font-semibold text-[#53716E] uppercase">
                {d}
              </div>
            ))}
          </div>

          {/* Day cells */}
          <div className="grid grid-cols-7">
            {calendarCells.map((day, i) => {
              if (day === null) return <div key={`empty-${i}`} className="h-16 border-b border-r border-[#DCE3EA] last:border-r-0" />;

              const ds       = dateStr(day);
              const isToday  = ds === toYMD(today);
              const isSel    = ds === selectedDate;
              const slots    = slotsForDate(ds);
              const hasDisp  = slots.some(s => s.type === 'disponible');
              const hasOcup  = slots.some(s => s.type === 'ocupado');

              return (
                <button
                  key={ds}
                  aria-label={`Seleccionar ${ds}`} onClick={() => setSelectedDate(ds)}
                  className={`h-16 border-b border-r border-[#DCE3EA] last:border-r-0 flex flex-col items-center pt-2 gap-1 transition-colors
                    ${isSel  ? 'bg-[#1F6662] text-white'       : ''}
                    ${isToday && !isSel ? 'bg-[#F2FBF9] font-bold' : ''}
                    ${!isSel && !isToday ? 'hover:bg-[#F5F7FA]' : ''}
                  `}
                >
                  <span className={`text-sm w-7 h-7 flex items-center justify-center rounded-full
                    ${isToday && !isSel ? 'bg-[#1F6662] text-white' : ''}
                  `}>
                    {day}
                  </span>
                  {/* dots */}
                  {slots.length > 0 && (
                    <div className="flex gap-0.5">
                      {hasDisp && <span className={`w-1.5 h-1.5 rounded-full ${isSel ? 'bg-white' : 'bg-[#227F78]'}`} />}
                      {hasOcup && <span className={`w-1.5 h-1.5 rounded-full ${isSel ? 'bg-white' : 'bg-[#1F6662]'}`}  />}
                    </div>
                  )}
                </button>
              );
            })}
          </div>

          {/* Legend */}
          <div className="px-6 py-3 border-t border-[#DCE3EA] flex gap-6">
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#227F78] inline-block" />
              <span className="text-xs text-[#53716E]">Disponible</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-[#1F6662] inline-block" />
              <span className="text-xs text-[#53716E]">Ocupado</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-7 h-5 rounded-full bg-[#1F6662] inline-flex items-center justify-center text-white text-xs">•</span>
              <span className="text-xs text-[#53716E]">Día seleccionado</span>
            </div>
          </div>
        </div>

        {/* Day detail panel */}
        <div className="bg-white rounded-lg shadow-sm overflow-hidden flex flex-col">
          <div className="px-5 py-4 border-b border-[#DCE3EA] flex justify-between items-center">
            <div>
              <p className="font-semibold text-sm text-[#0B2828]">{selectedLabel}</p>
              <p className="text-xs text-[#53716E] mt-0.5">{selectedSlots.length} sesión(es)</p>
            </div>
            <button
              onClick={() => setShowAddModal(true)}
              className="w-8 h-8 flex items-center justify-center bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {selectedSlots.length === 0 ? (
              <div className="text-center py-10 text-[#53716E]">
                <Calendar className="w-10 h-10 mx-auto mb-2 opacity-30" />
                <p className="text-sm">Sin horarios este día</p>
                <button
                  onClick={() => setShowAddModal(true)}
                  className="mt-3 text-xs text-[#1F6662] hover:underline"
                >
                  + Agregar disponibilidad
                </button>
              </div>
            ) : (
              selectedSlots.map(slot => (
                <div
                  key={slot.id}
                  className={`rounded-lg p-3 border-l-4 ${
                    slot.type === 'disponible'
                      ? 'bg-[#E4F3ED] border-[#227F78]'
                      : 'bg-[#F2FBF9] border-[#1F6662]'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-sm font-semibold">{slot.startTime} – {slot.endTime}</p>
                      <p className="text-xs text-[#53716E] mt-0.5">{slot.location}</p>
                      {slot.studentName && (
                        <p className="text-xs font-medium text-[#1C4543] mt-1">{slot.studentName}</p>
                      )}
                    </div>
                    <div className="flex gap-1 ml-2">
                      {slot.type === 'disponible' && (
                        <button aria-label={`Editar horario ${slot.startTime}`} onClick={() => openEdit(slot)} className="p-1 hover:bg-white rounded">
                          <Edit className="w-3.5 h-3.5 text-[#53716E]" />
                        </button>
                      )}
                      <button aria-label={`Eliminar horario ${slot.startTime}`} onClick={() => handleDelete(slot.id)} className="p-1 hover:bg-white rounded">
                        <Trash2 className="w-3.5 h-3.5 text-[#B83F4B]" />
                      </button>
                    </div>
                  </div>
                  <span className={`mt-2 inline-block text-xs px-2 py-0.5 rounded-full font-medium ${
                    slot.type === 'disponible' ? 'bg-[#E4F3ED] text-[#227F78]' : 'bg-[#D3F4EE] text-[#1C4543]'
                  }`}>
                    {slot.type === 'disponible' ? 'Disponible' : 'Ocupado'}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Modal agregar / editar */}
      {(showAddModal || editingSlot) && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div role="dialog" aria-modal="true" aria-label={editingSlot?'Editar horario':'Agregar disponibilidad'} className="bg-white rounded-lg max-w-md w-full p-6 max-h-[90dvh] overflow-y-auto">
            <div className="flex justify-between items-center mb-5">
              <div>
                <h3 className="font-semibold">{editingSlot ? 'Editar horario' : 'Agregar disponibilidad'}</h3>
                <p className="text-xs text-[#53716E] mt-0.5">{selectedLabel}</p>
              </div>
              <button disabled={guardando} onClick={closeModal} className="text-[#53716E] hover:text-[#53716E]">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label htmlFor="horario-inicio" className="block text-sm font-medium text-[#0B2828] mb-1">Hora inicio</label>
                  <select id="horario-inicio" disabled={guardando}
                    value={startTime}
                    onChange={e => setStartTime(e.target.value)}
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  >
                    <option value="">Seleccionar</option>
                    {timeSlots.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                  {erroresFormulario.inicioEn?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}
                </div>
                <div>
                  <label htmlFor="horario-fin" className="block text-sm font-medium text-[#0B2828] mb-1">Hora fin</label>
                  <select id="horario-fin" disabled={guardando}
                    value={endTime}
                    onChange={e => setEndTime(e.target.value)}
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  >
                    <option value="">Seleccionar</option>
                    {timeSlots.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>{erroresFormulario.finEn?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}
                </div>
              </div>

              <div>
                <label htmlFor="horario-ubicacion" className="block text-sm font-medium text-[#0B2828] mb-1">Ubicación</label>
                <input
                  type="text" id="horario-ubicacion" disabled={guardando} maxLength={1024}
                  value={location}
                  onChange={e => setLocation(e.target.value)}
                  placeholder="Ej: Aula 301, Virtual..."
                  className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                />{erroresFormulario.ubicacion?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}
              </div>
            </div>

            {errorFormulario&&<p role="alert" className="text-sm text-[#B83F4B] mt-4">{errorFormulario}</p>}<div className="flex gap-3 mt-6">
              <button disabled={guardando} onClick={closeModal} className="flex-1 px-4 py-2 border border-[#DCE3EA] text-[#0B2828] rounded-lg hover:bg-[#F5F7FA]">
                Cancelar
              </button>
              <button
                disabled={guardando} onClick={editingSlot ? handleEdit : handleAdd}
                className="flex-1 px-4 py-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
              >
                {guardando?'Guardando…':editingSlot?'Guardar cambios':'Agregar horario'}
              </button>
            </div>
          </div>
        </div>
      )}
      {aEliminar!==null&&<ConfirmarAccion titulo="¿Eliminar este horario?" confirmacion="Eliminar" pendiente={guardando} error={errorEliminar} alCerrar={()=>cambiarAEliminar(null)} alConfirmar={confirmarEliminacion}/>}
    </div>
  );
}
