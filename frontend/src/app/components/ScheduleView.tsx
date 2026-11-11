import { useState } from 'react';
import { Plus, Edit, Trash2, Clock, Calendar, X, ChevronLeft, ChevronRight } from 'lucide-react';

interface TimeSlot {
  id: number;
  date: string; // 'YYYY-MM-DD'
  startTime: string;
  endTime: string;
  location: string;
  type: 'disponible' | 'ocupado';
  studentName?: string;
}

const MESES = [
  'Enero','Febrero','Marzo','Abril','Mayo','Junio',
  'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'
];
const DIAS_SEMANA = ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'];

function toYMD(date: Date): string {
  return date.toISOString().split('T')[0];
}

export function ScheduleView() {

  const today = new Date();

  const [schedules, setSchedules] =
    useState<TimeSlot[]>([{ id: 1, date: toYMD(today), startTime: '10:00', endTime: '11:00', location: 'Aula 301', type: 'ocupado', studentName: 'Ana Laura Gómez' }, { id: 2, date: toYMD(today), startTime: '12:00', endTime: '13:00', location: 'Virtual', type: 'disponible' }]);

  const [currentYear, setCurrentYear] =
    useState(today.getFullYear());

  const [currentMonth, setCurrentMonth] =
    useState(today.getMonth());

  const [selectedDate, setSelectedDate] =
    useState(toYMD(today));

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
  const prevMonth = () => {
    if (currentMonth === 0) { setCurrentMonth(11); setCurrentYear(y => y - 1); }
    else setCurrentMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (currentMonth === 11) { setCurrentMonth(0); setCurrentYear(y => y + 1); }
    else setCurrentMonth(m => m + 1);
  };

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

  const slotsForDate = (d: string) => schedules.filter(s => s.date === d);

  // ── CRUD ──────────────────────────────────────────────────────────────────
  const handleAdd = () => {
    if (!startTime || !endTime || !location) { alert('Completa todos los campos'); return; }
    const newSlot: TimeSlot = {
      id: Date.now(),
      date: selectedDate,
      startTime, endTime, location,
      type: 'disponible'
    };
    setSchedules(prev => [...prev, newSlot]);
    resetForm(); setShowAddModal(false);
  };

  const handleEdit = () => {
    if (!editingSlot || !startTime || !endTime || !location) { alert('Completa todos los campos'); return; }
    setSchedules(prev => prev.map(s =>
      s.id === editingSlot.id ? { ...s, startTime, endTime, location } : s
    ));
    resetForm(); setEditingSlot(null);
  };

  const handleDelete = (id: number) => {
    if (confirm('¿Eliminar este horario?')) setSchedules(prev => prev.filter(s => s.id !== id));
  };

  const openEdit = (slot: TimeSlot) => {
    setEditingSlot(slot);
    setStartTime(slot.startTime);
    setEndTime(slot.endTime);
    setLocation(slot.location);
  };

  const resetForm = () => { setStartTime(''); setEndTime(''); setLocation(''); };
  const closeModal = () => { setShowAddModal(false); setEditingSlot(null); resetForm(); };

  // ── Día seleccionado ───────────────────────────────────────────────────────
  const selectedSlots = slotsForDate(selectedDate).sort((a,b) => a.startTime.localeCompare(b.startTime));

  const selectedDateObj = new Date(selectedDate + 'T00:00:00');
  const selectedLabel = `${selectedDateObj.getDate()} de ${MESES[selectedDateObj.getMonth()]} ${selectedDateObj.getFullYear()}`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Mis horarios</h2>
          
        </div>
        
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Disponibles',   val: schedules.filter(s => s.type==='disponible').length, icon: <Clock className="w-5 h-5 text-[#227F78]"/>,  bg: 'bg-[#E4F3ED]' },
          { label: 'Ocupados',      val: schedules.filter(s => s.type==='ocupado').length,    icon: <Calendar className="w-5 h-5 text-[#1F6662]"/>,  bg: 'bg-[#F2FBF9]' },
          { label: 'Este mes',      val: schedules.filter(s => s.date.startsWith(`${currentYear}-${String(currentMonth+1).padStart(2,'0')}`)).length, icon: <Calendar className="w-5 h-5 text-[#6F548F]"/>, bg: 'bg-[#F2EDF8]' },
          { label: 'Hoy',           val: slotsForDate(toYMD(today)).length,                   icon: <Clock className="w-5 h-5 text-[#986314]"/>,   bg: 'bg-[#FFF3D6]' },
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
            <button onClick={prevMonth} className="p-2 hover:bg-[#DCE3EA] rounded-lg">
              <ChevronLeft className="w-5 h-5 text-[#53716E]" />
            </button>
            <h3 className="font-semibold text-[#0B2828]">
              {MESES[currentMonth]} {currentYear}
            </h3>
            <button onClick={nextMonth} className="p-2 hover:bg-[#DCE3EA] rounded-lg">
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
                  onClick={() => setSelectedDate(ds)}
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
                        <button onClick={() => openEdit(slot)} className="p-1 hover:bg-white rounded">
                          <Edit className="w-3.5 h-3.5 text-[#53716E]" />
                        </button>
                      )}
                      <button onClick={() => handleDelete(slot.id)} className="p-1 hover:bg-white rounded">
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
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <div className="flex justify-between items-center mb-5">
              <div>
                <h3 className="font-semibold">{editingSlot ? 'Editar horario' : 'Agregar disponibilidad'}</h3>
                <p className="text-xs text-[#53716E] mt-0.5">{selectedLabel}</p>
              </div>
              <button onClick={closeModal} className="text-[#53716E] hover:text-[#53716E]">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#0B2828] mb-1">Hora inicio</label>
                  <select
                    value={startTime}
                    onChange={e => setStartTime(e.target.value)}
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  >
                    <option value="">Seleccionar</option>
                    {timeSlots.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#0B2828] mb-1">Hora fin</label>
                  <select
                    value={endTime}
                    onChange={e => setEndTime(e.target.value)}
                    className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                  >
                    <option value="">Seleccionar</option>
                    {timeSlots.map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-[#0B2828] mb-1">Ubicación</label>
                <input
                  type="text"
                  value={location}
                  onChange={e => setLocation(e.target.value)}
                  placeholder="Ej: Aula 301, Virtual..."
                  className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
                />
              </div>
            </div>

            <div className="flex gap-3 mt-6">
              <button onClick={closeModal} className="flex-1 px-4 py-2 border border-[#DCE3EA] text-[#0B2828] rounded-lg hover:bg-[#F5F7FA]">
                Cancelar
              </button>
              <button
                onClick={editingSlot ? handleEdit : handleAdd}
                className="flex-1 px-4 py-2 bg-[#1F6662] text-white rounded-lg hover:bg-[#1C4543]"
              >
                {editingSlot ? 'Guardar cambios' : 'Agregar horario'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
