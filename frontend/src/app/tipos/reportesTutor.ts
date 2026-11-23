// Panel de reportes del tutor conectado. Los números pueden llegar nulos cuando todavía no hay datos.
export interface ReporteTutor {
  id: number;
  estudiante: string;
  fecha: string;
  motivo: string;
  estado: 'pendiente' | 'completado';
}

export interface PuntoRendimientoTutor {
  mes: string;
  promedio: number;
}

export interface ProgresoEstudianteTutor {
  estudiante: string;
  inicial: number;
  actual: number;
}

export interface PanelReportesTutor {
  tutor: string;
  reportes: ReporteTutor[];
  estudiantesAtendidos: number;
  asistenciaPorcentaje: number | null;
  promedioGeneral: number | null;
  horasTutoria: number | null;
  rendimiento: PuntoRendimientoTutor[];
  progreso: ProgresoEstudianteTutor[];
}
