import { useCallback, useEffect, useRef, useState } from 'react';
import type { AsignacionTutor, PeriodoAcademico } from '../tipos/asignaciones';
import type { TutorCoordinacion } from '../tipos/coordinacion';
import type { ErroresFormulario } from '../tipos/autenticacion';
import { ErrorSolicitud } from '../servicios/autenticacion';
import { crearAsignacion, cambiarTutor, cerrarAsignacion, consultarEstudiantesAsignables } from '../servicios/asignaciones';
import { EstadoConsulta, usarConsultaCoordinacion } from './ConsultaCoordinacion';

export type OperacionAsignacion = { tipo: 'crear'; estudianteId?: number } | { tipo: 'cambiar' | 'finalizar' | 'cancelar'; asignacion: AsignacionTutor };

export function FormularioAsignacion({ operacion, periodos, tutores, periodoInicial, alCerrar, alGuardar }: {
  operacion: OperacionAsignacion; periodos: PeriodoAcademico[]; tutores: TutorCoordinacion[];
  periodoInicial: string; alCerrar: () => void; alGuardar: (mensaje: string) => void;
}) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [periodoId, cambiarPeriodoId] = useState(operacion.tipo === 'crear' ? (periodos.some(periodo => String(periodo.id) === periodoInicial && periodo.estado === 'activo') ? periodoInicial : String(periodos.find(periodo => periodo.estado === 'activo')?.id || '')) : String(operacion.asignacion.periodoId));
  const [estudianteId, cambiarEstudianteId] = useState(operacion.tipo === 'crear' ? String(operacion.estudianteId || '') : '');
  const [tutorId, cambiarTutorId] = useState('');
  const [motivo, cambiarMotivo] = useState('');
  const [guardando, cambiarGuardando] = useState(false);
  const [error, cambiarError] = useState('');
  const [errores, cambiarErrores] = useState<ErroresFormulario>({});
  const consultar = useCallback((comprobacion: AbortSignal) => periodoId && operacion.tipo === 'crear' ? consultarEstudiantesAsignables(Number(periodoId), comprobacion) : Promise.resolve([]), [periodoId, operacion.tipo]);
  const estudiantes = usarConsultaCoordinacion(consultar);
  const titulo = { crear: 'Nueva asignación', cambiar: 'Cambiar tutor', finalizar: 'Finalizar asignación', cancelar: 'Cancelar asignación' }[operacion.tipo];
  const requiereTutor = operacion.tipo === 'crear' || operacion.tipo === 'cambiar';
  const disponibles = tutores.filter(tutor => tutor.estado === 'activo' && (operacion.tipo !== 'cambiar' || tutor.id !== operacion.asignacion.tutorId));
  const periodosActivos = periodos.filter(periodo => periodo.estado === 'activo');
  const puedeElegirEstudiante = operacion.tipo === 'crear' && !estudiantes.cargando && !estudiantes.error;
  useEffect(() => { dialogo.current?.showModal(); return () => dialogo.current?.close(); }, []);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault(); if (guardando) return;
    const revisados: ErroresFormulario = {};
    if (operacion.tipo === 'crear') {
      if (!periodoId) revisados.periodoId = ['Selecciona un periodo activo.'];
      if (!estudianteId) revisados.estudianteId = ['Selecciona un estudiante.'];
    }
    if (requiereTutor && !tutorId) revisados.tutorId = ['Selecciona un tutor.'];
    cambiarErrores(revisados); cambiarError(''); if (Object.keys(revisados).length) return;
    cambiarGuardando(true);
    try {
      if (operacion.tipo === 'crear') await crearAsignacion({ periodoId: Number(periodoId), estudianteId: Number(estudianteId), tutorId: Number(tutorId), motivo: motivo.trim() });
      else if (operacion.tipo === 'cambiar') await cambiarTutor(operacion.asignacion.id, Number(tutorId), motivo.trim());
      else await cerrarAsignacion(operacion.asignacion.id, operacion.tipo);
      alGuardar(operacion.tipo === 'crear' ? 'Asignación creada.' : operacion.tipo === 'cambiar' ? 'Tutor cambiado. Se conservó la asignación anterior en el historial.' : operacion.tipo === 'finalizar' ? 'Asignación finalizada.' : 'Asignación cancelada.');
    } catch (fallo) {
      cambiarError(fallo instanceof Error ? fallo.message : 'No se pudo guardar la asignación.');
      if (fallo instanceof ErrorSolicitud) cambiarErrores(fallo.erroresPorCampo);
    } finally { cambiarGuardando(false); }
  }
  function mensajes(campo: string) { return errores[campo]?.map(texto => <p key={texto} className="text-xs text-[#B83F4B]">{texto}</p>); }
  const estilo = 'w-full border border-[#DCE3EA] rounded-lg px-3 py-2 disabled:bg-gray-100';
  return <dialog ref={dialogo} aria-labelledby="titulo-asignacion" onCancel={evento => { evento.preventDefault(); if (!guardando) alCerrar(); }}
    className="m-auto w-[calc(100%-2rem)] max-w-lg max-h-[90vh] overflow-y-auto rounded-xl p-6 text-[#0B2828] backdrop:bg-black/40">
    <h3 id="titulo-asignacion" className="font-semibold text-lg mb-4">{titulo}</h3>
    {operacion.tipo !== 'crear' && <div className="mb-4 text-sm"><p className="font-medium">{operacion.asignacion.estudiante} · {operacion.asignacion.matricula}</p><p>{operacion.asignacion.periodo}</p><p>Tutor actual: {operacion.asignacion.tutor}</p></div>}
    <form noValidate onSubmit={enviar} aria-busy={guardando} className="space-y-4">
      {operacion.tipo === 'crear' && <>
        <div><label htmlFor="periodo-asignacion" className="block mb-1 text-sm">Periodo</label><select id="periodo-asignacion" className={estilo} value={periodoId} disabled={guardando} aria-invalid={Boolean(errores.periodoId)} onChange={evento => { cambiarPeriodoId(evento.target.value); cambiarEstudianteId(''); }}><option value="">Selecciona un periodo</option>{periodosActivos.map(periodo => <option key={periodo.id} value={periodo.id}>{periodo.nombre}</option>)}</select>{mensajes('periodoId')}
          {!periodosActivos.length && <p className="text-sm text-[#53716E] mt-2">No hay periodos activos para crear asignaciones.</p>}</div>
        {periodoId && <><EstadoConsulta cargando={estudiantes.cargando} error={estudiantes.error} alReintentar={estudiantes.actualizar} />
          {puedeElegirEstudiante && <div><label htmlFor="estudiante-asignacion" className="block mb-1 text-sm">Estudiante</label><select id="estudiante-asignacion" className={estilo} value={estudianteId} disabled={guardando} aria-invalid={Boolean(errores.estudianteId)} onChange={evento => cambiarEstudianteId(evento.target.value)}><option value="">Selecciona un estudiante</option>{estudiantes.datos.filter(estudiante => estudiante.asignacionActivaId === null).map(estudiante => <option key={estudiante.id} value={estudiante.id}>{estudiante.nombre} · {estudiante.matricula}</option>)}</select>{mensajes('estudianteId')}<p className="text-xs text-[#53716E] mt-2">Solo aparecen estudiantes con cuenta y expediente activos en este periodo, sin asignación activa.</p></div>}</>}
      </>}
      {requiereTutor && <>
        <div><label htmlFor="tutor-asignacion" className="block mb-1 text-sm">{operacion.tipo === 'cambiar' ? 'Nuevo tutor' : 'Tutor'}</label><select id="tutor-asignacion" className={estilo} value={tutorId} disabled={guardando} aria-invalid={Boolean(errores.tutorId)} onChange={evento => cambiarTutorId(evento.target.value)}><option value="">Selecciona un tutor</option>{disponibles.map(tutor => <option key={tutor.id} value={tutor.id}>{tutor.nombre} · {tutor.departamento}</option>)}</select>{mensajes('tutorId')}{!disponibles.length && <p className="text-sm text-[#53716E] mt-2">No hay tutores activos disponibles.</p>}</div>
        <div><label htmlFor="motivo-asignacion" className="block mb-1 text-sm">Motivo (opcional)</label><textarea id="motivo-asignacion" maxLength={1000} rows={3} className={estilo} value={motivo} disabled={guardando} onChange={evento => cambiarMotivo(evento.target.value)} />{mensajes('motivo')}</div>
        {operacion.tipo === 'cambiar' && <p className="text-sm text-[#53716E]">La asignación actual se finalizará y se creará una nueva con el tutor seleccionado.</p>}
      </>}
      {!requiereTutor && <p className="text-sm text-[#53716E]">¿Confirmas que deseas {operacion.tipo} esta asignación? El registro se conservará en el historial.</p>}
      {error && <p role="alert" className="text-sm text-[#B83F4B]">{error}</p>}
      <div className="flex justify-end gap-3 pt-2"><button type="button" className="px-4 py-2 border rounded-lg" disabled={guardando} onClick={alCerrar}>Volver</button>
        <button type="submit" className="px-4 py-2 bg-[#1F6662] text-white rounded-lg disabled:opacity-50" disabled={guardando || (operacion.tipo === 'crear' && (!puedeElegirEstudiante || !periodoId))}>{guardando ? 'Guardando…' : requiereTutor ? 'Guardar asignación' : 'Confirmar'}</button></div>
    </form>
  </dialog>;
}
