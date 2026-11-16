export interface PeriodoAcademico {
  id: number; nombre: string; fechaInicio: string; fechaFin: string; estado: 'planeado' | 'activo' | 'cerrado';
}
export interface AsignacionTutor {
  id: number; estudianteId: number; estudiante: string; matricula: string; tutorId: number; tutor: string;
  periodoId: number; periodo: string; asignadoPor: number; inicioEn: string; finEn: string | null;
  estado: 'pendiente' | 'activa' | 'finalizada' | 'cancelada'; motivo: string | null;
  sesionesCompletadas: number; ultimaSesion: string | null;
}
export interface EstudianteAsignable {
  id: number; nombre: string; matricula: string; carrera: string; asignacionActivaId: number | null;
}
export interface DatosAsignacion {
  estudianteId: number; tutorId: number; periodoId: number; motivo?: string;
}

export interface FiltrosAsignaciones {
  pagina: number; tamano: number; periodoId: string; estado: string; busqueda: string;
}
export interface PaginaAsignaciones {
  contenido: AsignacionTutor[]; pagina: number; tamano: number; totalRegistros: number; totalPaginas: number;
  resumen: { total: number; activas: number; pendientes: number };
}
