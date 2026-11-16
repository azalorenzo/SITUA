import { useEffect,useRef,useState } from 'react';
import { X,Eye,EyeOff } from 'lucide-react';
import type { Departamento,TutorCoordinacion } from '../tipos/coordinacion';
import type { ErroresFormulario } from '../tipos/autenticacion';
import { ErrorSolicitud,validarAcceso } from '../servicios/autenticacion';
import { crearTutor,editarTutor,cambiarEstadoTutor } from '../servicios/tutores';
import '../../styles/tutores.css';

export type OperacionTutor={tipo:'crear'}|{tipo:'editar'|'estado';tutor:TutorCoordinacion};
export function FormularioTutor({operacion,departamentos,alCerrar,alGuardar}:{
  operacion:OperacionTutor;departamentos:Departamento[];alCerrar:()=>void;alGuardar:(mensaje:string)=>void;
}) {
  const dialogo=useRef<HTMLDialogElement>(null);
  const inicial=operacion.tipo==='crear'?null:operacion.tutor;
  const [nombre,cambiarNombre]=useState(inicial?.nombre||'');
  const [correo,cambiarCorreo]=useState(inicial?.correo||'');
  const [contrasena,cambiarContrasena]=useState('');
  const [departamentoId,cambiarDepartamentoId]=useState(String(inicial?.departamentoId||''));
  const [visible,cambiarVisible]=useState(false);
  const [guardando,cambiarGuardando]=useState(false);
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [error,cambiarError]=useState('');
  const activar=inicial?.estado==='inactivo';
  const titulo=operacion.tipo==='crear'?'Agregar tutor':operacion.tipo==='editar'?'Editar tutor':activar?'Reactivar tutor':'Desactivar tutor';
  useEffect(()=>{dialogo.current?.showModal();return ()=>dialogo.current?.close();},[]);
  function validar(campos={nombre,correo,contrasena,departamentoId}) {
    if(operacion.tipo==='estado') return {};
    const revisados=validarAcceso(campos.correo,campos.contrasena);
    if(operacion.tipo==='editar') delete revisados.contrasena;
    if(!campos.nombre.trim()||campos.nombre.trim().length>160) revisados.nombre=['Escribe un nombre de hasta 160 caracteres.'];
    if(!campos.departamentoId) revisados.departamentoId=['Selecciona un departamento.'];
    return revisados;
  }
  function revisarCampo(campo:string,valor:string) {
    const revisados=validar({...{nombre,correo,contrasena,departamentoId},[campo]:valor});
    cambiarError('');
    cambiarErrores(anteriores=>{
      const siguientes={...anteriores};
      if(revisados[campo]) siguientes[campo]=revisados[campo];else delete siguientes[campo];
      return siguientes;
    });
  }
  async function enviar(evento:React.FormEvent) {
    evento.preventDefault();if(guardando)return;
    const revisados=validar();cambiarErrores(revisados);cambiarError('');
    if(Object.keys(revisados).length){document.getElementById(`tutor-${Object.keys(revisados)[0]}`)?.focus();return;}
    cambiarGuardando(true);
    try {
      if(operacion.tipo==='crear') await crearTutor({nombre:nombre.trim(),correo:correo.trim(),contrasena,departamentoId:Number(departamentoId)});
      else if(operacion.tipo==='editar') await editarTutor(operacion.tutor.id,{nombre:nombre.trim(),correo:correo.trim(),departamentoId:Number(departamentoId)});
      else await cambiarEstadoTutor(operacion.tutor.id,activar);
      alGuardar(operacion.tipo==='crear'?'Tutor creado.':operacion.tipo==='editar'?'Tutor actualizado.':activar?'Cuenta de tutor reactivada.':'Cuenta de tutor desactivada.');
    } catch(fallo) {
      cambiarError(fallo instanceof Error?fallo.message:'No se pudo guardar el tutor.');
      if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);
    } finally {cambiarGuardando(false);}
  }
  function mensajes(campo:string){return errores[campo]&&<ul id={`error-tutor-${campo}`} className="error-campo-tutor">{errores[campo].map(texto=><li key={texto}>{texto}</li>)}</ul>;}
  return <dialog ref={dialogo} className="dialogo-tutor" aria-labelledby="titulo-dialogo-tutor" onCancel={evento=>{evento.preventDefault();if(!guardando)alCerrar();}}>
    <header><h3 id="titulo-dialogo-tutor">{titulo}</h3><button type="button" aria-label="Cerrar formulario" disabled={guardando} onClick={alCerrar}><X aria-hidden="true"/></button></header>
    <form noValidate onSubmit={enviar} aria-busy={guardando}>
      {operacion.tipo==='estado'?<div className="confirmacion-estado-tutor"><p className="font-medium">{inicial?.nombre}</p><p>{inicial?.correo}</p><p>{activar?'Esta cuenta podrá volver a iniciar sesión.':'Se bloquearán los nuevos accesos de esta cuenta. Sus asignaciones y sesiones se conservarán.'}</p></div>:<>
        <div className="campo-formulario-tutor"><label htmlFor="tutor-nombre">Nombre</label><input id="tutor-nombre" value={nombre} maxLength={160} autoComplete="name" placeholder="Escribir…" disabled={guardando} aria-invalid={Boolean(errores.nombre)} aria-describedby={errores.nombre?"error-tutor-nombre":undefined} onChange={evento=>{cambiarNombre(evento.target.value);revisarCampo('nombre',evento.target.value);}}/>{mensajes('nombre')}</div>
        <div className="campo-formulario-tutor"><label htmlFor="tutor-correo">Correo</label><input id="tutor-correo" value={correo} maxLength={254} type="email" autoComplete="email" placeholder="Escribir…" disabled={guardando} aria-invalid={Boolean(errores.correo)} aria-describedby={errores.correo?"error-tutor-correo":undefined} onChange={evento=>{cambiarCorreo(evento.target.value);revisarCampo('correo',evento.target.value);}}/>{mensajes('correo')}</div>
        {operacion.tipo==='crear'&&<div className="campo-formulario-tutor"><label htmlFor="tutor-contrasena">Contraseña</label><div className="contrasena-formulario-tutor"><input id="tutor-contrasena" value={contrasena} type={visible?'text':'password'} autoComplete="new-password" placeholder="Escribir…" disabled={guardando} aria-invalid={Boolean(errores.contrasena)} aria-describedby={errores.contrasena?"error-tutor-contrasena":undefined} onChange={evento=>{cambiarContrasena(evento.target.value);revisarCampo('contrasena',evento.target.value);}}/><button type="button" aria-label={visible?'Ocultar contraseña':'Mostrar contraseña'} aria-pressed={visible} onClick={()=>cambiarVisible(!visible)}>{visible?<EyeOff aria-hidden="true"/>:<Eye aria-hidden="true"/>}</button></div>{mensajes('contrasena')}<small>Al menos 8 caracteres, una mayúscula, un número y un carácter especial.</small></div>}
        <div className="campo-formulario-tutor"><label htmlFor="tutor-departamentoId">Departamento</label><select id="tutor-departamentoId" value={departamentoId} disabled={guardando} aria-invalid={Boolean(errores.departamentoId)} aria-describedby={errores.departamentoId?"error-tutor-departamentoId":undefined} onChange={evento=>{cambiarDepartamentoId(evento.target.value);revisarCampo('departamentoId',evento.target.value);}}><option value="">Seleccionar departamento</option>{departamentos.filter(departamento=>departamento.activo||departamento.id===inicial?.departamentoId).map(departamento=><option key={departamento.id} value={departamento.id}>{departamento.nombre}{departamento.activo?'':' (inactivo)'}</option>)}</select>{mensajes('departamentoId')}{!departamentos.some(departamento=>departamento.activo)&&operacion.tipo==='crear'&&<small>No hay departamentos activos disponibles.</small>}</div>
        {operacion.tipo==='editar'&&<p className="nota-formulario-tutor">La contraseña y los permisos de la cuenta se conservan.</p>}
      </>}
      {error&&<p role="alert" className="error-formulario-tutor">{error}</p>}
      <footer><button type="button" className="boton-cancelar-tutor" onClick={alCerrar} disabled={guardando}>Cancelar</button><button type="submit" className="boton-guardar-tutor" disabled={guardando}>{guardando?'Guardando…':operacion.tipo==='estado'?'Confirmar':'Guardar tutor'}</button></footer>
    </form>
  </dialog>;
}
