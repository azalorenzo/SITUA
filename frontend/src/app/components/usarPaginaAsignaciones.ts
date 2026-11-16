import { useEffect, useState } from 'react';
import { consultarAsignaciones } from '../servicios/asignaciones';
import type { FiltrosAsignaciones, PaginaAsignaciones } from '../tipos/asignaciones';

export function usarPaginaAsignaciones({ pagina, tamano, periodoId, estado, busqueda }: FiltrosAsignaciones) {
  const [datos, cambiarDatos] = useState<PaginaAsignaciones | null>(null);
  const [cargando, cambiarCargando] = useState(true);
  const [error, cambiarError] = useState('');
  const [intento, cambiarIntento] = useState(0);
  useEffect(() => {
    const cancelacion = new AbortController();
    cambiarCargando(true); cambiarError('');
    consultarAsignaciones(cancelacion.signal, { pagina, tamano, periodoId, estado, busqueda }).then(respuesta => {
      if (!cancelacion.signal.aborted) cambiarDatos(respuesta);
    }).catch(fallo => {
      if (!cancelacion.signal.aborted) cambiarError(fallo instanceof Error ? fallo.message : 'No se pudieron cargar las asignaciones.');
    }).finally(() => { if (!cancelacion.signal.aborted) cambiarCargando(false); });
    return () => cancelacion.abort();
  }, [pagina, tamano, periodoId, estado, busqueda, intento]);
  return { datos, cargando, error, actualizar: () => cambiarIntento(anterior => anterior + 1) };
}
