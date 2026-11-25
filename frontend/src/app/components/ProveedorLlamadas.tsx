import { createContext, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { Phone, PhoneOff, Video } from 'lucide-react';
import { Dialog, DialogContent, DialogTitle } from './ui/dialog';
import { aceptarLlamada, consultarLlamada, entrarLlamada, finalizarLlamada, iniciarLlamada, rechazarLlamada, type Llamada, type TipoLlamada } from '../servicios/llamadas';
import { obtenerToken } from '../servicios/autenticacion';

interface Contexto {
  habilitado: boolean;
  ocupado: boolean;
  iniciar: (conversacion: number, tipo: TipoLlamada) => void;
}
const Llamadas = createContext<Contexto>({ habilitado: false, ocupado: false, iniciar: () => {} });
export const usarLlamadas = () => useContext(Llamadas);
const mensajeError = (fallo: unknown) => fallo instanceof Error ? fallo.message : 'No se pudo conectar la llamada.';

function SalaLlamada({ id, alSalir }: { id: number; alSalir: () => void }) {
  const contenedor = useRef<HTMLDivElement>(null);
  const salida = useRef(alSalir);
  salida.current = alSalir;
  const [error, cambiarError] = useState('');
  const [intento, cambiarIntento] = useState(0);
  useEffect(() => {
    let cerrada = false;
    let sala: import('@daily-co/daily-js').DailyCall | undefined;
    cambiarError('');
    async function conectar() {
      try {
        const [acceso, modulo] = await Promise.all([entrarLlamada(id), import('@daily-co/daily-js')]);
        if (cerrada || !contenedor.current) return;
        sala = modulo.default.createFrame(contenedor.current, {
          iframeStyle: { width: '100%', height: '100%', border: '0', borderRadius: '12px' },
          showLeaveButton: true,
          showFullscreenButton: false,
        });
        sala.on('left-meeting', () => { if (!cerrada) salida.current(); });
        sala.on('error', () => { if (!cerrada) cambiarError('No se pudo conectar. Revisa los permisos de cámara y micrófono.'); });
        await sala.join({ url: acceso.url, token: acceso.token });
      } catch (fallo) {
        if (!cerrada) cambiarError(mensajeError(fallo));
      }
    }
    void conectar();
    return () => { cerrada = true; if (sala) void sala.destroy().catch(() => {}); };
  }, [id, intento]);
  return <div>
    {error && <div className="mb-3 text-sm text-red-700" role="alert">{error} <button className="underline" onClick={() => cambiarIntento(valor => valor + 1)}>Reintentar</button></div>}
    <div ref={contenedor} className="h-[55dvh] min-h-[240px] max-h-[540px] w-full overflow-hidden rounded-xl bg-[#F5F7FA]" />
  </div>;
}

export function ProveedorLlamadas({ children }: { children: ReactNode }) {
  const [llamada, cambiarLlamada] = useState<Llamada | null>(null);
  const [habilitado, cambiarHabilitado] = useState(false);
  const [ocupado, cambiarOcupado] = useState(false);
  const [error, cambiarError] = useState('');
  const [ahora, cambiarAhora] = useState(Date.now());
  const montado = useRef(false);
  const accion = useRef(false);
  const revision = useRef(0);
  const actual = useRef<Llamada | null>(null);
  function guardar(valor: Llamada | null) { actual.current = valor; cambiarLlamada(valor); }

  useEffect(() => {
    montado.current = true;
    let cancelado = false;
    let temporizador: ReturnType<typeof setTimeout>;
    async function consultar() {
      const version = revision.current;
      try {
        if (!obtenerToken()) return;
        const estado = await consultarLlamada();
        if (!cancelado && version === revision.current && !accion.current) {
          cambiarHabilitado(estado.habilitado);
          guardar(estado.llamada);
        }
      } catch {
        if (!cancelado && !obtenerToken()) { guardar(null); cambiarHabilitado(false); }
      } finally {
        if (!cancelado) temporizador = setTimeout(consultar, 2000);
      }
    }
    void consultar();
    const reloj = setInterval(() => cambiarAhora(Date.now()), 1000);
    return () => { cancelado = true; montado.current = false; clearTimeout(temporizador); clearInterval(reloj); };
  }, []);

  async function ejecutar(trabajo: () => Promise<Llamada | null>) {
    if (accion.current) return;
    accion.current = true; revision.current++; cambiarOcupado(true); cambiarError('');
    try {
      const resultado = await trabajo();
      if (montado.current) guardar(resultado);
    } catch (fallo) {
      if (montado.current) cambiarError(mensajeError(fallo));
    } finally {
      revision.current++; accion.current = false;
      if (montado.current) cambiarOcupado(false);
    }
  }
  function terminar() {
    const id = actual.current?.id;
    if (id) void ejecutar(async () => { await finalizarLlamada(id); return null; });
  }
  const segundos = llamada ? Math.max(0, Math.ceil((Date.parse(llamada.venceEn) - ahora) / 1000)) : 0;
  useEffect(() => {
    if (llamada && segundos === 0 && !accion.current) terminar();
  }, [llamada?.id, segundos]);
  const abierta = !!llamada || !!error;
  return <Llamadas.Provider value={{ habilitado, ocupado: ocupado || !!llamada, iniciar: (conversacion, tipo) => {
    if (!actual.current) void ejecutar(() => iniciarLlamada(conversacion, tipo));
  } }}>
    {children}
    <Dialog open={abierta} onOpenChange={valor => { if (!valor && !llamada) cambiarError(''); }}>
      <DialogContent aria-describedby={undefined} className={`${llamada?.estado === 'activa' ? 'sm:max-w-3xl' : 'sm:max-w-md'} max-h-[95dvh] overflow-auto bg-white text-[#0B2828] [&>button.absolute]:hidden`}
        onEscapeKeyDown={evento => { if (llamada) evento.preventDefault(); }} onPointerDownOutside={evento => { if (llamada) evento.preventDefault(); }}>
        <div className="flex items-center gap-3">
          {llamada?.tipo === 'video' ? <Video className="h-6 w-6 text-[#1F6662]" /> : <Phone className="h-6 w-6 text-[#1F6662]" />}
          <DialogTitle>{llamada ? (llamada.tipo === 'video' ? 'Videollamada' : 'Llamada') : 'Llamadas'}</DialogTitle>
        </div>
        {llamada && <div className="flex flex-wrap justify-between gap-2 text-sm"><span>{llamada.nombre}</span><span aria-label="Tiempo restante">{Math.floor(segundos / 60)}:{String(segundos % 60).padStart(2, '0')}</span></div>}
        {llamada?.estado === 'pendiente' && <p className="text-sm text-[#53716E]">{llamada.soyIniciador ? 'Esperando respuesta…' : 'Te está llamando.'}</p>}
        {llamada?.estado === 'activa' && segundos > 0 && <SalaLlamada id={llamada.id} alSalir={terminar} />}
        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        <div className="flex flex-wrap justify-end gap-3">
          {!llamada && <button onClick={() => cambiarError('')} className="rounded-lg border px-4 py-2">Cerrar</button>}
          {llamada?.estado === 'pendiente' && !llamada.soyIniciador && <>
            <button disabled={ocupado} onClick={() => void ejecutar(async () => { await rechazarLlamada(llamada.id); return null; })} className="rounded-lg border px-4 py-2 disabled:opacity-50">Rechazar</button>
            <button disabled={ocupado} onClick={() => void ejecutar(() => aceptarLlamada(llamada.id))} className="rounded-lg bg-[#1F6662] px-4 py-2 text-white disabled:opacity-50">Aceptar</button>
          </>}
          {llamada && (llamada.soyIniciador || llamada.estado === 'activa') && <button disabled={ocupado} onClick={terminar} className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-white disabled:opacity-50"><PhoneOff className="h-4 w-4" />{llamada.estado === 'pendiente' ? 'Cancelar' : 'Colgar'}</button>}
        </div>
      </DialogContent>
    </Dialog>
  </Llamadas.Provider>;
}
