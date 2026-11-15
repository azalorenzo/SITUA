import { useEffect, useRef, useState } from 'react';
import { ErrorSolicitud, ingresarConGoogle } from '../servicios/autenticacion';
import type { RespuestaInicioSesion } from '../tipos/autenticacion';

type BibliotecaGoogle = { accounts: { id: {
  initialize: (opciones: { client_id: string; nonce: string; callback: (respuesta: { credential: string }) => void; auto_select: boolean }) => void;
  renderButton: (elemento: HTMLElement, opciones: { theme: string; size: string; shape: string; text: string; locale: string; width: number }) => void;
} } };
let cargaGoogle: Promise<BibliotecaGoogle> | undefined;
function cargarGoogle(): Promise<BibliotecaGoogle> {
  if (cargaGoogle) return cargaGoogle;
  cargaGoogle = new Promise((resolver, rechazar) => {
    const biblioteca = (window as Window & { google?: BibliotecaGoogle }).google;
    if (biblioteca) { resolver(biblioteca); return; }
    const guion = document.createElement('script');
    guion.src = 'https://accounts.google.com/gsi/client'; guion.async = true;
    guion.onload = () => {
      const cargada = (window as Window & { google?: BibliotecaGoogle }).google;
      if (cargada) resolver(cargada); else rechazar(new Error('Google no pudo cargarse.'));
    };
    guion.onerror = () => { cargaGoogle = undefined; guion.remove(); rechazar(new Error('No se pudo cargar Google. Revisa tu conexión y vuelve a intentarlo.')); };
    document.head.appendChild(guion);
  });
  return cargaGoogle;
}

export default function AccesoGoogle({ alIngresar, recordar, desactivado }: {
  alIngresar: (respuesta: RespuestaInicioSesion, recordar: boolean) => void;
  recordar: boolean;
  desactivado: boolean;
}) {
  const cliente = import.meta.env.VITE_GOOGLE_CLIENTE_ID?.trim();
  const contenedor = useRef<HTMLDivElement>(null);
  const referencia = useRef({ alIngresar, recordar, desactivado });
  referencia.current = { alIngresar, recordar, desactivado };
  const bloqueo = useRef(false);
  const [credencial, cambiarCredencial] = useState('');
  const [comprobacion] = useState(() => crypto.randomUUID());
  const [campo, cambiarCampo] = useState<'matricula' | 'contrasenaVinculacion' | null>(null);
  const [valor, cambiarValor] = useState('');
  const [mensaje, cambiarMensaje] = useState('');
  const [enProceso, cambiarEnProceso] = useState(false);

  async function completar(credencialActual: string, dato?: { campo: 'matricula' | 'contrasenaVinculacion'; valor: string }) {
    if (bloqueo.current) return;
    bloqueo.current = true; cambiarEnProceso(true); cambiarMensaje('');
    try {
      const respuesta = await ingresarConGoogle({ credencial: credencialActual, comprobacion,
        ...(dato ? { [dato.campo]: dato.valor } : {}) });
      referencia.current.alIngresar(respuesta, referencia.current.recordar);
    } catch (error) {
      cambiarMensaje(error instanceof Error ? error.message : 'No se pudo acceder con Google.');
      if (error instanceof ErrorSolicitud) {
        if (error.erroresPorCampo.matricula) cambiarCampo('matricula');
        else if (error.erroresPorCampo.contrasenaVinculacion) cambiarCampo('contrasenaVinculacion');
      }
    } finally { bloqueo.current = false; cambiarEnProceso(false); }
  }

  useEffect(() => {
    if (!cliente) return;
    let activo = true;
    cargarGoogle().then(biblioteca => {
      if (!activo || !contenedor.current) return;
      biblioteca.accounts.id.initialize({ client_id: cliente, nonce: comprobacion, auto_select: false,
        callback: respuesta => {
          if (!activo || referencia.current.desactivado) return;
          cambiarCredencial(respuesta.credential); cambiarCampo(null); cambiarValor('');
          void completar(respuesta.credential);
        } });
      biblioteca.accounts.id.renderButton(contenedor.current, {
        theme: 'outline', size: 'large', shape: 'pill', text: 'continue_with', locale: 'es', width: Math.min(contenedor.current.clientWidth, 400),
      });
    }).catch(error => { if (activo) cambiarMensaje(error.message); });
    return () => { activo = false; };
  }, [cliente, comprobacion]);

  return <div className="acceso-google" aria-busy={enProceso}>
    {cliente ? <div ref={contenedor} className="contenedor-google" {...{ inert: desactivado || enProceso || campo !== null ? '' : undefined }} />
      : <><button type="button" className="boton-google-acceso" disabled><span className="letra-google" aria-hidden="true">G</span>Continuar con Google</button>
        <p className="aviso-acceso">Google estará disponible cuando se configure el acceso.</p></>}
    {enProceso && <p className="aviso-acceso" role="status">Verificando tu cuenta…</p>}
    {campo && <form className="completar-google" onSubmit={evento => {
      evento.preventDefault(); if (valor.trim()) void completar(credencial, { campo, valor });
    }}>
      <div className="campo-acceso"><label htmlFor="dato-google">{campo === 'matricula' ? 'Matrícula' : 'Contraseña actual de tu cuenta'}</label>
        <input id="dato-google" type={campo === 'matricula' ? 'text' : 'password'} value={valor}
          autoComplete={campo === 'matricula' ? 'off' : 'current-password'} required disabled={enProceso}
          onChange={evento => cambiarValor(evento.target.value)} /></div>
      <button type="submit" className="boton-principal-acceso" disabled={enProceso || !valor.trim()}>Continuar</button>
      <button type="button" className="enlace-acceso cancelar-google" disabled={enProceso} onClick={() => {
        cambiarCampo(null); cambiarCredencial(''); cambiarValor(''); cambiarMensaje('');
      }}>Cancelar</button>
    </form>}
    {mensaje && <p className="mensaje-acceso" role="alert">{mensaje}</p>}
  </div>;
}
