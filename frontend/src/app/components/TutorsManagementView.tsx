import { mockTutors } from '../mock/data';
import { Users, CheckCircle, Clock, Star, AlertTriangle, Search, Filter, Eye, Edit, Trash2, Plus } from 'lucide-react';
import { useState } from 'react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';

export function TutorsManagementView() {
  const [tutors, setTutors] = useState(mockTutors);
  const [showModal, setShowModal] = useState(false);

  const [nombre, setNombre] = useState("");
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [departamento, setDepartamento] = useState("");
  const [editando, setEditando] = useState(false);
  const [idTutorEditar, setIdTutorEditar] = useState("");
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const [tutorEliminar, setTutorEliminar] = useState<any>(null);
  
  const agregarTutor = () => {
    if (!nombre.trim() || !departamento.trim()) return;
    setTutors(prev => [...prev, { id_tutor: String(Date.now()), nombre, correo, departamento }]);
    setShowModal(false); setNombre(''); setCorreo(''); setPassword(''); setDepartamento('');
  };
  const editarTutor = () => {
    setTutors(prev => prev.map(t => t.id_tutor === idTutorEditar ? { ...t, nombre, departamento } : t));
    setShowModal(false);
  };
const abrirEditarTutor = (tutor: any) => {

  setEditando(true);

  setShowModal(true);

  setIdTutorEditar(tutor.id_tutor);



  setNombre(tutor.nombre);

  setDepartamento(tutor.departamento);

};

const eliminarTutor = () => {
  if (!tutorEliminar) return;
  setTutors(prev => prev.filter(t => t.id_tutor !== tutorEliminar.id_tutor));
  setShowDeleteModal(false); setTutorEliminar(null);
};

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-semibold mb-1">Gestión de Tutores</h2>
          
        </div>
            {
      showModal && (

        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">

          <div className="bg-white rounded-xl p-6 w-[400px]">

            <h2 className="text-xl font-semibold mb-4">
              Actualizar tutor
            </h2>

            <div className="space-y-4">

              <input
                type="text"
                placeholder="Nombre"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                className="w-full border border-[#DCE3EA] rounded-lg px-4 py-2"
              />

              <input
                type="email"
                placeholder="Correo"
                value={correo}
                onChange={(e) => setCorreo(e.target.value)}
                className="w-full border border-[#DCE3EA] rounded-lg px-4 py-2"
              />

              <input
                type="password"
                placeholder="Contraseña"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full border border-[#DCE3EA] rounded-lg px-4 py-2"
              />

              <input
                type="text"
                placeholder="Departamento"
                value={departamento}
                onChange={(e) => setDepartamento(e.target.value)}
                className="w-full border border-[#DCE3EA] rounded-lg px-4 py-2"
              />

            </div>

            <div className="flex justify-end gap-3 mt-6">

              <button
                onClick={() => setShowModal(false)}
                className="px-4 py-2 border rounded-lg"
              >
                Cancelar
              </button>

              <button
                onClick={editando ? editarTutor : agregarTutor}
                className="px-4 py-2 bg-[#1F6662] text-white rounded-lg"
              >
                Guardar
                
              </button>

            </div>

          </div>

        </div>

      )


    }

      {
  showDeleteModal && (

    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">

      <div className="bg-white rounded-xl p-6 w-[400px]">

        <h2 className="text-xl font-semibold mb-4 text-[#B83F4B]">
          Eliminar tutor
        </h2>

        <p className="text-[#0B2828]">
          ¿Seguro que deseas eliminar a:
        </p>

        <p className="font-semibold mt-2">
          {tutorEliminar?.nombre} ?
        </p>

        <div className="flex justify-end gap-3 mt-6">

          <button
            onClick={() => setShowDeleteModal(false)}
            className="px-4 py-2 border rounded-lg"
          >
            Cancelar
          </button>

          <button
            onClick={eliminarTutor}
            className="px-4 py-2 bg-[#B83F4B] text-white rounded-lg"
          >
            Eliminar
          </button>

        </div>

      </div>

    </div>

  )
}


        <button 
          onClick={() => { setEditando(false); setNombre(''); setCorreo(''); setPassword(''); setDepartamento(''); setShowModal(true); }}
          className="flex items-center gap-2 bg-[#1F6662] text-white px-4 py-2 rounded-lg hover:bg-[#1C4543]" 
          >
        
          <Plus className="w-4 h-4" />
          Agregar tutor
          
        </button>
      </div>

      {/* Stats Cards */}


    

      {/* Tutors Table */}
      <div className="bg-white rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#DCE3EA] flex justify-between items-center">
          <h3 className="font-semibold">Lista de tutores</h3>
          <div className="flex gap-3">
            <div className="relative">
              <Search className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 transform -translate-y-1/2" />
              <input
                type="text"
                placeholder="Buscar tutor..."
                className="pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]"
              />
            </div>
            <button className="flex items-center gap-2 px-4 py-2 border border-[#DCE3EA] rounded-lg hover:bg-[#F5F7FA]">
              <Filter className="w-4 h-4" />
              Filtros
            </button>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]">
  <tr>

    <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">
      Tutor
    </th>

    <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">
      Departamento
    </th>

    <th className="text-left py-4 px-6 text-sm font-semibold text-[#0B2828]">
      Acciones
    </th>

  </tr>
</thead>
          <tbody>
  {tutors.map((tutor) => (
    <tr
      key={tutor.id_tutor}
      className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]"
    >
      <td className="py-4 px-6">
        <div className="flex items-center gap-3">

          <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium">
            {tutor.nombre?.split(' ')[1]?.charAt(0)}
            {tutor.nombre?.split(' ')[2]?.charAt(0)}
          </div>

          <div>
            <p className="font-medium text-sm">
              {tutor.nombre}
            </p>

            <p className="text-xs text-[#53716E]">
              {tutor.departamento}
            </p>
          </div>

        </div>
      </td>

      <td className="py-4 px-6 text-sm text-[#0B2828]">
        {tutor.departamento}
      </td>

      <td className="py-4 px-6">
  <div className="flex gap-2">

    <button
      onClick={() => abrirEditarTutor(tutor)}
      className="p-1 hover:bg-[#DCE3EA] rounded"
    >
      <Edit className="w-4 h-4 text-[#53716E]" />
    </button>

    <button
      onClick={() => {

  setTutorEliminar(tutor);

  setShowDeleteModal(true);

}}

      className="p-1 hover:bg-[#DCE3EA] rounded"
    >
      <Trash2 className="w-4 h-4 text-[#B83F4B]" />
    </button>

  </div>
</td>
      

    </tr>
  ))}
</tbody>  
          </table>
        </div>
      </div>
    </div>
  );
}
