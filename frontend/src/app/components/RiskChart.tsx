import { PieChart,Pie,Cell,ResponsiveContainer,Tooltip } from 'recharts';
import type { ResumenInicio } from '../tipos/inicio';
const colores=['#1F6662','#986314','#B83F4B','#227F78','#5D7796','#8A6A9E'];
function GraficaMotivosInicio({motivos,total}:{motivos:ResumenInicio['motivos'];total:number}){
  return <section className="grafica-inicio-coordinacion" aria-labelledby="titulo-motivos-inicio"><h3 id="titulo-motivos-inicio">Motivos de reportes registrados</h3><p className="nota-inicio-coordinacion">{total} reportes de todos los periodos</p>
    {!motivos.length?<p className="vacio-inicio-coordinacion">No hay reportes registrados.</p>:<>
      <ResponsiveContainer width="100%" height={240}><PieChart><Pie data={motivos} dataKey="cantidad" nameKey="nombre" cx="50%" cy="50%" innerRadius={58} outerRadius={82}>{motivos.map((motivo,indice)=><Cell key={motivo.id} fill={colores[indice%colores.length]}/>)}</Pie><Tooltip/></PieChart></ResponsiveContainer>
      <ul>{motivos.map((motivo,indice)=><li key={motivo.id}><span><i style={{background:colores[indice%colores.length]}}/>{motivo.nombre}</span><strong>{motivo.cantidad} ({motivo.porcentaje}%)</strong></li>)}</ul>
    </>}
  </section>;
}
export { GraficaMotivosInicio as RiskChart };
