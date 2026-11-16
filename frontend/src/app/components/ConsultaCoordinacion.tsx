import { useEffect, useState } from 'react';
import { LoaderCircle, Search } from 'lucide-react';
import { ErrorSolicitud } from '../servicios/autenticacion';

export function usarConsultaCoordinacion<T>(consultar: (comprobacion: AbortSignal) => Promise<T[]>) {
  const [datos, cambiarDatos] = useState<T[]>([]);
  const [cargando, cambiarCargando] = useState(true);
  const [error, cambiarError] = useState('');
  const [intento, cambiarIntento] = useState(0);

  useEffect(() => {
    const cancelacion = new AbortController();
    cambiarCargando(true); cambiarError(''); cambiarDatos([]);
    consultar(cancelacion.signal).then(respuesta => {
      if (!cancelacion.signal.aborted) cambiarDatos(respuesta);
    }).catch(fallo => {
      if (cancelacion.signal.aborted) return;
      cambiarError(fallo instanceof ErrorSolicitud && fallo.estado === 401
        ? 'Tu sesión ya no es válida. Sal y vuelve a iniciar sesión.'
        : fallo instanceof Error ? fallo.message : 'No se pudo cargar la lista.');
    }).finally(() => { if (!cancelacion.signal.aborted) cambiarCargando(false); });
    return () => cancelacion.abort();
  }, [consultar, intento]);

  return { datos, cargando, error, actualizar: () => cambiarIntento(anterior => anterior + 1) };
}

export function EstadoConsulta({ cargando, error, alReintentar }: {
  cargando: boolean; error: string; alReintentar: () => void;
}) {
  if (cargando) return <p role="status" className="flex items-center gap-2 p-6 text-[#53716E]"><LoaderCircle className="w-5 h-5 animate-spin" aria-hidden="true" />Cargando datos…</p>;
  if (error) return <div className="p-6"><p role="alert" className="text-[#B83F4B]">{error}</p><button type="button" onClick={alReintentar} className="mt-3 text-[#1F6662] underline">Reintentar</button></div>;
  return null;
}

export function normalizarBusqueda(texto: string) {
  return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
}

export function InicialesPersona({ nombre }: { nombre: string }) {
  return <span aria-hidden="true" className="w-10 h-10 shrink-0 bg-[#1F6662] rounded-full flex items-center justify-center text-white text-sm">{nombre.trim().split(/\s+/).slice(0, 2).map(parte => parte[0]).join('')}</span>;
}

export function BusquedaCoordinacion({ tipo, busqueda, alBuscar, estado, alCambiarEstado }: {
  tipo: 'tutor' | 'estudiante'; busqueda: string; alBuscar: (valor: string) => void;
  estado: string; alCambiarEstado: (valor: string) => void;
}) {
  return <div className="flex flex-wrap gap-3">
    <div className="relative"><Search aria-hidden="true" className="w-5 h-5 text-[#53716E] absolute left-3 top-1/2 -translate-y-1/2" />
      <input type="search" aria-label={`Buscar ${tipo}`} placeholder={`Buscar ${tipo}…`} value={busqueda} onChange={evento => alBuscar(evento.target.value)}
        className="w-full sm:w-64 pl-10 pr-4 py-2 border border-[#DCE3EA] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#1F6662]" />
    </div>
    <select aria-label="Estado de la cuenta" value={estado} onChange={evento => alCambiarEstado(evento.target.value)} className="px-3 py-2 border border-[#DCE3EA] rounded-lg">
      <option value="todos">Todas las cuentas</option><option value="activo">Activas</option><option value="inactivo">Inactivas</option>
    </select>
  </div>;
}

export const estiloCelda = 'py-4 px-6 text-sm text-left';
export const estiloTarjeta = 'bg-white rounded-lg p-4 shadow-sm';
