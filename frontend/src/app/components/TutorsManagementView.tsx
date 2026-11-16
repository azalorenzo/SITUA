import { useEffect,useState } from 'react';
import { Plus,Pencil,UserRoundX,UserRoundCheck,RefreshCw,Search,ChevronLeft,ChevronRight } from 'lucide-react';
import { consultarDepartamentos } from '../servicios/tutores';
import { usarPaginaTutores } from './usarPaginaTutores';
import { usarConsultaCoordinacion,EstadoConsulta,InicialesPersona } from './ConsultaCoordinacion';
import { FormularioTutor } from './FormularioTutor';
import type { OperacionTutor } from './FormularioTutor';
import '../../styles/tutores.css';

function ListaTutoresCoordinacion() {
  const [busqueda,cambiarBusqueda]=useState('');
  const [busquedaAplicada,cambiarBusquedaAplicada]=useState('');
  const [estado,cambiarEstado]=useState('todos');
  const [departamentoId,cambiarDepartamentoId]=useState('');
  const [pagina,cambiarPagina]=useState(1);
  const [operacion,cambiarOperacion]=useState<OperacionTutor|null>(null);
  const [aviso,cambiarAviso]=useState('');
  const tutores=usarPaginaTutores({pagina,tamano:5,busqueda:busquedaAplicada,estado,departamentoId});
  const departamentos=usarConsultaCoordinacion(consultarDepartamentos);
  useEffect(()=>{
    if(busqueda===busquedaAplicada)return;
    const espera=window.setTimeout(()=>{cambiarBusquedaAplicada(busqueda);cambiarPagina(1);},300);
    return ()=>window.clearTimeout(espera);
  },[busqueda,busquedaAplicada]);
  const datos=tutores.datos;
  const filas=tutores.cargando||tutores.error?[]:datos?.contenido||[];
  const paginaActual=datos?.pagina||1;
  const paginas=datos?.totalPaginas||0;
  const total=datos?.totalRegistros||0;
  function actualizar(){tutores.actualizar();departamentos.actualizar();}
  return <section className="pagina-tutores" aria-labelledby="titulo-tutores">
    <header className="encabezado-tutores"><div><h2 id="titulo-tutores">Gestión de Tutores</h2><p>Administra las cuentas de tutores y sus departamentos</p></div><div className="botones-encabezado-tutores"><button type="button" className="boton-cancelar-tutor" disabled={tutores.cargando} onClick={actualizar}><RefreshCw aria-hidden="true"/>Actualizar</button><button type="button" className="boton-guardar-tutor" disabled={departamentos.cargando||Boolean(departamentos.error)||!departamentos.datos.some(departamento=>departamento.activo)} onClick={()=>cambiarOperacion({tipo:'crear'})}><Plus aria-hidden="true"/>Agregar tutor</button></div></header>
    {aviso&&<p role="status" className="aviso-tutores">{aviso}</p>}
    <div className="resumen-tutores">{[['Total de tutores',datos?.resumen.total],['Cuentas activas',datos?.resumen.activos],['Cuentas inactivas',datos?.resumen.inactivos]].map(([titulo,valor])=><div key={titulo}><p>{titulo}</p><strong>{tutores.cargando||tutores.error?'—':valor??'—'}</strong></div>)}</div>
    <div className="filtros-tutores"><div className="busqueda-tutores"><Search aria-hidden="true"/><input type="search" aria-label="Buscar tutor" placeholder="Buscar nombre, correo o departamento" maxLength={160} value={busqueda} onChange={evento=>cambiarBusqueda(evento.target.value)}/></div>
      <div><label htmlFor="filtro-departamento-tutor">Departamento</label><select id="filtro-departamento-tutor" value={departamentoId} disabled={departamentos.cargando||Boolean(departamentos.error)} onChange={evento=>{cambiarDepartamentoId(evento.target.value);cambiarPagina(1);}}><option value="">Todos los departamentos</option>{departamentos.datos.map(departamento=><option key={departamento.id} value={departamento.id}>{departamento.nombre}{departamento.activo?'':' (inactivo)'}</option>)}</select></div>
      <div><label htmlFor="filtro-estado-tutor">Estado de la cuenta</label><select id="filtro-estado-tutor" value={estado} onChange={evento=>{cambiarEstado(evento.target.value);cambiarPagina(1);}}><option value="todos">Todas las cuentas</option><option value="activo">Activas</option><option value="inactivo">Inactivas</option></select></div>
    </div>
    {departamentos.error&&<EstadoConsulta cargando={false} error={departamentos.error} alReintentar={departamentos.actualizar}/>}
    <div className="tabla-contenedor-tutores" aria-busy={tutores.cargando}>
      <EstadoConsulta cargando={tutores.cargando} error={tutores.error} alReintentar={tutores.actualizar}/>
      {filas.length>0&&<table aria-label="Lista de tutores"><thead><tr>{['Tutor','Departamento','Teléfono','Estado','Acciones'].map(titulo=><th scope="col" key={titulo}>{titulo}</th>)}</tr></thead><tbody>{filas.map(tutor=><tr key={tutor.id}>
        <td><div className="persona-tabla-tutores"><InicialesPersona nombre={tutor.nombre}/><div><p>{tutor.nombre}</p><small>{tutor.correo}</small></div></div></td><td>{tutor.departamento}</td><td>{tutor.telefono||'Sin teléfono'}</td><td><span className={`estado-cuenta-tutor ${tutor.estado}`}>{tutor.estado==='activo'?'Activa':'Inactiva'}</span></td>
        <td><div className="acciones-tabla-tutores"><button type="button" title="Editar tutor" aria-label="Editar tutor" disabled={departamentos.cargando||Boolean(departamentos.error)} onClick={()=>cambiarOperacion({tipo:'editar',tutor})}><Pencil aria-hidden="true"/></button><button type="button" className={tutor.estado==='activo'?'accion-desactivar-tutor':''} title={tutor.estado==='activo'?'Desactivar tutor':'Reactivar tutor'} aria-label={tutor.estado==='activo'?'Desactivar tutor':'Reactivar tutor'} onClick={()=>cambiarOperacion({tipo:'estado',tutor})}>{tutor.estado==='activo'?<UserRoundX aria-hidden="true"/>:<UserRoundCheck aria-hidden="true"/>}</button></div></td>
      </tr>)}</tbody></table>}
      {!tutores.cargando&&!tutores.error&&!filas.length&&<p className="lista-vacia-tutores">No hay tutores que coincidan con los filtros.</p>}
    </div>
    {!tutores.cargando&&!tutores.error&&<div className="pie-tabla-tutores"><p role="status">Mostrando {total?(paginaActual-1)*5+1:0}–{Math.min(paginaActual*5,total)} de {total} tutores</p><nav aria-label="Paginación de tutores"><button type="button" disabled={paginaActual<=1} onClick={()=>cambiarPagina(paginaActual-1)}><ChevronLeft aria-hidden="true"/>Anterior</button>{Array.from({length:paginas},(_,indice)=>indice+1).map(numero=><button type="button" key={numero} aria-label={`Página ${numero}`} aria-current={numero===paginaActual?'page':undefined} onClick={()=>cambiarPagina(numero)}>{numero}</button>)}<button type="button" disabled={!paginas||paginaActual>=paginas} onClick={()=>cambiarPagina(paginaActual+1)}>Siguiente<ChevronRight aria-hidden="true"/></button></nav></div>}
    {operacion&&<FormularioTutor operacion={operacion} departamentos={departamentos.datos} alCerrar={()=>cambiarOperacion(null)} alGuardar={mensaje=>{cambiarOperacion(null);cambiarAviso(mensaje);actualizar();}}/>}
  </section>;
}
export { ListaTutoresCoordinacion as TutorsManagementView };
