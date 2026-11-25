import { Pencil } from 'lucide-react';
import { EditarExpedienteAdministracion } from './GestionAdministracion';
import { useEffect, useState } from 'react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { consultarEstudiantes } from '../servicios/coordinacion';
import type { EstudianteCoordinacion, PaginaEstudiantes } from '../tipos/coordinacion';
import { EstadoConsulta, BusquedaCoordinacion, InicialesPersona, estiloCelda, estiloTarjeta } from './ConsultaCoordinacion';

const nivelesRiesgo = [
  { codigo: 'sin_riesgo', nombre: 'Sin riesgo', color: '#1F6662' },
  { codigo: 'bajo', nombre: 'Riesgo bajo', color: '#227F78' },
  { codigo: 'medio', nombre: 'Riesgo medio', color: '#986314' },
  { codigo: 'alto', nombre: 'Riesgo alto', color: '#B83F4B' },
  { codigo: null, nombre: 'Sin evaluación', color: '#94A3A0' },
];

function ListaEstudiantesCoordinacion() {
  const [editando, cambiarEdicion] = useState<number | null>(null);
  const [busqueda, cambiarBusqueda] = useState('');
  const [busquedaAplicada, cambiarBusquedaAplicada] = useState('');
  const [estado, cambiarEstado] = useState('todos');
  const [pagina, cambiarPagina] = useState(1);
  const [datos, cambiarDatos] = useState<PaginaEstudiantes|null>(null);
  const [cargando, cambiarCargando] = useState(true);
  const [error, cambiarError] = useState('');
  const [intento, cambiarIntento] = useState(0);
  const actualizar=()=>cambiarIntento(anterior=>anterior+1);
  useEffect(()=>{
    if(busqueda===busquedaAplicada)return;
    const espera=window.setTimeout(()=>{cambiarBusquedaAplicada(busqueda);cambiarPagina(1);},300);
    return ()=>window.clearTimeout(espera);
  },[busqueda,busquedaAplicada]);
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCargando(true);cambiarError('');
    consultarEstudiantes({pagina,tamano:10,busqueda:busquedaAplicada,estado},cancelacion.signal)
      .then(respuesta=>{if(!cancelacion.signal.aborted)cambiarDatos(respuesta);})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudo cargar la lista.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCargando(false);});
    return ()=>cancelacion.abort();
  },[pagina,busquedaAplicada,estado,intento]);
  const visibles=datos?.contenido||[];
  const resumen=datos?.resumen;
  const promedio=resumen?.promedio===null||resumen?.promedio===undefined?'Sin datos':resumen.promedio.toFixed(2);
  const distribucionRiesgo=(datos?.distribucionRiesgo||[]).map(nivel=>({...nivel,...nivelesRiesgo.find(opcion=>opcion.codigo===nivel.codigo)}));
  const distribucionSemestres=datos?.distribucionSemestres||[];
  const enRiesgo=datos?.enRiesgo||[];
  const paginaActual=datos?.pagina||1;

  function riesgoDe(estudiante: Pick<EstudianteCoordinacion,'nivelRiesgo'>) {
    return nivelesRiesgo.find(nivel => nivel.codigo === estudiante.nivelRiesgo) || nivelesRiesgo[4];
  }

  return <section className="space-y-6" aria-labelledby="titulo-estudiantes">
    <div className="flex flex-wrap justify-between items-center gap-3">
      <div><h2 id="titulo-estudiantes" className="font-semibold mb-1">Gestión de Estudiantes</h2><p className="text-sm text-[#53716E]">Consulta los estudiantes y su información académica más reciente</p></div>
      <button type="button" onClick={actualizar} disabled={cargando} className="px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50">Actualizar</button>
    </div>
    {!cargando && !error && datos && <>
      <div className="grid sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[['Total de estudiantes', resumen!.total], ['Cuentas activas', resumen!.activos], ['Promedio de registros disponibles', promedio], ['Riesgo medio o alto', resumen!.enRiesgo]].map(([titulo, valor]) =>
          <div key={titulo} className={estiloTarjeta}><p className="text-sm text-[#53716E]">{titulo}</p><p className="font-semibold">{valor}</p></div>)}
      </div>
      <p className="text-xs text-[#53716E]">Resumen del periodo más reciente registrado para cada estudiante; los periodos pueden ser distintos. Promedio calculado con {resumen!.conPromedio} registros. Las cuentas activas indican el acceso al sistema, no la inscripción académica.</p>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Estudiantes por nivel de riesgo</h3>
          {distribucionRiesgo.length ? <ResponsiveContainer width="100%" height={240}><PieChart><Pie data={distribucionRiesgo} cx="50%" cy="45%" innerRadius={45} outerRadius={70} dataKey="cantidad" nameKey="nombre">
            {distribucionRiesgo.map(nivel => <Cell key={nivel.nombre} fill={nivel.color} />)}
          </Pie><Tooltip /><Legend iconSize={10} /></PieChart></ResponsiveContainer> : <p className="text-sm text-[#53716E]">No hay datos para mostrar.</p>}
        </div>
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Distribución por semestre</h3>
          {distribucionSemestres.length ? <ResponsiveContainer width="100%" height={240}><BarChart data={distribucionSemestres}><CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA" /><XAxis dataKey="semestre" /><YAxis allowDecimals={false} /><Tooltip /><Bar dataKey="cantidad" name="Estudiantes" fill="#1F6662" radius={[6, 6, 0, 0]} /></BarChart></ResponsiveContainer> : <p className="text-sm text-[#53716E]">No hay semestres registrados.</p>}
          <p className="text-xs text-[#53716E]">Sin semestre registrado: {resumen!.sinSemestre}</p>
        </div>
        <div className="bg-white rounded-lg p-6 shadow-sm"><h3 className="font-semibold mb-4">Estudiantes con riesgo medio o alto</h3>
          {enRiesgo.length ? <ul className="space-y-3 max-h-64 overflow-y-auto">{enRiesgo.map(estudiante => <li key={estudiante.id} className="p-3 bg-[#F5F7FA] rounded-lg"><p className="text-sm font-medium">{estudiante.nombre}</p><p className="text-xs" style={{ color: riesgoDe(estudiante).color }}>{riesgoDe(estudiante).nombre}</p><p className="text-xs text-[#53716E]">{estudiante.matricula} · {estudiante.periodo}</p></li>)}</ul> : <p className="text-sm text-[#53716E]">No hay registros con riesgo medio o alto. Sin evaluación: {resumen!.sinEvaluacion}.</p>}
        </div>
      </div>
    </>}
    <div className="bg-white rounded-lg shadow-sm" aria-busy={cargando}>
      <div className="p-6 border-b border-[#DCE3EA] flex flex-wrap justify-between items-center gap-4"><h3 className="font-semibold">Lista de estudiantes</h3>
        <BusquedaCoordinacion tipo="estudiante" busqueda={busqueda} alBuscar={cambiarBusqueda} estado={estado} alCambiarEstado={valor=>{cambiarEstado(valor);cambiarPagina(1);}} />
      </div>
      <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar} />
      {!cargando && !error && datos && <>
        <p role="status" className="px-6 pt-4 text-sm text-[#53716E]">Mostrando {datos.totalRegistros?(paginaActual-1)*datos.tamano+1:0}–{Math.min(paginaActual*datos.tamano,datos.totalRegistros)} de {datos.totalRegistros} estudiantes</p>
        {visibles.length === 0 ? <p className="p-6 text-[#53716E]">{resumen!.total === 0 ? 'No hay estudiantes registrados.' : 'No se encontraron estudiantes con esos criterios.'}</p>
          : <div className="overflow-x-auto"><table className="w-full" aria-label="Lista de estudiantes"><thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]"><tr>
            {['Estudiante', 'Matrícula', 'Carrera', 'Periodo', 'Semestre', 'Promedio', 'Riesgo', 'Estado', 'Acciones'].map(titulo => <th key={titulo} scope="col" className={estiloCelda}>{titulo}</th>)}
          </tr></thead><tbody>{visibles.map(estudiante => <tr key={estudiante.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
            <td className={estiloCelda}><div className="flex items-center gap-3"><InicialesPersona nombre={estudiante.nombre} /><div><p className="font-medium">{estudiante.nombre}</p><p className="text-xs text-[#53716E]">{estudiante.correo}</p></div></div></td>
            <td className={estiloCelda}>{estudiante.matricula}</td><td className={estiloCelda}>{estudiante.carrera || 'Sin carrera registrada'}</td>
            <td className={estiloCelda}>{estudiante.periodo || 'Sin periodo registrado'}</td><td className={estiloCelda}>{estudiante.semestre ?? 'Sin datos'}</td>
            <td className={estiloCelda}>{estudiante.promedio === null ? 'Sin datos' : estudiante.promedio.toFixed(2)}</td>
            <td className={estiloCelda}><span style={{ color: riesgoDe(estudiante).color }}>{riesgoDe(estudiante).nombre}</span></td>
            <td className={estiloCelda}>{estudiante.estado === 'activo' ? 'Activa' : 'Inactiva'}</td><td className={estiloCelda}><button type="button" aria-label={`Editar expediente de ${estudiante.nombre}`} className="p-2 text-[#1F6662]" onClick={()=>cambiarEdicion(estudiante.id)}><Pencil className="w-4 h-4"/></button></td>
          </tr>)}</tbody></table></div>}
        {datos.totalPaginas>0&&<nav aria-label="Paginación de estudiantes" className="p-6 flex flex-wrap items-center justify-end gap-2">
          <button type="button" disabled={paginaActual<=1} onClick={()=>cambiarPagina(paginaActual-1)} className="px-3 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50">Anterior</button>
          {Array.from({length:datos.totalPaginas},(_,indice)=>indice+1).map(numero=><button type="button" key={numero} aria-label={`Página ${numero}`} aria-current={numero===paginaActual?'page':undefined} onClick={()=>cambiarPagina(numero)} className={`px-3 py-2 rounded-lg border border-[#DCE3EA] ${numero===paginaActual?'bg-[#1F6662] text-white':''}`}>{numero}</button>)}
          <button type="button" disabled={paginaActual>=datos.totalPaginas} onClick={()=>cambiarPagina(paginaActual+1)} className="px-3 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50">Siguiente</button>
        </nav>}
      </>}
    </div>
  {editando && <EditarExpedienteAdministracion estudianteId={editando} alCerrar={()=>cambiarEdicion(null)} alGuardar={()=>{cambiarEdicion(null);actualizar();}}/>}</section>;
}

export { ListaEstudiantesCoordinacion as StudentsManagementView };
