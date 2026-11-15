import { useState } from 'react';
import { ErrorSolicitud, registrarCuenta, validarRegistro } from '../servicios/autenticacion';
import type { DatosRegistro, ErroresFormulario, RespuestaInicioSesion } from '../tipos/autenticacion';

export default function RegistroCuenta({ alIngresar, alVolver }: {
  alIngresar: (respuesta: RespuestaInicioSesion, recordar: boolean) => void;
  alVolver: () => void;
}) {
  const [datos, cambiarDatos] = useState<DatosRegistro>({ nombre: '', matricula: '', correo: '', contrasena: '', confirmarContrasena: '' });
  const [errores, cambiarErrores] = useState<ErroresFormulario>({});
  const [mensaje, cambiarMensaje] = useState('');
  const [enProceso, cambiarEnProceso] = useState(false);
  const [visible, cambiarVisible] = useState(false);
  const campos: { nombre: keyof DatosRegistro; titulo: string; completar: string }[] = [
    { nombre: 'nombre', titulo: 'Nombre completo', completar: 'name' },
    { nombre: 'matricula', titulo: 'Matrícula', completar: 'off' },
    { nombre: 'correo', titulo: 'Correo', completar: 'email' },
    { nombre: 'contrasena', titulo: 'Contraseña', completar: 'new-password' },
    { nombre: 'confirmarContrasena', titulo: 'Confirmar contraseña', completar: 'new-password' },
  ];
  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    if (enProceso) return;
    const revisados = validarRegistro(datos);
    cambiarErrores(revisados); cambiarMensaje('');
    if (Object.keys(revisados).length) {
      document.getElementById(`registro-${Object.keys(revisados)[0]}`)?.focus();
      return;
    }
    cambiarEnProceso(true);
    try { alIngresar(await registrarCuenta(datos), false); }
    catch (error) {
      cambiarMensaje(error instanceof Error ? error.message : 'No se pudo crear tu cuenta.');
      if (error instanceof ErrorSolicitud) cambiarErrores(error.erroresPorCampo);
    } finally { cambiarEnProceso(false); }
  }
  return <>
    <header className="encabezado-acceso"><h1 id="titulo-inicio-sesion">Crear cuenta</h1><p>Regístrate como estudiante para comenzar</p></header>
    <form className="formulario-acceso formulario-registro" noValidate onSubmit={enviar} aria-busy={enProceso}>
      <div className="campos-acceso">{campos.map(campo => <div className="campo-acceso" key={campo.nombre}>
        <label htmlFor={`registro-${campo.nombre}`}>{campo.titulo}</label>
        <input id={`registro-${campo.nombre}`} name={campo.nombre} autoComplete={campo.completar}
          type={campo.nombre.includes('ontrasena') ? (visible ? 'text' : 'password') : campo.nombre === 'correo' ? 'email' : 'text'}
          value={datos[campo.nombre]} disabled={enProceso} aria-invalid={Boolean(errores[campo.nombre]?.length)}
          aria-describedby={errores[campo.nombre]?.length ? `error-${campo.nombre}` : undefined}
          onChange={evento => {
            const nuevos = { ...datos, [campo.nombre]: evento.target.value };
            cambiarDatos(nuevos); cambiarMensaje('');
            const revisados = validarRegistro(nuevos);
            cambiarErrores(anteriores => ({ ...anteriores, [campo.nombre]: revisados[campo.nombre] || [],
              ...(campo.nombre === 'contrasena' && datos.confirmarContrasena ? { confirmarContrasena: revisados.confirmarContrasena || [] } : {}) }));
          }} />
        {Boolean(errores[campo.nombre]?.length) && <ul id={`error-${campo.nombre}`} className="errores-campo-acceso" aria-live="polite">{errores[campo.nombre].map(texto => <li key={texto}>{texto}</li>)}</ul>}
      </div>)}</div>
      <p className="ayuda-registro">Usa al menos 8 caracteres, una mayúscula, un número y un carácter especial.</p>
      <label className="recordar-acceso"><input type="checkbox" checked={visible} onChange={evento => cambiarVisible(evento.target.checked)} />Mostrar contraseñas</label>
      <p className="mensaje-acceso" role="alert">{mensaje}</p>
      <button className="boton-principal-acceso" type="submit" disabled={enProceso}>{enProceso ? 'Creando cuenta…' : 'Crear cuenta'}</button>
      <p className="registro-acceso">¿Ya tienes cuenta? <button type="button" className="enlace-acceso" disabled={enProceso} onClick={alVolver}>Inicia sesión</button></p>
    </form>
  </>;
}
