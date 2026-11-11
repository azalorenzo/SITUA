import { mockTutors, mockSessions } from '../mock/data';
import { Calendar, ChevronLeft, ChevronRight, Clock, MapPin, User, Plus, Trash2, Edit, X } from 'lucide-react';
import { useState } from 'react';

interface AgendaEvent {
  id: string;
  title: string;
  day: number;
  time: string;
  tutor: string;
  location: string;
  color: string;
}

export function StudentAgendaView() {
  const [events, setEvents] = useState<AgendaEvent[]>(mockSessions.map(s => ({ id: s.id_sesion, title: s.tema, day: Number(s.fecha.slice(-2)), time: s.hora, tutor: s.tutor, location: s.lugar, color: 'bg-[#D3F4EE] text-[#1C4543] border-[#D3F4EE]' })));
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({ title: '', day: '', time: '', tutor: '', location: '' });
  const [tutores, setTutores] = useState(mockTutors);
   const [showDeleteModal, setShowDeleteModal] =
  useState(false);

const [sessionToDelete, setSessionToDelete] =
  useState<string | null>(null);

  const saveEvent = () => {
    const day = Number(form.day);
    if (!form.title.trim() || !form.tutor || !form.time || day < 1 || day > 30 || !Number.isInteger(day)) return;
    const event: AgendaEvent = { id: editingId || String(Date.now()), title: form.title, day, time: form.time, tutor: form.tutor, location: form.location, color: 'bg-[#D3F4EE] text-[#1C4543] border-[#D3F4EE]' };
    setEvents(prev => editingId ? prev.map(e => e.id === editingId ? event : e) : [...prev, event]);
    closeModal();
  };

  const openEdit = (e: AgendaEvent) => {
    setEditingId(e.id);
    setForm({ title: e.title, day: e.day.toString(), time: e.time, tutor: e.tutor, location: e.location });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
    setForm({ title: '', day: '', time: '', tutor: '', location: '' });
  };

  const deleteEvent = (id: string) => setEvents(prev => prev.filter(e => e.id !== id));

  return (
    <div className="space-y-6 relative">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Mi agenda</h2>
          <p className="text-sm text-[#53716E]">Junio 2026</p>
        </div>
        <button onClick={() => setIsModalOpen(true)} className="flex items-center gap-2 bg-[#1F6662] text-white px-4 py-2 rounded-lg hover:bg-[#1C4543]">
          <Plus className="w-4 h-4" /> Agregar evento
        </button>
      </div>


      {isModalOpen && (

        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg shadow-xl w-96 space-y-4">
            <div className="flex justify-between items-center">
                <h3 className="font-bold">{editingId ? 'Editar Evento' : 'Nuevo Evento'}</h3>
                <button onClick={closeModal}><X className="w-5 h-5"/></button>
            </div>
            <input className="w-full border p-2 rounded" placeholder="Nombre de la asesoría" value={form.title} onChange={e => setForm({...form, title: e.target.value})} />
            <input className="w-full border p-2 rounded" type="number" placeholder="Día" value={form.day} onChange={e => setForm({...form, day: e.target.value})} />
            <input
              type="time"
              className="w-full border p-2 rounded"
              value={form.time}
              onChange={e =>
                setForm({
                  ...form,
                  time: e.target.value
                })
              }
            />
            <select
              className="w-full border p-2 rounded"
              value={form.tutor}
              onChange={e =>
                setForm({
                  ...form,
                  tutor: e.target.value
                })
              }
            >

              <option value="">
                Selecciona un tutor
              </option>

              {tutores.map((tutor) => (

                <option
                  key={tutor.id_tutor}
                  value={tutor.nombre}
                >
                  {tutor.nombre}
                </option>

              ))}

            </select>
            <input className="w-full border p-2 rounded" placeholder="Lugar" value={form.location} onChange={e => setForm({...form, location: e.target.value})} />
            <button
              onClick={saveEvent}
              className="w-full bg-[#1F6662] text-white p-2 rounded"
            >
              {editingId ? 'Guardar Cambios' : 'Crear Evento'}
            </button>
                      </div>
        </div>
      )}


      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <div className="lg:col-span-2 bg-white rounded-lg shadow-sm p-6">


           <div className="grid grid-cols-7 gap-2">

            {['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'].map(d => <div key={d} className="text-center text-sm font-medium text-[#53716E] pb-2">{d}</div>)}
            {Array.from({ length: 30 }).map((_, i) => {
              const day = i + 1;
              const dayEvents = events.filter(e => e.day === day);
              const isToday = day === new Date().getDate();
              return (
                <div key={day} className="aspect-square border rounded-lg p-1 text-xs relative">
                  <div className={`w-6 h-6 flex items-center justify-center ${isToday ? 'bg-[#1F6662] text-white rounded-full' : ''}`}>
                    {day}
                  </div>
                  {dayEvents.map(e => <div key={e.id} className={`p-0.5 mt-1 rounded truncate ${e.color}`}>{e.title}</div>)}
                </div>
              );
            })}
          </div>
        </div>


        <div className="space-y-4">
          <h3 className="font-semibold">Próximas sesiones</h3>
          {[...events].sort((a,b) => a.day - b.day).map(s => (
            <div key={s.id} className={`p-4 rounded-lg border ${s.color}`}>
              <h4 className="font-medium text-sm">Jueves {s.day} de Junio - {s.title}</h4>
              <p className="text-xs italic">{s.tutor} | {s.location} | {s.time}</p>
              <div className="flex gap-2 mt-2">
                <button onClick={() => openEdit(s)} className="text-xs underline flex items-center gap-1"><Edit className="w-3 h-3"/> Editar</button>
                <button
                  onClick={() => {

                    setSessionToDelete(s.id);

                    setShowDeleteModal(true);

                  }}
                  className="text-xs text-[#B83F4B] flex items-center gap-1"
                >
                  <Trash2 className="w-3 h-3"/>
                  Eliminar
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {showDeleteModal && (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">

        <div className="bg-white rounded-xl p-6 w-96 shadow-xl">

          <h3 className="text-lg font-semibold mb-3">
            Cancelar tutoría
          </h3>

          <p className="text-[#53716E] mb-5">
            ¿Deseas cancelar esta sesión?
          </p>

          <div className="flex justify-end gap-3">

            <button
              onClick={() => {

                setShowDeleteModal(false);

                setSessionToDelete(null);

              }}
              className="px-4 py-2 border rounded-lg"
            >
              No
            </button>

            <button
              onClick={() => {

                if(sessionToDelete){

                  deleteEvent(sessionToDelete);

                }

                setShowDeleteModal(false);

                setSessionToDelete(null);

              }}
              className="px-4 py-2 bg-[#B83F4B] text-white rounded-lg"
            >
              Sí, cancelar
            </button>

          </div>

        </div>

      </div>

    )}    
    </div>
  );
}
