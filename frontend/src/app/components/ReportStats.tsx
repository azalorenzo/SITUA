import { FileCheck,TrendingUp,AlertCircle } from 'lucide-react';
import type { ResumenReportes } from '../tipos/reportes';
function EstadisticasReportes({resumen,cargando}:{resumen?:ResumenReportes;cargando:boolean}){
  const tarjetas=[{titulo:'Total de reportes',valor:resumen?.total,icono:FileCheck,tono:'verde'},{titulo:'Reportes completados',valor:resumen?.completados,icono:TrendingUp,tono:'verde'},{titulo:'Reportes pendientes',valor:resumen?.pendientes,icono:AlertCircle,tono:'amarillo'}];
  return <div className="estadisticas-reportes">{tarjetas.map(tarjeta=>{const Icono=tarjeta.icono;return <div key={tarjeta.titulo}><span className={`icono-estadistica-reporte ${tarjeta.tono}`}><Icono aria-hidden="true"/></span><div><p>{tarjeta.titulo}</p><strong>{cargando?'—':tarjeta.valor??'—'}</strong></div></div>;})}</div>;
}
export { EstadisticasReportes as ReportStats };
