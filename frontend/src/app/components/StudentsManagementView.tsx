import { useState } from 'react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { consultarEstudiantes } from '../servicios/coordinacion';
import type { EstudianteCoordinacion } from '../tipos/coordinacion';
import { usarConsultaCoordinacion, EstadoConsulta, BusquedaCoordinacion, InicialesPersona, normalizarBusqueda, estiloCelda, estiloTarjeta } from './ConsultaCoordinacion';

const nivelesRiesgo = [
  { codigo: 'sin_riesgo', nombre: 'Sin riesgo', color: '#1F6662' },
  { codigo: 'bajo', nombre: 'Riesgo bajo', color: '#227F78' },
  { codigo: 'medio', nombre: 'Riesgo medio', color: '#986314' },
  { codigo: 'alto', nombre: 'Riesgo alto', color: '#B83F4B' },
  { codigo: null, nombre: 'Sin evaluación', color: '#94A3A0' },
];

function ListaEstudiantesCoordinacion() {
  const { datos: estudiantes, cargando, error, actualizar } = usarConsultaCoordinacion(consultarEstudiantes);
  const [busqueda, cambiarBusqueda] = useState('');
  const [estado, cambiarEstado] = useState('todos');
  const termino = normalizarBusqueda(busqueda);
  const visibles = estudiantes.filter(estudiante => (estado === 'todos' || estudiante.estado === estado)
    && normalizarBusqueda(`${estudiante.nombre} ${estudiante.correo} ${estudiante.matricula} ${estudiante.carrera || ''}`).includes(termino));
  const conPromedio = estudiantes.filter(estudiante => estudiante.promedio !== null);
  const promedio = conPromedio.length ? (conPromedio.reduce((suma, estudiante) => suma + estudiante.promedio!, 0) / conPromedio.length).toFixed(2) : 'Sin datos';
  const distribucionRiesgo = nivelesRiesgo.map(nivel => ({ ...nivel, cantidad: estudiantes.filter(estudiante => estudiante.nivelRiesgo === nivel.codigo).length })).filter(nivel => nivel.cantidad > 0);
  const semestres = [...new Set(estudiantes.map(estudiante => estudiante.semestre).filter((semestre): semestre is number => semestre !== null))].sort((primero, segundo) => primero - segundo);
  const distribucionSemestres = semestres.map(semestre => ({ semestre: `${semestre}°`, cantidad: estudiantes.filter(estudiante => estudiante.semestre === semestre).length }));
  const enRiesgo = estudiantes.filter(estudiante => estudiante.nivelRiesgo === 'medio' || estudiante.nivelRiesgo === 'alto');

  function riesgoDe(estudiante: EstudianteCoordinacion) {
    return nivelesRiesgo.find(nivel => nivel.codigo === estudiante.nivelRiesgo) || nivelesRiesgo[4];
  }

  return <section className="space-y-6" aria-labelledby="titulo-estudiantes">
    <div className="flex flex-wrap justify-between items-center gap-3">
      <div><h2 id="titulo-estudiantes" className="font-semibold mb-1">Gestión de Estudiantes</h2><p className="text-sm text-[#53716E]">Consulta los estudiantes y su información académica más reciente</p></div>
      <button type="button" onClick={actualizar} disabled={cargando} className="px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50">Actualizar</button>
    </div>
    {!cargando && !error && <>
      <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[['Total de estudiantes', estudiantes.length], ['Cuentas activas', estudiantes.filter(estudiante => estudiante.estado === 'activo').length], ['Promedio de registros disponibles', promedio], ['Riesgo medio o alto', enRiesgo.length]].map(([titulo, valor]) =>
          <div key={titulo} className={estiloTarjeta}><p className="text-sm text-[#53716E]">{titulo}</p><p className="font-semibold">{valor}</p></div>)}
      </div>
      <p className="text-xs text-[#53716E]">Resumen del periodo más reciente registrado para cada estudiante; los periodos pueden ser distintos. Promedio calculado con {conPromedio.length} registros. Las cuentas activas indican el acceso al sistema, no la inscripción académica.</p>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Estudiantes por nivel de riesgo</h3>
          {distribucionRiesgo.length ? <ResponsiveContainer width="100%" height={240}><PieChart><Pie data={distribucionRiesgo} cx="50%" cy="45%" innerRadius={45} outerRadius={70} dataKey="cantidad" nameKey="nombre">
            {distribucionRiesgo.map(nivel => <Cell key={nivel.nombre} fill={nivel.color} />)}
          </Pie><Tooltip /><Legend iconSize={10} /></PieChart></ResponsiveContainer> : <p className="text-sm text-[#53716E]">No hay datos para mostrar.</p>}
        </div>
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Distribución por semestre</h3>
          {distribucionSemestres.length ? <ResponsiveContainer width="100%" height={240}><BarChart data={distribucionSemestres}><CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA" /><XAxis dataKey="semestre" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="cantidad" name="Estudiantes" fill="#1F6662" radius={[6, 6, 0, 0]} /></BarChart></ResponsiveContainer> : <p className="text-sm text-[#53716E]">No hay semestres registrados.</p>}
          <p className="text-xs text-[#53716E]">Sin semestre registrado: {estudiantes.filter(estudiante => estudiante.semestre === null).length}</p>
        </div>
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Estudiantes con riesgo medio o alto</h3>
          {enRiesgo.length ? <ul className="space-y-3 max-h-64 overflow-y-auto">{enRiesgo.map(estudiante => <li key={estudiante.id} className="p-3 bg-[#F5F7FA] rounded-lg"><p className="text-sm font-medium">{estudiante.nombre}</p><p className="text-xs" style={{ color: riesgoDe(estudiante).color }}>{riesgoDe(estudiante).nombre}</p><p className="text-xs text-[#53716E]">{estudiante.matricula} · {estudiante.periodo}</p></li>)}</ul> : <p className="text-sm text-[#53716E]">No hay registros con riesgo medio o alto. Sin evaluación: {estudiantes.filter(estudiante => estudiante.nivelRiesgo === null).length}.</p>}
        </div>
      </div>
    </>}
    <div className="bg-white rounded-lg shadow-sm" aria-busy={cargando}>
      <div className="p-6 border-b border-[#DCE3EA] flex flex-wrap justify-between items-center gap-4"><h3 className="font-semibold">Lista de estudiantes</h3>
        <BusquedaCoordinacion tipo="estudiante" busqueda={busqueda} alBuscar={cambiarBusqueda} estado={estado} alCambiarEstado={cambiarEstado} />
      </div>
      <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar} />
      {!cargando && !error && <>
        <p role="status" className="px-6 pt-4 text-sm text-[#53716E]">{visibles.length} de {estudiantes.length} estudiantes</p>
        {visibles.length === 0 ? <p className="p-6 text-[#53716E]">{estudiantes.length === 0 ? 'No hay estudiantes registrados.' : 'No se encontraron estudiantes con esos criterios.'}</p>
          : <div className="overflow-x-auto"><table className="w-full" aria-label="Lista de estudiantes"><thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]"><tr>
            {['Estudiante', 'Matrícula', 'Carrera', 'Periodo', 'Semestre', 'Promedio', 'Riesgo', 'Estado'].map(titulo => <th key={titulo} scope="col" className={estiloCelda}>{titulo}</th>)}
          </tr></thead><tbody>{visibles.map(estudiante => <tr key={estudiante.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
            <td className={estiloCelda}><div className="flex items-center gap-3"><InicialesPersona nombre={estudiante.nombre} /><div><p className="font-medium">{estudiante.nombre}</p><p className="text-xs text-[#53716E]">{estudiante.correo}</p></div></div></td>
            <td className={estiloCelda}>{estudiante.matricula}</td><td className={estiloCelda}>{estudiante.carrera || 'Sin carrera registrada'}</td>
            <td className={estiloCelda}>{estudiante.periodo || 'Sin periodo registrado'}</td><td className={estiloCelda}>{estudiante.semestre ?? 'Sin datos'}</td>
            <td className={estiloCelda}>{estudiante.promedio === null ? 'Sin datos' : estudiante.promedio.toFixed(2)}</td>
            <td className={estiloCelda}><span style={{ color: riesgoDe(estudiante).color }}>{riesgoDe(estudiante).nombre}</span></td>
            <td className={estiloCelda}>{estudiante.estado === 'activo' ? 'Activa' : 'Inactiva'}</td>
          </tr>)}</tbody></table></div>}
      </>}
    </div>
  </section>;
}

export { ListaEstudiantesCoordinacion as StudentsManagementView };
