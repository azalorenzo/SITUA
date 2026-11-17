import { useEffect, useRef, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { solicitarCodigo, cambiarContrasena } from '../servicios/recuperacion';
import { ErrorSolicitud, validarAcceso } from '../servicios/autenticacion';
import type { ErroresFormulario } from '../tipos/autenticacion';

export default function RecuperarContrasena({correoInicial,alVolver}:{correoInicial:string;alVolver:(correo:string,mensaje?:string)=>void}) {
  const [paso,cambiarPaso]=useState<'correo'|'codigo'>('correo');
  const [correo,cambiarCorreo]=useState(correoInicial);
  const [codigo,cambiarCodigo]=useState('');
  const [contrasena,cambiarClave]=useState('');
  const [confirmacion,cambiarConfirmacion]=useState('');
  const [enProceso,cambiarProceso]=useState(false);
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [mensaje,cambiarMensaje]=useState('');
  const [aviso,cambiarAviso]=useState('');
  const [espera,cambiarEspera]=useState(0);
  const ocupado=useRef(false);
  const titulo=useRef<HTMLHeadingElement>(null);
  useEffect(()=>{titulo.current?.focus();},[paso]);
  useEffect(()=>{
    if(!espera)return;
    const temporizador=window.setInterval(()=>cambiarEspera(anterior=>Math.max(0,anterior-1)),1000);
    return ()=>window.clearInterval(temporizador);
  },[espera>0]);
  function mostrarError(fallo:unknown) {
    cambiarMensaje(fallo instanceof Error?fallo.message:'No se pudo completar la solicitud. Intenta nuevamente.');
    if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);
  }
  async function pedirCodigo() {
    if(ocupado.current)return;
    const revisados=validarAcceso(correo,'');
    if(revisados.correo){cambiarErrores({correo:revisados.correo});return;}
    ocupado.current=true;cambiarProceso(true);cambiarErrores({});cambiarMensaje('');cambiarAviso('');
    try {
      const respuesta=await solicitarCodigo(correo);cambiarAviso(respuesta.mensaje);cambiarPaso('codigo');cambiarEspera(60);cambiarCodigo('');
    }catch(fallo){mostrarError(fallo);}finally{ocupado.current=false;cambiarProceso(false);}
  }
  async function enviar(evento:React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();if(ocupado.current)return;
    if(paso==='correo'){await pedirCodigo();return;}
    const revisados=validarAcceso(correo,contrasena);
    if(!/^[0-9]{6}$/.test(codigo))revisados.codigo=['Escribe el código de seis dígitos.'];
    if(contrasena!==confirmacion||!confirmacion)revisados.confirmacion=['Las contraseñas deben coincidir.'];
    cambiarErrores(revisados);cambiarMensaje('');if(Object.keys(revisados).length)return;
    ocupado.current=true;cambiarProceso(true);
    try {
      const respuesta=await cambiarContrasena({correo,codigo,contrasena,confirmacion});
      alVolver(correo,respuesta.mensaje);
    }catch(fallo){mostrarError(fallo);}finally{ocupado.current=false;cambiarProceso(false);}
  }
  const campos=paso==='correo'?[{nombre:'correo',etiqueta:'Correo',tipo:'email',valor:correo,cambiar:cambiarCorreo,autocompletar:'email'}]:[
    {nombre:'codigo',etiqueta:'Código de recuperación',tipo:'text',valor:codigo,cambiar:cambiarCodigo,autocompletar:'one-time-code'},
    {nombre:'contrasena',etiqueta:'Nueva contraseña',tipo:'password',valor:contrasena,cambiar:cambiarClave,autocompletar:'new-password'},
    {nombre:'confirmacion',etiqueta:'Confirmar nueva contraseña',tipo:'password',valor:confirmacion,cambiar:cambiarConfirmacion,autocompletar:'new-password'}];
  return <>
    <header className="encabezado-acceso"><h1 id="titulo-inicio-sesion" tabIndex={-1} ref={titulo}>{paso==='correo'?'Recuperar contraseña':'Cambiar contraseña'}</h1><p>{paso==='correo'?'Escribe el correo de tu cuenta para solicitar un código.':`Ingresa el código enviado a ${correo} y elige tu nueva contraseña.`}</p></header>
    <form className="formulario-acceso formulario-registro" onSubmit={enviar} noValidate aria-busy={enProceso}>
      <div className="campos-acceso">{campos.map(campo=><div className="campo-acceso" key={campo.nombre}>
        <label htmlFor={`recuperacion-${campo.nombre}`}>{campo.etiqueta}</label>
        <input id={`recuperacion-${campo.nombre}`} type={campo.tipo} autoComplete={campo.autocompletar} value={campo.valor} disabled={enProceso}
          maxLength={campo.nombre==='codigo'?6:campo.nombre==='correo'?254:72} inputMode={campo.nombre==='codigo'?'numeric':undefined}
          aria-invalid={Boolean(errores[campo.nombre]?.length)} aria-describedby={errores[campo.nombre]?.length?`error-recuperacion-${campo.nombre}`:undefined}
          onChange={evento=>{campo.cambiar(evento.target.value);cambiarErrores(anteriores=>({...anteriores,[campo.nombre]:[]}));}}/>
        {Boolean(errores[campo.nombre]?.length)&&<ul id={`error-recuperacion-${campo.nombre}`} className="errores-campo-acceso">{errores[campo.nombre].map(texto=><li key={texto}>{texto}</li>)}</ul>}
      </div>)}</div>
      {paso==='codigo'&&<p className="ayuda-registro">El código vence en 10 minutos. Incluye al menos 8 caracteres, una mayúscula, un número y un carácter especial en tu contraseña.</p>}
      <p className="mensaje-acceso" role="alert">{mensaje}</p>
      {aviso&&<p className="aviso-acceso" role="status">{aviso}</p>}
      <button type="submit" className="boton-principal-acceso" disabled={enProceso}>{enProceso?<><LoaderCircle className="giro-acceso" aria-hidden="true"/>Procesando…</>:paso==='correo'?'Enviar código':'Guardar contraseña'}</button>
    </form>
    {paso==='codigo'&&<p className="registro-acceso"><button type="button" className="enlace-acceso" disabled={enProceso||espera>0} onClick={pedirCodigo}>{espera>0?`Reenviar código en ${espera} s`:'Reenviar código'}</button> · <button type="button" className="enlace-acceso" disabled={enProceso} onClick={()=>{cambiarPaso('correo');cambiarErrores({});cambiarMensaje('');cambiarAviso('');cambiarCodigo('');cambiarClave('');cambiarConfirmacion('');}}>Cambiar correo</button></p>}
    <p className="registro-acceso"><button type="button" className="enlace-acceso" disabled={enProceso} onClick={()=>alVolver(correo)}>Volver a iniciar sesión</button></p>
    {paso==='correo'&&<p className="ayuda-registro">Si tu cuenta solo usa Google, recupera el acceso desde Google.</p>}
  </>;
}
