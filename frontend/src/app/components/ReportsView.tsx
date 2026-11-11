import { mockReports } from '../mock/data';
import { ReportGenerator } from './ReportGenerator';
import { ReportStats } from './ReportStats';
import { AcademicReasonsChart } from './AcademicReasonsChart';
import { ReportDetailsTable } from './ReportDetailsTable';
import { useState } from 'react';

export function ReportsView() {

  const [busquedaTutor, setBusquedaTutor] = useState("");

  const [estatusFiltro, setEstatusFiltro] = useState("Todos");
  const [reportes, setReportes] = useState(mockReports);
  
  const reportesFiltrados = reportes.filter((reporte: any) => {

  const coincideBusqueda =

  reporte.estudiante
    .toLowerCase()
    .includes(busquedaTutor.toLowerCase())

  ||

  reporte.tutor
    .toLowerCase()
    .includes(busquedaTutor.toLowerCase());



  const coincideEstatus =

    estatusFiltro === "Todos" ||

    reporte.estatus === estatusFiltro;

  return coincideBusqueda && coincideEstatus;


});
  return (
    <div className="space-y-6">
      <ReportGenerator

        busquedaTutor={busquedaTutor}

        setBusquedaTutor={setBusquedaTutor}

        estatusFiltro={estatusFiltro}

        setEstatusFiltro={setEstatusFiltro}

      />
      <ReportStats reportes={reportesFiltrados} />

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1 max-w-sm">
          <AcademicReasonsChart reportes={reportes} />
        </div>
        <div className="lg:col-span-3">
          <ReportDetailsTable

          reportes={reportesFiltrados}



        />
        </div>
      </div>
    </div>
  );

  
}
