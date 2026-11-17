import { Search } from 'lucide-react';
import type { CriteriosReportes,MotivoReporte } from '../tipos/reportes';
import type { PeriodoAcademico } from '../tipos/asignaciones';
import type { ErroresFormulario } from '../tipos/autenticacion';
function FiltrosGeneradorReportes({criterios,alCambiar,alBuscar,periodos,motivos,cargandoCatalogos,errores}:{
  criterios:CriteriosReportes;alCambiar:(campo:keyof CriteriosReportes,valor:string)=>void;alBuscar:(evento:React.FormEvent)=>void;
  periodos:PeriodoAcademico[];motivos:MotivoReporte[];cargandoCatalogos:boolean;errores:ErroresFormulario;
}){
  function mensajes(campo:string){return errores[campo]&&<ul id={`error-reporte-${campo}`} className="errores-filtro-reporte">{errores[campo].map(texto=><li key={texto}>{texto}</li>)}</ul>;}
  return <form className="filtros-reportes" noValidate onSubmit={alBuscar}>
    <h3>Selecciona los filtros del reporte</h3>
    <div className="campos-filtros-reportes">
      <div><label htmlFor="reporte-busqueda">Tutor o estudiante</label><div className="busqueda-reportes"><input id="reporte-busqueda" type="search" maxLength={160} placeholder="Nombre o matrícula" value={criterios.busqueda} onChange={evento=>alCambiar('busqueda',evento.target.value)}/><Search aria-hidden="true"/></div>{mensajes('busqueda')}</div>
      <div><label htmlFor="reporte-estado">Estado del reporte</label><select id="reporte-estado" value={criterios.estado} onChange={evento=>alCambiar('estado',evento.target.value)}><option value="">Todos</option><option value="completado">Completado</option><option value="pendiente">Pendiente</option></select>{mensajes('estado')}</div>
      <div><label htmlFor="reporte-periodoId">Periodo académico</label><select id="reporte-periodoId" value={criterios.periodoId} disabled={cargandoCatalogos} onChange={evento=>alCambiar('periodoId',evento.target.value)}><option value="">Todos los periodos</option>{periodos.map(periodo=><option key={periodo.id} value={periodo.id}>{periodo.nombre}</option>)}</select>{mensajes('periodoId')}</div>
      <div><label htmlFor="reporte-motivoId">Motivo</label><select id="reporte-motivoId" value={criterios.motivoId} disabled={cargandoCatalogos} onChange={evento=>alCambiar('motivoId',evento.target.value)}><option value="">Todos los motivos</option>{motivos.map(motivo=><option key={motivo.id} value={motivo.id}>{motivo.nombre}{motivo.activo?'':' (inactivo)'}</option>)}</select>{mensajes('motivoId')}</div>
      <div><label htmlFor="reporte-desde">Fecha inicial</label><input id="reporte-desde" type="date" value={criterios.desde} onChange={evento=>alCambiar('desde',evento.target.value)}/>{mensajes('desde')}</div>
      <div><label htmlFor="reporte-hasta">Fecha final</label><input id="reporte-hasta" type="date" value={criterios.hasta} aria-invalid={Boolean(errores.hasta)} aria-describedby={errores.hasta?'error-reporte-hasta':undefined} onChange={evento=>alCambiar('hasta',evento.target.value)}/>{mensajes('hasta')}</div>
    </div>
    <div className="acciones-filtros-reportes"><p>Los filtros se aplican al buscar. El PDF incluye los resultados de la búsqueda aplicada.</p><button type="submit" className="boton-principal-reporte"><Search aria-hidden="true"/>Buscar reporte</button></div>
  </form>;
}
export { FiltrosGeneradorReportes as ReportGenerator };
