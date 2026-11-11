import { useState } from "react";
import logoITO from "../../assets/logoITO.png";

export default function Login({ onEnter }: { onEnter: (role: 'coordinator' | 'student' | 'tutor') => void }) {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<'coordinator' | 'student' | 'tutor'>('tutor');
  const iniciarSesion = () => onEnter(role);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-100 via-white to-indigo-200">
      <div className="bg-white w-[400px] rounded-3xl shadow-2xl p-10">
        <div className="flex flex-col items-center mb-8">
          <img src={logoITO} alt="ITO" className="w-24 h-24 object-contain mb-4" />

          <h1 className="text-3xl font-bold text-gray-800">Tutorías</h1>

          <p className="text-gray-500 mt-2 text-center">Instituto Tecnológico de Oaxaca</p>
        </div>

        <div className="space-y-5">
          <p className="text-sm text-gray-500">Mockup interactivo. Accede sin credenciales reales.</p>
          <label className="block text-sm text-gray-600">Perfil de demostración
            <select value={role} onChange={e => setRole(e.target.value as typeof role)} className="w-full mt-2 px-4 py-3 border border-gray-300 rounded-xl">
              <option value="coordinator">Coordinación</option><option value="tutor">Tutor</option><option value="student">Estudiante</option>
            </select>
          </label>
          <div>
            <label className="block text-sm text-gray-600 mb-2">Correo electrónico</label>

            <input
              type="email"
              placeholder="ejemplo@correo.com"
              value={correo}
              onChange={(e) => setCorreo(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm text-gray-600 mb-2">Contraseña</label>

            <input
              type="password"
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  iniciarSesion();
                }
              }}
              className="w-full px-4 py-3 border border-gray-300 rounded-xl outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <button
            onClick={iniciarSesion}
            className="w-full bg-blue-600 hover:bg-blue-700 transition-all text-white py-3 rounded-xl font-semibold shadow-lg"
          >
            Iniciar Sesión
          </button>
        </div>

        <p className="text-center text-sm text-gray-400 mt-8">© 2026 Sistema de Tutorías</p>
      </div>
    </div>
  );
}
