import type { ErroresFormulario, RespuestaInicioSesion, UsuarioSesion } from '../tipos/autenticacion';

const direccionApi = (import.meta.env.VITE_URL_API || '/api').replace(/\/$/, '');
const claveSesion = 'tutoria.sesion';
let tokenEnMemoria: string | null = null;

export class ErrorSolicitud extends Error {
  constructor(public estado: number, mensaje: string, public erroresPorCampo: ErroresFormulario = {}) {
    super(mensaje);
  }
}

export function limpiarSesion() {
  tokenEnMemoria = null;
  try { localStorage.removeItem(claveSesion); } catch { /* Puede estar bloqueado en este navegador. */ }
  try { sessionStorage.removeItem(claveSesion); } catch { /* La sesión en memoria ya se eliminó. */ }
}

export function guardarSesion(respuesta: RespuestaInicioSesion, recordar: boolean) {
  limpiarSesion();
  tokenEnMemoria = respuesta.token;
  try {
    const almacenamiento = recordar ? localStorage : sessionStorage;
    almacenamiento.setItem(claveSesion, JSON.stringify({ token: respuesta.token, venceEn: respuesta.venceEn }));
  } catch { /* Si el navegador no permite guardar, el acceso dura mientras esta página siga abierta. */ }
}

export function obtenerToken(): string | null {
  if (tokenEnMemoria) return tokenEnMemoria;
  try {
    const contenido = localStorage.getItem(claveSesion) || sessionStorage.getItem(claveSesion);
    if (!contenido) return null;
    const sesion = JSON.parse(contenido);
    if (typeof sesion.token !== 'string' || typeof sesion.venceEn !== 'string'
      || !Number.isFinite(Date.parse(sesion.venceEn)) || Date.parse(sesion.venceEn) <= Date.now()) {
      limpiarSesion();
      return null;
    }
    tokenEnMemoria = sesion.token;
    return tokenEnMemoria;
  } catch {
    limpiarSesion();
    return null;
  }
}

export function obtenerVencimiento(): string | null {
  try {
    const contenido = localStorage.getItem(claveSesion) || sessionStorage.getItem(claveSesion);
    return contenido ? JSON.parse(contenido).venceEn : null;
  } catch { return null; }
}

export async function solicitarApi<T>(ruta: string, opciones: RequestInit = {}, protegida = true): Promise<T> {
  const cabeceras = new Headers(opciones.headers);
  if (opciones.body) cabeceras.set('Content-Type', 'application/json');
  if (protegida) {
    const token = obtenerToken();
    if (token) cabeceras.set('Authorization', `Bearer ${token}`);
  }
  let respuesta: Response;
  try {
    respuesta = await fetch(`${direccionApi}${ruta}`, { ...opciones, headers: cabeceras });
  } catch {
    throw new ErrorSolicitud(0, 'No se pudo conectar con el servidor. Revisa tu conexión e intenta nuevamente.');
  }
  const contenido = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    if (protegida && respuesta.status === 401) limpiarSesion();
    throw new ErrorSolicitud(respuesta.status, contenido?.mensaje || 'No se pudo completar la solicitud. Intenta nuevamente.',
      contenido?.erroresPorCampo || {});
  }
  if (!contenido) throw new ErrorSolicitud(0, 'El servidor devolvió una respuesta inesperada.');
  return contenido as T;
}

export function iniciarSesion(correo: string, contrasena: string) {
  return solicitarApi<RespuestaInicioSesion>('/autenticacion/iniciar-sesion', {
    method: 'POST', body: JSON.stringify({ correo: correo.trim(), contrasena }),
  }, false);
}

export function consultarSesion() {
  return solicitarApi<UsuarioSesion>('/autenticacion/sesion');
}

export function validarAcceso(correo: string, contrasena: string): ErroresFormulario {
  const errores: ErroresFormulario = {};
  if (!correo.trim()) errores.correo = ['Escribe tu correo.'];
  else if (correo.trim().length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim())) {
    errores.correo = ['Escribe un correo válido.'];
  }
  if (!contrasena.trim()) errores.contrasena = ['Escribe tu contraseña.'];
  else {
    const mensajes = [];
    if (contrasena.length < 8) mensajes.push('La contraseña debe tener al menos 8 caracteres.');
    if (new TextEncoder().encode(contrasena).length > 72) mensajes.push('La contraseña es demasiado larga.');
    if (!/[A-Z]/.test(contrasena) || !/[0-9]/.test(contrasena) || !/[^\p{L}\p{N}\s]/u.test(contrasena)) {
      mensajes.push('Incluye una mayúscula, un número y un carácter especial.');
    }
    if (mensajes.length) errores.contrasena = mensajes;
  }
  return errores;
}

export function registrarCuenta(datos: import('../tipos/autenticacion').DatosRegistro) {
  return solicitarApi<RespuestaInicioSesion>('/autenticacion/registrar', {
    method: 'POST', body: JSON.stringify({ ...datos, nombre: datos.nombre.trim(), correo: datos.correo.trim(), matricula: datos.matricula.trim() }),
  }, false);
}

export function ingresarConGoogle(datos: { credencial: string; comprobacion: string; matricula?: string; contrasenaVinculacion?: string }) {
  return solicitarApi<RespuestaInicioSesion>('/autenticacion/google', { method: 'POST', body: JSON.stringify(datos) }, false);
}

export function validarRegistro(datos: import('../tipos/autenticacion').DatosRegistro): ErroresFormulario {
  const errores = validarAcceso(datos.correo, datos.contrasena);
  if (!datos.nombre.trim() || datos.nombre.trim().length > 160) errores.nombre = ['Escribe tu nombre, de hasta 160 caracteres.'];
  if (!/^[\p{L}\p{N}_-]{3,30}$/u.test(datos.matricula.trim())) errores.matricula = ['Escribe una matrícula de 3 a 30 letras, números, guiones o guiones bajos.'];
  if (!datos.confirmarContrasena || datos.contrasena !== datos.confirmarContrasena) errores.confirmarContrasena = ['Las contraseñas deben coincidir.'];
  return errores;
}
