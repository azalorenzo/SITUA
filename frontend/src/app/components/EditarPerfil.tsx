import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';
import { consultarPerfil, guardarPerfil, type PerfilUsuario } from '../servicios/perfil';
import { ErrorSolicitud } from '../servicios/autenticacion';
import type { ErroresFormulario, UsuarioSesion } from '../tipos/autenticacion';

interface Formulario {
  nombre: string; correo: string; telefono: string; matricula: string; carreraId: string; semestre: string;
  departamentoId: string; aceptaSms: boolean;
}
const vacio: Formulario = { nombre: '', correo: '', telefono: '', matricula: '', carreraId: '', semestre: '', departamentoId: '', aceptaSms: false };
const campos = (perfil: PerfilUsuario): Formulario => ({
  nombre: perfil.usuario.nombre, correo: perfil.usuario.correo, telefono: perfil.telefono ?? '', matricula: perfil.matricula ?? '',
  carreraId: perfil.carreraId == null ? '' : String(perfil.carreraId), semestre: perfil.semestre == null ? '' : String(perfil.semestre),
  departamentoId: perfil.departamentoId == null ? '' : String(perfil.departamentoId), aceptaSms: perfil.aceptaSms,
});

export function EditarPerfil({ abierto, alCerrar, alActualizar }: { abierto: boolean; alCerrar: () => void; alActualizar: (usuario: UsuarioSesion) => void }) {
  const [perfil, cambiarPerfil] = useState<PerfilUsuario | null>(null);
  const [formulario, cambiarFormulario] = useState<Formulario>(vacio);
  const [cargando, cambiarCarga] = useState(false);
  const [guardando, cambiarGuardado] = useState(false);
  const [error, cambiarError] = useState('');
  const [errores, cambiarErrores] = useState<ErroresFormulario>({});
  const [guardado, cambiarGuardadoCorrecto] = useState(false);
  const [intento, cambiarIntento] = useState(0);
  const envio = useRef(false);
  useEffect(() => {
    if (!abierto) return;
    const cancelacion = new AbortController();
    cambiarCarga(true); cambiarPerfil(null); cambiarError(''); cambiarErrores({}); cambiarGuardadoCorrecto(false);
    consultarPerfil(cancelacion.signal).then(datos => {
      if (!cancelacion.signal.aborted) { cambiarPerfil(datos); cambiarFormulario(campos(datos)); }
    }).catch(fallo => {
      if (!cancelacion.signal.aborted) cambiarError(fallo instanceof Error ? fallo.message : 'No se pudo cargar tu perfil.');
    }).finally(() => { if (!cancelacion.signal.aborted) cambiarCarga(false); });
    return () => cancelacion.abort();
  }, [abierto, intento]);
  function campo<Clave extends keyof Formulario>(clave: Clave, valor: Formulario[Clave]) {
    cambiarFormulario(anterior => ({ ...anterior, [clave]: valor })); cambiarGuardadoCorrecto(false);
    cambiarErrores(anterior => ({ ...anterior, [clave]: [] }));
  }
  function cerrar() { if (!envio.current) alCerrar(); }
  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); if (!perfil || envio.current) return;
    const validaciones: ErroresFormulario = {};
    const nombre = formulario.nombre.trim(), telefono = formulario.telefono.trim();
    const correo = formulario.correo.trim().toLowerCase();
    if (!nombre || nombre.length > 160) validaciones.nombre = ['Escribe tu nombre, de hasta 160 caracteres.'];
    if (!correo || correo.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo)) validaciones.correo = ['Escribe un correo válido.'];
    if (telefono && !/^\+[1-9][0-9]{7,14}$/.test(telefono)) validaciones.telefono = ['Escribe el teléfono con código de país.'];
    if (perfil.rol === 'estudiante') {
      if (!/^[\p{L}\p{N}_-]{3,30}$/u.test(formulario.matricula.trim())) validaciones.matricula = ['Escribe una matrícula de 3 a 30 caracteres.'];
      if ((formulario.aceptaSms) && !telefono) validaciones.telefono = ['Escribe tu teléfono para recibir avisos.'];
      if (perfil.puedeEditarAcademico) {
        if (!formulario.carreraId) validaciones.carreraId = ['Selecciona tu carrera.'];
        if (!formulario.semestre || !Number.isInteger(Number(formulario.semestre)) || Number(formulario.semestre) < 1 || Number(formulario.semestre) > 12) validaciones.semestre = ['El semestre debe estar entre 1 y 12.'];
      }
    }
    if (perfil.rol === 'tutor' && !formulario.departamentoId) validaciones.departamentoId = ['Selecciona tu departamento.'];
    cambiarErrores(validaciones); cambiarGuardadoCorrecto(false); cambiarError('');
    if (Object.keys(validaciones).length) return;
    envio.current = true; cambiarGuardado(true);
    try {
      const estudiante = perfil.rol === 'estudiante';
      const respuesta = await guardarPerfil({ nombre, correo, telefono, matricula: estudiante ? formulario.matricula.trim() : null,
        carreraId: estudiante && formulario.carreraId ? Number(formulario.carreraId) : null, periodoId: estudiante ? perfil.periodoId : null,
        semestre: estudiante && formulario.semestre ? Number(formulario.semestre) : null,
        departamentoId: perfil.rol === 'tutor' ? Number(formulario.departamentoId) : null,
        aceptaSms: formulario.aceptaSms });
      cambiarPerfil(respuesta); cambiarFormulario(campos(respuesta)); alActualizar(respuesta.usuario); cambiarGuardadoCorrecto(true);
    } catch (fallo) {
      cambiarError(fallo instanceof Error ? fallo.message : 'No se pudieron guardar tus datos.');
      if (fallo instanceof ErrorSolicitud) cambiarErrores(fallo.erroresPorCampo);
    } finally { envio.current = false; cambiarGuardado(false); }
  }
  const estilo = 'mt-1 w-full rounded-lg border border-[#DCE3EA] bg-white px-3 py-2 disabled:bg-[#F5F7FA] disabled:text-[#53716E]';
  const mensajes = (clave: string) => errores[clave]?.map(texto => <p key={texto} className="mt-1 text-sm text-[#B83F4B]">{texto}</p>);
  return <Dialog open={abierto} onOpenChange={valor => { if (!valor) cerrar(); }}>
    <DialogContent aria-describedby={undefined} className="max-h-[90dvh] overflow-y-auto bg-white text-[#0B2828]"
      onEscapeKeyDown={evento => { if (guardando) evento.preventDefault(); }} onPointerDownOutside={evento => { if (guardando) evento.preventDefault(); }}>
      <DialogHeader><DialogTitle>Perfil</DialogTitle></DialogHeader>
      {cargando && <p role="status" className="text-sm text-[#53716E]">Cargando…</p>}
      {error && <p role="alert" className="text-sm text-[#B83F4B]">{error}</p>}
      {!cargando && !perfil && <button className="text-sm text-[#1F6662] underline" onClick={() => cambiarIntento(valor => valor + 1)}>Reintentar</button>}
      {!cargando && perfil && <form noValidate onSubmit={guardar} className="space-y-4">
        <fieldset disabled={guardando} className="space-y-4">
          <div><label htmlFor="editar-perfil-nombre">Nombre</label><input id="editar-perfil-nombre" autoComplete="name" maxLength={160} required className={estilo} value={formulario.nombre} aria-invalid={!!errores.nombre?.length} onChange={evento => campo('nombre', evento.target.value)} />{mensajes('nombre')}</div>
          <div><label htmlFor="editar-perfil-correo">Correo</label><input id="editar-perfil-correo" type="email" autoComplete="email" maxLength={254} required readOnly={!perfil.puedeEditarCorreo} title={!perfil.puedeEditarCorreo ? 'Correo vinculado con Google' : undefined} className={`${estilo} ${!perfil.puedeEditarCorreo ? 'bg-[#F5F7FA] text-[#53716E]' : ''}`} value={formulario.correo} aria-invalid={!!errores.correo?.length} onChange={evento => campo('correo', evento.target.value)} />{mensajes('correo')}</div>
          <div><label htmlFor="editar-perfil-telefono">Teléfono</label><input id="editar-perfil-telefono" type="tel" autoComplete="tel" maxLength={16} placeholder="+525512345678" className={estilo} value={formulario.telefono} aria-invalid={!!errores.telefono?.length} onChange={evento => campo('telefono', evento.target.value)} />{mensajes('telefono')}</div>
          {perfil.rol === 'estudiante' && <>
            <div><label htmlFor="editar-perfil-matricula">Matrícula</label><input id="editar-perfil-matricula" maxLength={30} required className={estilo} value={formulario.matricula} aria-invalid={!!errores.matricula?.length} onChange={evento => campo('matricula', evento.target.value)} />{mensajes('matricula')}</div>
            {perfil.periodo && <div><label htmlFor="editar-perfil-periodo">Periodo</label><input id="editar-perfil-periodo" readOnly className={`${estilo} bg-[#F5F7FA] text-[#53716E]`} value={perfil.periodo} /></div>}
            <div><label htmlFor="editar-perfil-carrera">Carrera</label><select id="editar-perfil-carrera" disabled={!perfil.puedeEditarAcademico} className={estilo} value={formulario.carreraId} aria-invalid={!!errores.carreraId?.length} onChange={evento => campo('carreraId', evento.target.value)}><option value="">Seleccionar carrera</option>{perfil.carreras.map(opcion => <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{mensajes('carreraId')}</div>
            <div><label htmlFor="editar-perfil-semestre">Semestre</label><input id="editar-perfil-semestre" type="number" min={1} max={12} step={1} disabled={!perfil.puedeEditarAcademico} className={estilo} value={formulario.semestre} aria-invalid={!!errores.semestre?.length} onChange={evento => campo('semestre', evento.target.value)} />{mensajes('semestre')}</div>
            <label className="flex items-start gap-2 text-sm"><input type="checkbox" className="mt-1" checked={formulario.aceptaSms} onChange={evento => campo('aceptaSms', evento.target.checked)} />Recibir confirmaciones de tutoría por SMS</label>
          </>}
          {perfil.rol === 'tutor' && <div><label htmlFor="editar-perfil-departamento">Departamento</label><select id="editar-perfil-departamento" className={estilo} value={formulario.departamentoId} required aria-invalid={!!errores.departamentoId?.length} onChange={evento => campo('departamentoId', evento.target.value)}><option value="">Seleccionar departamento</option>{perfil.departamentos.map(opcion => <option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{mensajes('departamentoId')}</div>}
        </fieldset>
        {guardado && <p role="status" className="text-sm text-[#1F6662]">Cambios guardados.</p>}
        <div className="flex justify-end gap-3 pt-2">
          <button type="button" disabled={guardando} onClick={cerrar} className="rounded-lg border border-[#DCE3EA] px-4 py-2 disabled:opacity-50">{guardado ? 'Cerrar' : 'Cancelar'}</button>
          <button type="submit" disabled={guardando} className="rounded-lg bg-[#1F6662] px-4 py-2 text-white disabled:opacity-50">{guardando ? 'Guardando…' : 'Guardar'}</button>
        </div>
      </form>}
    </DialogContent>
  </Dialog>;
}
