import { useEffect,useState } from 'react';
import { solicitarApi,ErrorSolicitud } from '../servicios/autenticacion';
import type { ErroresFormulario } from '../tipos/autenticacion';
import { Dialog as Ventana,DialogContent as Contenido,DialogHeader as Encabezado,DialogTitle as Titulo } from './ui/dialog';
interface Perfil { completo:boolean; carreras:{id:number;nombre:string}[]; periodos:{id:number;nombre:string}[]; }
interface Contacto { telefono:string|null; aceptaSms:boolean; }
export function CompletarPerfilEstudiante({alSalir,editarContacto=false,alCerrar=()=>{}}:{alSalir:()=>void;editarContacto?:boolean;alCerrar?:()=>void}) {
  const [datos,cambiarDatos]=useState<Perfil|null>(null);
  const [cargando,cambiarCarga]=useState(true);
  const [guardando,cambiarGuardado]=useState(false);
  const [error,cambiarError]=useState('');
  const [errores,cambiarErrores]=useState<ErroresFormulario>({});
  const [intento,cambiarIntento]=useState(0);
  const [carrera,cambiarCarrera]=useState('');
  const [periodo,cambiarPeriodo]=useState('');
  const [semestre,cambiarSemestre]=useState('');
  const [telefono,cambiarTelefono]=useState(''),[aceptaSms,cambiarSms]=useState(false);
  const validarTelefono=(valor:string,sms:boolean)=>valor&&!/^\+[1-9][0-9]{7,14}$/.test(valor)?'Escribe el número con código de país, por ejemplo +525512345678.':sms&&!valor?'Escribe tu teléfono para recibir avisos.':'';
  useEffect(()=>{
    const cancelacion=new AbortController();cambiarCarga(true);cambiarError('');
    Promise.all([solicitarApi<Perfil>('/estudiante/perfil',{signal:cancelacion.signal}),solicitarApi<Contacto>('/estudiante/contacto',{signal:cancelacion.signal})])
      .then(([respuesta,contacto])=>{if(!cancelacion.signal.aborted){cambiarDatos(respuesta);cambiarTelefono(contacto.telefono??'');cambiarSms(contacto.aceptaSms);}})
      .catch(fallo=>{if(!cancelacion.signal.aborted)cambiarError(fallo instanceof Error?fallo.message:'No se pudieron consultar tus datos.');})
      .finally(()=>{if(!cancelacion.signal.aborted)cambiarCarga(false);});
    return ()=>cancelacion.abort();
  },[intento,editarContacto]);
  async function guardar(evento:React.FormEvent<HTMLFormElement>) {
    evento.preventDefault();if(guardando)return;
    const validaciones:ErroresFormulario={};
    const mensajeTelefono=validarTelefono(telefono.trim(),aceptaSms);
    if(mensajeTelefono)validaciones.telefono=[mensajeTelefono];
    if(!datos?.completo){
      if(!carrera)validaciones.carreraId=['Selecciona tu carrera.'];
      if(!periodo)validaciones.periodoId=['Selecciona el periodo.'];
      if(!semestre||!Number.isInteger(Number(semestre))||Number(semestre)<1||Number(semestre)>12)validaciones.semestre=['El semestre debe estar entre 1 y 12.'];
    }
    cambiarErrores(validaciones);if(Object.keys(validaciones).length)return;
    cambiarGuardado(true);cambiarError('');
    try{await solicitarApi<Contacto>('/estudiante/contacto',{method:'PUT',body:JSON.stringify({telefono:telefono.trim(),aceptaSms})});if(!datos?.completo){const respuesta=await solicitarApi<Perfil>('/estudiante/perfil',{method:'PUT',body:JSON.stringify({carreraId:Number(carrera),periodoId:Number(periodo),semestre:Number(semestre)})});cambiarDatos(respuesta);}alCerrar();}
    catch(fallo){cambiarError(fallo instanceof Error?fallo.message:'No se pudieron guardar tus datos.');if(fallo instanceof ErrorSolicitud)cambiarErrores(fallo.erroresPorCampo);}
    finally{cambiarGuardado(false);}
  }
  const faltanOpciones=datos&&!datos.completo&&(!datos.carreras.length||!datos.periodos.length);
  const estilo='w-full border border-[#DCE3EA] rounded-lg px-3 py-2 mt-1 bg-white';
  return <Ventana open={!datos?.completo||editarContacto} onOpenChange={abierto=>{if(!abierto&&datos?.completo&&!guardando)alCerrar();}}><Contenido aria-describedby={undefined} className="bg-white text-[#0B2828] [&>button.absolute]:hidden max-h-[90vh] overflow-y-auto" onEscapeKeyDown={evento=>{if(!datos?.completo||guardando)evento.preventDefault();}} onPointerDownOutside={evento=>{if(!datos?.completo||guardando)evento.preventDefault();}}>
    <Encabezado><Titulo>{datos?.completo?'Datos de contacto':'Completa tus datos'}</Titulo></Encabezado>
    {cargando?<p role="status">Cargando…</p>:<>
      {datos&&(!datos.completo||editarContacto)&&<form noValidate onSubmit={guardar} className="space-y-4">
        {!datos.completo&&<><div><label htmlFor="perfil-carrera">Carrera</label><select id="perfil-carrera" className={estilo} required disabled={guardando} value={carrera} onChange={evento=>cambiarCarrera(evento.target.value)}><option value="">Seleccionar carrera</option>{datos.carreras.map(opcion=><option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{errores.carreraId?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        <div><label htmlFor="perfil-periodo">Periodo</label><select id="perfil-periodo" className={estilo} required disabled={guardando} value={periodo} onChange={evento=>cambiarPeriodo(evento.target.value)}><option value="">Seleccionar periodo</option>{datos.periodos.map(opcion=><option key={opcion.id} value={opcion.id}>{opcion.nombre}</option>)}</select>{errores.periodoId?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        <div><label htmlFor="perfil-semestre">Semestre</label><input id="perfil-semestre" className={estilo} required type="number" min={1} max={12} step={1} value={semestre} disabled={guardando} onChange={evento=>cambiarSemestre(evento.target.value)}/>{errores.semestre?.map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        </>}
        <div><label htmlFor="perfil-telefono">Teléfono</label><input id="perfil-telefono" type="tel" autoComplete="tel" inputMode="tel" maxLength={16} placeholder="+525512345678" className={estilo} disabled={guardando} value={telefono} onChange={evento=>{const valor=evento.target.value;cambiarTelefono(valor);const mensaje=validarTelefono(valor.trim(),aceptaSms);cambiarErrores(anteriores=>({...anteriores,telefono:mensaje?[mensaje]:[],telefonoParaAvisos:[]}));}}/>{[...(errores.telefono??[]),...(errores.telefonoParaAvisos??[])].map(texto=><p key={texto} className="text-sm text-[#B83F4B]">{texto}</p>)}</div>
        <label className="flex items-start gap-2 text-sm"><input type="checkbox" className="mt-1" checked={aceptaSms} disabled={guardando} onChange={evento=>{cambiarSms(evento.target.checked);const mensaje=validarTelefono(telefono.trim() ,evento.target.checked);cambiarErrores(anteriores=>({...anteriores,telefono:mensaje?[mensaje]:[],telefonoParaAvisos:[]}));}}/>Recibir confirmaciones de tutoría por SMS</label>
        {faltanOpciones&&<p role="status" className="text-sm">No hay carreras o periodos disponibles. Contacta a coordinación.</p>}
        <button type="submit" disabled={guardando||Boolean(faltanOpciones)} className="w-full bg-[#1F6662] text-white rounded-lg px-4 py-2 disabled:opacity-50">{guardando?'Guardando…':datos.completo?'Guardar':'Guardar y continuar'}</button>
      </form>}
      {error&&<p role="alert" className="text-sm text-[#B83F4B]">{error}</p>}
      {(!datos||faltanOpciones)&&<button type="button" onClick={()=>cambiarIntento(anterior=>anterior+1)} className="text-[#1F6662] underline">Reintentar</button>}
    </>}
    {datos?.completo&&<button type="button" disabled={guardando} className="text-sm text-[#53716E]" onClick={alCerrar}>Cancelar</button>}
    <button type="button" disabled={guardando} className="text-sm text-[#53716E]" onClick={alSalir}>Cerrar sesión</button>
  </Contenido></Ventana>;
}
