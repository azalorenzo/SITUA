import { PieChart,Pie,Cell,ResponsiveContainer,Tooltip } from 'recharts';
import type { MotivoDistribucion } from '../tipos/reportes';
const colores=['#1F6662','#986314','#B83F4B','#227F78','#5D7796','#8A6A9E'];
function GraficaMotivosReportes({motivos,cargando}:{motivos:MotivoDistribucion[];cargando:boolean}){
  return <section className="grafica-motivos-reportes" aria-labelledby="titulo-motivos-reportes"><h3 id="titulo-motivos-reportes">Motivos de reporte académico</h3>
    {cargando?<p className="nota-reportes">Consultando distribución…</p>:!motivos.length?<p className="nota-reportes">No hay datos para estos filtros.</p>:<>
      <ResponsiveContainer width="100%" height={220}><PieChart><Pie data={motivos} dataKey="cantidad" nameKey="nombre" cx="50%" cy="50%" innerRadius={57} outerRadius={78}>{motivos.map((motivo,indice)=><Cell key={motivo.id} fill={colores[indice%colores.length]}/>)}</Pie><Tooltip/></PieChart></ResponsiveContainer>
      <ul>{motivos.map((motivo,indice)=><li key={motivo.id}><span><i style={{background:colores[indice%colores.length]}}/>{motivo.nombre}</span><strong>{motivo.cantidad} ({motivo.porcentaje}%)</strong></li>)}</ul>
    </>}
  </section>;
}
export { GraficaMotivosReportes as AcademicReasonsChart };
