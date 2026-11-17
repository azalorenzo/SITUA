import { solicitarApi } from './autenticacion';
export function solicitarCodigo(correo:string) {
  return solicitarApi<{mensaje:string}>('/autenticacion/recuperacion/solicitar',{method:'POST',body:JSON.stringify({correo:correo.trim()})},false);
}
export function cambiarContrasena(datos:{correo:string;codigo:string;contrasena:string;confirmacion:string}) {
  return solicitarApi<{mensaje:string}>('/autenticacion/recuperacion/cambiar-contrasena',{method:'POST',body:JSON.stringify({...datos,correo:datos.correo.trim()})},false);
}
