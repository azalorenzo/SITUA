import { useCallback, useEffect, useRef, useState } from 'react';
import { Plus, Search, Users, CheckCircle, Clock, AlertTriangle, Pencil, Trash2, Eye, RefreshCw, Download, ChevronLeft, ChevronRight } from 'lucide-react';
import '../../styles/asignaciones.css';
import { consultarTutores } from '../servicios/coordinacion';
import { exportarAsignaciones, consultarPeriodos, consultarEstudiantesAsignables } from '../servicios/asignaciones';
import { EstadoConsulta, usarConsultaCoordinacion } from './ConsultaCoordinacion';
import { usarPaginaAsignaciones } from './usarPaginaAsignaciones';
import { FormularioAsignacion } from './FormularioAsignacion';
import type { AsignacionTutor } from '../tipos/asignaciones';
import type { OperacionAsignacion } from './FormularioAsignacion';

const nombresEstado = { activa: 'Activa', pendiente: 'Pendiente', finalizada: 'Finalizada', cancelada: 'Cancelada' };
function mostrarFechaCorta(fecha: string) {
  return new Intl.DateTimeFormat('es-MX', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(fecha + 'Z'));
}
function mostrarFecha(fecha: string | null) {
  if (!fecha) return 'Sin registro';
  return new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(fecha + 'Z'));
}

function FilaAsignacion({ asignacion, abierto, puedeCambiar, alVer, alOperar }: {
  asignacion: AsignacionTutor; abierto: boolean; puedeCambiar: boolean;
  alVer: () => void; alOperar: (operacion: OperacionAsignacion) => void;
}) {
  const vigente = asignacion.estado === 'activa' || asignacion.estado === 'pendiente';
  return <>
    <tr>
      <td><span className="nombre-asignacion">{asignacion.estudiante}</span><small>{asignacion.matricula}</small></td>
      <td>{asignacion.tutor}</td><td><time dateTime={asignacion.inicioEn + 'Z'} title={mostrarFecha(asignacion.inicioEn)}>{mostrarFechaCorta(asignacion.inicioEn)}</time></td>
      <td><span className={`estado-asignacion estado-${asignacion.estado}`}>{nombresEstado[asignacion.estado]}</span></td>
      <td><div className="acciones-fila-asignaciones">
        <button type="button" aria-label="Ver detalle" title="Ver detalle" aria-expanded={abierto} aria-controls={`detalle-asignacion-${asignacion.id}`} onClick={alVer}><Eye aria-hidden="true" /></button>
        {asignacion.estado === 'activa' && puedeCambiar && <button type="button" aria-label="Cambiar tutor" title="Cambiar tutor" onClick={() => alOperar({ tipo: 'cambiar', asignacion })}><Pencil aria-hidden="true" /></button>}
        {vigente && <><button type="button" aria-label="Finalizar" title="Finalizar asignación" onClick={() => alOperar({ tipo: 'finalizar', asignacion })}><CheckCircle aria-hidden="true" /></button><button type="button" className="accion-cancelar-asignacion" aria-label="Cancelar" title="Cancelar asignación" onClick={() => alOperar({ tipo: 'cancelar', asignacion })}><Trash2 aria-hidden="true" /></button></>}
      </div></td>
    </tr>
    {abierto && <tr id={`detalle-asignacion-${asignacion.id}`} className="detalle-asignacion"><td colSpan={5}><dl>
      {[['Periodo', asignacion.periodo], ['Inicio', mostrarFecha(asignacion.inicioEn)], ['Fin', mostrarFecha(asignacion.finEn)], ['Sesiones completadas', asignacion.sesionesCompletadas], ['Última sesión completada', mostrarFecha(asignacion.ultimaSesion)], ['Motivo', asignacion.motivo || 'Sin motivo registrado']].map(([titulo, valor]) => <div key={titulo}><dt>{titulo}</dt><dd>{valor}</dd></div>)}
    </dl></td></tr>}
  </>;
}

function AsignacionesCoordinacion() {
  const periodos = usarConsultaCoordinacion(consultarPeriodos);
  const tutores = usarConsultaCoordinacion(consultarTutores);
  const [periodoId, cambiarPeriodoId] = useState('');
  const [estado, cambiarEstado] = useState('todos');
  const [busqueda, cambiarBusqueda] = useState('');
  const [busquedaAplicada, cambiarBusquedaAplicada] = useState('');
  const [exportando, cambiarExportando] = useState(false);
  const [errorExportacion, cambiarErrorExportacion] = useState('');
  const [operacion, cambiarOperacion] = useState<OperacionAsignacion | null>(null);
  const [aviso, cambiarAviso] = useState('');
  const [pagina, cambiarPagina] = useState(1);
  const [detalleId, cambiarDetalleId] = useState<number | null>(null);
  const porPagina = 5;
  const filtros = { pagina, tamano: porPagina, periodoId, estado, busqueda: busquedaAplicada };
  const asignaciones = usarPaginaAsignaciones(filtros);
  const iniciado = useRef(false);
  useEffect(() => {
    if (!iniciado.current && periodos.datos.length) {
      iniciado.current = true;
      cambiarPeriodoId(String(periodos.datos.find(periodo => periodo.estado === 'activo')?.id || ''));
    }
  }, [periodos.datos]);
  const consultarCandidatos = useCallback((comprobacion: AbortSignal) => periodoId ? consultarEstudiantesAsignables(Number(periodoId), comprobacion) : Promise.resolve([]), [periodoId]);
  const candidatos = usarConsultaCoordinacion(consultarCandidatos);
  const sinAsignacion = candidatos.datos.filter(estudiante => estudiante.asignacionActivaId === null);
  const cargando = periodos.cargando || tutores.cargando;
  const error = periodos.error || tutores.error;
  const periodoActivo = periodos.datos.some(periodo => periodo.id === Number(periodoId) && periodo.estado === 'activo');
  function actualizar() { asignaciones.actualizar(); periodos.actualizar(); tutores.actualizar(); candidatos.actualizar(); }

  const totalPaginas = asignaciones.datos?.totalPaginas || 0;
  const paginaActual = asignaciones.datos?.pagina || 1;
  const totalRegistros = asignaciones.datos?.totalRegistros || 0;
  const filas = asignaciones.cargando || asignaciones.error ? [] : asignaciones.datos?.contenido || [];
  const resumenServidor = asignaciones.datos?.resumen;
  useEffect(() => {
    if (busqueda === busquedaAplicada) return;
    const espera = window.setTimeout(() => { cambiarBusquedaAplicada(busqueda); cambiarPagina(1); cambiarDetalleId(null); }, 300);
    return () => window.clearTimeout(espera);
  }, [busqueda, busquedaAplicada]);
  useEffect(() => { cambiarDetalleId(null); }, [pagina]);
  const puedeCrear = !cargando && !error && periodos.datos.some(periodo => periodo.estado === 'activo');
  const resumen = [
    { titulo: 'Total asignaciones', valor: asignaciones.cargando ? '—' : resumenServidor?.total ?? '—', icono: Users, tono: 'verde' },
    { titulo: 'Activas', valor: asignaciones.cargando ? '—' : resumenServidor?.activas ?? '—', icono: CheckCircle, tono: 'verde' },
    { titulo: 'Pendientes', valor: asignaciones.cargando ? '—' : resumenServidor?.pendientes ?? '—', icono: Clock, tono: 'amarillo' },
    { titulo: 'Sin asignación activa', valor: !periodoId || candidatos.cargando || candidatos.error ? '—' : sinAsignacion.length, icono: AlertTriangle, tono: 'rojo' },
  ];

  async function exportar() {
    if (exportando) return;
    cambiarExportando(true); cambiarErrorExportacion('');
    try {
    const registrosExportables = await exportarAsignaciones(filtros);
    const escapar = (valor: unknown) => {
      let texto = String(valor ?? '');
      if (/^[=+@\-\t\r]/.test(texto)) texto = "'" + texto;
      return '"' + texto.replaceAll('"', '""') + '"';
    };
    const registros = [['Estudiante', 'Matrícula', 'Tutor', 'Periodo', 'Inicio UTC', 'Fin UTC', 'Estado', 'Sesiones completadas', 'Última sesión UTC', 'Motivo'],
      ...registrosExportables.map(asignacion => [asignacion.estudiante, asignacion.matricula, asignacion.tutor, asignacion.periodo, asignacion.inicioEn,
        asignacion.finEn, nombresEstado[asignacion.estado], asignacion.sesionesCompletadas, asignacion.ultimaSesion, asignacion.motivo])];
    const archivo = new Blob(['\uFEFF' + registros.map(fila => fila.map(escapar).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' });
    const direccion = URL.createObjectURL(archivo);
    const enlace = document.createElement('a'); enlace.href = direccion; enlace.download = 'asignaciones.csv'; enlace.click();
    window.setTimeout(() => URL.revokeObjectURL(direccion), 1000);
    } catch (fallo) { cambiarErrorExportacion(fallo instanceof Error ? fallo.message : 'No se pudo exportar.'); }
    finally { cambiarExportando(false); }
  }

  return <section className="pagina-asignaciones" aria-labelledby="titulo-asignaciones">
    <header className="encabezado-asignaciones"><div><h2 id="titulo-asignaciones">Asignaciones</h2><p>Administra la relación entre tutores y estudiantes</p></div>
      <button type="button" className="boton-secundario-asignaciones" onClick={actualizar} disabled={cargando}><RefreshCw aria-hidden="true" />Actualizar</button>
    </header>
    {aviso && <p role="status" className="aviso-asignaciones">{aviso}</p>}
    <EstadoConsulta cargando={cargando} error={error} alReintentar={actualizar} />
    {!cargando && !error && <>
      <div className="resumen-asignaciones">{resumen.map(tarjeta => {
        const Icono = tarjeta.icono;
        return <div key={tarjeta.titulo} className="tarjeta-resumen-asignaciones"><span className={`icono-resumen-asignaciones tono-${tarjeta.tono}`}><Icono aria-hidden="true" /></span><div><p>{tarjeta.titulo}</p><strong>{tarjeta.valor}</strong></div></div>;
      })}</div>
      <div className="filtros-asignaciones">
        <div className="busqueda-asignaciones"><Search aria-hidden="true" /><input type="search" maxLength={160} aria-label="Buscar asignación" placeholder="Buscar estudiante, matrícula o tutor" value={busqueda} onChange={evento => cambiarBusqueda(evento.target.value)} /></div>
        <div className="selector-asignaciones"><label htmlFor="periodo-lista">Periodo académico</label><select id="periodo-lista" value={periodoId} onChange={evento => { cambiarPeriodoId(evento.target.value); cambiarPagina(1); cambiarDetalleId(null); }}><option value="">Todos los periodos</option>{periodos.datos.map(periodo => <option key={periodo.id} value={periodo.id}>{periodo.nombre} · {periodo.estado}</option>)}</select></div>
        <div className="selector-asignaciones"><label htmlFor="estado-asignaciones">Estado de la asignación</label><select id="estado-asignaciones" value={estado} onChange={evento => { cambiarEstado(evento.target.value); cambiarPagina(1); cambiarDetalleId(null); }}><option value="todos">Todos los estados</option>{Object.entries(nombresEstado).map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}</select></div>
      </div>
      <div className="contenedor-tabla-asignaciones" aria-busy={asignaciones.cargando}>
        <EstadoConsulta cargando={asignaciones.cargando} error={asignaciones.error} alReintentar={asignaciones.actualizar} />
        {filas.length ? <table className="tabla-asignaciones" aria-label="Historial de asignaciones"><colgroup><col style={{ width: '26%' }} /><col style={{ width: '23%' }} /><col style={{ width: '17%' }} /><col style={{ width: '12%' }} /><col style={{ width: '22%' }} /></colgroup>
          <thead><tr>{['Estudiante', 'Tutor', 'Fecha', 'Estado', 'Acciones'].map(titulo => <th key={titulo} scope="col">{titulo}</th>)}</tr></thead>
          <tbody>{filas.map(asignacion => <FilaAsignacion key={asignacion.id} asignacion={asignacion} abierto={detalleId === asignacion.id}
            puedeCambiar={periodos.datos.some(periodo => periodo.id === asignacion.periodoId && periodo.estado === 'activo')}
            alVer={() => cambiarDetalleId(detalleId === asignacion.id ? null : asignacion.id)} alOperar={cambiarOperacion} />)}</tbody>
        </table> : !asignaciones.cargando && !asignaciones.error && <p className="vacio-asignaciones">No hay asignaciones que coincidan con los filtros.</p>}
      </div>
      {!asignaciones.cargando && !asignaciones.error && <div className="pie-tabla-asignaciones"><p role="status">Mostrando {totalRegistros ? (paginaActual - 1) * porPagina + 1 : 0}–{Math.min(paginaActual * porPagina, totalRegistros)} de {totalRegistros} asignaciones</p>
        <nav aria-label="Paginación de asignaciones"><button type="button" disabled={paginaActual <= 1} onClick={() => cambiarPagina(paginaActual - 1)}><ChevronLeft aria-hidden="true" />Anterior</button>
          <div className="indices-paginacion-asignaciones">{Array.from({ length: totalPaginas }, (_, indice) => indice + 1).map(numero => <button type="button" key={numero} aria-label={`Página ${numero}`} aria-current={numero === paginaActual ? 'page' : undefined} onClick={() => cambiarPagina(numero)}>{numero}</button>)}</div>
          <button type="button" disabled={!totalPaginas || paginaActual >= totalPaginas} onClick={() => cambiarPagina(paginaActual + 1)}>Siguiente<ChevronRight aria-hidden="true" /></button></nav>
      </div>}
      <div className="paneles-asignaciones">
        <section className="panel-inferior-asignaciones" aria-labelledby="titulo-sin-asignacion"><h3 id="titulo-sin-asignacion">Estudiantes pendientes de asignar</h3>
          {!periodoId ? <p className="nota-asignaciones">Selecciona un periodo para consultar los estudiantes.</p> : <>
            <EstadoConsulta cargando={candidatos.cargando} error={candidatos.error} alReintentar={candidatos.actualizar} />
            {!candidatos.cargando && !candidatos.error && <>
              {sinAsignacion.length ? <ul className="pendientes-asignaciones">{sinAsignacion.map(estudiante => <li key={estudiante.id}><div><p>{estudiante.nombre}</p><span>{estudiante.matricula} · {estudiante.carrera}</span></div>{periodoActivo && <button type="button" className="boton-principal-asignaciones" onClick={() => cambiarOperacion({ tipo: 'crear', estudianteId: estudiante.id })}>Asignar tutor</button>}</li>)}</ul> : <p className="nota-asignaciones">No hay estudiantes elegibles sin asignación activa en este periodo.</p>}
              <p className="nota-asignaciones">Se incluyen estudiantes con cuenta y expediente activos en el periodo seleccionado.</p>
            </>}
          </>}
        </section>
        <section className="panel-inferior-asignaciones" aria-labelledby="titulo-acciones-asignaciones"><h3 id="titulo-acciones-asignaciones">Acciones rápidas</h3>
          <div className="acciones-rapidas-asignaciones"><button type="button" className="boton-principal-asignaciones" disabled={!puedeCrear} onClick={() => cambiarOperacion({ tipo: 'crear' })}><Plus aria-hidden="true" />Nueva asignación</button><button type="button" className="boton-secundario-asignaciones" disabled={!totalRegistros || asignaciones.cargando || Boolean(asignaciones.error) || exportando} onClick={exportar}><Download aria-hidden="true" />{exportando ? 'Exportando…' : 'Exportar asignaciones'}</button></div>
          {errorExportacion && <p role="alert" className="text-sm text-red-700">{errorExportacion}</p>}
          <p className="nota-asignaciones">La exportación incluye todos los registros que coinciden con los filtros. Usa el icono de detalle para consultar fechas, sesiones y motivo.</p>
        </section>
      </div>
    </>}
    {operacion && <FormularioAsignacion operacion={operacion} periodos={periodos.datos} tutores={tutores.datos} periodoInicial={periodoId} alCerrar={() => cambiarOperacion(null)} alGuardar={mensaje => { cambiarOperacion(null); cambiarAviso(mensaje); actualizar(); }} />}
  </section>;
}

export { AsignacionesCoordinacion as AssignmentsView };
