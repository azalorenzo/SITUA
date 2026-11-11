import { mockSessions } from '../mock/data';
import { useState } from 'react';
import { Clock, BookOpen, AlertCircle, Plus, Trash2, Edit, X } from 'lucide-react';

interface AgendaEvent { 
  id: number; 
  title: string; 
  day: string; 
  time: string; 
  tutor: string; 
  location: string; 
  type: 'virtual' | 'taller' | 'action'; 
}

export function StudentView() {
  const [items, setItems] = useState<AgendaEvent[]>([
 { id: 1, title: 'Repaso de programación', day: '12', time: '11:00', tutor: 'Elena Martínez Ruiz', location: 'Virtual', type: 'virtual' },
 { id: 2, title: 'Hábitos de estudio', day: '15', time: '12:00', tutor: 'Javier López García', location: 'Biblioteca', type: 'taller' },
 { id: 3, title: 'Revisar el material de la próxima tutoría', day: '', time: '', tutor: '', location: '', type: 'action' },
]);
  const [agendaEvents, setAgendaEvents] = useState(mockSessions);
  const [modal, setModal] = useState<{ open: boolean; type: string; data: Partial<AgendaEvent> | null }>({ 
    open: false, type: '', data: null 
  });
  const saveItem = () => {
    if (!modal.data?.title) return;
    const newItem = { ...modal.data, id: modal.data.id || Date.now(), type: modal.type };
    const updated = modal.data.id 
      ? items.map(i => i.id === modal.data?.id ? newItem : i) 
      : [...items, newItem];
    
    setItems(updated as AgendaEvent[]);
    setModal({ open: false, type: '', data: null });
  };

  const deleteItem = (id: number) => {
    const updated = items.filter(i => i.id !== id);
    setItems(updated);
  };
  return (
    <div className="space-y-6">    
      {/* 1. Próxima Tutoría */}
      <div className="bg-[#F2FBF9] p-4 rounded-lg shadow-sm border-l-4 border-[#1F6662] max-w-md ">
        <h3 className="font-semibold text-lg text-[#0B2828] mb-2">
          Próxima Tutoría
        </h3>

        {agendaEvents.length > 0 ? (

          <div className="space-y-1 text-sm">

            <p>
              <span className="font-semibold text-[#1F6662]">
                Materia:
              </span>
              {" "}
              {agendaEvents[0].tema}
            </p>

            <p>
              <span className="font-semibold text-[#1F6662]">
                Fecha:
              </span>
              {" "}
              {agendaEvents[0].fecha}
            </p>

            <p>
              <span className="font-semibold text-[#1F6662]">
                Lugar:
              </span>
              {" "}
              {agendaEvents[0].lugar}
            </p>

            <p>
              <span className="font-semibold text-[#1F6662]">
                Estado:
              </span>
              {" "}
              {agendaEvents[0].estado}
            </p>

          </div>

        ) : (

          <p className="text-[#53716E] italic">
            Sin sesiones programadas.
          </p>

        )}

      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 2. Sesiones Virtuales (Con Editar/Eliminar) */}
        <div className="bg-[#F2EDF8] p-6 rounded-lg border border-[#F2EDF8]">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-[#6F548F] flex items-center gap-2"><Clock className="w-4 h-4"/> Sesiones Virtuales</h3>
            <button onClick={() => setModal({ open: true, type: 'virtual', data: {} })} className="bg-[#F2EDF8] p-1 rounded-full"><Plus className="w-5 h-5 text-[#6F548F]"/></button>
          </div>
          {items.filter(i => i.type === 'virtual').map(i => (
            <div key={i.id} className="bg-white p-3 rounded shadow-sm mb-2 border-l-4 border-[#6F548F] flex justify-between items-center">
              <div>
                <p className="font-bold text-sm text-[#6F548F]">{i.title}</p>
                <p className="text-xs text-[#53716E]">Mtro: {i.tutor} | {i.time}</p>
              </div>
              <div className="flex gap-2">
                <button onClick={() => setModal({ open: true, type: 'virtual', data: i })}><Edit className="w-4 h-4 text-[#1F6662]"/></button>
                <button onClick={() => deleteItem(i.id)}><Trash2 className="w-4 h-4 text-[#B83F4B]"/></button>
              </div>
            </div>
          ))}
        </div>

        {/* 3. Talleres de Estudio (Con Editar/Eliminar) */}
        <div className="bg-[#E4F3ED] p-6 rounded-lg border border-[#E4F3ED]">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-[#227F78] flex items-center gap-2"><BookOpen className="w-4 h-4"/> Talleres de Estudio</h3>
            <button onClick={() => setModal({ open: true, type: 'taller', data: {} })} className="bg-[#E4F3ED] p-1 rounded-full"><Plus className="w-5 h-5 text-[#227F78]"/></button>
          </div>
          {items.filter(i => i.type === 'taller').map(i => (
            <div key={i.id} className="bg-white p-3 rounded shadow-sm mb-2 border-l-4 border-[#227F78] flex justify-between items-center">
              <div>
                <p className="font-bold text-sm text-[#227F78]">{i.title}</p>
                <p className="text-xs text-[#53716E]">Mtro: {i.tutor} | Lugar: {i.location} | {i.time}</p>
              </div>
              <div className="flex gap-2">
                <button onClick={() => setModal({ open: true, type: 'taller', data: i })}><Edit className="w-4 h-4 text-[#1F6662]"/></button>
                <button onClick={() => deleteItem(i.id)}><Trash2 className="w-4 h-4 text-[#B83F4B]"/></button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 4. Acciones Importantes (Intacta) */}
      <div className="bg-[#FFF3D6] p-6 rounded-lg border border-[#FFF3D6]">
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-semibold text-[#986314] flex items-center gap-2"><AlertCircle className="w-4 h-4"/> Acciones importantes</h3>
          <button onClick={() => setModal({ open: true, type: 'action', data: {} })} className="bg-[#FFF3D6] p-1 rounded-full"><Plus className="w-5 h-5 text-[#986314]"/></button>
        </div>
        {items.filter(i => i.type === 'action').map(a => (
          <div key={a.id} className="flex justify-between bg-white p-3 rounded mb-2 shadow-sm text-sm border-l-4 border-[#986314]">
            {a.title}
            <div className="flex gap-2">
              <button onClick={() => setModal({ open: true, type: 'action', data: a })}><Edit className="w-3 h-3 text-[#1F6662]"/></button>
              <button onClick={() => deleteItem(a.id)}><Trash2 className="w-3 h-3 text-[#B83F4B]"/></button>
            </div>
          </div>
        ))}
      </div>

      {/* Modal Unificado */}
      {modal.open && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg w-80 space-y-3">
            <h3 className="font-bold mb-2">{modal.data?.id ? 'Editar' : 'Agregar'} {modal.type}</h3>
            <input className="w-full border p-2 rounded" placeholder="Título/Materia" value={modal.data?.title || ''} onChange={e => setModal({...modal, data: {...modal.data, title: e.target.value}})}/>
            {modal.type !== 'action' && (
              <>
                <input className="w-full border p-2 rounded" placeholder="Maestro" value={modal.data?.tutor || ''} onChange={e => setModal({...modal, data: {...modal.data, tutor: e.target.value}})}/>
                <input className="w-full border p-2 rounded" placeholder="Hora" value={modal.data?.time || ''} onChange={e => setModal({...modal, data: {...modal.data, time: e.target.value}})}/>
                {modal.type === 'taller' && <input className="w-full border p-2 rounded" placeholder="Lugar" value={modal.data?.location || ''} onChange={e => setModal({...modal, data: {...modal.data, location: e.target.value}})}/> }
              </>
            )}
            <button onClick={saveItem} className="w-full bg-[#1F6662] text-white p-2 rounded">Guardar</button>
          </div>
        </div>
      )}
    </div>
  );
}