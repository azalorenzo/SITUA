import { useCallback, useEffect, useState } from 'react';
import { ErrorSolicitud } from './autenticacion';

// Mensaje para mostrar cuando una consulta falla.
export function mensajeDeError(fallo: unknown, predeterminado = 'No se pudo cargar la información.'): string {
  if (fallo instanceof ErrorSolicitud && fallo.estado === 401) return 'Tu sesión ya no es válida. Sal y vuelve a iniciar sesión.';
  return fallo instanceof Error ? fallo.message : predeterminado;
}

// Consulta un objeto al backend al abrir la pantalla y permite volver a pedirlo. Cancela la petición si la pantalla se cierra.
export function usarConsultaTutor<T>(consultar: (comprobacion: AbortSignal) => Promise<T>) {
  const [datos, setDatos] = useState<T | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    const cancelacion = new AbortController();
    setCargando(true);
    setError('');
    consultar(cancelacion.signal).then(respuesta => {
      if (!cancelacion.signal.aborted) setDatos(respuesta);
    }).catch(fallo => {
      if (!cancelacion.signal.aborted) setError(mensajeDeError(fallo));
    }).finally(() => {
      if (!cancelacion.signal.aborted) setCargando(false);
    });
    return () => cancelacion.abort();
  }, [consultar, intento]);

  const actualizar = useCallback(() => setIntento(anterior => anterior + 1), []);
  return { datos, cargando, error, actualizar };
}
