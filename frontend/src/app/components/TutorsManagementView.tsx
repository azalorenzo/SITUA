import { useState } from 'react';
import { consultarTutores } from '../servicios/coordinacion';
import { usarConsultaCoordinacion, EstadoConsulta, BusquedaCoordinacion, InicialesPersona, normalizarBusqueda, estiloCelda, estiloTarjeta } from './ConsultaCoordinacion';

function ListaTutoresCoordinacion() {
  const { datos: tutores, cargando, error, actualizar } = usarConsultaCoordinacion(consultarTutores);
  const [busqueda, cambiarBusqueda] = useState('');
  const [estado, cambiarEstado] = useState('todos');
  const termino = normalizarBusqueda(busqueda);
  const visibles = tutores.filter(tutor => (estado === 'todos' || tutor.estado === estado)
    && normalizarBusqueda(`${tutor.nombre} ${tutor.correo} ${tutor.departamento}`).includes(termino));

  return <section className="space-y-6" aria-labelledby="titulo-tutores">
    <div className="flex flex-wrap justify-between items-center gap-3">
      <div><h2 id="titulo-tutores" className="font-semibold mb-1">Gestión de Tutores</h2><p className="text-sm text-[#53716E]">Consulta los tutores y sus datos de contacto</p></div>
      <button type="button" onClick={actualizar} disabled={cargando} className="px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50">Actualizar</button>
    </div>
    {!cargando && !error && <div className="grid sm:grid-cols-3 gap-4">
      <div className={estiloTarjeta}><p className="text-sm text-[#53716E]">Total de tutores</p><p className="font-semibold">{tutores.length}</p></div>
      <div className={estiloTarjeta}><p className="text-sm text-[#53716E]">Cuentas activas</p><p className="font-semibold">{tutores.filter(tutor => tutor.estado === 'activo').length}</p></div>
      <div className={estiloTarjeta}><p className="text-sm text-[#53716E]">Cuentas inactivas</p><p className="font-semibold">{tutores.filter(tutor => tutor.estado === 'inactivo').length}</p></div>
    </div>}
    <div className="bg-white rounded-lg shadow-sm" aria-busy={cargando}>
      <div className="p-6 border-b border-[#DCE3EA] flex flex-wrap justify-between items-center gap-4">
        <h3 className="font-semibold">Lista de tutores</h3>
        <BusquedaCoordinacion tipo="tutor" busqueda={busqueda} alBuscar={cambiarBusqueda} estado={estado} alCambiarEstado={cambiarEstado} />
      </div>
      <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar} />
      {!cargando && !error && <>
        <p role="status" className="px-6 pt-4 text-sm text-[#53716E]">{visibles.length} de {tutores.length} tutores</p>
        {visibles.length === 0 ? <p className="p-6 text-[#53716E]">{tutores.length === 0 ? 'No hay tutores registrados.' : 'No se encontraron tutores con esos criterios.'}</p>
          : <div className="overflow-x-auto"><table className="w-full" aria-label="Lista de tutores"><thead className="bg-[#F5F7FA] border-b border-[#DCE3EA]"><tr>
            {['Tutor', 'Departamento', 'Teléfono', 'Estado'].map(titulo => <th scope="col" key={titulo} className={estiloCelda}>{titulo}</th>)}
          </tr></thead><tbody>{visibles.map(tutor => <tr key={tutor.id} className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA]">
            <td className={estiloCelda}><div className="flex items-center gap-3"><InicialesPersona nombre={tutor.nombre} /><div><p className="font-medium">{tutor.nombre}</p><p className="text-xs text-[#53716E]">{tutor.correo}</p></div></div></td>
            <td className={estiloCelda}>{tutor.departamento}</td><td className={estiloCelda}>{tutor.telefono || 'Sin teléfono'}</td>
            <td className={estiloCelda}><span className={tutor.estado === 'activo' ? 'text-[#227F78]' : 'text-[#53716E]'}>{tutor.estado === 'activo' ? 'Activa' : 'Inactiva'}</span></td>
          </tr>)}</tbody></table></div>}
      </>}
    </div>
  </section>;
}

export { ListaTutoresCoordinacion as TutorsManagementView };
