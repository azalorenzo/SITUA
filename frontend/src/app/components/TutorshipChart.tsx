import { LineChart,Line,XAxis,YAxis,CartesianGrid,Tooltip,ResponsiveContainer } from 'recharts';
import type { ResumenInicio } from '../tipos/inicio';
function GraficaTutoriasInicio({mensual,desde,hasta}:{mensual:ResumenInicio['mensual'];desde:string;hasta:string}){
  const fecha=(valor:string)=>new Intl.DateTimeFormat('es-MX',{day:'2-digit',month:'2-digit',year:'numeric',timeZone:'UTC'}).format(new Date(valor+'T00:00:00Z'));
  return <section className="grafica-inicio-coordinacion" aria-labelledby="titulo-tutorias-inicio"><h3 id="titulo-tutorias-inicio">Tutorías completadas en los últimos 6 meses</h3><p className="nota-inicio-coordinacion">Del {fecha(desde)} al {fecha(hasta)}. Fechas de las sesiones en UTC.</p>
    <ResponsiveContainer width="100%" height={300}><LineChart data={mensual} margin={{top:25,right:20,left:-15,bottom:12}}><CartesianGrid strokeDasharray="3 3" stroke="#DCE3EA"/><XAxis dataKey="etiqueta" interval={0} tick={{fontSize:11}}/><YAxis allowDecimals={false} domain={[0,'dataMax + 1']} tick={{fontSize:11}}/><Tooltip/><Line type="linear" dataKey="cantidad" name="Tutorías completadas" stroke="#1F6662" strokeWidth={3} dot={{r:4}}/></LineChart></ResponsiveContainer>
    <details className="datos-serie-inicio"><summary>Ver cantidades por mes</summary><ul>{mensual.map(mes=><li key={mes.mes}><span>{mes.etiqueta}</span><strong>{mes.cantidad}</strong></li>)}</ul></details>
  </section>;
}
export { GraficaTutoriasInicio as TutorshipChart };
