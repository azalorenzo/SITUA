import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend
} from 'recharts';

export function RiskChart({ dashboardStats }: any) {

  if (!dashboardStats) {

    return (

      <div className="bg-white rounded-lg p-6 shadow-sm">

        Cargando gráfica...

      </div>

    );

  }

  const data =

    dashboardStats.motivos.map(
      (item: any, index: number) => {

        const colores = [

          "#B83F4B",
          "#986314",
          "#227F78",
          "#1F6662"

        ];

        return {

          name: item.motivo,

          value: Number(item.total),

          color: colores[index % colores.length]

        };

      }

    );

  return (

  <div className="bg-white rounded-lg p-6 shadow-sm h-full flex flex-col">

    <h3 className="font-semibold mb-4">

      Motivos de riesgo

    </h3>

    <div className="flex-1 flex flex-col justify-center">

      <ResponsiveContainer width="100%" height={300}>

        <PieChart>

          <Pie
            data={data}
            cx="50%"
            cy="42%"
            innerRadius={65}
            outerRadius={95}
            paddingAngle={2}
            dataKey="value"
            nameKey="name"
          >

            {data.map((entry: any) => (

              <Cell
                key={`cell-${entry.name}`}
                fill={entry.color}
              />

            ))}

          </Pie>

          <Legend
            verticalAlign="bottom"
            align="center"
            wrapperStyle={{
              fontSize: "14px",
              paddingTop: "10px"
            }}
            formatter={(value, entry: any) =>
              `${value}: ${entry.payload.value}`
            }
          />

        </PieChart>

      </ResponsiveContainer>

    </div>

  </div>

);

}