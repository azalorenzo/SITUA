import { showMockAction } from '../mock/data';
import { Download } from 'lucide-react';
import { useState } from 'react';

export function ReportDetailsTable({

  reportes

}: any) {
  const [paginaActual, setPaginaActual] = useState(1);

  const reportesPorPagina = 5;

  const totalPaginas = Math.max(1, Math.ceil(reportes.length / reportesPorPagina));
  const paginaVisible = Math.min(paginaActual, totalPaginas);
  const ultimoIndice = paginaVisible * reportesPorPagina;

  const primerIndice = ultimoIndice - reportesPorPagina;

  const reportesPaginados = reportes.slice(
    primerIndice,
    ultimoIndice
  );


  return (

    <div className="bg-white rounded-xl p-5 shadow-sm h-full">

      <h3 className="font-semibold text-xl mb-5">
        Detalle de reportes
      </h3>

      <div className="overflow-x-auto">

        <table className="w-full text-sm">

          <thead>

            <tr className="border-b border-[#DCE3EA] text-[#53716E]">

              <th className="text-left py-3 px-3">
                Estudiante
              </th>

              <th className="text-left py-3 px-3">
                Tutor
              </th>

              <th className="text-left py-3 px-3">
                Fecha
              </th>

              <th className="text-left py-3 px-3">
                Estatus
              </th>

              <th className="text-left py-3 px-3">
                Motivo
              </th>

              <th className="text-left py-3 px-3">
                Acciones
              </th>

            </tr>

          </thead>

          <tbody>

            {reportesPaginados.map((reporte: any) => (

              <tr
                key={reporte.id_reporte}
                className="border-b border-[#DCE3EA] hover:bg-[#F5F7FA] transition"
              >

                {/* ESTUDIANTE */}

                <td className="py-4 px-3 text-xs md:text-sm font-medium text-[#0B2828]">

                  {reporte.estudiante}

                </td>

                {/* TUTOR */}

                <td className="py-4 px-3 text-xs md:text-sm text-[#0B2828]">

                  {reporte.tutor}

                </td>

                {/* FECHA */}

                <td className="py-4 px-3 text-xs md:text-sm text-[#53716E] whitespace-nowrap">

                  {reporte.fecha}

                </td>

                {/* ESTATUS */}

                <td className="py-4 px-3">

                  <span
                    className={`text-xs px-3 py-1 rounded-full font-medium ${
                      reporte.estatus === "Completado"
                        ? "bg-[#E4F3ED] text-[#227F78]"
                        : "bg-[#FFF3D6] text-[#986314]"
                    }`}
                  >

                    {reporte.estatus}

                  </span>

                </td>

                {/* MOTIVO */}

                <td className="py-4 px-3 text-xs md:text-sm text-[#53716E]">

                  {reporte.motivo}

                </td>

                {/* ACCIONES */}

                <td className="py-4 px-3">

                  <div className="flex flex-col md:flex-row gap-2">

                    {/* EXPORTAR */}

                        <button

                          onClick={() =>

                            showMockAction()

                          }

                          className="flex items-center justify-center gap-2 bg-[#1F6662] text-white px-3 py-2 rounded-lg hover:bg-[#1C4543] transition text-xs md:text-sm"

                        >

                          <Download className="w-4 h-4" />

                          Exportar

                        </button>

                    
                  </div>

                </td>

              </tr>

            ))}

          </tbody>

        </table>
        <div className="flex justify-between items-center mt-4">

          <button

            onClick={() =>
              setPaginaActual(paginaVisible - 1)
            }

            disabled={paginaVisible === 1}

            className="px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50"

          >

            Anterior

          </button>

          <span className="text-sm text-[#53716E]">

            Página {paginaVisible} de {totalPaginas}

          </span>

          <button

            onClick={() =>
              setPaginaActual(paginaVisible + 1)
            }

            disabled={paginaVisible === totalPaginas}

            className="px-4 py-2 rounded-lg border border-[#DCE3EA] disabled:opacity-50"

          >

            Siguiente

          </button>

        </div>

      </div>

    </div>

  );

}