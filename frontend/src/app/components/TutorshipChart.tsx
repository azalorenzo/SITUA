import {

  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer

} from 'recharts';

export function TutorshipChart({

  dashboardStats

}: any) {

  const nombresMeses = [

    "Ene",
    "Feb",
    "Mar",
    "Abr",
    "May",
    "Jun",
    "Jul",
    "Ago",
    "Sep",
    "Oct",
    "Nov",
    "Dic"

  ];

  const fechaActual = new Date();

  const mesesBase = [];

  for (let i = 5; i >= 0; i--) {

    const fecha = new Date();

    fecha.setMonth(
      fechaActual.getMonth() - i
    );

    mesesBase.push({

      nombre: nombresMeses[fecha.getMonth()],

      numero: fecha.getMonth() + 1

    });

  }

  const dataReal = dashboardStats?.mensual || [];

  const data = mesesBase.map((mesObj: any) => {

    const encontrado = dataReal.find(

      (item: any) =>

        Number(item.mes) === mesObj.numero

    );

    return {

      mes: mesObj.nombre,

      total: encontrado
        ? Number(encontrado.total)
        : 0

    };

  });

  return (

    <div className="bg-white rounded-lg p-6 shadow-sm h-full">

      <h3 className="font-semibold mb-4">

        Reporte de tutorías últimas 6 meses

      </h3>

      <ResponsiveContainer width="100%" height={300}>

        <LineChart data={data}>

          <CartesianGrid
            strokeDasharray="3 3"
            stroke="#DCE3EA"
          />

          <XAxis dataKey="mes" />

          <YAxis
            allowDecimals={false}
            domain={[0, 'dataMax + 1']}
          />

          <Tooltip />

          <Line
            type="monotone"
            dataKey="total"
            stroke="#1F6662"
            strokeWidth={3}
            dot={{ r: 5 }}
          />

        </LineChart>

      </ResponsiveContainer>

    </div>

  );

}