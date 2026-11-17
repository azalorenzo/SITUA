import { useState } from 'react';
import RecuperarContrasena from './RecuperarContrasena';
import RegistroCuenta from './RegistroCuenta';
import AccesoGoogle from './AccesoGoogle';
import { Eye, EyeOff, LoaderCircle } from 'lucide-react';
import { ErrorSolicitud, iniciarSesion, validarAcceso } from '../servicios/autenticacion';
import type { ErroresFormulario, RespuestaInicioSesion } from '../tipos/autenticacion';
import imagenBiblioteca from '../../assets/biblioteca-acceso.png';
import '../../styles/inicio-sesion.css';

interface PropiedadesInicioSesion {
  alIngresar: (respuesta: RespuestaInicioSesion, recordar: boolean) => void;
  mensajeInicial?: string;
}

export default function InicioSesion({ alIngresar, mensajeInicial = '' }: PropiedadesInicioSesion) {
  const [recuperacion, cambiarRecuperacion] = useState(false);
  const [registro, cambiarRegistro] = useState(false);
  const [contrasenaVisible, cambiarContrasenaVisible] = useState(false);
  const [correo, cambiarCorreo] = useState('');
  const [contrasena, cambiarContrasena] = useState('');
  const [recordar, cambiarRecordar] = useState(false);
  const [enProceso, cambiarEnProceso] = useState(false);
  const [errores, cambiarErrores] = useState<ErroresFormulario>({});
  const [mensaje, cambiarMensaje] = useState(mensajeInicial);
  const [aviso, cambiarAviso] = useState('');

  function revisarCampo(campo: 'correo' | 'contrasena', valor: string) {
    const revisados = validarAcceso(campo === 'correo' ? valor : correo, campo === 'contrasena' ? valor : contrasena);
    cambiarErrores(anteriores => ({ ...anteriores, [campo]: revisados[campo] || [] }));
    cambiarMensaje('');
  }

  async function enviarFormulario(evento: React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (enProceso) return;
    const revisados = validarAcceso(correo, contrasena);
    cambiarErrores(revisados);
    cambiarMensaje('');
    cambiarAviso('');
    if (Object.keys(revisados).length) {
      document.getElementById(revisados.correo ? 'correo-acceso' : 'contrasena-acceso')?.focus();
      return;
    }
    cambiarEnProceso(true);
    try {
      const respuesta = await iniciarSesion(correo, contrasena);
      alIngresar(respuesta, recordar);
    } catch (error) {
      if (error instanceof ErrorSolicitud) {
        cambiarErrores(error.erroresPorCampo);
        cambiarMensaje(error.message);
      } else cambiarMensaje('No se pudo iniciar sesión. Intenta nuevamente.');
    } finally { cambiarEnProceso(false); }
  }

  return (
    <main className="inicio-sesion">
      <span className="circulo-acceso circulo-acceso-superior" aria-hidden="true" />
      <span className="circulo-acceso circulo-acceso-inferior" aria-hidden="true" />

      <section className="tarjeta-acceso" aria-labelledby="titulo-inicio-sesion">
        <div className="panel-imagen-acceso">
          <img
            src={imagenBiblioteca}
            alt="Estudiantes y su tutora trabajando juntos en una biblioteca"
            className="imagen-biblioteca-acceso"
          />
        </div>

        <div className="panel-formulario-acceso">
          <p className="nombre-sistema-acceso">Control Académico</p>
          {recuperacion ? <RecuperarContrasena correoInicial={correo} alVolver={(direccion,exito)=>{cambiarCorreo(direccion);cambiarRecuperacion(false);cambiarContrasena('');cambiarErrores({});cambiarMensaje('');cambiarAviso(exito||'');}}/> : registro ? <RegistroCuenta alIngresar={alIngresar} alVolver={() => cambiarRegistro(false)} /> : <>
          <header className="encabezado-acceso">
            <h1 id="titulo-inicio-sesion">Iniciar sesión</h1>
            <p>Ingresa tus datos para continuar</p>
          </header>

          <form className="formulario-acceso" noValidate onSubmit={enviarFormulario} aria-busy={enProceso}>
            <div className="campos-acceso">
              <div className="campo-acceso">
                <label htmlFor="correo-acceso">Correo</label>
                <input id="correo-acceso" name="correo" type="email" autoComplete="username"
                  value={correo} disabled={enProceso} aria-invalid={Boolean(errores.correo?.length)}
                  aria-describedby={errores.correo?.length ? 'errores-correo' : undefined}
                  onChange={evento => { cambiarCorreo(evento.target.value); revisarCampo('correo', evento.target.value); }}
                  onBlur={() => revisarCampo('correo', correo)} />
                {Boolean(errores.correo?.length) && <ul id="errores-correo" className="errores-campo-acceso" aria-live="polite">{errores.correo.map(texto => <li key={texto}>{texto}</li>)}</ul>}
              </div>

              <div className="campo-acceso">
                <label htmlFor="contrasena-acceso">Contraseña</label>
                <div className="entrada-contrasena-acceso">
                  <input
                    id="contrasena-acceso"
                    name="contrasena"
                    type={contrasenaVisible ? 'text' : 'password'}
                    autoComplete="current-password"
                    value={contrasena} disabled={enProceso} aria-invalid={Boolean(errores.contrasena?.length)}
                    aria-describedby={errores.contrasena?.length ? 'errores-contrasena' : undefined}
                    onChange={evento => { cambiarContrasena(evento.target.value); revisarCampo('contrasena', evento.target.value); }}
                    onBlur={() => revisarCampo('contrasena', contrasena)}
                  />
                  <button
                    type="button"
                    className="visibilidad-contrasena-acceso"
                    onClick={() => cambiarContrasenaVisible(!contrasenaVisible)}
                    aria-label={contrasenaVisible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    aria-pressed={contrasenaVisible}
                    aria-controls="contrasena-acceso"
                  >
                    {contrasenaVisible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
                  </button>
                </div>
                {Boolean(errores.contrasena?.length) && <ul id="errores-contrasena" className="errores-campo-acceso" aria-live="polite">{errores.contrasena.map(texto => <li key={texto}>{texto}</li>)}</ul>}
              </div>
            </div>

            <div className="opciones-acceso">
              <label className="recordar-acceso">
                <input type="checkbox" name="recordar" checked={recordar} disabled={enProceso} onChange={evento => cambiarRecordar(evento.target.checked)} />
                <span>Recordar inicio de sesión</span>
              </label>
              <button type="button" className="enlace-acceso" disabled={enProceso} onClick={() => { cambiarRecuperacion(true); cambiarContrasena(''); cambiarAviso(''); }}>¿Olvidaste tu contraseña?</button>
            </div>

            <p className="mensaje-acceso" role="alert">{mensaje}</p>
            <button type="submit" className="boton-principal-acceso" disabled={enProceso}>
              {enProceso ? <><LoaderCircle className="giro-acceso" aria-hidden="true" /> Iniciando sesión…</> : 'Iniciar Sesión'}
            </button>

          </form>
          <div className="separador-acceso"><span>o continúa con</span></div>
          <AccesoGoogle alIngresar={alIngresar} recordar={recordar} desactivado={enProceso} />
          <p className="registro-acceso">¿No tienes cuenta? <button type="button" className="enlace-acceso" disabled={enProceso} onClick={() => { cambiarRegistro(true); cambiarContrasena(''); }}>Regístrate</button></p>
            {aviso && <p className="aviso-acceso" role="status">{aviso}</p>}
          </>}
        </div>
      </section>
    </main>
  );
}
