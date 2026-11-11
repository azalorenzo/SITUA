import { X, Calendar, Clock } from 'lucide-react';
import { useState } from 'react';

interface BookingModalProps {
  tutor: {
    name: string;
    subject: string;
    price: number;
  } | null;
  isOpen: boolean;
  onClose: () => void;
}

export function BookingModal({ tutor, isOpen, onClose }: BookingModalProps) {
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedTime, setSelectedTime] = useState('');
  const [message, setMessage] = useState('');

  if (!isOpen || !tutor) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    alert(`¡Reserva confirmada con ${tutor.name}!\nFecha: ${selectedDate}\nHora: ${selectedTime}`);
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex justify-between items-start mb-4">
          <div>
            <h2 className="font-semibold mb-1">Reservar clase con {tutor.name}</h2>
            <p className="text-[#53716E]">{tutor.subject}</p>
          </div>
          <button onClick={onClose} className="text-[#53716E] hover:text-[#53716E]">
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label className="flex items-center gap-2 text-sm mb-2">
              <Calendar className="w-4 h-4" />
              Fecha
            </label>
            <input
              type="date"
              required
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2"
            />
          </div>

          <div className="mb-4">
            <label className="flex items-center gap-2 text-sm mb-2">
              <Clock className="w-4 h-4" />
              Hora
            </label>
            <select
              required
              value={selectedTime}
              onChange={(e) => setSelectedTime(e.target.value)}
              className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2"
            >
              <option value="">Selecciona una hora</option>
              <option value="09:00">09:00 AM</option>
              <option value="10:00">10:00 AM</option>
              <option value="11:00">11:00 AM</option>
              <option value="14:00">02:00 PM</option>
              <option value="15:00">03:00 PM</option>
              <option value="16:00">04:00 PM</option>
              <option value="17:00">05:00 PM</option>
            </select>
          </div>

          <div className="mb-4">
            <label className="text-sm mb-2 block">Mensaje (opcional)</label>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Describe tus objetivos de aprendizaje..."
              className="w-full border border-[#DCE3EA] rounded-lg px-3 py-2 h-24 resize-none"
            />
          </div>

          <div className="bg-[#F5F7FA] rounded-lg p-3 mb-4">
            <div className="flex justify-between items-center">
              <span className="text-[#53716E]">Precio por hora:</span>
              <span className="font-semibold">${tutor.price}</span>
            </div>
          </div>

          <button
            type="submit"
            className="w-full bg-[#1F6662] text-white py-2 rounded-lg hover:bg-[#1C4543] transition-colors"
          >
            Confirmar Reserva
          </button>
        </form>
      </div>
    </div>
  );
}
