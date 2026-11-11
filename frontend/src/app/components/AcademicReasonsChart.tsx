import { PieChart, Pie, Cell, ResponsiveContainer, Legend } from 'recharts';

export function AcademicReasonsChart({ reportes }: any) {
  
  const bajoRendimiento = reportes.filter(
  (r: any) => r.motivo === "Bajo rendimiento"
).length;

const ansiedad = reportes.filter(
  (r: any) => r.motivo === "Ansiedad/Estrés"
).length;

const familiares = reportes.filter(
  (r: any) => r.motivo === "Problemas familiares"
).length;

const otros = reportes.filter(
  (r: any) => r.motivo === "Otros"
).length;

  const data = [

  {
    label: "Bajo rendimiento",
    value: bajoRendimiento,
    color: "#1F6662"
  },

  {
    label: "Ansiedad/Estrés",
    value: ansiedad,
    color: "#986314"
  },

  {
    label: "Problemas familiares",
    value: familiares,
    color: "#B83F4B"
  },

  {
    label: "Otros",
    value: otros,
    color: "#227F78"
  }

];



const total = data.reduce((sum, item) => sum + item.value, 0);
  

  return (
    <div className="bg-white rounded-lg p-6 shadow-sm h-full">
      <h3 className="font-semibold mb-4">Motivos de reporte académico</h3>
      <ResponsiveContainer width="100%" height={320}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={70}
            outerRadius={85}
            paddingAngle={3}
            dataKey="value"
            nameKey="label"
          >
            {data.map((entry) => (
              <Cell key={entry.label} fill={entry.color} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
      <div className="mt-4 space-y-2">

  {data.map((item) => {

    const porcentaje = total > 0
      ? ((item.value / total) * 100).toFixed(0)
      : 0;

    return (

      <div
        key={item.label}
        className="flex items-center justify-between text-sm"
      >

        <div className="flex items-center gap-2">

          <div
            className="w-3 h-3 rounded-full"
            style={{ backgroundColor: item.color }}
          />

          <span>
            {item.label}
          </span>

        </div>

        <span className="font-medium">

          {item.value} ({porcentaje}%)

        </span>

      </div>

    );

  })}

</div>
    </div>
  );
}
