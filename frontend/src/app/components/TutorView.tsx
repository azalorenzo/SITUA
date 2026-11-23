import { Users, CheckCircle, FileText, Calendar } from 'lucide-react';
import { consultarInicioTutor } from '../servicios/inicioTutor';
import { usarConsultaTutor } from '../servicios/usarConsultaTutor';
import type { EstudianteInicioTutor } from '../tipos/inicioTutor';
import { EstadoConsulta } from './ConsultaCoordinacion';

// Mismas etiquetas y colores de riesgo que la pantalla Mis estudiantes.
function etiquetaRiesgo(nivel: EstudianteInicioTutor['nivelRiesgo']): string {
  switch (nivel) {
    case 'alto': return 'Alto';
    case 'medio': return 'Medio';
    case 'bajo': return 'Bajo';
    case 'sin_riesgo': return 'Sin riesgo';
    default: return 'Sin evaluar';
  }
}

function colorRiesgo(nivel: EstudianteInicioTutor['nivelRiesgo']): string {
  switch (nivel) {
    case 'alto': return 'bg-[#FCEBED] text-[#B83F4B]';
    case 'medio': return 'bg-[#FFF3D6] text-[#986314]';
    case 'bajo':
    case 'sin_riesgo': return 'bg-[#E4F3ED] text-[#227F78]';
    default: return 'bg-[#DCE3EA] text-[#0B2828]';
  }
}

export function TutorView() {
  const consulta = usarConsultaTutor(consultarInicioTutor);
  const inicio = consulta.datos;
  const students = (inicio?.estudiantesDestacados ?? []).map(e => ({
    name: e.nombre, career: e.carrera, status: etiquetaRiesgo(e.nivelRiesgo), statusColor: colorRiesgo(e.nivelRiesgo)
  }));

  return (
    <div className="space-y-6">
      <EstadoConsulta cargando={consulta.cargando} error={consulta.error} alReintentar={consulta.actualizar} />

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 - h full">
        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2FBF9] p-3 rounded-lg">
              <Users className="w-6 h-6 text-[#1F6662]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Mis estudiantes</p>
              <p className="font-semibold">{inicio?.estudiantes ?? '—'}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#E4F3ED] p-3 rounded-lg">
              <CheckCircle className="w-6 h-6 text-[#227F78]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Tutorías realizadas</p>
              <p className="font-semibold">{inicio?.sesionesRealizadas ?? '—'}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#FFF3D6] p-3 rounded-lg">
              <FileText className="w-6 h-6 text-[#986314]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Reportes pendientes</p>
              <p className="font-semibold">{inicio?.reportesPendientes ?? '—'}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg p-6 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="bg-[#F2EDF8] p-3 rounded-lg">
              <Calendar className="w-6 h-6 text-[#6F548F]" />
            </div>
            <div>
              <p className="text-sm text-[#53716E]">Sesiones programadas</p>
              <p className="font-semibold">{inicio?.sesionesProgramadas ?? '—'}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Grid */}
<div className="w-full">

  <div className="bg-white rounded-lg p-6 shadow-sm">

    <div className="flex justify-between items-center mb-4">

      <h3 className="font-semibold">
        Mis estudiantes
      </h3>

      <button className="text-[#1F6662] text-sm hover:underline">
        Ver todos
      </button>

    </div>

    <table className="w-full text-sm">

      <thead className="bg-[#F5F7FA]">

        <tr>

          <th className="text-left py-3 px-4">
            Estudiante
          </th>

          <th className="text-left py-3 px-4">
            Carrera
          </th>

          <th className="text-left py-3 px-4">
            Estado
          </th>

        </tr>

      </thead>

      <tbody>

        {students.map((student, index) => (

          <tr
            key={index}
            className="border-b hover:bg-[#F5F7FA]"
          >

            <td className="py-3 px-4">

              <div className="flex items-center gap-3">

                <div className="w-10 h-10 bg-gradient-to-br from-[#1F6662] to-[#1F6662] rounded-full flex items-center justify-center text-white font-medium">

                  {student.name
                    .split(' ')
                    .map(n => n[0])
                    .join('')
                    .slice(0, 2)}

                </div>

                <span className="font-medium">

                  {student.name}

                </span>

              </div>

            </td>

            <td className="py-3 px-4 text-[#53716E]">

              {student.career}

            </td>

            <td className="py-3 px-4">

              <span
                className={`text-xs px-3 py-1 rounded-full ${student.statusColor}`}
              >

                {student.status}

              </span>

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