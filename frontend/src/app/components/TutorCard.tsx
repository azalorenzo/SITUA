import { Star, Clock, DollarSign } from 'lucide-react';

interface TutorCardProps {
  tutor: {
    id: number;
    name: string;
    subject: string;
    rating: number;
    reviews: number;
    price: number;
    experience: string;
    avatar: string;
    availability: string;
  };
  onBook: () => void;
}

export function TutorCard({ tutor, onBook }: TutorCardProps) {
  return (
    <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
      <div className="flex items-start gap-4 mb-4">
        <img
          src={tutor.avatar}
          alt={tutor.name}
          className="w-16 h-16 rounded-full object-cover"
        />
        <div className="flex-1">
          <h3 className="font-semibold mb-1">{tutor.name}</h3>
          <p className="text-[#53716E] mb-2">{tutor.subject}</p>
          <div className="flex items-center gap-1">
            <Star className="w-4 h-4 fill-[#986314] text-[#986314]" />
            <span className="font-medium">{tutor.rating}</span>
            <span className="text-[#53716E]">({tutor.reviews} reseñas)</span>
          </div>
        </div>
      </div>

      <p className="text-[#0B2828] mb-4">{tutor.experience}</p>

      <div className="flex items-center justify-between mb-4 text-sm">
        <div className="flex items-center gap-2 text-[#53716E]">
          <Clock className="w-4 h-4" />
          <span>{tutor.availability}</span>
        </div>
        <div className="flex items-center gap-1 font-semibold text-[#227F78]">
          <DollarSign className="w-4 h-4" />
          <span>{tutor.price}/hora</span>
        </div>
      </div>

      <button
        onClick={onBook}
        className="w-full bg-[#1F6662] text-white py-2 rounded-lg hover:bg-[#1C4543] transition-colors"
      >
        Reservar Clase
      </button>
    </div>
  );
}
